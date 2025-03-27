package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.todo.model.todo.Todo;
import org.todo.model.todo.TodoService;
import org.todo.view.TemplateEngine;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet("/add-todo")
public class AddTodoServlet extends HttpServlet {

	private static final TodoService todoService = TodoService.getInstance();

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		TemplateEngine.process("add-todo.html", request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String username = (String) request.getSession().getAttribute("username");
		if (username == null) {
			response.sendRedirect("login");
			return;
		}
		try {
			String title = request.getParameter("title");
			String category = request.getParameter("category");
			String param = request.getParameter("dueDate");
			LocalDate dueDate = param.isEmpty() ? LocalDate.now() : LocalDate.parse(param);

			PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
			title = policy.sanitize(title);
			category = policy.sanitize(category);

			todoService.addTodo(new Todo(title, category, dueDate), username);
			request.setAttribute("message", "Todo '" + title + "' has been added successfully");
			request.setAttribute("message-type", "success");
			doGet(request, response);
		} catch(DateTimeParseException e) {
			request.setAttribute("message", "Invalid due-date!");
			request.setAttribute("message-type", "danger");
			doGet(request, response);
		}
	}
}
