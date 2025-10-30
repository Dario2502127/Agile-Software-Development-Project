const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/database');

// Register new supplier
const register = (req, res) => {
  const { company_name, registration_number, email, password } = req.body;

  // Check if user exists
  db.query('SELECT * FROM users WHERE email = ?', [email], async (err, results) => {
    if (err) return res.status(500).json({ message: 'Database error' });
    if (results.length > 0) return res.status(400).json({ message: 'User already exists' });

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);
    const company_id = 'COMP' + Date.now();

    // Insert user
    db.query(
      'INSERT INTO users (company_name, registration_number, email, password, company_id) VALUES (?, ?, ?, ?, ?)',
      [company_name, registration_number, email, hashedPassword, company_id],
      (err, results) => {
        if (err) return res.status(500).json({ message: 'Error creating user' });
        res.status(201).json({ 
          message: 'User registered successfully',
          company_id: company_id
        });
      }
    );
  });
};

// Login user
const login = (req, res) => {
  const { email, password, staff_id } = req.body;

  let query = 'SELECT * FROM users WHERE email = ?';
  let params = [email];

  if (staff_id) {
    query = 'SELECT * FROM users WHERE staff_id = ?';
    params = [staff_id];
  }

  db.query(query, params, async (err, results) => {
    if (err) return res.status(500).json({ message: 'Database error' });
    if (results.length === 0) return res.status(400).json({ message: 'Invalid credentials' });

    const user = results[0];
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(400).json({ message: 'Invalid credentials' });

    // Create token
    const token = jwt.sign(
      { id: user.id, email: user.email, user_type: user.user_type },
      process.env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        email: user.email,
        company_name: user.company_name,
        company_id: user.company_id,
        staff_id: user.staff_id,
        user_type: user.user_type
      }
    });
  });
};

// Forgot password
const forgotPassword = (req, res) => {
  const { email } = req.body;
  
  db.query('SELECT * FROM users WHERE email = ?', [email], (err, results) => {
    if (err) return res.status(500).json({ message: 'Database error' });
    if (results.length === 0) return res.status(404).json({ message: 'Email not found' });
    
    res.json({ message: 'Password reset link has been sent to your email' });
  });
};

module.exports = { register, login, forgotPassword };