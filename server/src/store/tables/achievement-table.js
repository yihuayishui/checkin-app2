// 成就表操作
const { getDb } = require('../db-init');

// 成就定义
const ACHIEVEMENTS = {
  first_checkin: { code: 'first_checkin', name: '初次打卡', icon: '🥇', desc: '完成第一次打卡' },
  streak_7: { code: 'streak_7', name: '坚持一周', icon: '🔥', desc: '连续打卡 7 天' },
  streak_14: { code: 'streak_14', name: '半月达人', icon: '🔥🔥', desc: '连续打卡 14 天' },
  streak_30: { code: 'streak_30', name: '满月勇士', icon: '🔥🔥🔥', desc: '连续打卡 30 天' },
  same_day_7: { code: 'same_day_7', name: '心有灵犀', icon: '💑', desc: '双方同天打卡累计 7 次' },
  first_reward: { code: 'first_reward', name: '第一次兑换', icon: '🎁', desc: '成功兑换第一个奖励' },
  total_100: { code: 'total_100', name: '百日打卡', icon: '🏆', desc: '累计打卡 100 天' },
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
  if (streak >= 30) unlock(userId, 'streak_30');
  if (streak >= 14) unlock(userId, 'streak_14');
  if (streak >= 7) unlock(userId, 'streak_7');
}

/** 检查并解锁累计成就 */
function checkTotalDaysAchievement(userId, totalDays) {
  if (totalDays >= 100) unlock(userId, 'total_100');
  if (totalDays >= 1) unlock(userId, 'first_checkin');
}

module.exports = { getDefinitions, findByUserId, unlock, checkStreakAchievements, checkTotalDaysAchievement };
