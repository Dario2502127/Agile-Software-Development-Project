package org.todo.controller.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.tender.BidDao;
import org.todo.model.tender.Tender;
import org.todo.model.tender.TenderDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * REST-ish API for tenders & bids.
 *
 *  GET  /api/tenders                -> list tenders
 *  GET  /api/tenders/{id}           -> tender detail (JSON)
 *  GET  /api/tenders/{id}/bids      -> bids for tender
 *
 *  POST /api/tenders                -> create tender (JSON body)
 *  POST /api/tenders/{id}/bids      -> create bid   (form body)
 *  POST /api/tenders/{id}?action=...  (staff only):
 *        action=delete | close | award&bid_id=...&reason=...
 */
@WebServlet("/api/tenders/*")
public class TendersApiServlet extends HttpServlet {

	private final TenderDao tenderDao = new TenderDao();
	private final BidDao bidDao = new BidDao();

	/* ----------------------- GET ----------------------- */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = Optional.ofNullable(req.getPathInfo()).orElse("/");

		try {
			// /api/tenders
			if ("/".equals(path)) {
				var list = tenderDao.listAll();
				resp.getWriter().write(Json.tenders(list));
				return;
			}

			// /api/tenders/{id}/bids
			if (path.matches("^/\\d+/bids/?$")) {
				long id = Long.parseLong(path.split("/")[1]);
				var bids = bidDao.listFor(id);
				resp.getWriter().write(Json.bids(bids));
				return;
			}

			// /api/tenders/{id}
			if (path.matches("^/\\d+/?$")) {
				long id = Long.parseLong(path.substring(1));
				Tender t = tenderDao.find(id);
				if (t == null) {
					resp.setStatus(404);
					resp.getWriter().write("{\"error\":\"not found\"}");
				} else {
					resp.getWriter().write(Json.tender(t));
				}
				return;
			}

			resp.setStatus(404);
			resp.getWriter().write("{\"error\":\"unknown endpoint\"}");
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write(jsonError(e));
		}
	}

	/* ----------------------- POST ----------------------- */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = Optional.ofNullable(req.getPathInfo()).orElse("/");

		try {
			// CREATE tender  -> POST /api/tenders  (JSON body)
			if ("/".equals(path)) {
				Map<String, String> body = readJsonObject(req);

				Tender t = new Tender();
				t.name = s(body.get("name"));
				t.noticeDate = LocalDate.parse(s(body.get("notice_date")));
				t.closeDate = LocalDate.parse(s(body.get("close_date")));

				String dd = body.get("disclose_date");
				t.discloseDate = (dd == null || dd.isBlank()) ? null : LocalDate.parse(dd);

				String status = body.get("status");
				t.status = (status == null || status.isBlank()) ? "Open" : status;

				// Prefer staff email from session; fall back to provided field
				Object ses = req.getSession().getAttribute("staffEmail");
				t.staffEmail = (ses instanceof String && !((String) ses).isBlank())
						? (String) ses : s(body.getOrDefault("staff_email", "city.staff@example.com"));

				t.description = s(body.get("description"));
				t.termOfConstruction = s(body.get("term"));
				String price = body.get("estimated_price");
				t.estimatedPrice = (price == null || price.isBlank()) ? null : new BigDecimal(price);

				long id = tenderDao.create(t);
				t.id = id;

				resp.setStatus(201);
				resp.getWriter().write(Json.tender(t));
				return;
			}

			// CREATE bid -> POST /api/tenders/{id}/bids  (form body)
			if (path.matches("^/\\d+/bids/?$")) {
				long tenderId = Long.parseLong(path.split("/")[1]);
				var p = new URLSearchParams(req);

				String companyId = s(p.get("company_id"));
				String companyName = s(p.get("company_name"));
				BigDecimal bidPrice = new BigDecimal(s(p.get("bid_price")));

				bidDao.create(tenderId, companyId, companyName, bidPrice);
				resp.getWriter().write("{\"ok\":true}");
				return;
			}

			// STAFF actions -> POST /api/tenders/{id}?action=...
			if (path.matches("^/\\d+/?$")) {

				// Accept ANY non-empty staffId (string or long) – no numeric parsing.
				Object staffObj = req.getSession().getAttribute("staffId");
				boolean isStaff = staffObj != null && !staffObj.toString().isBlank();
				if (!isStaff) {
					resp.setStatus(403);
					resp.getWriter().write("{\"error\":\"staff login required\"}");
					return;
				}

				long tenderId = Long.parseLong(path.substring(1));
				String action = s(req.getParameter("action")).toLowerCase();

				switch (action) {
					case "delete" -> tenderDao.delete(tenderId);
					case "close"  -> tenderDao.close(tenderId);
					case "award"  -> {
						long bidId = Long.parseLong(s(req.getParameter("bid_id")));
						String reason = s(req.getParameter("reason"));
						tenderDao.award(tenderId, bidId, reason);
					}
					default -> {
						resp.setStatus(400);
						resp.getWriter().write("{\"error\":\"unknown action\"}");
						return;
					}
				}
				resp.getWriter().write("{\"ok\":true}");
				return;
			}

			resp.setStatus(404);
			resp.getWriter().write("{\"error\":\"unknown endpoint\"}");
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write(jsonError(e));
		}
	}

	/* -------- utilities -------- */

	private static String s(String v) { return v == null ? "" : v.trim(); }

	// Minimal form param reader (works for application/x-www-form-urlencoded)
	private static final class URLSearchParams {
		private final Map<String, String> m = new HashMap<>();
		URLSearchParams(HttpServletRequest req) throws IOException {
			String body = req.getReader().lines().reduce("", (a, b) -> a + b);
			for (String pair : body.split("&")) {
				if (pair.isBlank()) continue;
				int i = pair.indexOf('=');
				String k = i < 0 ? pair : pair.substring(0, i);
				String v = i < 0 ? ""   : pair.substring(i + 1);
				m.put(java.net.URLDecoder.decode(k, java.nio.charset.StandardCharsets.UTF_8),
						java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8));
			}
		}
		String get(String k){ return m.get(k); }
	}

	private static Map<String, String> readJsonObject(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = req.getReader()) {
			String line; while ((line = br.readLine()) != null) sb.append(line);
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
			if (c == '"') inStr = !inStr;
			else if (c == ',' && !inStr) { pairs.add(raw.substring(start, i)); start = i + 1; }
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
			if (c == '"') in = !in;
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
		return "{\"error\":\"" + msg.replace("\"","\\\"") + "\"}";
	}

	/* ---------- JSON helpers for this servlet ---------- */
	private static final class Json {
		static String tender(Tender t) {
			return "{"
					+ "\"id\":" + t.id
					+ ",\"name\":" + escStr(t.name)
					+ ",\"notice_date\":" + escStr(t.noticeDate.toString())
					+ ",\"close_date\":" + escStr(t.closeDate.toString())
					+ ",\"disclose_date\":" + (t.discloseDate == null ? "null" : escStr(t.discloseDate.toString()))
					+ ",\"status\":" + escStr(t.status)
					+ ",\"description\":" + escStr(t.description)
					+ ",\"term\":" + escStr(t.termOfConstruction)
					+ ",\"estimated_price\":" + (t.estimatedPrice == null ? "null" : t.estimatedPrice.toPlainString())
					+ ",\"winner_reason\":" + escStr(t.winnerReason)
					+ ",\"winner_bid_id\":" + (t.winnerBidId == null ? "null" : t.winnerBidId)
					+ "}";
		}
		static String tenders(List<Tender> list) {
			StringBuilder b = new StringBuilder("[");
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) b.append(',');
				b.append(tender(list.get(i)));
			}
			return b.append(']').toString();
		}
		static String bid(org.todo.model.tender.Bid b) {
			return "{"
					+ "\"id\":" + b.id
					+ ",\"tender_id\":" + b.tenderId
					+ ",\"company_id\":" + escStr(b.companyId)
					+ ",\"company_name\":" + escStr(b.companyName)
					+ ",\"bid_price\":" + b.bidPrice.toPlainString()
					+ ",\"created_at\":" + escStr(b.createdAt.toString())
					+ "}";
		}
		static String bids(List<org.todo.model.tender.Bid> list) {
			StringBuilder b = new StringBuilder("[");
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) b.append(',');
				b.append(bid(list.get(i)));
			}
			return b.append(']').toString();
		}
		private static String escStr(String s){
			if (s == null) return "null";
			return "\"" + s.replace("\\","\\\\").replace("\"","\\\"") + "\"";
		}
	}
}
