package org.todo.controller.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.tender.Tender;
import org.todo.model.tender.TenderDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/tenders/*")
public class TendersApiServlet extends HttpServlet {

	private final TenderDao dao = new TenderDao();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = req.getPathInfo(); // null, "/", or "/{id}"

		try {
			if (path == null || "/".equals(path)) {
				List<Tender> list = dao.listAll();
				resp.getWriter().write(Json.list(list));
			} else {
				long id = parseId(path);
				Tender t = dao.find(id);
				if (t == null) {
					resp.setStatus(404);
					resp.getWriter().write("{\"error\":\"not found\"}");
				} else {
					resp.getWriter().write(Json.obj(t));
				}
			}
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write(jsonError(e));
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = req.getPathInfo(); // could be "/" for create, or "/{id}" for actions

		try {
			// --- CREATE (JSON body) ---
			if (path == null || "/".equals(path)) {
				Map<String, String> body = readJsonObject(req);
				Tender t = new Tender();
				t.name = s(body.get("name"));
				t.noticeDate = LocalDate.parse(s(body.get("notice_date")));
				t.closeDate = LocalDate.parse(s(body.get("close_date")));

				String dd = body.get("disclose_date");
				t.discloseDate = (dd == null || dd.isBlank()) ? null : LocalDate.parse(dd);

				String status = body.get("status");
				t.status = (status == null || status.isBlank()) ? "Open" : status;

				// Use staffEmail from session if present (key fixed to "staffEmail")
				Object ses = req.getSession().getAttribute("staffEmail");
				t.staffEmail = (ses instanceof String && !((String) ses).isBlank())
						? (String) ses
						: s(body.getOrDefault("staff_email", "city.staff@example.com"));

				t.description = s(body.get("description"));
				t.termOfConstruction = s(body.get("term"));
				String price = body.get("estimated_price");
				t.estimatedPrice = (price == null || price.isBlank()) ? null : new BigDecimal(price);

				t.winnerReason = null;
				t.winnerBidId = null;

				long id = dao.create(t);
				t.id = id;

				resp.setStatus(201);
				resp.getWriter().write(Json.obj(t));
				return;
			}

			// --- STAFF-ONLY ACTIONS on /api/tenders/{id} ---
			Long staff = (Long) req.getSession().getAttribute("staffId"); // set by CityLoginServlet
			if (staff == null) {
				resp.setStatus(403);
				resp.getWriter().write("{\"error\":\"staff login required\"}");
				return;
			}

			long tenderId = parseId(path);
			String action = s(req.getParameter("action")).toLowerCase();

			switch (action) {
				case "delete" -> {
					dao.delete(tenderId);
					resp.getWriter().write("{\"ok\":true}");
				}
				case "close" -> {
					dao.close(tenderId);
					resp.getWriter().write("{\"ok\":true}");
				}
				case "award" -> {
					long bidId = Long.parseLong(s(req.getParameter("bid_id")));
					String reason = s(req.getParameter("reason"));
					dao.award(tenderId, bidId, reason);
					resp.getWriter().write("{\"ok\":true}");
				}
				default -> {
					resp.setStatus(400);
					resp.getWriter().write("{\"error\":\"unknown action\"}");
				}
			}
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write(jsonError(e));
		}
	}

	private static long parseId(String path) {
		String p = path.startsWith("/") ? path.substring(1) : path;
		return Long.parseLong(p);
	}

	private static String s(String v) {
		return v == null ? "" : v.trim();
	}

	/* ---- tiny JSON reader for a flat object ---- */
	private static Map<String, String> readJsonObject(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = req.getReader()) {
			String line;
			while ((line = br.readLine()) != null) sb.append(line);
		}
		String raw = sb.toString().trim();
		Map<String, String> out = new HashMap<>();
		if (raw.isEmpty() || raw.equals("{}")) return out;

		if (raw.charAt(0) == '{') raw = raw.substring(1);
		if (raw.charAt(raw.length() - 1) == '}') raw = raw.substring(0, raw.length() - 1);

		List<String> pairs = new ArrayList<>();
		int i = 0, start = 0; boolean inStr = false;
		while (i < raw.length()) {
			char c = raw.charAt(i);
			if (c == '\"') inStr = !inStr;
			else if (c == ',' && !inStr) {
				pairs.add(raw.substring(start, i));
				start = i + 1;
			}
			i++;
		}
		pairs.add(raw.substring(start));

		for (String pair : pairs) {
			int colon = indexOfColonOutsideQuotes(pair);
			if (colon <= 0) continue;
			String k = unq(pair.substring(0, colon).trim());
			String v = pair.substring(colon + 1).trim();
			if (v.startsWith("\"") && v.endsWith("\"")) v = unq(v);
			out.put(k, v);
		}
		return out;
	}

	private static int indexOfColonOutsideQuotes(String s) {
		boolean in = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\"') in = !in;
			else if (c == ':' && !in) return i;
		}
		return -1;
	}

	private static String unq(String s) {
		if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
		return s.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private static String jsonError(Exception e) {
		String msg = e.getMessage() == null ? e.toString() : e.getMessage();
		return "{\"error\":\"" + esc(msg) + "\"}";
	}

	/** Tiny JSON helpers — only for this servlet. */
	private static final class Json {
		static String list(List<Tender> list) {
			StringBuilder b = new StringBuilder("[");
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) b.append(',');
				b.append(obj(list.get(i)));
			}
			return b.append(']').toString();
		}

		static String obj(Tender t) {
			StringBuilder b = new StringBuilder("{");
			b.append("\"id\":").append(t.id).append(',')
					.append("\"name\":\"").append(esc(t.name)).append("\",")
					.append("\"notice_date\":\"").append(t.noticeDate).append("\",")
					.append("\"close_date\":\"").append(t.closeDate).append("\",")
					.append("\"disclose_date\":").append(t.discloseDate == null ? "null" : ("\"" + t.discloseDate + "\"")).append(',')
					.append("\"status\":\"").append(esc(t.status)).append("\",")
					.append("\"staff_email\":\"").append(esc(t.staffEmail)).append("\",")
					.append("\"description\":\"").append(esc(nz(t.description))).append("\",")
					.append("\"term\":\"").append(esc(nz(t.termOfConstruction))).append("\",")
					.append("\"estimated_price\":").append(t.estimatedPrice == null ? "null" : t.estimatedPrice.toPlainString());
			return b.append("}").toString();
		}
	}

	private static String nz(String v) { return v == null ? "" : v; }

	private static String esc(String v) {
		return v == null ? "" : v
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n");
	}
}
