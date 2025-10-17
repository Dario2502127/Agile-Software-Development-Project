document.getElementById("city-login-form").addEventListener("submit", function(e){
  e.preventDefault();

  const staffId = document.getElementById("staff-id").value.trim();
  const password = document.getElementById("city-password").value.trim();

  if (!staffId || !password) {
    alert("Please fill in all fields.");
    return;
  }

  // Simulate staff login
  if (staffId === "city123" && password === "admin") {
    alert("Welcome City Staff!");
    window.location.href = "city-staff.html"; // Redirect to CityTender dashboard
  } else {
    alert("Invalid staff ID or password.");
  }
});
