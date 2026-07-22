// 通用工具函数
// 注意：服务器时间为 UTC，所有时间操作需转为北京时间 (UTC+8)

/** 统一成功响应 */
function success(data, message = '操作成功') {
  return { code: 200, message, data };
}

/** 获取当前北京时间 ISO 字符串 */
function now() {
  const cst = new Date(new Date().getTime() + 8 * 60 * 60 * 1000);
  return cst.toISOString().replace('Z', '+08:00');
}

/** 获取今天北京时间日期 YYYY-MM-DD */
function today() {
  const cst = new Date(new Date().getTime() + 8 * 60 * 60 * 1000);
  return cst.toISOString().split('T')[0];
}

/** 获取当前北京时间月份 YYYY-MM */
function currentMonth() {
  const cst = new Date(new Date().getTime() + 8 * 60 * 60 * 1000);
  return cst.toISOString().slice(0, 7);
}

/** 校验时间窗：check 是否在 HH:MM 范围内（北京时间 UTC+8） */
function isInTimeWindow(startTime, endTime) {
  if (!startTime && !endTime) return true;
  const now = new Date();
  // 转换为北京时间 (UTC+8)
  const cst = new Date(now.getTime() + 8 * 60 * 60 * 1000);
  const currentMinutes = cst.getUTCHours() * 60 + cst.getUTCMinutes();

  if (startTime) {
    const [sh, sm] = startTime.split(':').map(Number);
    if (currentMinutes < sh * 60 + sm) return false;
  }
  if (endTime) {
    const [eh, em] = endTime.split(':').map(Number);
    if (currentMinutes > eh * 60 + em) return false;
  }
  return true;
}

module.exports = { success, now, today, currentMonth, isInTimeWindow };
