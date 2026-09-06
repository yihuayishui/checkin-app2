// 搭档关系路由
const { Router } = require('express');
const uuid = require('uuid');
const userTable = require('../../store/tables/user-table');
const pairTable = require('../../store/tables/pair-table');
const auth = require('../../middleware/auth');
const { ApiError } = require('../../middleware/error-handler');
const { success } = require('../../utils/helper');
const { sendPush } = require('../../utils/fcm');
const socketHandler = require('../../ws/socket-handler');

const router = Router();

let getIO = null;
function setIO(fn) { getIO = fn; }

function notifyPartner(partnerId, event, data) {
  if (!getIO) return;
  const io = getIO();
  if (!io) return;
  io.to(partnerId).emit(event, data);
}

// ── 搭档状态 ──
router.get('/status', auth, (req, res) => {
  const pair = pairTable.findByUserId(req.userId);
  if (!pair || pair.length === 0 || pair[0].status === 'NONE') {
    return res.json(success({ status: 'NONE', pair: null }));
  }
  const p = pair[0];
  const partnerId = p.user_a === req.userId ? p.user_b : p.user_a;
  const partner = userTable.findById(partnerId);
  res.json(success({
    status: p.status,
    pair: {
      pairId: p.id, partnerId,
      partnerUsername: partner ? partner.username : '未知',
      partnerAvatarUrl: partner ? partner.avatar_url : null,
      partnerOnline: partner ? socketHandler.isOnline(partnerId) : false,
      requestedBy: p.requested_by, unbindRequestedBy: p.unbind_requested_by,
      createdAt: p.created_at,
    },
  }));
});

// ── 发送搭档请求 ──
router.post('/request', auth, (req, res) => {
  const { targetUserId } = req.body;
  if (!targetUserId) throw new ApiError(400, '目标用户 ID 不能为空');
  if (targetUserId === req.userId) throw new ApiError(400, '不能向自己发送搭档请求');
  if (!userTable.findById(targetUserId)) throw new ApiError(404, '目标用户不存在');

  const existing = pairTable.findExistingPair(req.userId, targetUserId);
  if (existing) {
    if (existing.status === 'BOUND') throw new ApiError(409, '你们已经是搭档了');
    if (existing.status === 'PENDING') throw new ApiError(409, '已存在待处理的搭档请求');
  }

  const pairId = uuid.v4();
  const pair = pairTable.create(pairId, req.userId, targetUserId, req.userId);

  notifyPartner(targetUserId, 'pair:notification', { type: 'REQUEST', pairId, fromUserId: req.userId });
  sendPush(targetUserId, '搭档请求', `${userTable.findById(req.userId)?.username} 向你发来了搭档请求`, pairId, 'pair_request');

  res.status(201).json(success({ pairId, status: 'PENDING' }, '搭档请求已发送'));
});

// ── 撤回搭档请求 ──
router.post('/cancel-request', auth, (req, res) => {
  const { targetUserId } = req.body;
  const pair = pairTable.findExistingPair(req.userId, targetUserId);
  if (!pair || pair.status !== 'PENDING') throw new ApiError(404, '没有可撤回的请求');
  pairTable.remove(pair.id);
  notifyPartner(targetUserId, 'pair:notification', { type: 'REQUEST_CANCELLED', pairId: pair.id });
  res.json(success(null, '请求已撤回'));
});

// ── 回应搭档请求 ──
router.post('/respond', auth, (req, res) => {
  const { pairId, accept } = req.body;
  if (!pairId) throw new ApiError(400, 'pairId 不能为空');
  const pair = pairTable.findById(pairId);
  if (!pair) throw new ApiError(404, '搭档请求不存在');
  if (pair.status !== 'PENDING') throw new ApiError(409, '该请求已被处理');
  if (pair.user_b !== req.userId) throw new ApiError(403, '无权操作');

  const requesterId = pair.user_a;
  if (accept) {
    pairTable.accept(pairId);
    notifyPartner(requesterId, 'pair:notification', { type: 'ACCEPTED', pairId });
    sendPush(requesterId, '搭档请求已接受', `${userTable.findById(req.userId)?.username} 接受了你的搭档请求`, pairId, 'pair_accepted');
    return res.json(success({ status: 'BOUND' }, '已接受搭档请求'));
  } else {
    pairTable.remove(pairId);
    notifyPartner(requesterId, 'pair:notification', { type: 'REJECTED', pairId });
    sendPush(requesterId, '搭档请求已拒绝', `${userTable.findById(req.userId)?.username} 拒绝了你的搭档请求`, pairId, 'pair_rejected');
    return res.json(success({ status: 'NONE' }, '已拒绝搭档请求'));
  }
});

// ── 发起解绑 ──
router.post('/unbind-request', auth, (req, res) => {
  const pair = pairTable.findBoundByUserId(req.userId);
  if (!pair) throw new ApiError(404, '未找到搭档关系');
  pairTable.setUnbindRequest(pair.id, req.userId);
  const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
  notifyPartner(partnerId, 'pair:notification', { type: 'UNBIND_REQUEST', pairId: pair.id, fromUserId: req.userId });
  sendPush(partnerId, '解绑请求', '搭档请求解除绑定', pair.id, 'unbind_request');
  res.json(success({ pairId: pair.id }, '解绑请求已发送'));
});

// ── 回应解绑 ──
router.post('/unbind-respond', auth, (req, res) => {
  const { accept } = req.body;
  const pair = pairTable.findBoundByUserId(req.userId);
  if (!pair) throw new ApiError(404, '未找到搭档关系');
  if (!pair.unbind_requested_by || pair.unbind_requested_by === req.userId) throw new ApiError(400, '没有待处理的解绑请求');

  const requesterId = pair.unbind_requested_by;
  if (accept) {
    pairTable.remove(pair.id);
    notifyPartner(requesterId, 'pair:notification', { type: 'UNBIND_ACCEPTED', pairId: pair.id });
    sendPush(requesterId, '解绑已同意', '搭档已同意解绑', null, 'unbind_accepted');
    return res.json(success({ status: 'NONE' }, '已解绑'));
  } else {
    pairTable.clearUnbindRequest(pair.id);
    notifyPartner(requesterId, 'pair:notification', { type: 'UNBIND_REJECTED', pairId: pair.id });
    return res.json(success({ status: 'BOUND' }, '已拒绝解绑'));
  }
});

module.exports = { router, setIO };
