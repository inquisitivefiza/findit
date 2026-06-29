const router = require('express').Router();
const db = require('../db');
const auth = require('../middleware/authMiddleware');

router.post('/:id/confirm', auth, async (req, res) => {
  await db.execute('UPDATE matches SET status = "CONFIRMED" WHERE id = ?', [req.params.id]);
  res.json({ message: 'Match confirmed' });
});

router.post('/:id/reject', auth, async (req, res) => {
  await db.execute('UPDATE matches SET status = "REJECTED" WHERE id = ?', [req.params.id]);
  res.json({ message: 'Match rejected' });
});

module.exports = router;