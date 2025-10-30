const express = require('express');
const { getTenders, getTenderById } = require('../controllers/tenderController');
const router = express.Router();

router.get('/', getTenders);
router.get('/:id', getTenderById);

module.exports = router;