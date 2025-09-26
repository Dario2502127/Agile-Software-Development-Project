package org.todo.model.tender;

import org.todo.model.db.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TenderDao {

	private Tender map(ResultSet rs) throws SQLException {
		Tender t = new Tender();
		t.id = rs.getLong("id");
		t.name = rs.getString("name");
		t.noticeDate = rs.getDate("notice_date").toLocalDate();
		t.closeDate = rs.getDate("close_date").toLocalDate();
		Date dd = rs.getDate("disclose_date");
		t.discloseDate = (dd == null ? null : dd.toLocalDate());
		t.status = rs.getString("status");
		t.staffEmail = rs.getString("staff_email");
		t.description = rs.getString("description");
		t.termOfConstruction = rs.getString("term_of_construction");
		t.estimatedPrice = rs.getBigDecimal("estimated_price");
		t.winnerReason = rs.getString("winner_reason");
		long wb = rs.getLong("winner_bid_id");
		t.winnerBidId = rs.wasNull() ? null : wb;
		t.category = rs.getString("category");
		return t;
	}

	public List<Tender> listAll() throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select * from tenders order by close_date asc")) {
			ResultSet rs = ps.executeQuery();
			List<Tender> out = new ArrayList<>();
			while (rs.next()) out.add(map(rs));
			return out;
		}
	}

	public Tender find(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select * from tenders where id=?")) {
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return map(rs);
			return null;
		}
	}

	public long create(Tender t) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("""
                 insert into tenders(name, notice_date, close_date, disclose_date, status, staff_email,
                                     description, term_of_construction, estimated_price, winner_reason, winner_bid_id, category)
                 values(?,?,?,?,?,?,?,?,?,?,?,?)
                 """, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, t.name);
			ps.setDate(2, Date.valueOf(t.noticeDate));
			ps.setDate(3, Date.valueOf(t.closeDate));
			if (t.discloseDate == null) ps.setNull(4, Types.DATE); else ps.setDate(4, Date.valueOf(t.discloseDate));
			ps.setString(5, t.status == null ? "Open" : t.status);
			ps.setString(6, t.staffEmail);
			ps.setString(7, t.description);
			ps.setString(8, t.termOfConstruction);
			if (t.estimatedPrice == null) ps.setNull(9, Types.DECIMAL); else ps.setBigDecimal(9, t.estimatedPrice);
			ps.setString(10, t.winnerReason);
			if (t.winnerBidId == null) ps.setNull(11, Types.BIGINT); else ps.setLong(11, t.winnerBidId);
			ps.setString(12, t.category);
			ps.executeUpdate();
			ResultSet keys = ps.getGeneratedKeys();
			keys.next();
			return keys.getLong(1);
		}
	}

	public void update(Tender t) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("""
                 update tenders set name=?, notice_date=?, close_date=?, disclose_date=?, status=?, staff_email=?,
                       description=?, term_of_construction=?, estimated_price=?, winner_reason=?, winner_bid_id=?, category=?
                 where id=?
                 """)) {
			ps.setString(1, t.name);
			ps.setDate(2, Date.valueOf(t.noticeDate));
			ps.setDate(3, Date.valueOf(t.closeDate));
			if (t.discloseDate == null) ps.setNull(4, Types.DATE); else ps.setDate(4, Date.valueOf(t.discloseDate));
			ps.setString(5, t.status);
			ps.setString(6, t.staffEmail);
			ps.setString(7, t.description);
			ps.setString(8, t.termOfConstruction);
			if (t.estimatedPrice == null) ps.setNull(9, Types.DECIMAL); else ps.setBigDecimal(9, t.estimatedPrice);
			ps.setString(10, t.winnerReason);
			if (t.winnerBidId == null) ps.setNull(11, Types.BIGINT); else ps.setLong(11, t.winnerBidId);
			ps.setString(12, t.category);
			ps.setLong(13, t.id);
			ps.executeUpdate();
		}
	}

	public void delete(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("delete from tenders where id=?")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}

	public void close(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("update tenders set status='Closed' where id=?")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}

	public void award(long tenderId, long bidId, String reason) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement(
					 "update tenders set status='Awarded', disclose_date=current_date, winner_bid_id=?, winner_reason=? where id=?")) {
			ps.setLong(1, bidId);
			ps.setString(2, reason);
			ps.setLong(3, tenderId);
			ps.executeUpdate();
		}
	}
}
