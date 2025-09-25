document.getElementById("forgot-form").addEventListener("submit", function(event) {
  event.preventDefault();
  const email = document.getElementById("email").value.trim();
  if (email) {
    alert("A password reset link has been sent to " + email);
    window.location.href = "login"; // back to servlet route
  } else {
    alert("Please enter a valid email.");
  }
});
