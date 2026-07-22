// Express 应用配置
const express = require('express');
const cors = require('cors');
const path = require('path');
const { initDatabase } = require('./store/db-init');
const { errorHandler } = require('./middleware/error-handler');
const { getIO } = require('./ws/socket-handler');
const config = require('./config');

// 初始化数据库（better-sqlite3 同步初始化）
let dbReady = false;
try {
  initDatabase();
  dbReady = true;
} catch (err) {
  console.error('[DB] 初始化失败:', err);
}

const app = express();

// 中间件
app.use(cors());
app.use(express.json());

// 静态文件（上传的图片）
app.use('/uploads', express.static(config.UPLOAD_DIR));

// 请求日志
app.use((req, _res, next) => {
  console.log(`[REQ] ${req.method} ${req.originalUrl}`);
  next();
});

// 健康检查
app.get('/api/v1/health', (_req, res) => {
  res.json({ code: 200, message: '服务正常', data: { status: 'ok', dbReady, time: new Date().toISOString() } });
});

// ─── 路由挂载 ───

const userRoute = require('./api/v1/user-route');
userRoute.setIO(getIO);
app.use('/api/v1/user', userRoute.router);

const pairRoute = require('./api/v1/pair-route');
pairRoute.setIO(getIO);
app.use('/api/v1/pair', pairRoute.router);

app.use('/api/v1/task', require('./api/v1/task-route'));

const checkinRoute = require('./api/v1/checkin-route');
checkinRoute.setIO(getIO);
app.use('/api/v1/checkin', checkinRoute.router);

app.use('/api/v1/reward', require('./api/v1/reward-route'));
app.use('/api/v1/points', require('./api/v1/points-route'));
app.use('/api/v1/dashboard', require('./api/v1/dashboard-route'));
app.use('/api/v1/achievements', require('./api/v1/achievement-route'));
app.use('/api/v1/notifications', require('./api/v1/notification-route'));
app.use('/api/v1/upload', require('./api/v1/upload-route'));
app.use('/api/v1/export', require('./api/v1/export-route'));
app.use('/api/v1/sync', require('./api/v1/sync-route'));
app.use('/api/v1/guide', require('./api/v1/guide-route'));
app.use('/api/v1/config', require('./api/v1/config-route'));

// 错误处理
app.use(errorHandler);

module.exports = app;
