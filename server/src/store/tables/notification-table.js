// 通知表操作
const { getDb } = require('../db-init');

function findByUserId(userId, limit = 50) {
  return getDb().prepare(
    'SELECT * FROM notification WHERE user_id = ? ORDER BY created_at DESC LIMIT ?'
  ).all(userId, limit);
}

function create(userId, type, title, body, relatedId) {
  const result = getDb().prepare(`
    INSERT INTO notification (user_id, type, title, body, related_id, is_read, created_at)
    VALUES (?, ?, ?, ?, ?, 0, datetime('now'))
  `).run(userId, type, title, body || null, relatedId || null);
  return { id: result.lastInsertRowid, user_id: userId, type, title, body, related_id: relatedId };
}

function markRead(id, userId) {
  getDb().prepare('UPDATE notification SET is_read = 1 WHERE id = ? AND user_id = ?').run(id, userId);
}

function markAllRead(userId) {
  getDb().prepare('UPDATE notification SET is_read = 1 WHERE user_id = ?').run(userId);
}

function unreadCount(userId) {
  const row = getDb().prepare(
    'SELECT COUNT(*) as count FROM notification WHERE user_id = ? AND is_read = 0'
  ).get(userId);
  return row ? row.count : 0;
}

module.exports = { findByUserId, create, markRead, markAllRead, unreadCount };
