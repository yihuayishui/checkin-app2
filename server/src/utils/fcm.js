// FCM HTTP v1 API 推送工具
const config = require('../config');
const userTable = require('../store/tables/user-table');
const notificationTable = require('../store/tables/notification-table');
const fs = require('fs');

// 非阻塞引用极光推送，避免冷启动时加载失败
let sendJpush = null;
try {
  sendJpush = require('./jpush').sendJpush;
} catch (_) {}

let cachedToken = null;
let tokenExpiry = 0;

/**
 * 使用服务帐号 JSON 获取 OAuth 2.0 access token
 * 文档: https://firebase.google.com/docs/cloud-messaging/auth-server
 */
async function getAccessToken() {
  // 缓存 token，过期前 5 分钟刷新
  if (cachedToken && Date.now() < tokenExpiry - 300000) {
    return cachedToken;
  }

  try {
    const keyData = JSON.parse(fs.readFileSync(config.FCM_SERVICE_ACCOUNT_PATH, 'utf8'));

    // 构造 JWT
    const header = { alg: 'RS256', typ: 'JWT' };
    const now = Math.floor(Date.now() / 1000);
    const claim = {
      iss: keyData.client_email,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      aud: 'https://oauth2.googleapis.com/token',
      exp: now + 3600,
      iat: now,
    };

    const { default: cryptoModule } = await import('crypto');
    const sign = cryptoModule.createSign('RSA-SHA256');
    sign.update(Buffer.from(JSON.stringify(header)).toString('base64url') + '.' +
                Buffer.from(JSON.stringify(claim)).toString('base64url'));
    const signature = sign.sign(keyData.private_key, 'base64url');

    const jwt = Buffer.from(JSON.stringify(header)).toString('base64url') + '.' +
                Buffer.from(JSON.stringify(claim)).toString('base64url') + '.' +
                signature;

    // 换取 access token
    const res = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion: jwt,
      }),
    });

    const data = await res.json();
    cachedToken = data.access_token;
    tokenExpiry = now * 1000 + (data.expires_in || 3600) * 1000;
    return cachedToken;
  } catch (err) {
    console.error('[FCM] 获取 access token 失败:', err.message);
    return null;
  }
}

/**
 * 通过 FCM HTTP v1 API 推送通知
 * 文档: https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages/send
 */
async function sendPush(userId, title, body, relatedId, type) {
  try {
    // 先存入数据库通知（拿到 id 用于客户端标记已读）
    const notif = notificationTable.create(userId, type, title, body, relatedId);
    const notifId = notif.id;

    // 极光推送：独立于 FCM，提前执行，确保 JPush 不受 FCM 影响
    if (sendJpush) {
      sendJpush(userId, title, body, relatedId, type, notifId).catch(e =>
        console.error('[JPush] 异步推送异常:', e.message)
      );
    }

    // FCM 推送（国内不可用，不影响 JPush）
    if (!fs.existsSync(config.FCM_SERVICE_ACCOUNT_PATH)) {
      console.log('[FCM] 服务帐号文件不存在，跳过 FCM 推送');
      return;
    }

    const fcmToken = userTable.getFcmToken(userId);
    if (!fcmToken) return;

    const accessToken = await getAccessToken();
    if (!accessToken) return;

    const url = `https://fcm.googleapis.com/v1/projects/${config.FCM_PROJECT_ID}/messages:send`;

    const payload = {
      message: {
        token: fcmToken,
        notification: {
          title: title,
          body: body,
        },
        data: {
          type: type || 'unknown',
          relatedId: relatedId || '',
          notifId: String(notifId),  // 传递通知 ID，用于滑出删除时标记已读
        },
      },
    };

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const errText = await res.text();
      console.error(`[FCM] 推送失败 (${res.status}):`, errText);
      // 如果 token 已失效（设备卸载/重新安装），清掉它避免重复推送
      try {
        const errJson = JSON.parse(errText);
        if (errJson?.error?.message === 'NotRegistered') {
          console.log(`[FCM] Token 已失效，清空用户 ${userId} 的 FCM token`);
          userTable.updateFcmToken(userId, null);
        }
      } catch (_) {}
    }
  } catch (err) {
    console.error('[FCM] 推送异常:', err.message);
  }
}

module.exports = { sendPush };
