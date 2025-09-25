package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/tender-detail")
public class TenderDetailPageServlet extends HttpServlet {
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// dynamic authLink like others
		boolean loggedIn = req.getSession().getAttribute("username") != null
				|| req.getSession().getAttribute("staffId") != null;
		String authLink = loggedIn ? "<a class=\"cta\" href=\"logout\">Logout</a>" : "<a class=\"cta\" href=\"login\">Sign In</a>";
		req.setAttribute("authLink", authLink);
		TemplateEngine.process("tender-detail.html", req, resp);
	}
}
