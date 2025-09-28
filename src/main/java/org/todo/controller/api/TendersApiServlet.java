package org.todo.controller.api;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.todo.model.company.Company;
import org.todo.model.company.CompanyDao;
import org.todo.model.db.DB;
import org.todo.model.tender.BidDao;
import org.todo.model.tender.Tender;
import org.todo.model.tender.TenderDao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;

/**
 * REST-ish API for tenders & bids.
 *
 *  GET  /api/tenders                -> list tenders (visibility: staff=all, others: notice_date<=today)
 *  GET  /api/tenders/{id}           -> tender detail (JSON) (same visibility rule)
 *  GET  /api/tenders/{id}/bids      -> bids for tender (same visibility rule)
 *
 *  POST /api/tenders                -> create tender (JSON body)
 *  POST /api/tenders/{id}/bids      -> create bid   (multipart form; requires logged-in company & category eligibility)
 *  POST /api/tenders/{id}?action=...  (staff only):
 *        action=delete | close | award&bid_id=...&reason=...
 */
@WebServlet("/api/tenders/*")
@MultipartConfig(maxFileSize = 15 * 1024 * 1024) // to receive assignment file
public class TendersApiServlet extends HttpServlet {

	private final TenderDao tenderDao = new TenderDao();
	private final BidDao bidDao = new BidDao();
	private final CompanyDao companyDao = new CompanyDao();

	/* ----------------------- helpers ----------------------- */

	private static boolean isStaff(HttpServletRequest req) {
		Object staffObj = req.getSession().getAttribute("staffId");
		return staffObj != null && !staffObj.toString().isBlank();
	}
	private static String s(String v) { return v == null ? "" : v.trim(); }

