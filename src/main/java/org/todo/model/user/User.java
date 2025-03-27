package org.todo.model.user;

public class User {

	private String name;
	private String password;

	public User() {
	}

	public User(String name, String password) {
		this.name = name;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public String getPassword() {
		return password;
	}

}
