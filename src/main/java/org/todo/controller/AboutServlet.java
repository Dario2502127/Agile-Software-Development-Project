package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/about")
public class AboutServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		boolean loggedIn = req.getSession().getAttribute("username") != null
				|| req.getSession().getAttribute("staffId") != null;

		String authLink = loggedIn
				? "<a class=\"cta\" href=\"logout\">Logout</a>"
				: "<a class=\"cta\" href=\"login\">Sign In</a>";

		req.setAttribute("authLink", authLink);
		TemplateEngine.process("about.html", req, resp);
	}
}
