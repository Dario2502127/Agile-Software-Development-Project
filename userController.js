const db = require('../config/database');

const getProfile = (req, res) => {
  const userId = req.user.id;
  
  db.query('SELECT id, company_name, email, company_id, staff_id, user_type FROM users WHERE id = ?', 
  [userId], (err, results) => {
    if (err) return res.status(500).json({ message: 'Database error' });
    if (results.length === 0) return res.status(404).json({ message: 'User not found' });
    res.json(results[0]);
  });
};

module.exports = { getProfile };