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
}
