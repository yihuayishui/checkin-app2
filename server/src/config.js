// 打卡 App 配置
const path = require('path');

module.exports = {
  // 服务器端口
  PORT: process.env.PORT || 3001,

  // JWT 密钥及过期时间
  JWT_SECRET: process.env.JWT_SECRET || 'checkin-app-v2-secret-2026',
  JWT_EXPIRES_IN: '30d',

  // SQLite 数据库文件路径
  DB_PATH: process.env.DB_PATH || path.join(__dirname, '..', 'db', 'checkin-v2.db'),

  // 图片上传目录
  UPLOAD_DIR: path.join(__dirname, '..', 'uploads'),

  // 同步参数
  SYNC_BATCH_SIZE: 100,

  // 积分默认分配比例（个人:奖励池）
  DEFAULT_PERSONAL_RATIO: 0.5,
  DEFAULT_POOL_RATIO: 0.5,

  // 补签卡每月数量
  MAKEUP_CARDS_PER_MONTH: 2,

  // 断签惩罚（默认关闭，0=不启用）
  STREAK_PENALTY_DAYS: 0,
  STREAK_PENALTY_POINTS: 10,

  // FCM v1 API 配置
  FCM_PROJECT_ID: process.env.FCM_PROJECT_ID || 'checkinpartner-811ab',
  FCM_SERVICE_ACCOUNT_PATH: process.env.FCM_SERVICE_ACCOUNT_PATH || path.join(__dirname, '..', 'service-account.json'),
};
