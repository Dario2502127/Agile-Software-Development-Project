package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.todo.view.TemplateEngine;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@WebServlet("/city-login")
public class CityLoginServlet extends HttpServlet {

	// Allowed city staff (use ONLY the first name as the username)
	private static final Set<String> STAFF_FIRSTNAMES = Set.of(
			"dario",
			"samudika",
			"yasodha",
			"prabuddhi",
			"chengyao",
			"sanuji"
	);

	// Simple demo password. Change if you like.
	private static final String PASSWORD = "admin";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		TemplateEngine.process("city-login.html", req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// IMPORTANT: the login form must use input names: staffId, password
		String staffId  = trim(req.getParameter("staffId"));
		String password = trim(req.getParameter("password"));

		if (staffId == null || password == null) {
			req.setAttribute("message", "Please enter your first name and password.");
			TemplateEngine.process("city-login.html", req, resp);
			return;
		}

		String userKey = staffId.toLowerCase(Locale.ROOT);

		if (STAFF_FIRSTNAMES.contains(userKey) && PASSWORD.equals(password)) {
			HttpSession s = req.getSession(true);

			// Mark as staff
			s.setAttribute("staffId", staffId);                 // e.g. "Dario"
			s.setAttribute("staffEmail", buildEmailFor(staffId)); // used by detail page for edit-ownership guard

			// Clear any company context if it exists
			s.removeAttribute("username");
			s.removeAttribute("companyName");
			s.removeAttribute("companyDbId");

			resp.sendRedirect("tender");
		} else {
			req.setAttribute("message", "Invalid staff name or password!");
			TemplateEngine.process("city-login.html", req, resp);
		}
	}

	private static String trim(String v) { return v == null ? null : v.trim(); }

	// Lightweight fake email for display/ownership; adjust domain if you want.
	private static String buildEmailFor(String firstName) {
		return firstName.toLowerCase(Locale.ROOT) + "@city.gov";
	}
}
