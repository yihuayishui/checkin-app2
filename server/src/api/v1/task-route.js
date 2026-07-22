// 任务路由
const { Router } = require('express');
const uuid = require('uuid');
const taskTable = require('../../store/tables/task-table');
const pairTable = require('../../store/tables/pair-table');
const auth = require('../../middleware/auth');
const { ApiError } = require('../../middleware/error-handler');
const { success } = require('../../utils/helper');
const { sendPush } = require('../../utils/fcm');

const router = Router();

// ── 任务列表（按分类过滤） ──
router.get('/list', auth, (req, res) => {
  const { filter } = req.query;
  const pair = pairTable.findBoundByUserId(req.userId);
  const partnerId = pair ? (pair.user_a === req.userId ? pair.user_b : pair.user_a) : null;

  let tasks = [];
  switch (filter) {
    case 'mine':
      // 分配给我的（我创建的 + 搭档给我创建的）
      tasks = taskTable.findByUserId(req.userId);
      break;
    case 'created':
      // 我创建的
      tasks = taskTable.findByCreatorId(req.userId);
      break;
    case 'for_partner':
      // 我给搭档创建的
      if (partnerId) tasks = taskTable.findCreatedForPartner(req.userId, partnerId);
      break;
    case 'from_partner':
      // 搭档给我创建的
      if (partnerId) tasks = taskTable.findCreatedByPartner(req.userId, partnerId);
      break;
    case 'done':
      // 已完成的
      tasks = taskTable.findByCreatorId(req.userId).filter(t => t.status === 'DONE')
        .concat(partnerId ? taskTable.findCreatedByPartner(req.userId, partnerId).filter(t => t.status === 'DONE') : []);
      break;
    default:
      tasks = taskTable.findByUserId(req.userId);
  }

  res.json(success({ tasks }));
});

// ── 我创建的 ──
router.get('/created-by-me', auth, (req, res) => {
  const tasks = taskTable.findByCreatorId(req.userId);
  res.json(success({ tasks }));
});

// ── 创建任务 ──
router.post('/create', auth, (req, res) => {
  const { taskId, userId, name, frequency, pointPerCheck, startTime, endTime } = req.body;
  if (!userId || !name) throw new ApiError(400, '用户 ID 和任务名称不能为空');

  const task = taskTable.create({
    task_id: taskId || uuid.v4(),
    user_id: userId,
    creator_id: req.userId,
    name,
    frequency: frequency || 'DAILY',
    point_per_check: pointPerCheck || 10,
    start_time: startTime || null,
    end_time: endTime || null,
    is_active: 1,
    status: 'ACTIVE',
  });

  res.status(201).json(success({ task }, '任务创建成功'));
});

