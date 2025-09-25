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
		return b;
	}

	public List<Bid> listFor(long tenderId) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select * from bids where tender_id=? order by bid_price asc")) {
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
