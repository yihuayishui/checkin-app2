// 用户设置路由
const { Router } = require('express');
const configTable = require('../../store/tables/config-table');
const auth = require('../../middleware/auth');
const { success } = require('../../utils/helper');

const router = Router();

router.get('/', auth, (req, res) => {
  res.json(success(configTable.getConfig(req.userId)));
});

router.put('/', auth, (req, res) => {
  const updated = configTable.updateConfig(req.userId, req.body);
  res.json(success(updated, '设置已更新'));
});

module.exports = router;
