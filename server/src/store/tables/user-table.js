// 用户表操作
const { getDb } = require('../db-init');

function findByUsername(username) {
  return getDb().prepare('SELECT * FROM user WHERE username = ?').get(username);
}

function findById(id) {
  return getDb().prepare('SELECT * FROM user WHERE id = ?').get(id);
}

function create(id, username, hashedPassword) {
  getDb().prepare(
    "INSERT INTO user (id, username, password, personal_points, pool_points, created_at) VALUES (?, ?, ?, 0, 0, datetime('now'))"
  ).run(id, username, hashedPassword);
  return findById(id);
}

function search(query, excludeUserId) {
  return getDb().prepare(
    "SELECT id, username, avatar_url, created_at FROM user WHERE LOWER(username) LIKE LOWER(?) AND id != ? ORDER BY username LIMIT 20"
  ).all(`%${query}%`, excludeUserId);
}

function getPoints(userId) {
  const row = getDb().prepare('SELECT personal_points, pool_points FROM user WHERE id = ?').get(userId);
  return row ? { personalPoints: row.personal_points, poolPoints: row.pool_points } : null;
}

function updatePoints(userId, personalDelta, poolDelta) {
  getDb().prepare(
    'UPDATE user SET personal_points = MAX(0, personal_points + ?), pool_points = MAX(0, pool_points + ?), updated_at = datetime(\'now\') WHERE id = ?'
  ).run(personalDelta, poolDelta, userId);
  return getPoints(userId);
}

function setPoints(userId, personalPoints, poolPoints) {
  getDb().prepare(
    'UPDATE user SET personal_points = ?, pool_points = ?, updated_at = datetime(\'now\') WHERE id = ?'
  ).run(Math.max(0, personalPoints), Math.max(0, poolPoints), userId);
  return getPoints(userId);
}

function setPoolPoints(userId, poolPoints) {
  getDb().prepare(
    'UPDATE user SET pool_points = ?, updated_at = datetime(\'now\') WHERE id = ?'
  ).run(Math.max(0, poolPoints), userId);
}

function deductPoolPoints(userId, amount) {
  const user = findById(userId);
  if (!user || user.pool_points < amount) return false;
  getDb().prepare('UPDATE user SET pool_points = pool_points - ?, updated_at = datetime(\'now\') WHERE id = ?')
    .run(amount, userId);
  return true;
}

function updatePassword(userId, hashedPassword) {
  getDb().prepare("UPDATE user SET password = ?, updated_at = datetime('now') WHERE id = ?")
    .run(hashedPassword, userId);
}

function updateAvatar(userId, avatarUrl) {
  getDb().prepare("UPDATE user SET avatar_url = ?, updated_at = datetime('now') WHERE id = ?")
    .run(avatarUrl, userId);
}

function updateFcmToken(userId, token) {
  getDb().prepare("UPDATE user SET fcm_token = ?, updated_at = datetime('now') WHERE id = ?")
    .run(token, userId);
}

function getFcmToken(userId) {
  const row = getDb().prepare('SELECT fcm_token FROM user WHERE id = ?').get(userId);
  return row ? row.fcm_token : null;
}

function setVacation(userId, isVacation) {
  getDb().prepare("UPDATE user SET is_vacation = ?, updated_at = datetime('now') WHERE id = ?")
    .run(isVacation ? 1 : 0, userId);
}

function deleteUser(userId) {
  getDb().prepare('DELETE FROM user WHERE id = ?').run(userId);
}

module.exports = {
  findByUsername, findById, create, search,
  getPoints, updatePoints, setPoints, setPoolPoints, deductPoolPoints,
  updatePassword, updateAvatar, updateFcmToken, getFcmToken,
  setVacation, deleteUser,
};
