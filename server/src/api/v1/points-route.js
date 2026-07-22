// 积分路由
const { Router } = require('express');
const pointTable = require('../../store/tables/point-table');
const auth = require('../../middleware/auth');
const { success } = require('../../utils/helper');

const router = Router();

// ── 积分流水 ──
router.get('/transactions', auth, (req, res) => {
  const limit = parseInt(req.query.limit) || 50;
  const offset = parseInt(req.query.offset) || 0;
  const transactions = pointTable.findByUserId(req.userId, limit, offset);
  res.json(success({ transactions }));
});

module.exports = router;
