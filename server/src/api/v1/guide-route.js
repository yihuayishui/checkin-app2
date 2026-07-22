// 引导路由
const { Router } = require('express');
const uuid = require('uuid');
const guideTable = require('../../store/tables/guide-table');
const taskTable = require('../../store/tables/task-table');
const auth = require('../../middleware/auth');
const { success } = require('../../utils/helper');

const router = Router();

// ── 获取示例任务 ──
router.get('/tasks', auth, (req, res) => {
  if (guideTable.isDone(req.userId)) {
    return res.json(success({ isDone: true, tasks: [] }));
  }
  const tasks = guideTable.getSampleTasks(req.userId);
  res.json(success({ isDone: false, tasks }));
});

// ── 完成引导（自动创建示例任务） ──
router.post('/skip', auth, (req, res) => {
  guideTable.markDone(req.userId);
  res.json(success(null, '引导已完成'));
});

// ── 接受引导并创建示例任务 ──
router.post('/accept', auth, (req, res) => {
  const samples = guideTable.getSampleTasks(req.userId);
  const created = samples.map(s => taskTable.create({
    task_id: uuid.v4(),
    user_id: req.userId,
    creator_id: req.userId,
    name: s.name,
    frequency: s.frequency,
    point_per_check: s.pointPerCheck,
    start_time: s.startTime,
    end_time: s.endTime,
    is_active: 1,
    status: 'ACTIVE',
  }));

  guideTable.markDone(req.userId);
  res.json(success({ tasks: created }, `已创建 ${created.length} 个示例任务`));
});

module.exports = router;
