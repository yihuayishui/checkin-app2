package com.checkin.partner.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/** 用户（本地缓存） */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val avatarUrl: String?,
    val personalPoints: Int = 0,
    val poolPoints: Int = 0,
    val isVacation: Boolean = false,
    val createdAt: String = "",
)

/** 搭档关系 */
@Entity(tableName = "pair")
data class PairEntity(
    @PrimaryKey val pairId: String,
    val status: String, // NONE / PENDING / BOUND
    val partnerId: String?,
    val partnerUsername: String?,
    val partnerAvatarUrl: String?,
    val requestedBy: String?,
    val unbindRequestedBy: String?,
    val createdAt: String?,
)

/** 任务 */
@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "creator_id") val creatorId: String,
    val name: String,
    val frequency: String = "DAILY",       // DAILY / WEEKLY / ONCE
    @ColumnInfo(name = "point_per_check") val pointPerCheck: Int = 10,
    @ColumnInfo(name = "start_time") val startTime: String?,  // HH:mm
    @ColumnInfo(name = "end_time") val endTime: String?,      // HH:mm
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    val status: String = "ACTIVE",          // ACTIVE / DONE
    @ColumnInfo(name = "pending_edit_by") val pendingEditBy: String? = null,
    @ColumnInfo(name = "pending_delete_by") val pendingDeleteBy: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

/** 打卡记录 */
@Entity(tableName = "checkin_record")
data class CheckinRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "checkin_time") val checkinTime: String,
    val note: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "is_makeup") val isMakeup: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

/** 奖励 */
@Entity(tableName = "reward")
data class RewardEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "creator_id") val creatorId: String,
    val name: String,
    @ColumnInfo(name = "required_points") val requiredPoints: Int,
    @ColumnInfo(name = "expires_at") val expiresAt: String?,
    val status: String = "ACTIVE",
    @ColumnInfo(name = "applicant_id") val applicantId: String?,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
    @ColumnInfo(name = "pending_delete_by") val pendingDeleteBy: String? = null,
    @ColumnInfo(name = "claim_requested_by") val claimRequestedBy: String? = null,
)

/** 积分流水 */
@Entity(tableName = "point_transaction")
data class PointTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val amount: Int,
    val type: String,         // EARN / SPEND
    val category: String,     // CHECKIN / REWARD / REVERT / CHECKIN_POOL
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

/** 通知 */
@Entity(tableName = "notification")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String?,
    @ColumnInfo(name = "related_id") val relatedId: String?,
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

/** 成就 */
@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val code: String,
    val name: String,
    val icon: String,
    @ColumnInfo(name = "description") val desc: String,
    val unlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: String?,
)

/** 用户设置 */
@Entity(tableName = "user_config")
data class UserConfigEntity(
    @PrimaryKey val userId: String,
    @ColumnInfo(name = "reminder_time") val reminderTime: String?,
    @ColumnInfo(name = "notify_checkin") val notifyCheckin: Boolean = true,
    @ColumnInfo(name = "notify_reward") val notifyReward: Boolean = true,
    @ColumnInfo(name = "notify_pair") val notifyPair: Boolean = true,
    @ColumnInfo(name = "personal_ratio") val personalRatio: Float = 0.5f,
    @ColumnInfo(name = "pool_ratio") val poolRatio: Float = 0.5f,
    @ColumnInfo(name = "streak_penalty_on") val streakPenaltyOn: Boolean = false,
    @ColumnInfo(name = "theme_mode") val themeMode: String = "system",
)
