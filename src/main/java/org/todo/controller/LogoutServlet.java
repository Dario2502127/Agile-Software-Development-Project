package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.getSession().invalidate();
		TemplateEngine.process("logout.html", request, response);
	}
}
