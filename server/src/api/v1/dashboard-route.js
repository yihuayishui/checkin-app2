// 看板/统计路由
const { Router } = require('express');
const checkinTable = require('../../store/tables/checkin-table');
const taskTable = require('../../store/tables/task-table');
const userTable = require('../../store/tables/user-table');
const pairTable = require('../../store/tables/pair-table');
const auth = require('../../middleware/auth');
const { success, today } = require('../../utils/helper');
const { calcStreak } = require('./checkin-route');

const router = Router();

// ── 首页看板 ──
router.get('/today', auth, (req, res) => {
  const t = today();
  const pair = pairTable.findBoundByUserId(req.userId);

  // 我的数据
  const myCheckins = checkinTable.findTodayByUser(req.userId, t);
  const myActiveTasks = taskTable.findByUserId(req.userId);

  // 加上今天打卡完成的一次性任务（虽然已标记DONE，但当天仍需展示）
  const todayOnceTaskIds = [
    ...new Set(
      myCheckins
        .filter(r => {
          const task = taskTable.findById(r.task_id);
          return task && task.frequency === 'ONCE';
        })
        .map(r => r.task_id)
    )
  ];
  const todayOnceTasks = todayOnceTaskIds
    .map(id => taskTable.findById(id))
    .filter(Boolean);

  // 合并去重（按 task_id 去重）
  const myTaskMap = new Map();
  myActiveTasks.forEach(t => myTaskMap.set(t.task_id, t));
  todayOnceTasks.forEach(t => myTaskMap.set(t.task_id, t));
  const myTasks = [...myTaskMap.values()];
  const myPoints = userTable.getPoints(req.userId);
  const myStreak = calcStreak(req.userId);

  // 构建今日任务打卡状态
  const todayTaskStatus = myTasks.map(task => {
    const approvedCheckin = myCheckins.find(
      r => r.task_id === task.task_id && r.status === 'APPROVED'
    );
    const pendingCheckin = myCheckins.find(
      r => r.task_id === task.task_id && r.status === 'PENDING'
    );
    return {
      task,
      checkedIn: !!approvedCheckin,
      pendingApproval: pendingCheckin ? {
        recordId: pendingCheckin.record_id,
      } : null,
    };
  });

  let partnerData = null;
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    const partnerCheckins = checkinTable.findTodayByUser(partnerId, t);
    const partnerActiveTasks = taskTable.findByUserId(partnerId);

    // 加上搭档今天打卡完成的一次性任务
    const partnerOnceTaskIds = [
      ...new Set(
        partnerCheckins
          .filter(r => {
            const task = taskTable.findById(r.task_id);
            return task && task.frequency === 'ONCE';
          })
          .map(r => r.task_id)
      )
    ];
    const partnerOnceTasks = partnerOnceTaskIds
      .map(id => taskTable.findById(id))
      .filter(Boolean);

    const partnerTaskMap = new Map();
    partnerActiveTasks.forEach(t => partnerTaskMap.set(t.task_id, t));
    partnerOnceTasks.forEach(t => partnerTaskMap.set(t.task_id, t));
    const partnerTasks = [...partnerTaskMap.values()];
    const partnerStreak = calcStreak(partnerId);
    const partnerUser = userTable.findById(partnerId);

    partnerData = {
      userId: partnerId,
      username: partnerUser ? partnerUser.username : '未知',
      isVacation: partnerUser ? !!partnerUser.is_vacation : false,
      checkins: partnerCheckins,
      streak: partnerStreak,
      todayTaskStatus: partnerTasks.map(task => {
        // 已确认的打卡（APPROVED 才算已打卡）
        const approvedCheckin = partnerCheckins.find(
          r => r.task_id === task.task_id && r.status === 'APPROVED'
        );
        // 待确认的打卡（需我同意）
        const pendingCheckin = partnerCheckins.find(
          r => r.task_id === task.task_id && r.status === 'PENDING'
        );

        return {
          task,
          checkedIn: !!approvedCheckin,
          pendingApproval: pendingCheckin ? {
            recordId: pendingCheckin.record_id,
          } : null,
        };
      }),
    };
  }

  // 最新动态
  const recentCheckins = checkinTable.findByUserId(req.userId, 3);

  res.json(success({
    todayDate: t,
    myData: {
      checkins: myCheckins,
      todayTaskStatus,
      streak: myStreak,
      personalPoints: myPoints ? myPoints.personalPoints : 0,
      poolPoints: myPoints ? myPoints.poolPoints : 0,
    },
    partnerData,
    recentCheckins,
  }));
});

// ── 打卡日历 ──
router.get('/calendar', auth, (req, res) => {
  const { month } = req.query;
  if (!month) return res.status(400).json({ code: 400, message: '月份不能为空' });

  const myDates = checkinTable.findDatesInMonth(req.userId, month).map(d => d.date);

  const pair = pairTable.findBoundByUserId(req.userId);
  const partnerDates = pair
    ? checkinTable.findDatesInMonth(pair.user_a === req.userId ? pair.user_b : pair.user_a, month).map(d => d.date)
    : [];

  res.json(success({ month, myDates, partnerDates }));
});

// ── 月度统计 ──
router.get('/stats', auth, (req, res) => {
  const { month } = req.query;
  const m = month || new Date().toISOString().slice(0, 7);

  const myDays = checkinTable.countDistinctDaysInMonth(req.userId, m);

  const pair = pairTable.findBoundByUserId(req.userId);
  const partnerDays = pair
    ? checkinTable.countDistinctDaysInMonth(pair.user_a === req.userId ? pair.user_b : pair.user_a, m)
    : 0;

  res.json(success({ month: m, myDays, partnerDays }));
});

// ── 连续打卡天数 ──
router.get('/streak', auth, (req, res) => {
  const myStreak = calcStreak(req.userId);

  const pair = pairTable.findBoundByUserId(req.userId);
  const partnerStreak = pair ? calcStreak(pair.user_a === req.userId ? pair.user_b : pair.user_a) : 0;

  res.json(success({ myStreak, partnerStreak }));
});

module.exports = router;
