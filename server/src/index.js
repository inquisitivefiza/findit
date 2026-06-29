const express = require('express');
const cors = require('cors');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const itemRoutes = require('./routes/items');
const matchRoutes = require('./routes/matches');

const app = express();
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static('uploads')); // serve uploaded images

app.use('/auth', authRoutes);
app.use('/items', itemRoutes);
app.use('/matches', matchRoutes);

app.listen(3000, '0.0.0.0', () => {
  console.log('Server running on port 3000');
});