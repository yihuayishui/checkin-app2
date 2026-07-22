// 增量同步路由
const { Router } = require('express');
const checkinTable = require('../../store/tables/checkin-table');
const taskTable = require('../../store/tables/task-table');
const rewardTable = require('../../store/tables/reward-table');
const pairTable = require('../../store/tables/pair-table');
const auth = require('../../middleware/auth');
const { success, now } = require('../../utils/helper');

const router = Router();

router.get('/', auth, (req, res) => {
  const { since } = req.query;
  const sinceTime = since || '1970-01-01T00:00:00.000Z';

  const checkins = checkinTable.findSince(req.userId, sinceTime);
  const tasks = taskTable.findByUserId(req.userId);

  // 搭档数据
  const pair = pairTable.findBoundByUserId(req.userId);
  let partnerCheckins = [];
  let partnerTasks = [];
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    partnerCheckins = checkinTable.findSince(partnerId, sinceTime);
    partnerTasks = taskTable.findByUserId(partnerId);
  }

  const rewards = rewardTable.findAll();

  res.json(success({
    timestamp: now(),
    myData: { checkins, tasks },
    partnerData: { checkins: partnerCheckins, tasks: partnerTasks },
    rewards,
  }, '同步完成'));
});

module.exports = router;
