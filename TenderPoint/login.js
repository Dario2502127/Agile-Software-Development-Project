document.getElementById("login-form").addEventListener("submit", function (e) {
  e.preventDefault();

  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value.trim();

  if (!email || !password) {
    alert("Please enter both email and password.");
    return;
  }

  // Simple demo authentication (replace with real API call)
  if (email === "admin@example.com" && password === "admin123") {
    alert("Login successful!");
    window.location.href = "Tender.html"; // redirect to homepage
  } else {
    alert("Invalid email or password. Try again.");
  }

  

});
