package org.todo.model.company;

import java.util.ArrayList;
import java.util.List;

public class Company {
	public Long id;
	public String companyUid;      // login / business id (unique)
	public String name;
	public String passwordHash;    // SHA-256 hex (never expose over UI)
	public List<String> categories = new ArrayList<>();

	// --- lockout ---
	public int failedAttempts = 0;
	public boolean locked = false;
}
