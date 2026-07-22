package com.checkin.partner.network.api

import com.checkin.partner.network.dto.*
import retrofit2.Response
import retrofit2.http.*

// 打卡搭档 Retrofit API 接口
interface ApiService {

    // ── 用户 ──
    @POST("api/v1/user/register")
    suspend fun register(@Body req: RegisterRequest): Response<ApiResponse<AuthData>>

    @POST("api/v1/user/login")
    suspend fun login(@Body req: LoginRequest): Response<ApiResponse<AuthData>>

    @GET("api/v1/user/me")
    suspend fun getProfile(): Response<ApiResponse<UserProfile>>

    @GET("api/v1/user/points")
    suspend fun getPoints(): Response<ApiResponse<PointsData>>

    @GET("api/v1/user/search")
    suspend fun searchUser(@Query("q") query: String): Response<ApiResponse<SearchResult>>

    @POST("api/v1/user/fcm-token")
    suspend fun uploadFcmToken(@Body req: FcmTokenRequest): Response<ApiResponse<Any>>

    @PUT("api/v1/user/avatar")
    suspend fun updateAvatar(@Body body: Map<String, String>): Response<ApiResponse<Any>>

    @PUT("api/v1/user/password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<ApiResponse<Any>>

    @POST("api/v1/user/vacation/start")
    suspend fun vacationStart(): Response<ApiResponse<Any>>

    @POST("api/v1/user/vacation/end")
    suspend fun vacationEnd(): Response<ApiResponse<Any>>

    @DELETE("api/v1/user/account")
    suspend fun deleteAccount(@Body body: Map<String, String>): Response<ApiResponse<Any>>

    // ── 搭档 ──
    @GET("api/v1/pair/status")
    suspend fun getPairStatus(): Response<ApiResponse<PairStatus>>

    @POST("api/v1/pair/request")
    suspend fun sendPairRequest(@Body req: PairRequest): Response<ApiResponse<Any>>

    @POST("api/v1/pair/cancel-request")
    suspend fun cancelPairRequest(@Body req: PairRequest): Response<ApiResponse<Any>>

    @POST("api/v1/pair/respond")
    suspend fun respondPair(@Body req: PairRespond): Response<ApiResponse<Any>>

    @POST("api/v1/pair/unbind-request")
    suspend fun unbindRequest(): Response<ApiResponse<Any>>

    @POST("api/v1/pair/unbind-respond")
    suspend fun unbindRespond(@Body body: Map<String, Boolean>): Response<ApiResponse<Any>>

    // ── 任务 ──
    @GET("api/v1/task/list")
    suspend fun getTasks(@Query("filter") filter: String? = null): Response<ApiResponse<TaskList>>

    @GET("api/v1/task/created-by-me")
    suspend fun getCreatedTasks(): Response<ApiResponse<TaskList>>

    @POST("api/v1/task/create")
    suspend fun createTask(@Body req: TaskCreateRequest): Response<ApiResponse<TaskResult>>

    @PUT("api/v1/task/update")
    suspend fun updateTask(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<TaskResult>>

    @DELETE("api/v1/task/delete/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: String): Response<ApiResponse<Any>>

    @POST("api/v1/task/delete-request/{taskId}")
    suspend fun requestDeleteTask(@Path("taskId") taskId: String): Response<ApiResponse<Any>>

    @POST("api/v1/task/delete-respond")
    suspend fun respondDeleteTask(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Any>>

    @POST("api/v1/task/{taskId}/reactivate")
    suspend fun reactivateTask(@Path("taskId") taskId: String): Response<ApiResponse<TaskResult>>

    @POST("api/v1/task/edit-request/{taskId}")
    suspend fun requestEditTask(@Path("taskId") taskId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Any>>

    @POST("api/v1/task/edit-respond")
    suspend fun respondEditTask(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ApiResponse<Any>>

    // ── 打卡 ──
    @GET("api/v1/checkin/list")
    suspend fun getCheckins(@Query("userId") userId: String, @Query("date") date: String? = null): Response<ApiResponse<CheckinList>>

    @GET("api/v1/checkin/today")
    suspend fun getTodayCheckins(@Query("userId") userId: String, @Query("taskId") taskId: String? = null): Response<ApiResponse<Any>>

    @POST("api/v1/checkin/create")
    suspend fun createCheckin(@Body req: CheckinCreateRequest): Response<ApiResponse<CheckinCreateResult>>

    @DELETE("api/v1/checkin/delete/{recordId}")
    suspend fun deleteCheckin(@Path("recordId") recordId: String): Response<ApiResponse<Any>>

    @POST("api/v1/checkin/makeup")
    suspend fun makeupCheckin(@Body body: Map<String, String>): Response<ApiResponse<Any>>

    @GET("api/v1/checkin/makeup-cards")
    suspend fun getMakeupCards(): Response<ApiResponse<MakeupCardsData>>

    @POST("api/v1/checkin/{id}/comment")
    suspend fun addComment(@Path("id") id: String, @Body body: Map<String, String>): Response<ApiResponse<Any>>

    @GET("api/v1/checkin/{id}/comments")
    suspend fun getComments(@Path("id") id: String): Response<ApiResponse<Any>>

    // ── 奖励 ──
    @GET("api/v1/reward/list")
    suspend fun getRewards(): Response<ApiResponse<RewardList>>

    @POST("api/v1/reward/create")
    suspend fun createReward(@Body req: RewardCreateRequest): Response<ApiResponse<RewardResult>>

    @DELETE("api/v1/reward/delete/{id}")
    suspend fun deleteReward(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("api/v1/reward/{id}/delete-request")
    suspend fun requestDeleteReward(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("api/v1/reward/{id}/delete-respond")
    suspend fun respondDeleteReward(@Path("id") id: String, @Body body: Map<String, Boolean>): Response<ApiResponse<Any>>

    @POST("api/v1/reward/{id}/exchange")
    suspend fun exchangeReward(@Path("id") id: String): Response<ApiResponse<RewardResult>>

    @POST("api/v1/reward/{id}/cancel-exchange")
    suspend fun cancelExchange(@Path("id") id: String): Response<ApiResponse<RewardResult>>

    @POST("api/v1/reward/{id}/confirm")
    suspend fun confirmExchange(@Path("id") id: String, @Body body: Map<String, Boolean>): Response<ApiResponse<RewardResult>>

    @POST("api/v1/reward/{id}/claim")
    suspend fun claimReward(@Path("id") id: String): Response<ApiResponse<RewardResult>>

    @POST("api/v1/reward/{id}/claim-request")
    suspend fun claimRequestReward(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("api/v1/reward/{id}/claim-respond")
    suspend fun claimRespondReward(@Path("id") id: String, @Body body: Map<String, Boolean>): Response<ApiResponse<Any>>

    // ── 看板 ──
    @GET("api/v1/dashboard/today")
    suspend fun getDashboard(): Response<ApiResponse<DashboardToday>>

    @GET("api/v1/dashboard/calendar")
    suspend fun getCalendar(@Query("month") month: String): Response<ApiResponse<CalendarData>>

    @GET("api/v1/dashboard/stats")
    suspend fun getStats(@Query("month") month: String?): Response<ApiResponse<StatsData>>

    @GET("api/v1/dashboard/streak")
    suspend fun getStreak(): Response<ApiResponse<StreakData>>

    // ── 积分 ──
    @GET("api/v1/points/transactions")
    suspend fun getTransactions(@Query("limit") limit: Int = 50): Response<ApiResponse<TransactionList>>

    // ── 通知 ──
    @GET("api/v1/notifications/list")
    suspend fun getNotifications(): Response<ApiResponse<NotificationListData>>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<ApiResponse<Any>>

    @POST("api/v1/notifications/read-all")
    suspend fun markAllRead(): Response<ApiResponse<Any>>

    @DELETE("api/v1/notifications/clear")
    suspend fun clearNotifications(): Response<ApiResponse<Any>>

    // ── 成就 ──
    @GET("api/v1/achievements/list")
    suspend fun getAchievements(): Response<ApiResponse<AchievementList>>

    // ── 同步 ──
    @GET("api/v1/sync")
    suspend fun sync(@Query("since") since: String?): Response<ApiResponse<SyncData>>

    // ── 设置 ──
    @GET("api/v1/config")
    suspend fun getConfig(): Response<ApiResponse<UserConfigData>>

    @PUT("api/v1/config")
    suspend fun updateConfig(@Body config: UserConfigData): Response<ApiResponse<UserConfigData>>
}

// ── 包装类 ──
data class SearchResult(val users: List<SearchUser>)
data class TaskList(val tasks: List<TaskData>)
data class TaskResult(val task: TaskData)
data class CheckinList(val records: List<CheckinData>)
data class RewardList(val rewards: List<RewardData>, val poolPoints: Int)
data class RewardResult(val reward: RewardData)
data class CalendarData(val month: String, val myDates: List<String>, val partnerDates: List<String>)
data class StatsData(val month: String, val myDays: Int, val partnerDays: Int)
data class TransactionList(val transactions: List<TransactionData>)
data class AchievementList(val achievements: List<Any>)
data class SyncData(
    val timestamp: String,
    val myData: SyncUserData,
    val partnerData: SyncUserData?,
    val rewards: List<RewardData>,
)
data class SyncUserData(val checkins: List<CheckinData>, val tasks: List<TaskData>)
