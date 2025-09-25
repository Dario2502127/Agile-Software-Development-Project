package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.todo.model.db.DB;

@WebServlet("/__dbcheck")
public class DbCheckServlet extends HttpServlet {
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain; charset=UTF-8");
		try (Connection c = DB.get()) {
			DatabaseMetaData md = c.getMetaData();
			resp.getWriter().println("OK");
			resp.getWriter().println("Driver: " + md.getDriverName() + " " + md.getDriverVersion());
			resp.getWriter().println("URL: " + md.getURL());
		} catch (Throwable t) {
			t.printStackTrace(resp.getWriter()); // shows exact failure
		}
	}
}
