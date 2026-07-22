package com.checkin.partner.network.dto

import com.google.gson.annotations.SerializedName

// ── 通用响应 ──
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
)

// ── 用户 ──
data class RegisterRequest(val username: String, val password: String)
data class LoginRequest(val username: String, val password: String)
data class AuthData(
    val userId: String,
    val username: String,
    val token: String,
    val avatarUrl: String? = null,
)
data class PointsData(val personalPoints: Int, val poolPoints: Int)
data class SearchUser(val userId: String, val username: String, val avatarUrl: String? = null)
data class UserProfile(val userId: String, val username: String, val avatarUrl: String?, val isVacation: Boolean = false, val createdAt: String)
data class FcmTokenRequest(val token: String)

// ── 搭档 ──
data class PairStatus(
    val status: String,
    val pair: PairInfo?,
)
data class PairInfo(
    val pairId: String,
    val partnerId: String,
    val partnerUsername: String,
    val partnerAvatarUrl: String?,
    val partnerOnline: Boolean?,
    val requestedBy: String?,
    val unbindRequestedBy: String?,
    val createdAt: String?,
)
data class PairRequest(val targetUserId: String)
data class PairRespond(val pairId: String, val accept: Boolean)

// ── 任务 ──
data class TaskCreateRequest(
    val taskId: String? = null,
    val userId: String,
    val name: String,
    val frequency: String = "DAILY",
    val pointPerCheck: Int = 10,
    val startTime: String? = null,
    val endTime: String? = null,
)
data class TaskData(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("creator_id") val creatorId: String,
    val name: String,
    val frequency: String,
    @SerializedName("point_per_check") val pointPerCheck: Int = 10,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    @SerializedName("is_active") val isActive: Int = 1,
    val status: String = "ACTIVE",
    @SerializedName("pending_edit_by") val pendingEditBy: String? = null,
    @SerializedName("pending_delete_by") val pendingDeleteBy: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
)

// ── 打卡 ──
data class CheckinCreateRequest(
    val recordId: String? = null,
    val taskId: String,
    val userId: String,
    val note: String? = null,
    val imageUrl: String? = null,
    val checkinTime: String? = null,
)
data class CheckinData(
    @SerializedName("record_id") val recordId: String,
    @SerializedName("task_id") val taskId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("checkin_time") val checkinTime: String,
    val note: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_makeup") val isMakeup: Int = 0,
    @SerializedName("created_at") val createdAt: String = "",
)
data class CheckinCreateResult(
    val record: CheckinData,
    val points: PointsData?,
    val personalPointsEarned: Int?,
    val poolPointsEarned: Int?,
)

// ── 奖励 ──
data class RewardCreateRequest(val id: String? = null, val name: String, val requiredPoints: Int, val expiresAt: String? = null)
data class RewardData(
    val id: String,
    @SerializedName("creator_id") val creatorId: String,
    val name: String,
    @SerializedName("required_points") val requiredPoints: Int,
    @SerializedName("expires_at") val expiresAt: String?,
    val status: String,
    @SerializedName("applicant_id") val applicantId: String?,
    @SerializedName("canExchange") val canExchange: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("pending_delete_by") val pendingDeleteBy: String? = null,
    @SerializedName("claim_requested_by") val claimRequestedBy: String? = null,
)

// ── 看板 ──
data class DashboardToday(
    val todayDate: String,
    val myData: MyDashboardData,
    val partnerData: PartnerDashboardData?,
    val recentCheckins: List<CheckinData>,
)
data class MyDashboardData(
    val checkins: List<CheckinData>,
    val todayTaskStatus: List<TaskWithStatus>,
    val streak: Int,
    val personalPoints: Int,
    val poolPoints: Int,
)
data class PartnerDashboardData(
    val userId: String,
    val username: String,
    val isVacation: Boolean,
    val checkins: List<CheckinData>,
    val streak: Int,
    val todayTaskStatus: List<TaskWithStatus>,
)
data class TaskWithStatus(
    val task: TaskData,
    val checkedIn: Boolean,
)
data class StreakData(val myStreak: Int, val partnerStreak: Int)

// ── 积分流水 ──
data class TransactionData(
    val id: Long = 0,
    @SerializedName("transaction_id") val transactionId: String = "",
    @SerializedName("user_id") val userId: String = "",
    val amount: Int = 0,
    val type: String = "",
    val category: String = "",
    val description: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
)

data class MakeupCardsData(val month: String, val remaining: Int, val total: Int)

// ── 通知 ──
data class NotificationData(
    val id: Long,
    @SerializedName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String?,
    @SerializedName("related_id") val relatedId: String?,
    @SerializedName("is_read") val isRead: Int = 0,
    @SerializedName("created_at") val createdAt: String = "",
)
data class NotificationListData(val notifications: List<NotificationData>, val unreadCount: Int)

// ── 设置 ──
data class UserConfigData(
    val reminderTime: String?,
    val notifyCheckin: Boolean = true,
    val notifyReward: Boolean = true,
    val notifyPair: Boolean = true,
    val personalRatio: Float = 0.5f,
    val poolRatio: Float = 0.5f,
    val streakPenaltyOn: Boolean = false,
    val streakPenaltyDays: Int = 0,
    val themeMode: String = "system",
)

// ── 帮助函数 ──
fun TaskData.toEntity(): com.checkin.partner.data.entity.TaskEntity =
    com.checkin.partner.data.entity.TaskEntity(
        taskId = taskId, userId = userId, creatorId = creatorId,
        name = name, frequency = frequency, pointPerCheck = pointPerCheck,
        startTime = startTime, endTime = endTime,
        isActive = isActive != 0, status = status, pendingEditBy = pendingEditBy, pendingDeleteBy = pendingDeleteBy, createdAt = createdAt,
    )

fun CheckinData.toEntity(): com.checkin.partner.data.entity.CheckinRecordEntity =
    com.checkin.partner.data.entity.CheckinRecordEntity(
        recordId = recordId, taskId = taskId, userId = userId,
        checkinTime = checkinTime, note = note, imageUrl = imageUrl,
        isMakeup = isMakeup != 0, createdAt = createdAt,
    )
