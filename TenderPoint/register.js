document.getElementById("register-form").addEventListener("submit", function(e){
  e.preventDefault();

  const name = document.getElementById("name").value.trim();
  const registration = document.getElementById("registration").value.trim();
  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value.trim();
  const confirmPassword = document.getElementById("confirm-password").value.trim();

  if (!name || !registration || !email || !password || !confirmPassword) {
    alert("Please fill in all fields.");
    return;
  }

  if (password !== confirmPassword) {
    alert("Passwords do not match.");
    return;
  }

  // Demo registration (replace with server-side API)
  alert(`Account created successfully for ${name} (${email})!`);
  window.location.href = "login.html"; // redirect to login page
});
