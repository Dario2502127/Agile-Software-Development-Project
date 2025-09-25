package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.todo.model.tender.Bid;
import org.todo.model.tender.BidDao;
import org.todo.model.tender.Tender;
import org.todo.model.tender.TenderDao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/admin/db")
public class AdminDbServlet extends HttpServlet {
	private final TenderDao tenderDao = new TenderDao();
	private final BidDao bidDao = new BidDao();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// staff only
		if (req.getSession().getAttribute("staffId") == null) {
			resp.setStatus(403);
			resp.getWriter().write("<h3>403 — staff login required</h3>");
			return;
		}
		resp.setContentType("text/html; charset=UTF-8");
		StringBuilder out = new StringBuilder();
		out.append("<html><head><title>DB Inspect</title><style>")
				.append("body{font-family:sans-serif;padding:16px}")
				.append("table{border-collapse:collapse;width:100%;margin:10px 0}")
				.append("th,td{border:1px solid #ddd;padding:6px;text-align:left}")
				.append("</style></head><body>");
		out.append("<h2>Tenders</h2>");
		try {
			List<Tender> tenders = tenderDao.listAll();
			out.append("<table><thead><tr>")
					.append("<th>ID</th><th>Name</th><th>Notice</th><th>Close</th><th>Disclose</th><th>Status</th><th>WinnerBid</th>")
					.append("</tr></thead><tbody>");
			for (Tender t : tenders) {
				out.append("<tr>")
						.append("<td>").append(t.id).append("</td>")
						.append("<td>").append(escape(t.name)).append("</td>")
						.append("<td>").append(t.noticeDate).append("</td>")
						.append("<td>").append(t.closeDate).append("</td>")
						.append("<td>").append(t.discloseDate==null?"-":t.discloseDate).append("</td>")
						.append("<td>").append(escape(t.status)).append("</td>")
						.append("<td>").append(t.winnerBidId==null?"-":t.winnerBidId).append("</td>")
						.append("</tr>");
			}
			out.append("</tbody></table>");

			out.append("<h2>Bids (by tender)</h2>");
			for (Tender t : tenders) {
				List<Bid> bids = bidDao.listFor(t.id);
				out.append("<h3>Tender ").append(t.id).append(" — ").append(escape(t.name)).append("</h3>");
				out.append("<table><thead><tr>")
						.append("<th>ID</th><th>Company ID</th><th>Company name</th><th>Price</th><th>Created</th>")
						.append("</tr></thead><tbody>");
				for (Bid b : bids) {
					out.append("<tr>")
							.append("<td>").append(b.id).append("</td>")
							.append("<td>").append(escape(b.companyId)).append("</td>")
							.append("<td>").append(escape(b.companyName)).append("</td>")
							.append("<td>").append(b.bidPrice).append("</td>")
							.append("<td>").append(b.createdAt).append("</td>")
							.append("</tr>");
				}
				out.append("</tbody></table>");
			}

		} catch (SQLException e) {
			out.append("<pre>").append(escape(e.toString())).append("</pre>");
		}
		out.append("</body></html>");
		resp.getWriter().write(out.toString());
	}

	private static String escape(String s){ return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