	private static String normalizeCategory(String x) {
		if (x == null) return "";
		// collapse whitespace and lowercase for tolerant matching
		return x.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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
					+ ",\"category\":" + escStr(t.category)
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

	/* ----------------------- GET ----------------------- */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		String path = Optional.ofNullable(req.getPathInfo()).orElse("/");
		boolean staff = isStaff(req);
		LocalDate today = LocalDate.now();

		try {
			// /api/tenders  (list)
			if ("/".equals(path)) {
				var list = tenderDao.listAll();
				if (!staff) {
					// Only tenders with notice_date <= today for non-staff
					list.removeIf(t -> t.noticeDate != null && t.noticeDate.isAfter(today));
				}
				resp.getWriter().write(Json.tenders(list));
				return;
			}

			// /api/tenders/{id}/bids  (respect visibility)
			if (path.matches("^/\\d+/bids/?$")) {
				long id = Long.parseLong(path.split("/")[1]);
				if (!staff) {
					Tender t = tenderDao.find(id);
					if (t == null || (t.noticeDate != null && t.noticeDate.isAfter(today))) {
						resp.setStatus(404);
						resp.getWriter().write("{\"error\":\"not found\"}");
						return;
					}
				}
				var bids = bidDao.listFor(id);
				resp.getWriter().write(Json.bids(bids));
				return;
			}

			// /api/tenders/{id} (detail)  (respect visibility)
			if (path.matches("^/\\d+/?$")) {
				long id = Long.parseLong(path.substring(1));
				Tender t = tenderDao.find(id);
				if (t == null) {
					resp.setStatus(404);
					resp.getWriter().write("{\"error\":\"not found\"}");
					return;
				}
				if (!staff && t.noticeDate != null && t.noticeDate.isAfter(today)) {
					// Hide tenders before notice to non-staff
					resp.setStatus(404);
					resp.getWriter().write("{\"error\":\"not found\"}");
					return;
				}
				resp.getWriter().write(Json.tender(t));
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
				t.noticeDate = java.time.LocalDate.parse(s(body.get("notice_date")));
				t.closeDate = java.time.LocalDate.parse(s(body.get("close_date")));

				String dd = body.get("disclose_date");
				t.discloseDate = (dd == null || dd.isBlank()) ? null : java.time.LocalDate.parse(dd);

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

				// single category
				t.category = s(body.get("category"));

				long id = tenderDao.create(t);
				t.id = id;

				resp.setStatus(201);
				resp.getWriter().write(Json.tender(t));
				return;
			}

			// CREATE bid -> POST /api/tenders/{id}/bids  (multipart form) + eligibility checks
			if (path.matches("^/\\d+/bids/?$")) {
				// must be logged in as a company
				var session = req.getSession(false);
				String loginUid = (session == null) ? null : (String) session.getAttribute("username");
				Long companyDbId = (session == null) ? null : (Long) session.getAttribute("companyDbId");
				String companyNameFromSes = (session == null) ? null : (String) session.getAttribute("companyName");
				if (loginUid == null || loginUid.isBlank()) {
					resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					resp.getWriter().write("{\"error\":\"login required\"}");
					return;
				}

				long tenderId = Long.parseLong(path.split("/")[1]);
				Tender t = tenderDao.find(tenderId);
				if (t == null) {
					resp.setStatus(404);
					resp.getWriter().write("{\"error\":\"tender not found\"}");
					return;
				}

				// date/status guards
				LocalDate today = LocalDate.now();
				String st = (t.status == null ? "" : t.status).toLowerCase(Locale.ROOT);
				if (st.contains("closed") || st.contains("award")) {
					resp.setStatus(403);
					resp.getWriter().write("{\"error\":\"tender is not accepting bids\"}");
					return;
				}
				if (t.noticeDate != null && today.isBefore(t.noticeDate)) {
					resp.setStatus(403);
					resp.getWriter().write("{\"error\":\"bidding not open yet\"}");
					return;
				}
				if (t.closeDate != null && !today.isBefore(t.closeDate)) {
					resp.setStatus(403);
					resp.getWriter().write("{\"error\":\"bidding period is over\"}");
					return;
				}

				// category eligibility (companies may have multiple categories)
				String tenderCatNorm = normalizeCategory(t.category);
				if (!tenderCatNorm.isEmpty()) {
					Company co = (companyDbId != null)
							? companyDao.find(companyDbId)
							: companyDao.findByUid(loginUid);
					if (co == null) {
						resp.setStatus(403);
						resp.getWriter().write("{\"error\":\"company profile not found\"}");
						return;
					}
					boolean ok = false;
					for (String c : co.categories) {
						if (normalizeCategory(c).equals(tenderCatNorm)) { ok = true; break; }
					}
					if (!ok) {
						resp.setStatus(403);
						resp.getWriter().write("{\"error\":\"company not eligible for this tender category\"}");
						return;
					}
				}

				// read multipart params
				String companyId   = s(req.getParameter("company_id"));
				String companyName = s(req.getParameter("company_name"));
				String priceStr    = s(req.getParameter("bid_price"));
				if (companyId.isEmpty())   companyId   = loginUid;
				if (companyName.isEmpty()) companyName = (companyNameFromSes == null || companyNameFromSes.isBlank())
						? loginUid : companyNameFromSes;
				if (priceStr.isEmpty()) {
					resp.setStatus(400);
					resp.getWriter().write("{\"error\":\"bid_price required\"}");
					return;
				}
				BigDecimal bidPrice = new BigDecimal(priceStr);

				long bidId = bidDao.create(tenderId, companyId, companyName, bidPrice);

				// optional assignment file
				Part part = null;
				try { part = req.getPart("assignment"); } catch (Exception ignored) {}
				if (part != null && part.getSize() > 0) {
					try (Connection c = DB.get();
						 PreparedStatement ps = c.prepareStatement(
								 "insert into bid_files(bid_id, filename, content_type, data) values(?,?,?,?)");
						 InputStream in = part.getInputStream()) {
						ps.setLong(1, bidId);
						ps.setString(2, s(part.getSubmittedFileName()));
						ps.setString(3, s(part.getContentType()));
						ps.setBinaryStream(4, in, (int) part.getSize());
						ps.executeUpdate();
					}
				}

				resp.getWriter().write("{\"ok\":true}");
				return;
			}

			// STAFF actions -> POST /api/tenders/{id}?action=...
			if (path.matches("^/\\d+/?$")) {
				if (!isStaff(req)) {
					resp.setStatus(403);
					resp.getWriter().write("{\"error\":\"staff login required\"}");
					return;
				}

				long tenderId = Long.parseLong(path.substring(1));
				String action = s(req.getParameter("action")).toLowerCase(Locale.ROOT);

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
}
