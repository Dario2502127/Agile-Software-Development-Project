package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/tender-detail")
public class TenderDetailPageServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Tender id -> pass to template
		String tid = req.getParameter("id");
		req.setAttribute("tenderId", tid == null ? "" : tid);

		// Session context
		Object user = req.getSession().getAttribute("username");        // company uid
		Object companyName = req.getSession().getAttribute("companyName");
		Object staff = req.getSession().getAttribute("staffId");        // any non-empty -> staff

		boolean isStaff = staff != null && !staff.toString().isBlank();
		req.setAttribute("isStaff", isStaff ? "1" : "");

		if (user != null) {
			req.setAttribute("authLink", "<a class=\"cta\" href=\"logout\">Logout</a>");
			req.setAttribute("companyId", String.valueOf(user));
			req.setAttribute("companyName", companyName == null ? "" : String.valueOf(companyName));
		} else if (isStaff) {
			req.setAttribute("authLink", "<a class=\"cta\" href=\"logout\">Logout</a>");
			req.setAttribute("companyId", "");
			req.setAttribute("companyName", "");
		} else {
			req.setAttribute("authLink", "<a class=\"nav-link\" href=\"login\">Login</a>");
			req.setAttribute("companyId", "");
			req.setAttribute("companyName", "");
		}

		TemplateEngine.process("tender-detail.html", req, resp);
	}
}
