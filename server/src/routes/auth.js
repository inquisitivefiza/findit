const router = require('express').Router();
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const db = require('../db');

router.post('/register', async (req, res) => {
  const { name, email, password, college_id } = req.body;
  try {
    const hash = await bcrypt.hash(password, 10);
    const [result] = await db.execute(
      'INSERT INTO users (name, email, password_hash, college_id) VALUES (?, ?, ?, ?)',
      [name, email, hash, college_id]
    );
    res.json({ message: 'Registered successfully', userId: result.insertId });
  } catch (err) {
    console.error('[ERROR] POST /auth/register:', err);
    res.status(400).json({ error: err.message || err.code || 'Unknown error' });
  }
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const [rows] = await db.execute('SELECT * FROM users WHERE email = ?', [email]);
    if (!rows.length) return res.status(401).json({ error: 'Invalid credentials' });

    const user = rows[0];
    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) return res.status(401).json({ error: 'Invalid credentials' });

    const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, { expiresIn: '7d' });
    res.json({ token, user: { id: user.id, name: user.name, email: user.email } });
  } catch (err) {
    console.error('[ERROR] POST /auth/login:', err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;