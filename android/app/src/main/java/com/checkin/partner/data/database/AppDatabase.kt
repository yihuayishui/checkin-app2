package com.checkin.partner.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.checkin.partner.data.dao.*
import com.checkin.partner.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        PairEntity::class,
        TaskEntity::class,
        CheckinRecordEntity::class,
        RewardEntity::class,
        PointTransactionEntity::class,
        NotificationEntity::class,
        AchievementEntity::class,
        UserConfigEntity::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pairDao(): PairDao
    abstract fun taskDao(): TaskDao
    abstract fun checkinDao(): CheckinDao
    abstract fun rewardDao(): RewardDao
    abstract fun pointDao(): PointDao
    abstract fun notificationDao(): NotificationDao
    abstract fun achievementDao(): AchievementDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "checkin_partner.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
