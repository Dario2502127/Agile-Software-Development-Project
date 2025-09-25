package org.todo.controller;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.todo.model.db.Schema;

@WebListener
public class AppInitListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Schema.ensure(); // create tables if missing
	}
}
