// 任务表操作
const { getDb } = require('../db-init');

function findById(taskId) {
  return getDb().prepare('SELECT * FROM task WHERE task_id = ?').get(taskId);
}

/** 按用户ID查找（我需要做的任务） */
function findByUserId(userId) {
  return getDb().prepare(
    "SELECT * FROM task WHERE user_id = ? AND status != 'DONE' ORDER BY created_at DESC"
  ).all(userId);
}

/** 按创建者ID查找（我创建的） */
function findByCreatorId(creatorId) {
  return getDb().prepare(
    'SELECT * FROM task WHERE creator_id = ? ORDER BY created_at DESC'
  ).all(creatorId);
}

/** 我创建、分配给搭档的任务 */
function findCreatedForPartner(creatorId, partnerId) {
  return getDb().prepare(
    'SELECT * FROM task WHERE creator_id = ? AND user_id = ? ORDER BY created_at DESC'
  ).all(creatorId, partnerId);
}

/** 对方创建、分配给我的任务 */
function findCreatedByPartner(userId, partnerId) {
  return getDb().prepare(
    'SELECT * FROM task WHERE creator_id = ? AND user_id = ? ORDER BY created_at DESC'
  ).all(partnerId, userId);
}

function create(task) {
  getDb().prepare(`
    INSERT INTO task (task_id, user_id, creator_id, name, frequency, point_per_check, start_time, end_time, is_active, status, require_approval, created_at, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
  `).run(
    task.task_id, task.user_id, task.creator_id, task.name,
    task.frequency || 'DAILY', task.point_per_check || 10,
    task.start_time || null, task.end_time || null,
    task.is_active !== undefined ? (task.is_active ? 1 : 0) : 1,
    task.status || 'ACTIVE',
    task.require_approval ? 1 : 0
  );
  return findById(task.task_id);
}

function update(task) {
  getDb().prepare(`
    UPDATE task SET name=?, frequency=?, point_per_check=?, start_time=?, end_time=?, is_active=?, require_approval=?, updated_at=datetime('now')
    WHERE task_id=?
  `).run(task.name, task.frequency, task.point_per_check, task.start_time, task.end_time, task.is_active, task.require_approval ? 1 : 0, task.task_id);
  return findById(task.task_id);
}

function deactivate(taskId) {
  getDb().prepare("UPDATE task SET is_active = 0, updated_at = datetime('now') WHERE task_id = ?").run(taskId);
}

function remove(taskId) {
  getDb().prepare('DELETE FROM task WHERE task_id = ?').run(taskId);
}

/** 一次性任务标记为已完成 */
function markDone(taskId) {
  getDb().prepare("UPDATE task SET status = 'DONE', is_active = 0, updated_at = datetime('now') WHERE task_id = ?").run(taskId);
}

/** 重新激活已完成任务 */
function reactivate(taskId) {
  getDb().prepare("UPDATE task SET status = 'ACTIVE', is_active = 1, updated_at = datetime('now') WHERE task_id = ?").run(taskId);
  return findById(taskId);
}

module.exports = {
  findById, findByUserId, findByCreatorId,
  findCreatedForPartner, findCreatedByPartner,
  create, update, deactivate, remove, markDone, reactivate,
};
