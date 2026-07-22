// 成就路由
const { Router } = require('express');
const achievementTable = require('../../store/tables/achievement-table');
const auth = require('../../middleware/auth');
const { success } = require('../../utils/helper');

const router = Router();

router.get('/list', auth, (req, res) => {
  res.json(success({ achievements: achievementTable.findByUserId(req.userId) }));
});

module.exports = router;
