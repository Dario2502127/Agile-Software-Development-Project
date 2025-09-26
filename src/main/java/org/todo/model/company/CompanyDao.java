package org.todo.model.company;

import org.todo.model.db.DB;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class CompanyDao {

	private Company mapCompany(ResultSet rs) throws SQLException {
		Company c = new Company();
		c.id = rs.getLong("id");
		c.companyUid = rs.getString("company_uid");
		c.name = rs.getString("name");
		c.passwordHash = rs.getString("password_hash");
		return c;
	}

	private static String sha256(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder b = new StringBuilder(d.length * 2);
			for (byte x : d) b.append(String.format("%02x", x));
			return b.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* ---------------- helpers for category normalization ---------------- */

	/** Split a category name that may itself contain a packed list. */
	private static List<String> splitMaybePacked(String raw) {
		if (raw == null) return List.of();
		String s = raw.trim();
		if (s.isEmpty()) return List.of();

		// Looks like ["A","B"]
		if ((s.startsWith("[") && s.endsWith("]"))) {
			try {
				// very small permissive parser: strip [ ], split on commas not inside quotes
				s = s.substring(1, s.length() - 1).trim();
				if (s.isEmpty()) return List.of();
				List<String> out = new ArrayList<>();
				boolean in = false; int start = 0;
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (c == '"') in = !in;
					else if (c == ',' && !in) {
						out.add(s.substring(start, i).trim());
						start = i + 1;
					}
				}
				out.add(s.substring(start).trim());
				return out.stream()
						.map(x -> {
							String v = x;
							if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
							return v.replace("\\\"", "\"").replace("\\\\", "\\").trim();
						})
						.filter(v -> !v.isEmpty())
						.collect(Collectors.toList());
			} catch (Exception ignored) { /* fall through */ }
		}

		// Simple comma-separated fallback
		if (s.contains(",")) {
			return Arrays.stream(s.split(","))
					.map(String::trim).filter(v -> !v.isEmpty()).collect(Collectors.toList());
		}

		// Single clean name
		return List.of(s);
	}

	private List<String> categoriesFor(Connection c, long companyId) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("""
             select cat.name
             from company_categories cc
             join categories cat on cat.id = cc.category_id
             where cc.company_id=?
             order by cat.name
        """)) {
			ps.setLong(1, companyId);
			ResultSet rs = ps.executeQuery();

			Set<String> uniq = new LinkedHashSet<>();
			while (rs.next()) {
				String name = rs.getString(1);
				for (String n : splitMaybePacked(name)) {
					if (!n.isBlank()) uniq.add(n.trim());
				}
			}
			return new ArrayList<>(uniq);
		}
	}

	public List<Company> listAll() throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select * from companies order by name asc")) {
			ResultSet rs = ps.executeQuery();
			List<Company> list = new ArrayList<>();
			while (rs.next()) {
				Company co = mapCompany(rs);
				co.categories = categoriesFor(c, co.id);
				list.add(co);
			}
			return list;
		}
	}

	public Company find(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select * from companies where id=?")) {
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) return null;
			Company co = mapCompany(rs);
			co.categories = categoriesFor(c, co.id);
			return co;
		}
	}

	public Company findByUid(String uid) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("select * from companies where company_uid=?")) {
			ps.setString(1, uid);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) return null;
			Company co = mapCompany(rs);
			co.categories = categoriesFor(c, co.id);
			return co;
		}
	}

	private long upsertCategory(Connection c, String name) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement(
				"merge into categories(name) key(name) values(?)", Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, name.trim());
			ps.executeUpdate();
		}
		try (PreparedStatement ps2 = c.prepareStatement("select id from categories where name=?")) {
			ps2.setString(1, name.trim());
			ResultSet rs = ps2.executeQuery();
			rs.next();
			return rs.getLong(1);
		}
	}

	private void replaceCategories(Connection c, long companyId, List<String> names) throws SQLException {
		try (PreparedStatement del = c.prepareStatement("delete from company_categories where company_id=?")) {
			del.setLong(1, companyId);
			del.executeUpdate();
		}
		if (names == null) return;
		try (PreparedStatement ins = c.prepareStatement(
				"insert into company_categories(company_id, category_id) values(?,?)")) {
			for (String n : names) {
				if (n == null || n.isBlank()) continue;
				for (String one : splitMaybePacked(n)) {  // accept any input style
					long cid = upsertCategory(c, one);
					ins.setLong(1, companyId);
					ins.setLong(2, cid);
					ins.addBatch();
				}
			}
			ins.executeBatch();
		}
	}

	public long create(String uid, String name, String rawPassword, List<String> categories) throws SQLException {
		try (Connection c = DB.get()) {
			c.setAutoCommit(false);
			try (PreparedStatement ps = c.prepareStatement(
					"insert into companies(company_uid, name, password_hash) values(?,?,?)",
					Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, uid);
				ps.setString(2, name);
				ps.setString(3, sha256(rawPassword == null ? "" : rawPassword));
				ps.executeUpdate();
				ResultSet keys = ps.getGeneratedKeys();
				keys.next();
				long id = keys.getLong(1);

				replaceCategories(c, id, categories);
				c.commit();
				return id;
			} catch (SQLException e) {
				c.rollback();
				throw e;
			} finally {
				c.setAutoCommit(true);
			}
		}
	}

	public void update(long id, String uid, String name, String newRawPasswordOrNull, List<String> categories) throws SQLException {
		try (Connection c = DB.get()) {
			c.setAutoCommit(false);
			try {
				if (newRawPasswordOrNull == null || newRawPasswordOrNull.isBlank()) {
					try (PreparedStatement ps = c.prepareStatement(
							"update companies set company_uid=?, name=? where id=?")) {
						ps.setString(1, uid);
						ps.setString(2, name);
						ps.setLong(3, id);
						ps.executeUpdate();
					}
				} else {
					try (PreparedStatement ps = c.prepareStatement(
							"update companies set company_uid=?, name=?, password_hash=? where id=?")) {
						ps.setString(1, uid);
						ps.setString(2, name);
						ps.setString(3, sha256(newRawPasswordOrNull));
						ps.setLong(4, id);
						ps.executeUpdate();
					}
				}
				replaceCategories(c, id, categories);
				c.commit();
			} catch (SQLException e) {
				c.rollback();
				throw e;
			} finally {
				c.setAutoCommit(true);
			}
		}
	}

	public void delete(long id) throws SQLException {
		try (Connection c = DB.get();
			 PreparedStatement ps = c.prepareStatement("delete from companies where id=?")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		}
	}
}
