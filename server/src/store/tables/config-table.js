// 用户设置表操作
const { getDb } = require('../db-init');

function getConfig(userId) {
  let row = getDb().prepare('SELECT * FROM user_config WHERE user_id = ?').get(userId);
  if (!row) {
    // 创建默认配置
    getDb().prepare(`
      INSERT INTO user_config (user_id, reminder_time, notify_checkin, notify_reward, notify_pair, personal_ratio, pool_ratio, streak_penalty_on, streak_penalty_days, theme_mode)
      VALUES (?, NULL, 1, 1, 1, 0.5, 0.5, 0, 0, 'system')
    `).run(userId);
    row = getDb().prepare('SELECT * FROM user_config WHERE user_id = ?').get(userId);
  }
  return {
    reminderTime: row.reminder_time,
    notifyCheckin: !!row.notify_checkin,
    notifyReward: !!row.notify_reward,
    notifyPair: !!row.notify_pair,
    personalRatio: row.personal_ratio,
    poolRatio: row.pool_ratio,
    streakPenaltyOn: !!row.streak_penalty_on,
    streakPenaltyDays: row.streak_penalty_days,
    themeMode: row.theme_mode,
  };
}

function updateConfig(userId, config) {
  const fields = [];
  const values = [];

  if (config.reminderTime !== undefined) { fields.push('reminder_time = ?'); values.push(config.reminderTime); }
  if (config.notifyCheckin !== undefined) { fields.push('notify_checkin = ?'); values.push(config.notifyCheckin ? 1 : 0); }
  if (config.notifyReward !== undefined) { fields.push('notify_reward = ?'); values.push(config.notifyReward ? 1 : 0); }
  if (config.notifyPair !== undefined) { fields.push('notify_pair = ?'); values.push(config.notifyPair ? 1 : 0); }
  if (config.personalRatio !== undefined) { fields.push('personal_ratio = ?'); values.push(config.personalRatio); }
  if (config.poolRatio !== undefined) { fields.push('pool_ratio = ?'); values.push(config.poolRatio); }
  if (config.streakPenaltyOn !== undefined) { fields.push('streak_penalty_on = ?'); values.push(config.streakPenaltyOn ? 1 : 0); }
  if (config.streakPenaltyDays !== undefined) { fields.push('streak_penalty_days = ?'); values.push(config.streakPenaltyDays); }
  if (config.themeMode !== undefined) { fields.push('theme_mode = ?'); values.push(config.themeMode); }

  if (fields.length === 0) return getConfig(userId);

  values.push(userId);
  getDb().prepare(`UPDATE user_config SET ${fields.join(', ')} WHERE user_id = ?`).run(...values);
  return getConfig(userId);
}

module.exports = { getConfig, updateConfig };
