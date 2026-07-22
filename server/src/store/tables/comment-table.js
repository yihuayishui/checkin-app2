// 打卡留言表操作
const { getDb } = require('../db-init');

function findByRecordId(recordId) {
  return getDb().prepare(
    'SELECT c.*, u.username FROM checkin_comment c JOIN user u ON c.user_id = u.id WHERE c.record_id = ? ORDER BY c.created_at ASC'
  ).all(recordId);
}

function create(recordId, userId, content) {
  const result = getDb().prepare(
    "INSERT INTO checkin_comment (record_id, user_id, content, created_at) VALUES (?, ?, ?, datetime('now'))"
  ).run(recordId, userId, content);
  return { id: result.lastInsertRowid, record_id: recordId, user_id: userId, content };
}

module.exports = { findByRecordId, create };
