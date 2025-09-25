package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.view.TemplateEngine;

import java.io.IOException;
@WebServlet("/forgot")
public class ForgotServlet extends HttpServlet {
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		TemplateEngine.process("forgot.html", req, resp);
	}
}

