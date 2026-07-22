// 搭档关系表操作
const { getDb } = require('../db-init');

function findById(id) {
  return getDb().prepare('SELECT * FROM pair WHERE id = ?').get(id);
}

/**
 * 查找用户的所有搭档关系（任何状态）
 */
function findByUserId(userId) {
  return getDb().prepare(
    'SELECT * FROM pair WHERE (user_a = ? OR user_b = ?) ORDER BY updated_at DESC LIMIT 1'
  ).all(userId, userId);
}

/**
 * 查找已绑定的搭档关系
 */
function findBoundByUserId(userId) {
  return getDb().prepare(
    "SELECT * FROM pair WHERE (user_a = ? OR user_b = ?) AND status = 'BOUND' LIMIT 1"
  ).get(userId, userId);
}

/**
 * 查找两个用户之间的已有关系
 */
function findExistingPair(userA, userB) {
  return getDb().prepare(
    'SELECT * FROM pair WHERE (user_a = ? AND user_b = ?) OR (user_a = ? AND user_b = ?) LIMIT 1'
  ).get(userA, userB, userB, userA);
}

function create(id, userA, userB, requestedBy) {
  getDb().prepare(
    "INSERT INTO pair (id, user_a, user_b, status, requested_by, created_at, updated_at) VALUES (?, ?, ?, 'PENDING', ?, datetime('now'), datetime('now'))"
  ).run(id, userA, userB, requestedBy);
  return findById(id);
}

function accept(pairId) {
  getDb().prepare("UPDATE pair SET status = 'BOUND', updated_at = datetime('now') WHERE id = ?").run(pairId);
  return findById(pairId);
}

function remove(pairId) {
  getDb().prepare('DELETE FROM pair WHERE id = ?').run(pairId);
}

function setUnbindRequest(pairId, userId) {
  getDb().prepare("UPDATE pair SET unbind_requested_by = ?, updated_at = datetime('now') WHERE id = ?")
    .run(userId, pairId);
}

function clearUnbindRequest(pairId) {
  getDb().prepare("UPDATE pair SET unbind_requested_by = NULL, updated_at = datetime('now') WHERE id = ?")
    .run(pairId);
}

module.exports = {
  findById, findByUserId, findBoundByUserId, findExistingPair,
  create, accept, remove, setUnbindRequest, clearUnbindRequest,
};
