// 奖励表操作
const { getDb } = require('../db-init');

function findById(id) {
  return getDb().prepare('SELECT * FROM reward WHERE id = ?').get(id);
}

function findByCreatorId(creatorId) {
  return getDb().prepare(
    'SELECT * FROM reward WHERE creator_id = ? ORDER BY created_at DESC'
  ).all(creatorId);
}

function findAll() {
  return getDb().prepare(
    "SELECT * FROM reward WHERE status != 'DELETED' ORDER BY created_at DESC"
  ).all();
}

/**
 * 按创作者 ID 列表查询奖励
 * @param {string[]} creatorIds
 */
function findByCreatorIds(creatorIds) {
  if (!creatorIds || creatorIds.length === 0) return [];
  const placeholders = creatorIds.map(() => '?').join(',');
  return getDb().prepare(
    `SELECT * FROM reward WHERE status != 'DELETED' AND creator_id IN (${placeholders}) ORDER BY created_at DESC`
  ).all(...creatorIds);
}

function create(reward) {
  getDb().prepare(`
    INSERT INTO reward (id, creator_id, name, required_points, expires_at, status, created_at, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
  `).run(reward.id, reward.creator_id, reward.name, reward.required_points,
    reward.expires_at || null, reward.status || 'ACTIVE');
  return findById(reward.id);
}

function update(reward) {
  getDb().prepare(`
    UPDATE reward SET name=?, required_points=?, expires_at=?, status=?, applicant_id=?, updated_at=datetime('now') WHERE id=?
  `).run(reward.name, reward.required_points, reward.expires_at,
    reward.status, reward.applicant_id || null, reward.id);
  return findById(reward.id);
}

function remove(id) {
  getDb().prepare('DELETE FROM reward WHERE id = ?').run(id);
}

/** 申请兑换 */
function applyExchange(rewardId, userId) {
  getDb().prepare(
    "UPDATE reward SET applicant_id = ?, status = 'PENDING', updated_at = datetime('now') WHERE id = ?"
  ).run(userId, rewardId);
  return findById(rewardId);
}

/** 撤回兑换申请 */
function cancelExchange(rewardId) {
  getDb().prepare(
    "UPDATE reward SET applicant_id = NULL, status = 'ACTIVE', updated_at = datetime('now') WHERE id = ?"
  ).run(rewardId);
  return findById(rewardId);
}

/** 同意/拒绝兑换 */
function confirmExchange(rewardId, accepted) {
  const status = accepted ? 'CONFIRMED' : 'ACTIVE';
  const sql = accepted
    ? "UPDATE reward SET status = 'CONFIRMED', updated_at = datetime('now') WHERE id = ?"
    : "UPDATE reward SET applicant_id = NULL, status = 'ACTIVE', updated_at = datetime('now') WHERE id = ?";
  getDb().prepare(sql).run(rewardId);
  return findById(rewardId);
}

/** 标记已兑现 */
function markClaimed(rewardId) {
  getDb().prepare(
    "UPDATE reward SET status = 'CLAIMED', updated_at = datetime('now') WHERE id = ?"
  ).run(rewardId);
  return findById(rewardId);
}

module.exports = {
  findById, findByCreatorId, findByCreatorIds, findAll, create, update, remove,
  applyExchange, cancelExchange, confirmExchange, markClaimed,
};
