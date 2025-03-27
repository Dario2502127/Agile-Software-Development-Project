package org.todo.model.user;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UserService {

	private static final Logger logger = Logger.getLogger(UserService.class.getName());

	private static final UserService instance = new UserService();
	private final List<User> users = new ArrayList<>();

	public static UserService getInstance() {
		return instance;
	}

	private UserService() {}

	public User findUser(String username) {
		return users.stream().filter(user -> user.getName().equals(username)).findFirst().orElse(null);
	}

	public void registerUser(String username, String password) throws UserAlreadyExistsException {
		if (findUser(username) != null) {
			throw new UserAlreadyExistsException();
		}
		User user = new User(username, password);
		users.add(user);
		logger.info("User " + username + " registered");
	}

	public void authenticateUser(String username, String password) throws InvalidCredentialsException {
		User user = findUser(username);
		if (user == null || !user.getPassword().equals(password)) {
			throw new InvalidCredentialsException();
		}
		logger.info("User " + username + " authenticated");
	}
}
