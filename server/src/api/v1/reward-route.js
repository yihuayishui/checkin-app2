// 奖励路由（完整兑换流程）
const { Router } = require('express');
const uuid = require('uuid');
const rewardTable = require('../../store/tables/reward-table');
const userTable = require('../../store/tables/user-table');
const pairTable = require('../../store/tables/pair-table');
const pointTable = require('../../store/tables/point-table');
const achievementTable = require('../../store/tables/achievement-table');
const auth = require('../../middleware/auth');
const { ApiError } = require('../../middleware/error-handler');
const { success } = require('../../utils/helper');
const { sendPush } = require('../../utils/fcm');

const router = Router();

// ── 奖励列表 ──
router.get('/list', auth, (req, res) => {
  const rewards = rewardTable.findAll();
  const points = userTable.getPoints(req.userId);
  const poolPoints = points ? points.poolPoints : 0;

  // 附加可兑换状态
  const enriched = rewards.map(r => ({
    ...r,
    canExchange: r.status === 'ACTIVE' && poolPoints >= r.required_points,
  }));

  res.json(success({ rewards: enriched, poolPoints }));
});

// ── 上架奖励 ──
router.post('/create', auth, (req, res) => {
  const { id, name, requiredPoints, expiresAt } = req.body;
  if (!name || !requiredPoints) throw new ApiError(400, '奖励名称和所需积分不能为空');
  if (requiredPoints <= 0) throw new ApiError(400, '所需积分必须为正整数');

  const reward = rewardTable.create({
    id: id || uuid.v4(),
    creator_id: req.userId,
    name,
    required_points: requiredPoints,
    expires_at: expiresAt || null,
    status: 'ACTIVE',
  });

  res.status(201).json(success({ reward }, '奖励上架成功'));
});

// ── 编辑奖励 ──
router.put('/update', auth, (req, res) => {
  const { id, name, requiredPoints, expiresAt, status } = req.body;
  const existing = rewardTable.findById(id);
  if (!existing) throw new ApiError(404, '奖励不存在');

  const updated = rewardTable.update({
    id,
    name: name || existing.name,
    required_points: requiredPoints || existing.required_points,
    expires_at: expiresAt !== undefined ? expiresAt : existing.expires_at,
    status: status || existing.status,
    applicant_id: existing.applicant_id,
  });

  res.json(success({ reward: updated }, '奖励更新成功'));
});

// ── 请求删除奖励（需搭档确认） ──
router.post('/:id/delete-request', auth, (req, res) => {
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');

  const db = require('../../store/db-init').getDb();
  db.prepare('UPDATE reward SET pending_delete_by = ? WHERE id = ?').run(req.userId, req.params.id);

  // 通知搭档
  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    sendPush(partnerId, '删除奖励请求', `搭档请求删除奖励「${reward.name}」`, req.params.id, 'reward_delete_request');
  }

  res.json(success(null, '删除请求已发送，等待搭档确认'));
});

// ── 回应删除请求 ──
router.post('/:id/delete-respond', auth, (req, res) => {
  const { accept } = req.body;
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');

  if (accept) {
    rewardTable.remove(req.params.id);
    res.json(success(null, '奖励已删除'));
  } else {
    const db = require('../../store/db-init').getDb();
    db.prepare('UPDATE reward SET pending_delete_by = NULL WHERE id = ?').run(req.params.id);
    res.json(success(null, '已拒绝删除'));
  }
});

// ── 申请兑换 ──
router.post('/:id/exchange', auth, (req, res) => {
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');
  if (reward.status !== 'ACTIVE') throw new ApiError(400, '该奖励当前不可兑换');

  // 检查积分
  const points = userTable.getPoints(req.userId);
  if (!points || points.poolPoints < reward.required_points) {
    throw new ApiError(400, `奖励池积分不足，需要 ${reward.required_points}，当前 ${points ? points.poolPoints : 0}`);
  }

  const updated = rewardTable.applyExchange(req.params.id, req.userId);

  // 通知对方
  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    sendPush(partnerId, '兑换申请', `${userTable.findById(req.userId)?.username} 申请兑换「${reward.name}」`, req.params.id, 'reward_exchange');
  }

  res.json(success({ reward: updated }, '兑换申请已发送，等待搭档确认'));
});

