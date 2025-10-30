const db = require('../config/database');

// Get all tenders
const getTenders = (req, res) => {
  const { q, sector } = req.query;
  
  let query = `SELECT *, 
    DATE_FORMAT(closing_date, '%Y-%m-%d') as closing,
    DATE_FORMAT(established_date, '%Y-%m-%d') as Established
    FROM tenders WHERE 1=1`;
  
  const params = [];

  if (q) {
    query += ' AND (title LIKE ? OR reference LIKE ?)';
    params.push(`%${q}%`, `%${q}%`);
  }

  if (sector && sector !== '') {
    query += ' AND sector = ?';
    params.push(sector);
  }

  query += ' ORDER BY closing_date ASC';

  db.query(query, params, (err, results) => {
    if (err) {
      console.error('Database error:', err);
      return res.status(500).json({ message: 'Error fetching tenders' });
    }
    res.json(results);
  });
};

// Get tender by ID
const getTenderById = (req, res) => {
  const { id } = req.params;
  
  db.query('SELECT * FROM tenders WHERE id = ?', [id], (err, results) => {
    if (err) return res.status(500).json({ message: 'Database error' });
    if (results.length === 0) return res.status(404).json({ message: 'Tender not found' });
    res.json(results[0]);
  });
};

module.exports = { getTenders, getTenderById };