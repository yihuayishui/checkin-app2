// 极光推送 HTTP v3 API 工具
// 文档: https://docs.jiguang.cn/jpush/server/push/rest_api_v3_push
const config = require('../config');
const userTable = require('../store/tables/user-table');

/**
 * 通过极光 REST API 推送通知
 * @param {string} userId 目标用户 ID
 * @param {string} title 标题
 * @param {string} body 内容
 * @param {string|null} relatedId 关联 ID
 * @param {string} type 通知类型
 * @param {number} notifId 通知记录 ID（由调用方创建，避免重复）
 */
async function sendJpush(userId, title, body, relatedId, type, notifId) {
  try {
    const regId = userTable.getJpushRegId(userId);
    if (!regId) return; // 没有注册 ID，跳过

    // Basic Auth
    const auth = Buffer.from(`${config.JPUSH_APP_KEY}:${config.JPUSH_MASTER_SECRET}`).toString('base64');

    const payload = {
      platform: 'android',
      audience: {
        registration_id: [regId],
      },
      notification: {
        android: {
          alert: body,
          title: title,
          priority: 1,
          extras: {
            type: type || 'unknown',
            relatedId: relatedId || '',
            notifId: String(notifId || ''),
          },
        },
      },
    };

    const res = await fetch('https://api.jpush.cn/v3/push', {
      method: 'POST',
      headers: {
        'Authorization': `Basic ${auth}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    const data = await res.json();
    if (res.ok) {
      console.log(`[JPush] 推送成功: ${data.msg_id}`);
    } else {
      console.error(`[JPush] 推送失败 (${res.status}):`, JSON.stringify(data));
      // 如果 Registration ID 无效，清掉它
      if (data.error?.code === 1008 || data.error?.message?.includes('not exist')) {
        console.log(`[JPush] Registration ID 已失效，清空用户 ${userId} 的 JPush regId`);
        userTable.updateJpushRegId(userId, null);
      }
    }
  } catch (err) {
    console.error('[JPush] 推送异常:', err.message);
  }
}

module.exports = { sendJpush };
