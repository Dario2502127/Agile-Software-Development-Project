package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.company.Company;
import org.todo.model.company.CompanyDao;
import org.todo.view.TemplateEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	private final CompanyDao companyDao = new CompanyDao();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Already logged in as company? go home.
		if (req.getSession(false) != null && req.getSession(false).getAttribute("username") != null) {
			resp.sendRedirect(req.getContextPath() + "/");
			return;
		}
		// Show login page (message placeholder supported)
		if (req.getAttribute("message") == null) req.setAttribute("message", "");
		TemplateEngine.process("login.html", req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String action = s(req.getParameter("action"));
		if (!"login".equalsIgnoreCase(action)) { doGet(req, resp); return; }

		String companyId = s(req.getParameter("companyId"));
		String password  = s(req.getParameter("password"));

		if (companyId.isBlank() || password.isBlank()) {
			req.setAttribute("message", "Please enter company-id and password.");
			TemplateEngine.process("login.html", req, resp);
			return;
		}

		try {
			Company c = companyDao.findByUid(companyId);
			if (c == null || !hash(password).equalsIgnoreCase(c.passwordHash)) {
				req.setAttribute("message", "Invalid company-id or password.");
				TemplateEngine.process("login.html", req, resp);
				return;
			}

			// Success: set company session (and clear any staff login)
			var ses = req.getSession(true);
			ses.setAttribute("username", c.companyUid);   // used by BidsApiServlet
			ses.setAttribute("companyName", c.name);
			ses.setAttribute("companyDbId", c.id);
			ses.removeAttribute("staffId");
			ses.removeAttribute("staffEmail");

			resp.sendRedirect(req.getContextPath() + "/");
		} catch (Exception e) {
			req.setAttribute("message", "Login failed: " + e.getMessage());
			TemplateEngine.process("login.html", req, resp);
		}
	}

	/* ----------------- helpers ----------------- */

	private static String s(String v) { return v == null ? "" : v.trim(); }

	private static String hash(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder b = new StringBuilder(d.length * 2);
			for (byte x : d) b.append(String.format("%02x", x));
			return b.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
