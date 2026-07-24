// 成就表操作
const { getDb } = require('../db-init');

// 成就定义
const ACHIEVEMENTS = {
  streak_7: { code: 'streak_7', name: '坚持一周', icon: '🔥', group: 'personal', desc: '连续打卡 7 天' },
  streak_14: { code: 'streak_14', name: '半月达人', icon: '🔥🔥', group: 'personal', desc: '连续打卡 14 天' },
  streak_30: { code: 'streak_30', name: '满月勇士', icon: '🔥🔥🔥', group: 'personal', desc: '连续打卡 30 天' },
  total_100: { code: 'total_100', name: '百日打卡', icon: '🏆', group: 'personal', desc: '累计打卡 100 天' },
  streak_180: { code: 'streak_180', name: '半年连续打卡', icon: '🌟', group: 'personal', desc: '连续打卡 180 天' },
  streak_365: { code: 'streak_365', name: '一年连续打卡', icon: '💫', group: 'personal', desc: '连续打卡 365 天' },
  same_day_7: { code: 'same_day_7', name: '心有灵犀', icon: '💑', group: 'partner', desc: '双方同天打卡累计 7 次' },
  same_day_30: { code: 'same_day_30', name: '同频搭档', icon: '💞', group: 'partner', desc: '双方同天打卡累计 30 次' },
  same_day_100: { code: 'same_day_100', name: '百天同行', icon: '💕', group: 'partner', desc: '双方同天打卡累计 100 次' },
  same_day_180: { code: 'same_day_180', name: '半年同步', icon: '💗', group: 'partner', desc: '双方同天打卡累计 180 次' },
  same_day_365: { code: 'same_day_365', name: '一年同心', icon: '❤️', group: 'partner', desc: '双方同天打卡累计 365 次' },
};

function getDefinitions() {
  return Object.values(ACHIEVEMENTS);
}

function findByUserId(userId) {
  const unlocked = getDb().prepare(
    'SELECT code, unlocked_at FROM achievement WHERE user_id = ?'
  ).all(userId);
  return getDefinitions().map(def => {
    const u = unlocked.find(a => a.code === def.code);
    return { ...def, unlocked: !!u, unlockedAt: u ? u.unlocked_at : null };
  });
}

/** 解锁一个成就（幂等） */
function unlock(userId, code) {
  if (!ACHIEVEMENTS[code]) return false;
  try {
    getDb().prepare(
      "INSERT OR IGNORE INTO achievement (user_id, code, unlocked_at) VALUES (?, ?, datetime('now'))"
    ).run(userId, code);
    return getDb().changes > 0;
  } catch { return false; }
}

/** 检查并解锁连胜成就 */
function checkStreakAchievements(userId, streak) {
  if (streak >= 365) unlock(userId, 'streak_365');
  if (streak >= 180) unlock(userId, 'streak_180');
  if (streak >= 30) unlock(userId, 'streak_30');
  if (streak >= 14) unlock(userId, 'streak_14');
  if (streak >= 7) unlock(userId, 'streak_7');
}

/** 检查并解锁累计打卡成就 */
function checkTotalDaysAchievement(userId, totalDays) {
  if (totalDays >= 100) unlock(userId, 'total_100');
}

/** 检查并解锁同天打卡成就 */
function checkSameDayAchievements(userId, sameDayCount) {
  if (sameDayCount >= 365) unlock(userId, 'same_day_365');
  if (sameDayCount >= 180) unlock(userId, 'same_day_180');
  if (sameDayCount >= 100) unlock(userId, 'same_day_100');
  if (sameDayCount >= 30) unlock(userId, 'same_day_30');
  if (sameDayCount >= 7) unlock(userId, 'same_day_7');
}

module.exports = { getDefinitions, findByUserId, unlock, checkStreakAchievements, checkTotalDaysAchievement, checkSameDayAchievements };
