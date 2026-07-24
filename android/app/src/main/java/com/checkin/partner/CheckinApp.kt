package com.checkin.partner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import cn.jpush.android.api.JPushInterface
import com.checkin.partner.data.database.AppDatabase
import com.checkin.partner.network.api.RetrofitClient

class CheckinApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化数据库
        database = AppDatabase.getInstance(this)

        // 初始化网络
        RetrofitClient.init(this)

        // 初始化极光推送
        JPushInterface.setDebugMode(true)
        JPushInterface.init(this)

        // 创建通知渠道
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_CHECKIN,
                    "打卡提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_REWARD,
                    "奖励通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_PAIR,
                    "搭档通知",
                    NotificationManager.IMPORTANCE_HIGH
                ),
            )
            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    companion object {
        const val CHANNEL_CHECKIN = "channel_checkin"
        const val CHANNEL_REWARD = "channel_reward"
        const val CHANNEL_PAIR = "channel_pair"

        lateinit var instance: CheckinApp
            private set
    }
}
