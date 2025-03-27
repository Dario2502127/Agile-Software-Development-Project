package org.todo.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.todo.model.user.InvalidCredentialsException;
import org.todo.model.user.UserAlreadyExistsException;
import org.todo.model.user.UserService;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	private final UserService userService = UserService.getInstance();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		TemplateEngine.process("login.html", req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String username = req.getParameter("username");
		String password = req.getParameter("password");
		String action = req.getParameter("action");

		try {
			if ("register".equals(action)) {
				userService.registerUser(username, password);
			} else {
				userService.authenticateUser(username, password);
			}
			HttpSession session = req.getSession();
			session.setAttribute("username", username);
			resp.sendRedirect("todo-list");
			return;
		} catch (UserAlreadyExistsException e) {
			req.setAttribute("message", "User already exists!");
		} catch (InvalidCredentialsException e) {
			req.setAttribute("message", "Invalid username or password!");
		}
		req.setAttribute("username", username);
		doGet(req, resp);
	}
}