// ── 更新任务 ──
router.put('/update', auth, (req, res) => {
  const { taskId, name, frequency, pointPerCheck, startTime, endTime, isActive } = req.body;
  const existing = taskTable.findById(taskId);
  if (!existing) throw new ApiError(404, '任务不存在');

  const updated = taskTable.update({
    task_id: taskId,
    name: name || existing.name,
    frequency: frequency || existing.frequency,
    point_per_check: pointPerCheck != null ? pointPerCheck : existing.point_per_check,
    start_time: startTime !== undefined ? startTime : existing.start_time,
    end_time: endTime !== undefined ? endTime : existing.end_time,
    is_active: isActive !== undefined ? (isActive ? 1 : 0) : existing.is_active,
  });

  console.log(`[DEBUG] PUT /update: taskId=${taskId}, reqUserId=${req.userId}, existing.user_id=${existing.user_id}, existing.creator_id=${existing.creator_id}`);

  // 确定需要通知的对方（编辑者以外的那一方）
  // 任务有两个角色：执行者(user_id) 和 创建者(creator_id)
  // 如果编辑者是执行者 → 通知创建者
  // 如果编辑者是创建者 → 通知执行者
  // 如果两者是同一人（自己的任务）→ 不通知
  const notifyUserId = existing.user_id === req.userId ? existing.creator_id : existing.user_id;

  if (notifyUserId && notifyUserId !== req.userId) {
    const changes = [];
    if (name !== undefined && name !== existing.name) changes.push(`名称: ${existing.name} → ${name}`);
    if (frequency !== undefined && frequency !== existing.frequency) {
      const freqMap = { DAILY: '每日', WEEKLY: '每周', ONCE: '一次性' };
      changes.push(`频次: ${freqMap[existing.frequency] || existing.frequency} → ${freqMap[frequency] || frequency}`);
    }
    if (pointPerCheck !== undefined && Number(pointPerCheck) !== existing.point_per_check) changes.push(`积分: ${existing.point_per_check} → ${Number(pointPerCheck)}`);
    if (startTime !== undefined && (startTime || '') !== (existing.start_time || '')) changes.push(`开始时间: ${existing.start_time || '不限'} → ${startTime || '不限'}`);
    if (endTime !== undefined && (endTime || '') !== (existing.end_time || '')) changes.push(`结束时间: ${existing.end_time || '不限'} → ${endTime || '不限'}`);
    if (isActive !== undefined && isActive !== (existing.is_active === 1)) changes.push(`状态: ${existing.is_active ? '启用' : '停用'} → ${isActive ? '启用' : '停用'}`);

    const changeDesc = changes.length > 0 ? '\n' + changes.join('\n') : '';
    console.log(`[DEBUG] Sending notification to user ${notifyUserId}: title=✏️ 任务「${existing.name}」已更新, changes=${changeDesc || '无变更'}`);
    sendPush(notifyUserId, `✏️ 任务「${existing.name}」已更新`, `搭档编辑了任务${changeDesc}`, taskId, 'task_edit_request');

    // WebSocket 通知
    try {
      const { getIO } = require('../../ws/socket-handler');
      const io = getIO();
      if (io) io.to(notifyUserId).emit('task:sync', { taskId, fromUserId: req.userId });
    } catch(_) {}
  } else {
    console.log(`[DEBUG] notifyUserId === reqUserId or empty, no notification needed`);
  }

  res.json(success({ task: updated }, '任务更新成功'));
});

// ── 删除任务（只能删自己创建的） ──
router.delete('/delete/:taskId', auth, (req, res) => {
  const { taskId } = req.params;
  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');
  if (task.creator_id !== req.userId) throw new ApiError(403, '只能删除自己创建的任务，搭档创建的任务需要对方确认');
  const db = require('../../store/db-init').getDb();
  db.prepare('DELETE FROM checkin_record WHERE task_id = ?').run(taskId);
  taskTable.remove(taskId);
  res.json(success(null, '任务已删除'));
});

// ── 请求删除搭档创建的任务 ──
router.post('/delete-request/:taskId', auth, (req, res) => {
  const { taskId } = req.params;
  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');
  if (task.creator_id === req.userId) throw new ApiError(400, '自己创建的任务请直接删除');

  const db = require('../../store/db-init').getDb();
  db.prepare('UPDATE task SET pending_delete_by = ? WHERE task_id = ?').run(req.userId, taskId);

  // 通知任务创建者
  const { sendPush } = require('../../utils/fcm');
  sendPush(task.creator_id, '删除任务请求', `搭档请求删除任务「${task.name}」`, taskId, 'task_delete_request');

  // WS 推送给搭档
  try {
    const { getIO } = require('../../ws/socket-handler');
    const io = getIO();
    if (io) io.to(task.creator_id).emit('task:delete', { taskId, fromUserId: req.userId });
  } catch(_) {}

  res.json(success(null, '删除请求已发送，等待搭档确认'));
});

// ── 回应删除请求 ──
router.post('/delete-respond', auth, (req, res) => {
  const { taskId, accept } = req.body;
  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');
  if (task.creator_id !== req.userId) throw new ApiError(403, '只有任务创建者可以回应删除请求');
  if (!task.pending_delete_by) throw new ApiError(400, '没有待处理的删除请求');

  if (accept) {
    const db = require('../../store/db-init').getDb();
    db.prepare('DELETE FROM checkin_record WHERE task_id = ?').run(taskId);
    taskTable.remove(taskId);
    res.json(success(null, '任务已删除'));
  } else {
    const db = require('../../store/db-init').getDb();
    db.prepare('UPDATE task SET pending_delete_by = NULL WHERE task_id = ?').run(taskId);
    res.json(success(null, '已拒绝删除'));
  }
});

