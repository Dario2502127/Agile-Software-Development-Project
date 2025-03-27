package org.todo.model.todo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class TodoService {

	private static final Logger logger = Logger.getLogger(TodoService.class.getName());

	private static final TodoService instance = new TodoService();
	private final List<Todo> todos = new ArrayList<>();
	private int lastId = 0;

	public static TodoService getInstance() {
		return instance;
	}

	private TodoService() {}

	public List<Todo> getTodos() {
		return todos;
	}

	public List<Todo> findTodos(String category) {
		return todos.stream().filter(todo -> Objects.equals(todo.getCategory(), category)).toList();
	}

	public List<Todo> getTodos(String username) {
		return todos.stream().filter(todo -> Objects.equals(todo.getUsername(), username)).toList();
	}

	public List<Todo> findTodos(String username, String category) {
		return getTodos(username).stream().filter(todo -> Objects.equals(todo.getCategory(), category)).toList();
	}

	public Todo findTodo(int id) throws TodoNotFoundException {
		return todos.stream().filter(todo -> Objects.equals(todo.getId(), id)).findFirst()
				.orElseThrow(TodoNotFoundException::new);
	}

	public void addTodo(Todo todo) {
		todo.setId(++lastId);
		todos.add(todo);
		logger.info("Todo " + todo.getId() + " added");
	}

	public void addTodo(Todo todo, String username) {
		todo.setUsername(username);
		addTodo(todo);
	}

	public void updateTodo(Todo todo) throws TodoNotFoundException {
		Todo oldTodo = findTodo(todo.getId());
		oldTodo.setTitle(todo.getTitle());
		oldTodo.setCategory(todo.getCategory());
		oldTodo.setDueDate(todo.getDueDate());
		oldTodo.setCompleted(todo.isCompleted());
		logger.info("Todo " + todo.getId() + " updated");
	}

	public void removeTodo(int id) throws TodoNotFoundException {
		Todo todo = findTodo(id);
		todos.remove(todo);
		logger.info("Todo " + todo.getId() + " removed");
	}
}
