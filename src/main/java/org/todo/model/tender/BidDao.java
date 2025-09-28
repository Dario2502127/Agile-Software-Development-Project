package org.todo.model.tender;

import org.todo.model.db.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDao {

	private Bid map(ResultSet rs) throws SQLException {
		Bid b = new Bid();
		b.id = rs.getLong("id");
		b.tenderId = rs.getLong("tender_id");
		b.companyId = rs.getString("company_id");
		b.companyName = rs.getString("company_name");
		b.bidPrice = rs.getBigDecimal("bid_price");
		b.createdAt = rs.getTimestamp("created_at").toLocalDateTime();

		long aid = rs.getLong("attachment_id");
		b.attachmentId = rs.wasNull() ? null : aid;
		return b;
	}

	/** Lists bids for a tender, cheapest first, and includes attachment_id if present. */
	public List<Bid> listFor(long tenderId) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("""
                 select b.*,
                        (select id from bid_files bf where bf.bid_id=b.id) as attachment_id
                 from bids b
                 where b.tender_id=?
                 order by b.bid_price asc
             """)) {
			ps.setLong(1, tenderId);
			ResultSet rs = ps.executeQuery();
			List<Bid> out = new ArrayList<>();
			while (rs.next()) out.add(map(rs));
			return out;
		}
	}

	public long create(long tenderId, String companyId, String companyName, BigDecimal price) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement(
					 "insert into bids(tender_id, company_id, company_name, bid_price) values(?,?,?,?)",
					 Statement.RETURN_GENERATED_KEYS)) {
			ps.setLong(1, tenderId);
			ps.setString(2, companyId);
			ps.setString(3, companyName);
			ps.setBigDecimal(4, price);
			ps.executeUpdate();
			ResultSet keys = ps.getGeneratedKeys();
			keys.next();
			return keys.getLong(1);
		}
	}

	public void delete(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("delete from bids where id=?")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}

    /* =========================
       File attachment helpers
       ========================= */

	/**
	 * Save (or replace) the attachment for a bid.
	 * Table schema guarantees a single file per bid (unique on bid_id).
	 */
	public void saveFile(long bidId, String filename, String contentType, byte[] data) throws SQLException {
		try (Connection c = DB.get()) {
			c.setAutoCommit(false);
			try {
				// Ensure only one file per bid (unique in schema). Replace if exists.
				try (PreparedStatement del = c.prepareStatement("delete from bid_files where bid_id=?")) {
					del.setLong(1, bidId);
					del.executeUpdate();
				}
				try (PreparedStatement ins = c.prepareStatement("""
                        insert into bid_files(bid_id, filename, content_type, data)
                        values(?,?,?,?)
                    """)) {
					ins.setLong(1, bidId);
					ins.setString(2, filename);
					ins.setString(3, contentType);
					ins.setBytes(4, data);
					ins.executeUpdate();
				}
				c.commit();
			} catch (SQLException e) {
				c.rollback();
				throw e;
			} finally {
				c.setAutoCommit(true);
			}
		}
	}

	/** Small DTO for downloads (optional). */
	public static final class BidFile {
		public Long id;
		public Long bidId;
		public String filename;
		public String contentType;
		public byte[] data;
	}

	/** Optional: read a file by its id (useful for a /api/bid-file endpoint). */
	public BidFile findFile(long fileId) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("""
                 select id, bid_id, filename, content_type, data
                 from bid_files where id=?
             """)) {
			ps.setLong(1, fileId);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) return null;
			BidFile f = new BidFile();
			f.id = rs.getLong("id");
			f.bidId = rs.getLong("bid_id");
			f.filename = rs.getString("filename");
			f.contentType = rs.getString("content_type");
			f.data = rs.getBytes("data");
			return f;
		}
	}
}
