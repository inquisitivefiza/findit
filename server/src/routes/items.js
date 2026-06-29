const router = require('express').Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const db = require('../db');
const auth = require('../middleware/authMiddleware');
const { getHash, similarityScore } = require('../utils/imageHash');

// ─── Ensure uploads folder exists ────────────────────────────────────────────
const UPLOAD_DIR = path.join(__dirname, '../../uploads');
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

// ─── Multer config ────────────────────────────────────────────────────────────
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOAD_DIR),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, `${Date.now()}-${Math.random().toString(36).slice(2)}${ext}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB max
  fileFilter: (req, file, cb) => {
    const allowed = ['.jpg', '.jpeg', '.png', '.webp'];
    const ext = path.extname(file.originalname).toLowerCase();
    if (allowed.includes(ext)) {
      cb(null, true);
    } else {
      cb(new Error('Only jpg, jpeg, png, webp images are allowed'));
    }
  },
});

// ─── POST /items ──────────────────────────────────────────────────────────────
router.post('/', auth, upload.single('image'), async (req, res) => {
  const { type, title, description, category, location } = req.body;

  // Validate required fields
  if (!type || !title || !category || !location) {
    return res.status(400).json({
      error: 'Missing required fields: type, title, category, location',
    });
  }

  if (!['LOST', 'FOUND'].includes(type)) {
    return res.status(400).json({ error: 'type must be LOST or FOUND' });
  }

  const imageUrl = req.file
    ? `/uploads/${req.file.filename}`
    : null;

  try {
    // ── Step 1: Generate image hash ──────────────────────────────────────────
    let imageHash = null;
    if (req.file) {
      try {
        imageHash = await getHash(req.file.path);
        console.log(`[HASH] Generated for ${req.file.filename}:`, imageHash);
      } catch (hashErr) {
        // Don't block item creation if hashing fails — just log it
        console.error('[HASH] Failed to generate hash:', hashErr.message);
      }
    }

    // ── Step 2: Insert item ──────────────────────────────────────────────────
    const [result] = await db.execute(
      `INSERT INTO items 
        (user_id, type, title, description, category, image_url, image_hash, location) 
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [req.user.userId, type, title, description ?? null, category, imageUrl, imageHash, location]
    );

    const newItemId = result.insertId;
    console.log(`[ITEM] Created item #${newItemId} — type: ${type}, category: ${category}`);

    // ── Step 3: Auto-match against opposite type ─────────────────────────────
    let matchesCreated = 0;

    if (imageHash && category) {
      const oppositeType = type === 'LOST' ? 'FOUND' : 'LOST';

      const [candidates] = await db.execute(
        `SELECT id, image_hash 
         FROM items 
         WHERE type = ? 
           AND category = ? 
           AND status = 'OPEN' 
           AND image_hash IS NOT NULL
           AND id != ?`,
        [oppositeType, category, newItemId]
      );

      console.log(`[MATCH] Comparing against ${candidates.length} ${oppositeType} candidates`);

      for (const candidate of candidates) {
        const score = similarityScore(imageHash, candidate.image_hash);
        console.log(`[MATCH] vs item #${candidate.id} → score: ${(score * 100).toFixed(1)}%`);

        if (score >= 0.6) {
          const lostId  = type === 'LOST'  ? newItemId : candidate.id;
          const foundId = type === 'FOUND' ? newItemId : candidate.id;

          await db.execute(
            `INSERT INTO matches (lost_item_id, found_item_id, similarity_score) 
             VALUES (?, ?, ?)`,
            [lostId, foundId, score]
          );

          matchesCreated++;
          console.log(`[MATCH] ✓ Match created — lost #${lostId} ↔ found #${foundId} (${(score * 100).toFixed(1)}%)`);
        }
      }
    } else {
      console.log('[MATCH] Skipped — no image hash or category missing');
    }

    return res.status(201).json({
      message: 'Item posted successfully',
      itemId: newItemId,
      matchesFound: matchesCreated,
    });
  } catch (err) {
    console.error('[ERROR] POST /items:', err.message);
    return res.status(500).json({ error: 'Internal server error', detail: err.message });
  }
});

// ─── GET /items ───────────────────────────────────────────────────────────────
router.get('/', async (req, res) => {
  try {
    const { type, category } = req.query;

    let query = 'SELECT * FROM items WHERE status = "OPEN"';
    const params = [];

    if (type) {
      if (!['LOST', 'FOUND'].includes(type)) {
        return res.status(400).json({ error: 'type must be LOST or FOUND' });
      }
      query += ' AND type = ?';
      params.push(type);
    }

    if (category) {
      query += ' AND category = ?';
      params.push(category);
    }

    query += ' ORDER BY created_at DESC';

    const [rows] = await db.execute(query, params);
    return res.json({ count: rows.length, items: rows });
  } catch (err) {
    console.error('[ERROR] GET /items:', err.message);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── GET /items/:id ───────────────────────────────────────────────────────────
router.get('/:id', async (req, res) => {
  try {
    const [rows] = await db.execute('SELECT * FROM items WHERE id = ?', [req.params.id]);
    if (!rows.length) {
      return res.status(404).json({ error: `Item #${req.params.id} not found` });
    }
    return res.json(rows[0]);
  } catch (err) {
    console.error('[ERROR] GET /items/:id:', err.message);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── GET /items/:id/matches ───────────────────────────────────────────────────
router.get('/:id/matches', auth, async (req, res) => {
  try {
    const itemId = req.params.id;

    // Verify item exists first
    const [itemRows] = await db.execute('SELECT id FROM items WHERE id = ?', [itemId]);
    if (!itemRows.length) {
      return res.status(404).json({ error: `Item #${itemId} not found` });
    }

    const [rows] = await db.execute(
      `SELECT 
         m.id,
         m.similarity_score,
         m.status,
         m.created_at,
         l.id         AS lost_item_id,
         l.title      AS lost_title,
         l.image_url  AS lost_image,
         l.location   AS lost_location,
         f.id         AS found_item_id,
         f.title      AS found_title,
         f.image_url  AS found_image,
         f.location   AS found_location
       FROM matches m
       JOIN items l ON m.lost_item_id  = l.id
       JOIN items f ON m.found_item_id = f.id
       WHERE (m.lost_item_id = ? OR m.found_item_id = ?)
         AND m.status = 'PENDING'
       ORDER BY m.similarity_score DESC`,
      [itemId, itemId]
    );

    console.log(`[MATCH] GET /items/${itemId}/matches → ${rows.length} matches`);
    return res.json({ count: rows.length, matches: rows });
  } catch (err) {
    console.error('[ERROR] GET /items/:id/matches:', err.message);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── PUT /items/:id/resolve ───────────────────────────────────────────────────
router.put('/:id/resolve', auth, async (req, res) => {
  try {
    const [result] = await db.execute(
      'UPDATE items SET status = "RESOLVED" WHERE id = ? AND user_id = ?',
      [req.params.id, req.user.userId]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ error: 'Item not found or not owned by you' });
    }

    return res.json({ message: 'Item marked as resolved' });
  } catch (err) {
    console.error('[ERROR] PUT /items/:id/resolve:', err.message);
    return res.status(500).json({ error: 'Internal server error' });
  }
});

router.post('/test-upload', upload.single('image'), (req, res) => {
  console.log('[TEST] req.file:', req.file);
  console.log('[TEST] req.body:', req.body);
  res.json({
    fileReceived: !!req.file,
    file: req.file || null,
    body: req.body
  });
});

module.exports = router;