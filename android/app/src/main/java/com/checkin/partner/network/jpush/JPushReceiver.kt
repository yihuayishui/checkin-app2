package com.checkin.partner.network.jpush

import android.content.Context
import android.util.Log
import cn.jpush.android.api.NotificationMessage
import cn.jpush.android.service.JPushMessageReceiver
import com.checkin.partner.MainActivity
import com.checkin.partner.network.api.RetrofitClient
import com.checkin.partner.network.dto.JpushRegIdRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 极光推送消息接收器（JPush SDK 原生回调方式）
 * - onRegister(Context, String)：收到 Registration ID 后上传到后端
 * - onNotifyMessageOpened：点击通知跳转页面
 */
class JPushReceiver : JPushMessageReceiver() {

    override fun onRegister(context: Context, regId: String) {
        Log.d("JPush", "onRegister: regId=$regId")
        if (regId.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.getApi().uploadJpushRegId(JpushRegIdRequest(regId))
                } catch (_: Exception) { }
            }
        }
    }

    override fun onNotifyMessageOpened(context: Context, message: NotificationMessage) {
        Log.d("JPush", "通知打开: ${message.notificationTitle}")

        // notificationExtras 是 JSON 字符串
        val extras = message.notificationExtras
        val type = if (extras != null) JSONObject(extras).optString("type", "") else ""
        val relatedId = if (extras != null) JSONObject(extras).optString("relatedId", "") else ""
        val notifId = if (extras != null) JSONObject(extras).optLong("notifId", -1L) else -1L

        val navRoute = getNavRoute(type, relatedId)

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_route", navRoute)
            putExtra("notif_id", notifId)
        }
        context.startActivity(intent)
    }

    private fun getNavRoute(type: String, relatedId: String): String {
        return when (type) {
            "checkin" -> if (relatedId.isNotEmpty()) "checkin/detail/$relatedId" else "home"
            "comment" -> if (relatedId.isNotEmpty()) "checkin/detail/$relatedId" else "home"
            "penalty" -> "home"
            "pair_request", "pair_accepted", "pair_rejected",
            "unbind_request", "unbind_accepted", "unbind_rejected",
            "vacation_start", "vacation_end" -> "pair"
            "reward_exchange", "reward_confirmed", "reward_rejected",
            "reward_delete_request", "reward_claim_request" -> "reward/list"
            "task_delete_request", "task_edit_request", "task_assign" -> "task/list"
            "password_reset_request", "password_reset_done" -> "profile"
            else -> "profile/notifications"
        }
    }
}
