package org.todo.controller.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.todo.model.tender.BidDao;
import org.todo.model.tender.Tender;
import org.todo.model.tender.TenderDao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Bids API
 *
 * Routes (prefix mapping):
 *   GET    /api/bids/{tenderId}         -> list bids for a tender
 *   POST   /api/bids/{tenderId}         -> create bid for a tender  (requires logged-in supplier = session "username")
 *   DELETE /api/bids?id={bidId}         -> delete bid (staff only)
 */
@WebServlet("/api/bids/*")
public class BidsApiServlet extends HttpServlet {
	private final BidDao dao = new BidDao();
	private final TenderDao tenderDao = new TenderDao();

	/** path like "/123" -> 123 */
	private long tenderId(HttpServletRequest req) {
		String p = req.getPathInfo();       // e.g. "/123"
		if (p == null || p.length() < 2) throw new IllegalArgumentException("tender id missing");
		return Long.parseLong(p.substring(1));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		try {
			long tid = tenderId(req);
			resp.getWriter().write(Json.bids(dao.listFor(tid)));
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\":\""+e.getMessage()+"\"}");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");

		// ✅ Require a logged-in company (session "username")
		HttpSession ses = req.getSession(false);
		String user = (ses == null) ? null : (String) ses.getAttribute("username");
		if (user == null || user.isBlank()) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.getWriter().write("{\"error\":\"You must be logged in as a company to place a bid.\"}");
			return;
		}

		try {
			long tid = tenderId(req);

			// ✅ Block bids if tender is not Open or its close date has passed
			Tender t = tenderDao.find(tid);
			if (t == null) {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				resp.getWriter().write("{\"error\":\"tender not found\"}");
				return;
			}
			boolean pastCloseDate = (t.closeDate != null) && LocalDate.now().isAfter(t.closeDate);
			boolean notOpen = t.status != null && !t.status.equalsIgnoreCase("Open");
			if (pastCloseDate || notOpen) {
				resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
				resp.getWriter().write("{\"error\":\"Bidding is closed for this tender.\"}");
				return;
			}

			String companyId   = req.getParameter("company_id");   // may be blank -> default to username
			String companyName = req.getParameter("company_name"); // may be blank -> default to username
			String price       = req.getParameter("bid_price");

			if (price == null || price.isBlank()) {
				resp.setStatus(400);
				resp.getWriter().write("{\"error\":\"bid_price required\"}");
				return;
			}

			long id = dao.create(
					tid,
					(companyId == null || companyId.isBlank()) ? user : companyId,
					(companyName == null || companyName.isBlank()) ? user : companyName,
					new BigDecimal(price)
			);
			resp.getWriter().write("{\"ok\":true,\"id\":"+id+"}");
		} catch (SQLException e) {
			resp.setStatus(500);
			resp.getWriter().write("{\"error\":\""+e.getMessage()+"\"}");
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\":\""+e.getMessage()+"\"}");
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json; charset=UTF-8");
		// staff only
		if (req.getSession().getAttribute("staffId") == null) { resp.setStatus(403); return; }
		long id = Long.parseLong(req.getParameter("id"));
		try {
			dao.delete(id);
			resp.getWriter().write("{\"ok\":true}");
		} catch (SQLException e) {
			resp.setStatus(500);
			resp.getWriter().write("{\"error\":\""+e.getMessage()+"\"}");
		}
	}
}
