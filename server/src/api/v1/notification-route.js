// 通知路由
const { Router } = require('express');
const notificationTable = require('../../store/tables/notification-table');
const auth = require('../../middleware/auth');
const { success } = require('../../utils/helper');

const router = Router();

router.get('/list', auth, (req, res) => {
  const limit = parseInt(req.query.limit) || 50;
  const notifications = notificationTable.findByUserId(req.userId, limit);
  const unread = notificationTable.unreadCount(req.userId);
  res.json(success({ notifications, unreadCount: unread }));
});

router.post('/:id/read', auth, (req, res) => {
  notificationTable.markRead(parseInt(req.params.id), req.userId);
  res.json(success(null, '已读'));
});

router.post('/read-all', auth, (req, res) => {
  notificationTable.markAllRead(req.userId);
  res.json(success(null, '全部已读'));
});

router.delete('/clear', auth, (req, res) => {
  const db = require('../../store/db-init').getDb();
  db.prepare('DELETE FROM notification WHERE user_id = ?').run(req.userId);
  res.json(success(null, '已清空'));
});

module.exports = router;
