// 补签卡表操作
const { getDb } = require('../db-init');

function findByUserAndMonth(userId, month) {
  return getDb().prepare(
    'SELECT * FROM makeup_card WHERE user_id = ? AND month = ?'
  ).get(userId, month);
}

function upsert(userId, month, totalCards, usedCards) {
  const existing = findByUserAndMonth(userId, month);
  if (existing) {
    getDb().prepare(
      "UPDATE makeup_card SET total_cards = ?, used_cards = ?, last_updated = datetime('now') WHERE user_id = ? AND month = ?"
    ).run(totalCards, usedCards, userId, month);
  } else {
    getDb().prepare(
      "INSERT INTO makeup_card (user_id, total_cards, used_cards, month, last_updated) VALUES (?, ?, ?, ?, datetime('now'))"
    ).run(userId, totalCards, usedCards, month);
  }
  return findByUserAndMonth(userId, month);
}

/** 使用一张补签卡，返回是否成功 */
function useCard(userId, month) {
  const card = findByUserAndMonth(userId, month);
  if (!card || card.used_cards >= card.total_cards) return false;
  getDb().prepare(
    "UPDATE makeup_card SET used_cards = used_cards + 1, last_updated = datetime('now') WHERE user_id = ? AND month = ?"
  ).run(userId, month);
  return true;
}

/** 获取可用补签卡数量 */
function remainingCards(userId, month) {
  const card = findByUserAndMonth(userId, month);
  if (!card) return 2; // 默认每月2张
  return Math.max(0, card.total_cards - card.used_cards);
}

module.exports = { findByUserAndMonth, upsert, useCard, remainingCards };
