document.getElementById("forgot-form").addEventListener("submit", function(event) {
  event.preventDefault();

  const email = document.getElementById("email").value.trim();

  if (email) {
    // In a real app, send this to backend
    alert("A password reset link has been sent to " + email);
    window.location.href = "login.html"; // Redirect back to login
  } else {
    alert("Please enter a valid email.");
  }
});
