package com.checkin.partner.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.checkin.partner.CheckinApp
import com.checkin.partner.MainActivity
import com.checkin.partner.R
import com.checkin.partner.network.api.RetrofitClient
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 打卡提醒 Worker
 * 在设定的提醒时间检查用户是否已打卡，未打卡则发送系统通知
 */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val token = RetrofitClient.token ?: return Result.success()

        return try {
            val api = RetrofitClient.getApi()
            val userId = RetrofitClient.getSavedUserId() ?: return Result.success()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

            // 调用API查今日打卡
            val res = kotlinx.coroutines.runBlocking {
                api.getTodayCheckins(userId)
            }
            if (res.isSuccessful && res.body()?.code == 200) {
                val data = res.body()?.data as? Map<*, *>
                val records = data?.get("records") as? List<*>
                if (records.isNullOrEmpty()) {
                    showNotification(applicationContext, "⏰ 打卡提醒", "今天还没打卡哦，快去完成任务吧！")
                }
            }
            Result.success()
        } catch (e: Exception) {
            // 网络不通不影响提醒（简单策略：直接提醒）
            showNotification(applicationContext, "⏰ 打卡提醒", "别忘了今天的打卡哦~")
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "checkin_reminder"

        /**
         * 根据 HH:mm 格式的提醒时间调度每日提醒
         */
        fun schedule(context: Context, reminderTime: String?) {
            val workManager = WorkManager.getInstance(context)

            if (reminderTime.isNullOrBlank() || reminderTime.length < 5) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val parts = reminderTime.split(":")
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }

            val delayMinutes = ((target.timeInMillis - now.timeInMillis) / 60000).coerceAtLeast(1)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        private fun showNotification(context: Context, title: String, body: String) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CheckinApp.CHANNEL_CHECKIN)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }
}
