// 引导状态表操作
const { getDb } = require('../db-init');

function isDone(userId) {
  const row = getDb().prepare('SELECT is_done FROM guide_status WHERE user_id = ?').get(userId);
  return row ? !!row.is_done : false;
}

function markDone(userId) {
  getDb().prepare(
    "INSERT OR REPLACE INTO guide_status (user_id, is_done) VALUES (?, 1)"
  ).run(userId);
}

/** 返回示例任务 */
function getSampleTasks(userId) {
  return [
    { name: '晨跑 30 分钟', frequency: 'DAILY', pointPerCheck: 10, startTime: '06:00', endTime: '09:00' },
    { name: '阅读 30 分钟', frequency: 'DAILY', pointPerCheck: 10, startTime: '08:00', endTime: '23:00' },
    { name: '早睡打卡', frequency: 'DAILY', pointPerCheck: 5, startTime: '21:00', endTime: '23:59' },
  ];
}

module.exports = { isDone, markDone, getSampleTasks };
