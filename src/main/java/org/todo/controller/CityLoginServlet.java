package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/city-login")
public class CityLoginServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		TemplateEngine.process("city-login.html", req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// IMPORTANT: names must be "staffId" and "password" in the HTML <input name="">
		String staffId  = trim(req.getParameter("staffId"));
		String password = trim(req.getParameter("password"));

		if ("city123".equals(staffId) && "admin".equals(password)) {
			HttpSession s = req.getSession(true);
			s.setAttribute("staffId", staffId);
			// optional: email/name for display
			s.setAttribute("staffEmail", "city.staff@example.com");
			resp.sendRedirect("tender");
		} else {
			req.setAttribute("message", "Invalid staff ID or password!");
			TemplateEngine.process("city-login.html", req, resp);
		}
	}

	private static String trim(String v) { return v == null ? null : v.trim(); }
}
