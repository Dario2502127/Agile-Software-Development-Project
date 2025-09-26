package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/companies")
public class CompaniesPageServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Only staff can see the page; others → city staff login
		Object staff = req.getSession().getAttribute("staffId");
		if (staff == null || staff.toString().isBlank()) {
			resp.sendRedirect("city-login");
			return;
		}

		// Provide values for placeholders used in the template
		req.setAttribute("authLink", "<a class=\"nav-link\" href=\"logout\">Logout</a>");

		// Render src/main/resources/templates/companies.html
		TemplateEngine.process("companies.html", req, resp);
	}
}