// ── 请求编辑搭档创建的任务 ──
router.post('/edit-request/:taskId', auth, (req, res) => {
  const { taskId } = req.params;
  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');
  if (task.creator_id === req.userId) throw new ApiError(400, '自己创建的任务请直接编辑');

  const { name, frequency, pointPerCheck, startTime, endTime, isActive } = req.body;
  const db = require('../../store/db-init').getDb();
  db.prepare('UPDATE task SET pending_edit_by = ?, pending_edit_data = ? WHERE task_id = ?')
    .run(req.userId, JSON.stringify({ name, frequency, pointPerCheck, startTime, endTime, isActive }), taskId);

  // 生成编辑内容变更说明
  const changes = [];
  if (name !== undefined && name !== task.name) changes.push(`名称: ${task.name} → ${name}`);
  if (frequency !== undefined && frequency !== task.frequency) {
    const freqMap = { DAILY: '每日', WEEKLY: '每周', ONCE: '一次性' };
    changes.push(`频次: ${freqMap[task.frequency] || task.frequency} → ${freqMap[frequency] || frequency}`);
  }
  if (pointPerCheck !== undefined && Number(pointPerCheck) !== task.point_per_check) changes.push(`积分: ${task.point_per_check} → ${Number(pointPerCheck)}`);
  if (startTime !== undefined && (startTime || '') !== (task.start_time || '')) changes.push(`开始时间: ${task.start_time || '不限'} → ${startTime || '不限'}`);
  if (endTime !== undefined && (endTime || '') !== (task.end_time || '')) changes.push(`结束时间: ${task.end_time || '不限'} → ${endTime || '不限'}`);
  if (isActive !== undefined && isActive !== (task.is_active === 1)) changes.push(`状态: ${task.is_active ? '启用' : '停用'} → ${isActive ? '启用' : '停用'}`);

  const changeDesc = changes.length > 0 ? '\n' + changes.join('\n') : '';

  const { sendPush } = require('../../utils/fcm');
  sendPush(task.creator_id, `✏️ 编辑任务「${task.name}」`, `搭档请求编辑任务${changeDesc}`, taskId, 'task_edit_request');
  try { const { getIO } = require('../../ws/socket-handler'); const io = getIO(); if (io) io.to(task.creator_id).emit('task:sync', { taskId, fromUserId: req.userId }); } catch(_) {}

  res.json(success(null, '编辑请求已发送，等待搭档确认'));
});

// ── 回应编辑请求 ──
router.post('/edit-respond', auth, (req, res) => {
  const { taskId, accept } = req.body;
  const task = taskTable.findById(taskId);
  if (!task) throw new ApiError(404, '任务不存在');
  if (task.creator_id !== req.userId) throw new ApiError(403, '只有任务创建者可以回应');
  if (!task.pending_edit_by) throw new ApiError(400, '没有待处理的编辑请求');

  if (accept && task.pending_edit_data) {
    const data = JSON.parse(task.pending_edit_data);
    taskTable.update({
      task_id: taskId,
      name: data.name || task.name,
      frequency: data.frequency || task.frequency,
      point_per_check: data.pointPerCheck != null ? parseInt(data.pointPerCheck) : task.point_per_check,
      start_time: data.startTime !== undefined ? data.startTime : task.start_time,
      end_time: data.endTime !== undefined ? data.endTime : task.end_time,
      is_active: data.isActive !== undefined ? data.isActive : task.is_active,
    });
  }
  const db = require('../../store/db-init').getDb();
  db.prepare('UPDATE task SET pending_edit_by = NULL, pending_edit_data = NULL WHERE task_id = ?').run(taskId);
  res.json(success(null, accept ? '编辑已应用' : '已拒绝编辑'));
});

// ── 重新激活 ──
router.post('/:id/reactivate', auth, (req, res) => {
  const task = taskTable.findById(req.params.id);
  if (!task) throw new ApiError(404, '任务不存在');
  const activated = taskTable.reactivate(req.params.id);
  res.json(success({ task: activated }, '任务已重新激活'));
});

module.exports = router;
