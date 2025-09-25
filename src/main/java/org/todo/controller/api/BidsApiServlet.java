package org.todo.controller.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.todo.model.tender.BidDao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

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

		// ANY logged-in supplier can bid (uses existing session "username")
		String user = (String) req.getSession().getAttribute("username");
		if (user == null) { resp.setStatus(403); resp.getWriter().write("{\"error\":\"login required\"}"); return; }

		try {
			long tid = tenderId(req);

			String companyId = req.getParameter("company_id"); // could be same as username
			String companyName = req.getParameter("company_name");
			String price = req.getParameter("bid_price");

			long id = dao.create(
					tid,
					companyId == null || companyId.isBlank() ? user : companyId,
					companyName == null || companyName.isBlank() ? user : companyName,
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
		// staff only
		if (req.getSession().getAttribute("staffId") == null) { resp.setStatus(403); return; }
		long id = Long.parseLong(req.getParameter("id"));
		try {
			dao.delete(id);
			resp.getWriter().write("{\"ok\":true}");
		} catch (SQLException e) { resp.setStatus(500); resp.getWriter().write("{\"error\":\""+e.getMessage()+"\"}"); }
	}
}
