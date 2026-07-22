// 打卡路由
const { Router } = require('express');
const uuid = require('uuid');
const checkinTable = require('../../store/tables/checkin-table');
const taskTable = require('../../store/tables/task-table');
const userTable = require('../../store/tables/user-table');
const pairTable = require('../../store/tables/pair-table');
const makeupTable = require('../../store/tables/makeup-table');
const pointTable = require('../../store/tables/point-table');
const achievementTable = require('../../store/tables/achievement-table');
const commentTable = require('../../store/tables/comment-table');
const configTable = require('../../store/tables/config-table');
const notificationTable = require('../../store/tables/notification-table');
const auth = require('../../middleware/auth');
const { ApiError } = require('../../middleware/error-handler');
const { success, now, today, currentMonth, isInTimeWindow } = require('../../utils/helper');
const { sendPush } = require('../../utils/fcm');

const router = Router();

let getIO = null;
function setIO(fn) { getIO = fn; }

// ── 打卡记录列表 ──
router.get('/list', auth, (req, res) => {
  const { userId, date } = req.query;
  if (!userId) throw new ApiError(400, '用户 ID 不能为空');

  if (date) {
    return res.json(success({ records: checkinTable.findByUserAndDate(userId, date) }));
  }
  res.json(success({ records: checkinTable.findByUserId(userId) }));
});

// ── 今日打卡 ──
router.get('/today', auth, (req, res) => {
  const { userId, taskId } = req.query;
  const t = today();

  if (taskId) {
    const record = checkinTable.findTodayByTaskAndUser(taskId, userId, t);
    return res.json(success({ checkedIn: !!record, record }));
  }

  res.json(success({ records: checkinTable.findTodayByUser(userId, t) }));
});

// ── 创建打卡 ──
router.post('/create', auth, (req, res) => {
  const { recordId, taskId, userId, note, imageUrl, checkinTime } = req.body;
  if (!taskId || !userId) throw new ApiError(400, '任务 ID 和用户 ID 不能为空');

  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');

  // 时间窗校验
  if (!isInTimeWindow(task.start_time, task.end_time)) {
    throw new ApiError(400, `当前不在打卡时间窗内（${task.start_time || '不限'}~${task.end_time || '不限'}）`);
  }

  // 每日限一次
  const t = today();
  const existing = checkinTable.findTodayByTaskAndUser(taskId, userId, t);
  if (existing) throw new ApiError(409, '今日已打卡，请勿重复打卡');

  // 创建打卡记录
  const record = checkinTable.create({
    record_id: recordId || uuid.v4(),
    task_id: taskId,
    user_id: userId,
    checkin_time: checkinTime || now(),
    note: note || null,
    image_url: imageUrl || null,
    is_makeup: 0,
  });

  // 积分分配
  const totalPoints = task.point_per_check || 10;
  const userConfig = configTable.getConfig(userId);
  const personalPoints = Math.floor(totalPoints * userConfig.personalRatio);
  const poolPoints = totalPoints - personalPoints;

  const updatedPoints = userTable.updatePoints(userId, personalPoints, poolPoints);

  // 积分流水
  if (personalPoints > 0) {
    pointTable.create({ userId, amount: personalPoints, type: 'EARN', category: 'CHECKIN', description: `打卡: ${task.name}` });
  }
  if (poolPoints > 0) {
    pointTable.create({ userId, amount: poolPoints, type: 'EARN', category: 'CHECKIN_POOL', description: `奖励池: ${task.name}` });
  }

  // 同步搭档奖励池
  const pair = pairTable.findBoundByUserId(userId);
  if (pair) {
    const partnerId = pair.user_a === userId ? pair.user_b : pair.user_a;
    userTable.setPoolPoints(partnerId, updatedPoints.poolPoints);

    // 通知搭档
    sendPush(partnerId, '搭档打卡了！', `完成了「${task.name}」打卡`, record.record_id, 'checkin');
  }

    // 断签惩罚检查
    if (userConfig.streakPenaltyOn && userConfig.streakPenaltyDays > 0) {
      const dates = checkinTable.findDistinctDaysSince(userId, '2000-01-01');
      if (dates.length > 0) {
        const lastDate = new Date(dates[0].date);
        const todayDate = new Date(today());
        const diffDays = Math.floor((todayDate - lastDate) / 86400000);
        if (diffDays > 1 && diffDays >= userConfig.streakPenaltyDays) {
          const penalty = Math.min(10, userTable.getPoints(userId)?.poolPoints || 0);
          if (penalty > 0) {
            userTable.updatePoints(userId, 0, -penalty);
            pointTable.create({ userId, amount: penalty, type: 'SPEND', category: 'PENALTY',
              description: `断签惩罚：连续${diffDays}天未打卡` });
            notificationTable.create(userId, 'penalty', '断签惩罚',
              `连续${diffDays}天未打卡，扣除奖励池${penalty}分`, null);
          }
        }
      }
    }

    // 一次性任务完成后自动标记
    if (task.frequency === 'ONCE') {
      taskTable.markDone(taskId);
    }

  // 成就检查
  const totalDays = checkinTable.countTotalDays(userId);
  const streak = calcStreak(userId);
  achievementTable.checkTotalDaysAchievement(userId, totalDays);
  achievementTable.checkStreakAchievements(userId, streak);

  // WebSocket 广播
  if (getIO) {
    const { broadcastPointsUpdate } = require('../../ws/socket-handler');
    broadcastPointsUpdate(getIO(), userId, updatedPoints);
  }

  res.status(201).json(success({
    record,
    points: updatedPoints,
    personalPointsEarned: personalPoints,
    poolPointsEarned: poolPoints,
  }, '打卡成功'));
});