// ── 撤回兑换申请 ──
router.post('/:id/cancel-exchange', auth, (req, res) => {
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');
  if (reward.status !== 'PENDING') throw new ApiError(400, '没有可撤回的兑换申请');
  if (reward.applicant_id !== req.userId) throw new ApiError(403, '只能撤回自己的申请');

  const updated = rewardTable.cancelExchange(req.params.id);
  res.json(success({ reward: updated }, '兑换申请已撤回'));
});

// ── 同意/拒绝兑换 ──
router.post('/:id/confirm', auth, (req, res) => {
  const { accept } = req.body;
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');
  if (reward.status !== 'PENDING') throw new ApiError(400, '该奖励没有待确认的兑换');

  if (accept) {
    // 扣积分
    const applicantId = reward.applicant_id;
    const success2 = userTable.deductPoolPoints(applicantId, reward.required_points);
    if (!success2) throw new ApiError(400, '积分不足');

    // 同步搭档池子
    const pair = pairTable.findBoundByUserId(applicantId);
    if (pair) {
      const partnerId = pair.user_a === applicantId ? pair.user_b : pair.user_a;
      userTable.setPoolPoints(partnerId, userTable.getPoints(applicantId).poolPoints);
    }

    // 积分流水
    pointTable.create({ userId: applicantId, amount: reward.required_points, type: 'SPEND', category: 'REWARD', description: `兑换: ${reward.name}` });

    const updated = rewardTable.confirmExchange(req.params.id, true);

    // 通知
    sendPush(applicantId, '兑换已通过', `搭档同意了「${reward.name}」的兑换申请`, req.params.id, 'reward_confirmed');

    // 成就
    achievementTable.unlock(applicantId, 'first_reward');

    return res.json(success({ reward: updated }, '已同意兑换，积分已扣除'));
  } else {
    const updated = rewardTable.confirmExchange(req.params.id, false);
    sendPush(reward.applicant_id, '兑换被拒绝', `搭档拒绝了「${reward.name}」的兑换申请`, req.params.id, 'reward_rejected');
    return res.json(success({ reward: updated }, '已拒绝兑换'));
  }
});

// ── 申请兑现（需搭档确认） ──
router.post('/:id/claim-request', auth, (req, res) => {
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');
  if (reward.status !== 'CONFIRMED') throw new ApiError(400, '奖励尚未通过兑换确认');

  const db = require('../../store/db-init').getDb();
  db.prepare('UPDATE reward SET claim_requested_by = ? WHERE id = ?').run(req.userId, req.params.id);

  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    sendPush(partnerId, '兑现确认请求', `搭档想把「${reward.name}」标记为已兑现`, req.params.id, 'reward_claim_request');
  }

  res.json(success(null, '兑现请求已发送，等待搭档确认'));
});

// ── 回应兑现请求 ──
router.post('/:id/claim-respond', auth, (req, res) => {
  const { accept } = req.body;
  const reward = rewardTable.findById(req.params.id);
  if (!reward) throw new ApiError(404, '奖励不存在');
  if (!reward.claim_requested_by) throw new ApiError(400, '没有待确认的兑现请求');
  if (reward.claim_requested_by === req.userId) throw new ApiError(400, '不能确认自己的请求');

  if (accept) {
    rewardTable.markClaimed(req.params.id);
    const db = require('../../store/db-init').getDb();
    db.prepare('UPDATE reward SET claim_requested_by = NULL WHERE id = ?').run(req.params.id);
    res.json(success(null, '奖励已兑现'));
  } else {
    const db = require('../../store/db-init').getDb();
    db.prepare('UPDATE reward SET claim_requested_by = NULL WHERE id = ?').run(req.params.id);
    res.json(success(null, '已拒绝兑现'));
  }
});

module.exports = router;
