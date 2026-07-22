// 积分流水表操作
const { getDb } = require('../db-init');
const uuid = require('uuid');

function findByUserId(userId, limit = 100, offset = 0) {
  return getDb().prepare(
    'SELECT * FROM point_transaction WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?'
  ).all(userId, limit, offset);
}

function create({ userId, partnerId, amount, type, category, description }) {
  const txId = uuid.v4();
  getDb().prepare(`
    INSERT INTO point_transaction (transaction_id, user_id, partner_id, amount, type, category, description, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
  `).run(txId, userId, partnerId || null, amount, type, category || 'CHECKIN', description || null);
  return { transaction_id: txId, user_id: userId, amount, type, category, description };
}

module.exports = { findByUserId, create };
