// 数据导出路由
const { Router } = require('express');
const checkinTable = require('../../store/tables/checkin-table');
const auth = require('../../middleware/auth');
const { success } = require('../../utils/helper');

const router = Router();

router.get('/checkins', auth, (req, res) => {
  const records = checkinTable.findByUserId(req.userId, 10000);
  const csv = ['日期,任务ID,备注,是否补签'];
  records.forEach(r => {
    csv.push(`${r.checkin_time},${r.task_id},${r.note || ''},${r.is_makeup ? '是' : '否'}`);
  });

  res.setHeader('Content-Type', 'text/csv; charset=utf-8');
  res.setHeader('Content-Disposition', 'attachment; filename=checkins.csv');
  res.send('\uFEFF' + csv.join('\n'));
});

module.exports = router;
