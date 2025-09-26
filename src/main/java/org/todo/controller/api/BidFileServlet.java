package org.todo.controller.api;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.tender.BidFileDao;

import java.io.IOException;

@WebServlet("/api/bid-file")
public class BidFileServlet extends HttpServlet {
	private final BidFileDao dao = new BidFileDao();

	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String idStr = req.getParameter("id");
		if (idStr == null || idStr.isBlank()) { resp.setStatus(400); resp.getWriter().write("missing id"); return; }
		try {
			long id = Long.parseLong(idStr);
			var meta = dao.meta(id);
			if (meta == null) { resp.setStatus(404); resp.getWriter().write("not found"); return; }

			String ctype = (meta.contentType == null || meta.contentType.isBlank()) ? "application/octet-stream" : meta.contentType;
			resp.setContentType(ctype);
			String name = (meta.filename == null || meta.filename.isBlank()) ? ("assignment-" + id) : meta.filename;
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + name.replace("\"","") + "\"");

			boolean ok = dao.stream(id, resp.getOutputStream());
			if (!ok) { resp.setStatus(404); resp.getWriter().write("not found"); }
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("error: " + e.getMessage());
		}
	}
}
