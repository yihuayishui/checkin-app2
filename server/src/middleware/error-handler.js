// 统一错误处理
class ApiError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

function errorHandler(err, req, res, _next) {
  console.error(`[ERROR] ${req.method} ${req.path}:`, err.message);

  if (err instanceof ApiError) {
    return res.status(err.code).json({ code: err.code, message: err.message });
  }
  if (err.name === 'JsonWebTokenError' || err.name === 'TokenExpiredError') {
    return res.status(401).json({ code: 401, message: '令牌无效或已过期' });
  }
  return res.status(500).json({ code: 500, message: '服务器内部错误' });
}

module.exports = { errorHandler, ApiError };
