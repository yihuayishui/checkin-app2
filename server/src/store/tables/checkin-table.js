// 打卡记录表操作
const { getDb } = require('../db-init');

function findById(id) {
  return getDb().prepare('SELECT * FROM checkin_record WHERE id = ?').get(id);
}

function findByRecordId(recordId) {
  return getDb().prepare('SELECT * FROM checkin_record WHERE record_id = ?').get(recordId);
}

function findByUserId(userId, limit = 50) {
  return getDb().prepare(
    'SELECT * FROM checkin_record WHERE user_id = ? ORDER BY checkin_time DESC LIMIT ?'
  ).all(userId, limit);
}

function findByUserAndDate(userId, dateStr) {
  return getDb().prepare(
    "SELECT * FROM checkin_record WHERE user_id = ? AND checkin_time LIKE ? ORDER BY checkin_time"
  ).all(userId, `${dateStr}%`);
}

/** 查询某任务今天是否已打卡 */
function findTodayByTaskAndUser(taskId, userId, dateStr) {
  return getDb().prepare(
    "SELECT * FROM checkin_record WHERE task_id = ? AND user_id = ? AND checkin_time LIKE ? AND is_makeup = 0 LIMIT 1"
  ).get(taskId, userId, `${dateStr}%`);
}

function findTodayByUser(userId, dateStr) {
  return getDb().prepare(
    "SELECT * FROM checkin_record WHERE user_id = ? AND checkin_time LIKE ? ORDER BY checkin_time"
  ).all(userId, `${dateStr}%`);
}

function create(record) {
  getDb().prepare(`
    INSERT INTO checkin_record (record_id, task_id, user_id, checkin_time, note, image_url, is_makeup, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
  `).run(record.record_id, record.task_id, record.user_id, record.checkin_time,
    record.note || null, record.image_url || null, record.is_makeup || 0);
  return findByRecordId(record.record_id);
}

/** 删除打卡记录（撤回打卡） */
function deleteByRecordId(recordId) {
  getDb().prepare('DELETE FROM checkin_record WHERE record_id = ?').run(recordId);
}

/** 某月不重复打卡天数 */
function countDistinctDaysInMonth(userId, monthStr) {
  const row = getDb().prepare(
    "SELECT COUNT(DISTINCT substr(checkin_time, 1, 10)) as days FROM checkin_record WHERE user_id = ? AND checkin_time LIKE ?"
  ).get(userId, `${monthStr}%`);
  return row ? row.days : 0;
}

/** 查询用户在指定时间段内的打卡日期列表（用于计算连续天数） */
function findDistinctDaysSince(userId, sinceDate) {
  return getDb().prepare(
    "SELECT DISTINCT substr(checkin_time, 1, 10) as date FROM checkin_record WHERE user_id = ? AND checkin_time >= ? ORDER BY date DESC"
  ).all(userId, sinceDate);
}

/** 查询有打卡记录的所有日期（日历用） */
function findDatesInMonth(userId, monthStr) {
  return getDb().prepare(
    "SELECT DISTINCT substr(checkin_time, 1, 10) as date FROM checkin_record WHERE user_id = ? AND checkin_time LIKE ?"
  ).all(userId, `${monthStr}%`);
}

/** 增量同步 */
function findSince(userId, sinceTimestamp) {
  return getDb().prepare(
    'SELECT * FROM checkin_record WHERE user_id = ? AND created_at > ? ORDER BY created_at ASC'
  ).all(userId, sinceTimestamp);
}

/** 累计打卡天数 */
function countTotalDays(userId) {
  const row = getDb().prepare(
    'SELECT COUNT(DISTINCT substr(checkin_time, 1, 10)) as total FROM checkin_record WHERE user_id = ?'
  ).get(userId);
  return row ? row.total : 0;
}

/** 某天双方都打卡了 */
function countSameDayWithPartner(userId, partnerId) {
  const row = getDb().prepare(`
    SELECT COUNT(*) as count FROM (
      SELECT substr(a.checkin_time, 1, 10) as day FROM checkin_record a
      WHERE a.user_id = ?
      AND EXISTS (SELECT 1 FROM checkin_record b WHERE b.user_id = ? AND substr(b.checkin_time, 1, 10) = day)
      GROUP BY day
    )
  `).get(userId, partnerId);
  return row ? row.count : 0;
}

module.exports = {
  findById, findByRecordId, findByUserId, findByUserAndDate,
  findTodayByTaskAndUser, findTodayByUser, create, deleteByRecordId,
  countDistinctDaysInMonth, findDistinctDaysSince, findDatesInMonth,
  findSince, countTotalDays, countSameDayWithPartner,
};
