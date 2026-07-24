package com.checkin.partner.network.fcm

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.checkin.partner.CheckinApp
import com.checkin.partner.MainActivity
import com.checkin.partner.R
import com.checkin.partner.network.api.RetrofitClient
import com.checkin.partner.network.dto.FcmTokenRequest
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckinFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "新Token: $token")

        // 上传到后端
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.getApi().uploadFcmToken(FcmTokenRequest(token))
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "打卡搭档"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "unknown"
        val relatedId = message.data["relatedId"]
        val notifId = message.data["notifId"]?.toLongOrNull() ?: -1L

        val channelId = when (type) {
            "checkin", "comment" -> CheckinApp.CHANNEL_CHECKIN
            "reward_exchange", "reward_confirmed", "reward_claimed" -> CheckinApp.CHANNEL_REWARD
            else -> CheckinApp.CHANNEL_PAIR
        }

        // 点击通知 → 打开 App
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_route", getNavRoute(type, relatedId))
            putExtra("notif_id", notifId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 滑出删除 → 标记已读，不启动 App
        val deleteIntent = Intent(this, NotificationDismissReceiver::class.java).apply {
            putExtra("notif_id", notifId)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            this, 9999, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .build()

        // Android 13+ 需要通知权限，没有权限则不弹
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        // 使用 notifId 作为通知 ID，与 WebSocket 弹的通知共用同一 ID
        // 如果两个通道同时到达，后到的覆盖先到的，避免重复通知
        val notificationId = if (notifId > 0) notifId.toInt() else System.currentTimeMillis().toInt()
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun getNavRoute(type: String, relatedId: String?): String {
        return when (type) {
            "checkin" -> if (relatedId != null) "checkin/detail/$relatedId" else "home"
            "comment" -> if (relatedId != null) "checkin/detail/$relatedId" else "home"
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
