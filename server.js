const express = require('express');
const cors = require('cors');
require('dotenv').config();

// Import routes
const authRoutes = require('./routes/auth');
const tenderRoutes = require('./routes/tenders');
const userRoutes = require('./routes/users');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/tenders', tenderRoutes);
app.use('/api/users', userRoutes);

// Test route
app.get('/', (req, res) => {
  res.json({ 
    message: '🎉 TenderPoint API is running!',
    version: '1.0.0',
    endpoints: {
      auth: '/api/auth',
      tenders: '/api/tenders', 
      users: '/api/users'
    }
  });
});

// Start server
app.listen(PORT, () => {
  console.log('='.repeat(50));
  console.log('🚀 TenderPoint Backend Server Started');
  console.log('='.repeat(50));
  console.log(`✅ Port: ${PORT}`);
  console.log(`✅ Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`✅ Database: ${process.env.DB_NAME}`);
  console.log(`✅ API URL: http://localhost:${PORT}`);
  console.log('='.repeat(50));
});