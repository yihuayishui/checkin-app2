package com.checkin.partner.network.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.checkin.partner.network.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 通知被滑出删除时标记已读，不启动 App
 */
class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getLongExtra("notif_id", -1L)
        if (notifId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.getApi().markNotificationRead(notifId)
                } catch (_: Exception) { }
            }
        }
    }
}