// ── 删除打卡（撤回） ──
router.delete('/delete/:recordId', auth, (req, res) => {
  const record = checkinTable.findByRecordId(req.params.recordId);
  if (!record) throw new ApiError(404, '打卡记录不存在');

  const task = taskTable.findById(record.task_id);
  const totalPoints = task ? task.point_per_check : 10;
  const userConfig = configTable.getConfig(record.user_id);
  const personalPoints = Math.floor(totalPoints * userConfig.personalRatio);
  const poolPoints = totalPoints - personalPoints;

  checkinTable.deleteByRecordId(req.params.recordId);
  userTable.updatePoints(record.user_id, -personalPoints, -poolPoints);

  pointTable.create({ userId: record.user_id, amount: personalPoints, type: 'SPEND', category: 'REVERT', description: `撤回打卡: ${task ? task.name : '未知'}` });

  const pair = pairTable.findBoundByUserId(record.user_id);
  if (pair) {
    const partnerId = pair.user_a === record.user_id ? pair.user_b : pair.user_a;
    userTable.setPoolPoints(partnerId, userTable.getPoints(record.user_id).poolPoints);
  }

  res.json(success(null, '打卡已撤回，积分已扣除'));
});

// ── 补签卡查询 ──
router.get('/makeup-cards', auth, (req, res) => {
  const month = new Date().toISOString().slice(0, 7);
  const remaining = makeupTable.remainingCards(req.userId, month);
  res.json(success({ month, remaining, total: 2 }));
});

// ── 补签卡打卡 ──
router.post('/makeup', auth, (req, res) => {
  const { taskId, userId, checkinDate, note, imageUrl } = req.body;
  if (!taskId || !userId || !checkinDate) throw new ApiError(400, '参数不完整');

  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');

  const month = checkinDate.slice(0, 7);
  const remaining = makeupTable.remainingCards(userId, month);
  if (remaining <= 0) throw new ApiError(400, '本月补签卡已用完');

  makeupTable.useCard(userId, month);

  const record = checkinTable.create({
    record_id: uuid.v4(),
    task_id: taskId,
    user_id: userId,
    checkin_time: `${checkinDate}T12:00:00.000Z`,
    note: note || null,
    image_url: imageUrl || null,
    is_makeup: 1,
  });

  res.status(201).json(success({ record, remainingCards: remaining - 1 }, '补签成功'));
});

// ── 留言回复 ──
router.post('/:id/comment', auth, (req, res) => {
  const { content } = req.body;
  if (!content) throw new ApiError(400, '留言内容不能为空');
  const record = checkinTable.findByRecordId(req.params.id);
  if (!record) throw new ApiError(404, '打卡记录不存在');

  const comment = commentTable.create(req.params.id, req.userId, content);

  // 通知对方
  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    sendPush(partnerId, '新回复', `${userTable.findById(req.userId)?.username} 回复了你的打卡`, req.params.id, 'comment');
  }

  res.json(success({ comment }));
});

// ── 查看留言 ──
router.get('/:id/comments', auth, (req, res) => {
  res.json(success({ comments: commentTable.findByRecordId(req.params.id) }));
});

// ── 辅助：计算连续打卡天数 ──
function calcStreak(userId) {
  const todayDate = today();
  const dates = checkinTable.findDistinctDaysSince(userId, '2000-01-01');
  if (dates.length === 0) return 0;

  let streak = 0;
  const dateSet = new Set(dates.map(d => d.date));

  // 从今天开始往回数
  let current = new Date(todayDate);
  if (!dateSet.has(todayDate)) {
    current.setDate(current.getDate() - 1);
    if (!dateSet.has(current.toISOString().split('T')[0])) return 0;
  }

  while (streak < 365) {
    const ds = current.toISOString().split('T')[0];
    if (!dateSet.has(ds)) break;
    streak++;
    current.setDate(current.getDate() - 1);
  }

  return streak;
}

module.exports = { router, setIO, calcStreak };
