// JWT 认证中间件
const jwt = require('jsonwebtoken');
const config = require('../config');
const { ApiError } = require('./error-handler');

function authMiddleware(req, _res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    throw new ApiError(401, '未提供认证令牌');
  }

  try {
    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, config.JWT_SECRET);
    req.userId = decoded.userId;
    next();
  } catch {
    throw new ApiError(401, '令牌无效或已过期');
  }
}

module.exports = authMiddleware;
