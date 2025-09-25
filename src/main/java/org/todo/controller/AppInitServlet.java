package org.todo.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import org.todo.model.db.Schema;

/** Initializes DB schema once at app startup. */
@WebServlet(value = "/__init", loadOnStartup = 1)
public class AppInitServlet extends HttpServlet {
	@Override
	public void init() throws ServletException {
		Schema.ensure();
	}
}
