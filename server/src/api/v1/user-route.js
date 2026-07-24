// 用户相关路由
const { Router } = require('express');
const bcrypt = require('bcryptjs');
const uuid = require('uuid');
const jwt = require('jsonwebtoken');
const config = require('../../config');
const userTable = require('../../store/tables/user-table');
const pairTable = require('../../store/tables/pair-table');
const configTable = require('../../store/tables/config-table');
const auth = require('../../middleware/auth');
const { ApiError } = require('../../middleware/error-handler');
const { success } = require('../../utils/helper');
const { sendPush } = require('../../utils/fcm');

const router = Router();

// Socket.IO 引用（由 app.js 注入）
let getIO = null;
function setIO(fn) { getIO = fn; }

// ── 注册 ──
router.post('/register', (req, res) => {
  const { username, password } = req.body;
  if (!username || username.trim().length < 2 || username.trim().length > 20) {
    throw new ApiError(400, '用户名长度需在 2-20 个字符之间');
  }
  if (!password || password.length < 6) {
    throw new ApiError(400, '密码长度不能少于 6 位');
  }
  if (userTable.findByUsername(username.trim())) {
    throw new ApiError(409, '用户名已被占用');
  }

  const userId = uuid.v4();
  const hashed = bcrypt.hashSync(password, 10);
  const user = userTable.create(userId, username.trim(), hashed);
  const token = jwt.sign({ userId }, config.JWT_SECRET, { expiresIn: config.JWT_EXPIRES_IN });

  res.status(201).json(success({ userId: user.id, username: user.username, token }, '注册成功'));
});

// ── 登录 ──
router.post('/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) throw new ApiError(400, '用户名和密码不能为空');

  const user = userTable.findByUsername(username);
  if (!user || !bcrypt.compareSync(password, user.password)) {
    throw new ApiError(401, '用户名或密码错误');
  }

  const token = jwt.sign({ userId: user.id }, config.JWT_SECRET, { expiresIn: config.JWT_EXPIRES_IN });
  res.json(success({
    userId: user.id, username: user.username, avatarUrl: user.avatar_url, token,
  }, '登录成功'));
});

// ── 个人信息 ──
router.get('/me', auth, (req, res) => {
  const user = userTable.findById(req.userId);
  if (!user) throw new ApiError(404, '用户不存在');
  res.json(success({
    userId: user.id, username: user.username, avatarUrl: user.avatar_url, isVacation: !!user.is_vacation, createdAt: user.created_at,
  }));
});

// ── 积分查询 ──
router.get('/points', auth, (req, res) => {
  const points = userTable.getPoints(req.userId);
  if (!points) throw new ApiError(404, '用户不存在');
  res.json(success(points));
});

// ── 积分上传（以本地为准覆写） ──
router.post('/points/upload', auth, (req, res) => {
  const { personalPoints, poolPoints } = req.body;
  if (personalPoints == null || poolPoints == null) throw new ApiError(400, '积分值不能为空');

  const updated = userTable.setPoints(req.userId, Math.max(0, personalPoints), Math.max(0, poolPoints));
  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    userTable.setPoolPoints(partnerId, Math.max(0, poolPoints));
  }
  if (getIO) {
    const { broadcastPointsUpdate } = require('../../ws/socket-handler');
    broadcastPointsUpdate(getIO(), req.userId, updated);
  }
  res.json(success(updated, '积分已同步'));
});

// ── 搜索用户 ──
router.get('/search', auth, (req, res) => {
  const { q } = req.query;
  if (!q) throw new ApiError(400, '搜索关键词不能为空');
  const users = userTable.search(q.trim(), req.userId);
  res.json(success({ users: users.map(u => ({ userId: u.id, username: u.username, avatarUrl: u.avatar_url })) }));
});

// ── 改密码 ──
router.put('/password', auth, (req, res) => {
  const { oldPassword, newPassword } = req.body;
  if (!oldPassword || !newPassword || newPassword.length < 6) throw new ApiError(400, '密码格式不正确');

  const user = userTable.findById(req.userId);
  if (!bcrypt.compareSync(oldPassword, user.password)) throw new ApiError(400, '原密码错误');

  userTable.updatePassword(req.userId, bcrypt.hashSync(newPassword, 10));
  res.json(success(null, '密码修改成功'));
});

