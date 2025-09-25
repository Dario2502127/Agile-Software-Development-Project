package org.todo.model.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Central place to obtain JDBC connections. */
public final class DB {
	// File-based H2 database in your user home (persists between restarts)
	private static final String URL = "jdbc:h2:~/tenderpoint_db;AUTO_SERVER=TRUE";
	private static final String USER = "sa";
	private static final String PASSWORD = "";

	private DB() {}

	static {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("H2 driver not on classpath", e);
		}
	}

	public static Connection get() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}
}
