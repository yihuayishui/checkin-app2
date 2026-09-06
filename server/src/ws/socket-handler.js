// Socket.IO 事件处理
const pairTable = require('../store/tables/pair-table');
const userTable = require('../store/tables/user-table');

let io = null;
const socketMap = new Map();

function initSocketHandler(socketIO) {
  io = socketIO;

  io.on('connection', (socket) => {
    console.log(`[WS] 新连接: ${socket.id}`);
    let currentUserId = null;

    function getPartnerSocketId(userId) {
      const pair = pairTable.findBoundByUserId(userId);
      if (!pair) return null;
      const partnerId = pair.user_a === userId ? pair.user_b : pair.user_a;
      return socketMap.get(partnerId) || null;
    }

    function notifyPartnerOnline(userId, online) {
      const partnerSocketId = getPartnerSocketId(userId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('partner:online', { userId, online, timestamp: new Date().toISOString() });
      }
    }

    // ── 认证 ──
    socket.on('auth:login', ({ userId } = {}) => {
      if (!userId) {
        socket.emit('error:msg', { message: 'userId 不能为空' });
        return;
      }
      currentUserId = userId;
      socket.join(userId);
      socketMap.set(userId, socket.id);
      console.log(`[WS] 认证: ${userId} -> ${socket.id}`);
      socket.emit('auth:ok', { userId, message: '认证成功' });
      notifyPartnerOnline(userId, true);
    });

    // ── 打卡转发 ──
    socket.on('checkin:create', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('partner:checkin', {
          fromUserId: currentUserId,
          fromUsername: userTable.findById(currentUserId)?.username || '未知',
          ...data,
          timestamp: new Date().toISOString(),
        });
      }
      socket.emit('checkin:ack', { recordId: data.recordId, timestamp: new Date().toISOString() });
    });

    // ── 同步 ──
    socket.on('sync:request', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('sync:request:forward', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
      }
    });

    socket.on('sync:response', (data = {}) => {
      if (!currentUserId) return;
      const { targetUserId } = data;
      if (targetUserId) {
        const targetSocket = socketMap.get(targetUserId);
        if (targetSocket) {
          io.to(targetSocket).emit('sync:response:forward', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
        }
      }
    });

    // ── 任务同步 ──
    socket.on('task:sync', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('task:sync', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
      }
    });

    socket.on('task:delete', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('task:delete', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
      }
    });

    // ── 奖励同步 ──
    socket.on('reward:sync', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('reward:sync', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
      }
    });

    socket.on('reward:exchange:request', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('reward:exchange:request', {
          fromUserId: currentUserId,
          fromUsername: userTable.findById(currentUserId)?.username || '未知',
          ...data,
          timestamp: new Date().toISOString(),
        });
      }
    });

    socket.on('reward:exchange:confirm', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('reward:exchange:confirm', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
      }
    });

    // ── 搭档通知 ──
    socket.on('pair:notification', (data = {}) => {
      if (!currentUserId) return;
      const partnerSocketId = getPartnerSocketId(currentUserId);
      if (partnerSocketId) {
        io.to(partnerSocketId).emit('pair:notification', { fromUserId: currentUserId, ...data, timestamp: new Date().toISOString() });
      }
    });

    // ── 断开 ──
    socket.on('disconnect', () => {
      console.log(`[WS] 断开: ${socket.id} (userId: ${currentUserId})`);
      if (currentUserId) {
        socketMap.delete(currentUserId);
        notifyPartnerOnline(currentUserId, false);
      }
    });
  });
}

function getIO() { return io; }

// 查询用户是否在线（WebSocket 连接已认证）
function isOnline(userId) {
  return socketMap.has(userId);
}

function broadcastPointsUpdate(ioInstance, userId, points) {
  if (!ioInstance) return;
  const payload = {
    fromUserId: userId,
    personalPoints: points.personalPoints,
    poolPoints: points.poolPoints,
    timestamp: new Date().toISOString(),
  };
  ioInstance.to(userId).emit('points:update', payload);
  const pair = pairTable.findBoundByUserId(userId);
  if (pair) {
    const partnerId = pair.user_a === userId ? pair.user_b : pair.user_a;
    const partnerSocket = socketMap.get(partnerId);
    if (partnerSocket) {
      ioInstance.to(partnerSocket).emit('points:update', payload);
    }
  }
}

module.exports = { initSocketHandler, getIO, broadcastPointsUpdate, isOnline };