// ── 密码重置请求（搭档互证） ──
router.post('/password/reset', (req, res) => {
  const { username } = req.body;
  if (!username) throw new ApiError(400, '用户名不能为空');

  const user = userTable.findByUsername(username);
  if (!user) throw new ApiError(404, '用户不存在');

  const pair = pairTable.findBoundByUserId(user.id);
  if (!pair) throw new ApiError(400, '你还没有搭档，无法通过搭档验证重置密码');

  const partnerId = pair.user_a === user.id ? pair.user_b : pair.user_a;

  // 通知搭档确认
  sendPush(partnerId, '密码重置请求', `${username} 请求重置密码，请确认是否允许`, user.id, 'password_reset_request');

  res.json(success({ partnerId }, '已通知搭档确认'));
});

// ── 搭档确认密码重置 ──
router.post('/password/reset/confirm', auth, (req, res) => {
  const { targetUserId } = req.body;
  if (!targetUserId) throw new ApiError(400, '目标用户 ID 不能为空');

  const newPassword = uuid.v4().slice(0, 8);
  userTable.updatePassword(targetUserId, bcrypt.hashSync(newPassword, 10));

  sendPush(targetUserId, '密码已重置', `你的新密码是: ${newPassword}`, null, 'password_reset_done');

  res.json(success(null, '密码已重置，新密码已发送给该用户'));
});

// ── 上传头像 ──
router.put('/avatar', auth, (req, res) => {
  const { avatarUrl } = req.body;
  if (!avatarUrl) throw new ApiError(400, '头像 URL 不能为空');
  userTable.updateAvatar(req.userId, avatarUrl);
  res.json(success({ avatarUrl }, '头像更新成功'));
});

// ── FCM Token 上传 ──
router.post('/fcm-token', auth, (req, res) => {
  const { token } = req.body;
  if (!token) throw new ApiError(400, 'FCM token 不能为空');
  userTable.updateFcmToken(req.userId, token);
  res.json(success(null, 'Token 已更新'));
});

// ── 极光推送 Registration ID 上传 ──
router.post('/jpush-reg-id', auth, (req, res) => {
  const { regId } = req.body;
  if (!regId) throw new ApiError(400, 'Registration ID 不能为空');
  userTable.updateJpushRegId(req.userId, regId);
  res.json(success(null, 'RegId 已更新'));
});

// ── 请假 ──
router.post('/vacation/start', auth, (req, res) => {
  userTable.setVacation(req.userId, true);
  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    sendPush(partnerId, '请假通知', '搭档开启了请假模式', null, 'vacation_start');
    try { const { getIO } = require('../../ws/socket-handler'); const io = getIO(); if (io) io.to(partnerId).emit('pair:notification', { type: 'vacation_start', fromUserId: req.userId }); } catch(_) {}
  }
  res.json(success(null, '请假模式已开启'));
});

router.post('/vacation/end', auth, (req, res) => {
  userTable.setVacation(req.userId, false);
  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) {
    const partnerId = pair.user_a === req.userId ? pair.user_b : pair.user_a;
    sendPush(partnerId, '销假通知', '搭档已关闭请假模式', null, 'vacation_end');
    try { const { getIO } = require('../../ws/socket-handler'); const io = getIO(); if (io) io.to(partnerId).emit('pair:notification', { type: 'vacation_end', fromUserId: req.userId }); } catch(_) {}
  }
  res.json(success(null, '请假模式已关闭'));
});

// ── 注销账号 ──
router.delete('/account', auth, (req, res) => {
  const { password } = req.body;
  const user = userTable.findById(req.userId);
  if (!bcrypt.compareSync(password, user.password)) throw new ApiError(400, '密码错误，无法注销');

  const pair = pairTable.findBoundByUserId(req.userId);
  if (pair) pairTable.remove(pair.id);
  userTable.deleteUser(req.userId);
  res.json(success(null, '账号已注销'));
});

module.exports = { router, setIO };
