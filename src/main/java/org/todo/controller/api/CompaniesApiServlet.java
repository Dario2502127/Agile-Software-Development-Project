package org.todo.controller.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.company.Company;
import org.todo.model.company.CompanyDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST-ish API for companies.
 *
 *  GET    /api/companies             -> list companies
 *  GET    /api/companies/{id}        -> company detail
 *  POST   /api/companies             -> create company (JSON; staff only)
 *  POST   /api/companies/{id}        -> update company (JSON; staff only; password optional)
 *  POST   /api/companies/{id}?action=unlock -> unlock account (staff only)
 *  DELETE /api/companies/{id}        -> delete company (staff only)
 */
@WebServlet("/api/companies/*")
public class CompaniesApiServlet extends HttpServlet {

	private final CompanyDao dao = new CompanyDao();

	/* ----------------------- GET ----------------------- */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = Optional.ofNullable(req.getPathInfo()).orElse("/");

		try {
			if ("/".equals(path)) {
				var list = dao.listAll();
				resp.getWriter().write(Json.list(list));
				return;
			}
			if (path.matches("^/\\d+/?$")) {
				long id = Long.parseLong(path.substring(1));
				Company c = dao.find(id);
				if (c == null) { resp.setStatus(404); resp.getWriter().write("{\"error\":\"not found\"}"); }
				else resp.getWriter().write(Json.one(c));
				return;
			}
			resp.setStatus(404);
			resp.getWriter().write("{\"error\":\"unknown endpoint\"}");
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write(err(e));
		}
	}

	/* ----------------------- POST (create/update/unlock) ----------------------- */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = Optional.ofNullable(req.getPathInfo()).orElse("/");

		// staff only for any mutation
		if (!isStaff(req)) { resp.setStatus(403); resp.getWriter().write("{\"error\":\"staff login required\"}"); return; }

		try {
			// unlock action: POST /api/companies/{id}?action=unlock
			if (path.matches("^/\\d+/?$") && "unlock".equalsIgnoreCase(s(req.getParameter("action")))) {
				long id = Long.parseLong(path.substring(1));
				dao.unlock(id);
				Company c = dao.find(id);
				resp.getWriter().write(Json.one(c));
				return;
			}

			Map<String, String> b = readJson(req); // handles arrays without breaking
			String uid = s(b.get("company_id"));
			String name = s(b.get("company_name"));
			String password = b.get("password"); // may be null on update
			List<String> cats = parseCategories(b.get("categories"));

			if (uid.isBlank() || name.isBlank()) {
				resp.setStatus(400);
				resp.getWriter().write("{\"error\":\"company_id and company_name required\"}");
				return;
			}

			if ("/".equals(path)) {
				long id = dao.create(uid, name, password == null ? "" : password, cats);
				Company c = dao.find(id);
				resp.setStatus(201);
				resp.getWriter().write(Json.one(c));
				return;
			}

			if (path.matches("^/\\d+/?$")) {
				long id = Long.parseLong(path.substring(1));
				dao.update(id, uid, name, password, cats);
				Company c = dao.find(id);
				resp.getWriter().write(Json.one(c));
				return;
			}

			resp.setStatus(404);
			resp.getWriter().write("{\"error\":\"unknown endpoint\"}");
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write(err(e));
		}
	}

	/* ----------------------- DELETE ----------------------- */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!isStaff(req)) { resp.setStatus(403); resp.getWriter().write("{\"error\":\"staff login required\"}"); return; }
		String path = Optional.ofNullable(req.getPathInfo()).orElse("/");
		try {
			if (path.matches("^/\\d+/?$")) {
				long id = Long.parseLong(path.substring(1));
				dao.delete(id);
				resp.getWriter().write("{\"ok\":true}");
			} else {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\":\"unknown endpoint\"}");
			}
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write(err(e));
		}
	}

	/* ----------------------- helpers ----------------------- */

	private static boolean isStaff(HttpServletRequest req) {
		Object staffObj = req.getSession().getAttribute("staffId");
		return staffObj != null && !staffObj.toString().isBlank();
	}

	private static String s(String v){ return v==null ? "" : v.trim(); }

	/**
	 * Accepts:
	 * - JSON array string: ["A","B"]
	 * - comma separated:   A,B
	 * - single value:      A
	 * - null/empty -> []
	 */
	private static List<String> parseCategories(String raw) {
		if (raw == null || raw.isBlank()) return List.of();
		String t = raw.trim();
		if (t.startsWith("[") && t.endsWith("]")) {
			String inner = t.substring(1, t.length()-1).trim();
			if (inner.isEmpty()) return List.of();
			List<String> out = new ArrayList<>();
			boolean inStr = false;
			int start = 0;
			for (int i = 0; i < inner.length(); i++) {
				char c = inner.charAt(i);
				if (c == '"') inStr = !inStr;
				else if (c == ',' && !inStr) {
					out.add(inner.substring(start, i).trim());
					start = i + 1;
				}
			}
			out.add(inner.substring(start).trim());
			List<String> cleaned = new ArrayList<>();
			for (String v : out) {
				String s = v;
				if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length()-1);
				s = s.replace("\\\"", "\"").replace("\\\\","\\").trim();
				if (!s.isEmpty()) cleaned.add(s);
			}
			return cleaned;
		}
		// comma-separated fallback
		String[] parts = t.split(",");
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			String v = p.trim();
			if (!v.isEmpty()) out.add(v);
		}
		return out;
	}

	/** JSON reader that keeps arrays intact by tracking bracket depth in addition to quote state. */
	private static Map<String,String> readJson(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = req.getReader()) {
			String line; while ((line = br.readLine()) != null) sb.append(line);
		}
		String raw = sb.toString().trim();
		Map<String,String> out = new HashMap<>();
		if (raw.isEmpty() || raw.equals("{}")) return out;

		if (raw.charAt(0) == '{') raw = raw.substring(1);
		if (raw.charAt(raw.length() - 1) == '}') raw = raw.substring(0, raw.length() - 1);

		List<String> pairs = new ArrayList<>();
		int i = 0, start = 0;
		boolean inStr = false;
		int bracketDepth = 0; // tracks [] depth
		while (i < raw.length()) {
			char c = raw.charAt(i);
			if (c == '"') {
				boolean escaped = i > 0 && raw.charAt(i - 1) == '\\';
				if (!escaped) inStr = !inStr;
			} else if (!inStr) {
				if (c == '[') bracketDepth++;
				else if (c == ']') bracketDepth = Math.max(0, bracketDepth - 1);
				else if (c == ',' && bracketDepth == 0) {
					pairs.add(raw.substring(start, i));
					start = i + 1;
				}
			}
			i++;
		}
		pairs.add(raw.substring(start));

		for (String pair : pairs) {
			int colon = indexOfColonOutsideQuotesAndBrackets(pair);
			if (colon <= 0) continue;
			String k = unq(pair.substring(0, colon).trim());
			String v = pair.substring(colon + 1).trim();
			if (v.startsWith("\"") && v.endsWith("\"")) v = unq(v);
			out.put(k, v);
		}
		return out;
	}

	private static int indexOfColonOutsideQuotesAndBrackets(String s) {
		boolean in = false; int depth = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"') {
				boolean escaped = i > 0 && s.charAt(i - 1) == '\\';
				if (!escaped) in = !in;
			} else if (!in) {
				if (c == '[') depth++;
				else if (c == ']') depth = Math.max(0, depth - 1);
				else if (c == ':' && depth == 0) return i;
			}
		}
		return -1;
	}

	private static String unq(String s) {
		if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
		return s.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private static String err(Exception e) {
		String msg = e.getMessage() == null ? e.toString() : e.getMessage();
		return "{\"error\":\"" + msg.replace("\"","\\\"") + "\"}";
	}

	/* ----------- JSON for Companies ----------- */
	private static final class Json {
		static String one(Company c) {
			String cats = c.categories.stream()
					.map(s -> "\"" + s.replace("\\","\\\\").replace("\"","\\\"") + "\"")
					.collect(Collectors.joining(","));
			return "{"
					+ "\"id\":" + c.id
					+ ",\"company_id\":\"" + c.companyUid.replace("\\","\\\\").replace("\"","\\\"") + "\""
					+ ",\"company_name\":\"" + c.name.replace("\\","\\\\").replace("\"","\\\"") + "\""
					+ ",\"categories\":[" + cats + "]"
					+ ",\"failed_attempts\":" + c.failedAttempts
					+ ",\"locked\":" + (c.locked ? "true" : "false")
					+ "}";
		}
		static String list(List<Company> list) {
			return "[" + list.stream().map(Json::one).collect(Collectors.joining(",")) + "]";
		}
	}
}
