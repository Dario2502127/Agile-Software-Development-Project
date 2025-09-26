package org.todo.model.tender;

import org.todo.model.db.DB;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;

public class BidFileDao {

	public long create(long bidId, String filename, String contentType, InputStream data, long size) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("""
                 merge into bid_files(bid_id, filename, content_type, data)
                 key(bid_id) values(?,?,?,?)
             """, Statement.RETURN_GENERATED_KEYS)) {
			ps.setLong(1, bidId);
			ps.setString(2, filename);
			ps.setString(3, contentType);
			ps.setBinaryStream(4, data, size <= Integer.MAX_VALUE ? (int) size : size);
			ps.executeUpdate();

			// fetch id
			try (PreparedStatement sel = c.prepareStatement("select id from bid_files where bid_id=?")) {
				sel.setLong(1, bidId);
				ResultSet rs = sel.executeQuery();
				rs.next();
				return rs.getLong(1);
			}
		}
	}

	public FileMeta meta(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select filename, content_type from bid_files where id=?")) {
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) return null;
			FileMeta m = new FileMeta();
			m.filename = rs.getString(1);
			m.contentType = rs.getString(2);
			return m;
		}
	}

	public boolean stream(long id, OutputStream out) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select data from bid_files where id=?")) {
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) return false;
			try (var in = rs.getBinaryStream(1)) {
				in.transferTo(out);
				return true;
			} catch (Exception e) {
				throw new SQLException("stream failed", e);
			}
		}
	}

	public static final class FileMeta {
		public String filename;
		public String contentType;
	}
}
