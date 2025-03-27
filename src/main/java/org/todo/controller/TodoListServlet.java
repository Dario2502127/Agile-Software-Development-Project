package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.todo.Todo;
import org.todo.model.todo.TodoService;
import org.todo.view.TemplateEngine;

import java.io.IOException;

@WebServlet("/todo-list")
public class TodoListServlet extends HttpServlet {

	private static final TodoService todoService = TodoService.getInstance();

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String username = (String) request.getSession().getAttribute("username");
		if (username == null) {
			response.sendRedirect("login");
			return;
		}

		request.setAttribute("username", username);
		StringBuilder builder = new StringBuilder();
		for (Todo todo : todoService.getTodos(username)) {
			builder.append("<li>").append(todo).append(" <a href=\"delete-todo?id="+todo.getId()+"\">Delete</a></li>");
		}
		if (builder.isEmpty()) {
			builder.append("<li>[Empty list]</li>");
		}
		request.setAttribute("todos", "<ul>" + builder + "</ul>");
		TemplateEngine.process("todo-list.html", request, response);
	}
}
