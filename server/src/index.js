// 服务入口
const http = require('http');
const { Server } = require('socket.io');
const app = require('./app');
const config = require('./config');
const { initSocketHandler } = require('./ws/socket-handler');

const server = http.createServer(app);

const io = new Server(server, {
  cors: { origin: '*', methods: ['GET', 'POST'] },
  pingInterval: 10000,
  pingTimeout: 5000,
});

initSocketHandler(io);

server.listen(config.PORT, () => {
  console.log(`[Server] 打卡服务 v2 已启动 → http://localhost:${config.PORT}`);
  console.log(`[Server] WebSocket 已就绪`);
  console.log(`[Server] 数据库: ${config.DB_PATH}`);
});
