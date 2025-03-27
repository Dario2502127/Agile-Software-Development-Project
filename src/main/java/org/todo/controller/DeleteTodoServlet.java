package org.todo.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.todo.TodoNotFoundException;
import org.todo.model.todo.TodoService;

import java.io.IOException;

@WebServlet("/delete-todo")
public class DeleteTodoServlet extends HttpServlet {

	private TodoService todoService = TodoService.getInstance();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String username = (String) req.getSession().getAttribute("username");
		if (username == null) {
			resp.sendRedirect("login");
			return;
		}
		try {
			int id = Integer.parseInt(req.getParameter("id"));
			if (todoService.findTodo(id).getUsername().equals(username)) {
				todoService.removeTodo(id);
				resp.sendRedirect("todo-list");
			} else {
				resp.setStatus(403);
			}
		} catch (NumberFormatException e) {
			resp.setStatus(400);
		} catch (TodoNotFoundException e) {
			resp.setStatus(404);
		}
	}
}
