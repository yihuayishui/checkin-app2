package com.checkin.partner.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.checkin.partner.CheckinApp
import com.checkin.partner.R
import com.checkin.partner.data.entity.*
import com.checkin.partner.network.api.RetrofitClient
import com.checkin.partner.network.dto.*
import com.checkin.partner.network.ws.WebSocketManager
import cn.jpush.android.api.JPushInterface
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitClient.getApi()
    private val db = (application as CheckinApp).database
    val ws = WebSocketManager()

    // ── 认证状态 ──
    private val _isLoggedIn = MutableStateFlow(RetrofitClient.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentUsername = MutableStateFlow("")
    val currentUsername: StateFlow<String> = _currentUsername.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _isVacation = MutableStateFlow(false)
    val isVacation: StateFlow<Boolean> = _isVacation.asStateFlow()

    // ── 看板 ──
    private val _dashboard = MutableStateFlow<DashboardToday?>(null)
    val dashboard: StateFlow<DashboardToday?> = _dashboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 首次加载状态：页面在网络返回前显示骨架屏，避免空白闪现。
    private val _dashboardLoaded = MutableStateFlow(false)
    val dashboardLoaded: StateFlow<Boolean> = _dashboardLoaded.asStateFlow()

    private val _tasksLoaded = MutableStateFlow(false)
    val tasksLoaded: StateFlow<Boolean> = _tasksLoaded.asStateFlow()

    private val _rewardsLoaded = MutableStateFlow(false)
    val rewardsLoaded: StateFlow<Boolean> = _rewardsLoaded.asStateFlow()

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── 任务 ──
    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasks: StateFlow<List<TaskEntity>> = _tasks.asStateFlow()

    // ── 奖励 ──
    private val _rewards = MutableStateFlow<List<RewardEntity>>(emptyList())
    val rewards: StateFlow<List<RewardEntity>> = _rewards.asStateFlow()
    private val _rewardVersion = MutableStateFlow(0)
    val rewardVersion: StateFlow<Int> = _rewardVersion.asStateFlow()
    private val _poolPoints = MutableStateFlow(0)
    val poolPoints: StateFlow<Int> = _poolPoints.asStateFlow()

    // ── 成就 ──
    private val _achievements = MutableStateFlow<List<com.checkin.partner.network.api.AchievementData>>(emptyList())
    val achievements: StateFlow<List<com.checkin.partner.network.api.AchievementData>> = _achievements.asStateFlow()

    // ── 成就解锁弹窗 ──
    private val _achievementUnlocked = MutableStateFlow<NewAchievement?>(null)
    val achievementUnlocked: StateFlow<NewAchievement?> = _achievementUnlocked.asStateFlow()

    fun clearAchievementUnlocked() { _achievementUnlocked.value = null }

    // ── 打卡成功弹窗 ──
    private val _checkinSuccess = MutableStateFlow<CheckinSuccess?>(null)
    val checkinSuccess: StateFlow<CheckinSuccess?> = _checkinSuccess.asStateFlow()

    fun clearCheckinSuccess() { _checkinSuccess.value = null }

    // ── 打卡按钮乐观更新 ──
    // 点击"打卡"后立即本地置为已完成（不等网络），避免"点击后要等一会儿才变已完成"的错觉。
    // 服务端最终状态以 dashboard 刷新为准；失败/回滚时从乐观集合移除，按钮恢复可点。
    private val _optimisticCheckin = MutableStateFlow<Set<String>>(emptySet())
    val optimisticCheckin: StateFlow<Set<String>> = _optimisticCheckin.asStateFlow()

    // 正在提交打卡的任务 id（按钮显示加载中，防重复点击）
    private val _checkingTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val checkingTaskIds: StateFlow<Set<String>> = _checkingTaskIds.asStateFlow()

    // ── 通知 ──
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    // ── 个人积分 ──
    private val _personalPoints = MutableStateFlow(0)
    val personalPoints: StateFlow<Int> = _personalPoints.asStateFlow()

    // ── 积分流水 ──
    private val _transactions = MutableStateFlow<List<PointTransactionEntity>>(emptyList())
    val transactions: StateFlow<List<PointTransactionEntity>> = _transactions.asStateFlow()

    // ── 离线待补打卡 ──
    private val _pendingCheckinCount = MutableStateFlow(0)
    val pendingCheckinCount: StateFlow<Int> = _pendingCheckinCount.asStateFlow()
    private val _pendingCheckinTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingCheckinTaskIds: StateFlow<Set<String>> = _pendingCheckinTaskIds.asStateFlow()

    // ── 搭档 ──
    private val _pairStatus = MutableStateFlow<PairStatus?>(null)
    val pairStatus: StateFlow<PairStatus?> = _pairStatus.asStateFlow()

    // 搭档在线状态（WebSocket 实时更新，null=未知）
    private val _partnerOnline = MutableStateFlow<Boolean?>(null)
    val partnerOnline: StateFlow<Boolean?> = _partnerOnline.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchUser>>(emptyList())
    val searchResults: StateFlow<List<SearchUser>> = _searchResults.asStateFlow()

    private val _partnerUsername = MutableStateFlow<String?>(null)
    val partnerUsername: StateFlow<String?> = _partnerUsername.asStateFlow()

    // ── 持久化已通知到系统通知栏的 ID，避免重复弹窗 ──
    private val prefs = application.getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var dashboardRefreshJob: Job? = null
    private var notificationRefreshJob: Job? = null
    private var notificationAlertPending = false
    private val shownNotificationIds: MutableSet<Long> = loadShownIds()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private fun loadShownIds(): MutableSet<Long> {
        val s = prefs.getString("shown_notification_ids", null)
        if (s != null) {
            return try {
                s.split(",").mapNotNull { it.toLongOrNull() }.toMutableSet()
            } catch (_: Exception) { mutableSetOf() }
        }
        // 兼容旧版本：之前用 StringSet 存的，读出来转成新格式再删掉
        val oldSet = prefs.getStringSet("shown_notification_ids", null) ?: return mutableSetOf()
        val ids = oldSet.mapNotNull { it.toLongOrNull() }.toMutableSet()
        prefs.edit().remove("shown_notification_ids").apply()
        saveShownIds() // 存成新格式
        return ids
    }

    private fun saveShownIds() {
        prefs.edit().putString("shown_notification_ids", shownNotificationIds.joinToString(",")).apply()
    }

    // ── 通知点击导航事件 ──
    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    init {
        // token 失效（401）→ 清登录态、断开 WS，由导航层跳回登录页。
        // 回调可能在 OkHttp 线程并发触发多次，用 _isLoggedIn 判断保证幂等。
        RetrofitClient.onSessionExpired = {
            if (_isLoggedIn.value) {
                _isLoggedIn.value = false
                _currentUserId.value = ""
                _partnerOnline.value = null
                _error.value = "登录已过期，请重新登录"
                ws.disconnect()
            }
        }

        // 网络恢复监听 → 自动补发离线打卡
        registerNetworkMonitor()

        // 恢复登录状态
        val savedToken = RetrofitClient.token
        if (!savedToken.isNullOrEmpty()) {
            _isLoggedIn.value = true
            _currentUserId.value = RetrofitClient.getSavedUserId() ?: ""
            _currentUsername.value = RetrofitClient.getSavedUsername() ?: ""

            // 冷启动：连接 WebSocket + 拉取通知（弹系统通知栏）+ 上传 FCM/JPush Token + 加载头像
            val uid = _currentUserId.value
            if (uid.isNotBlank()) {
                // 先恢复本地快照，再静默请求网络。这样从后台回来或进程被系统回收后，
                // 首页不会先出现空白和 0 分，而是立即显示上次已知的数据。
                restoreLocalState(uid)
                ws.connect(uid)
                setupWsListeners()
                refreshNotifications()
                uploadCurrentFcmToken()
                uploadJpushRegId()
                loadAvatar()
                refreshAchievements()
                loadPendingCheckinState()
                flushPendingCheckins()
            }
        }
    }

    /**
     * 恢复当前账号的本地快照。所有数据都按 userId 隔离，避免切换账号时串数据。
     */
    private fun restoreLocalState(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = db.userDao().getById(userId)
            // 看板缓存只在当天有效：过期的缓存会让首页一直显示旧的打卡状态，
            // 甚至让昨天打过卡的任务显示"已完成"、连打卡按钮都不出现
            val todayBeijing = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("GMT+8")
            }.format(Date())
            val cachedDashboard = prefs.getString("dashboard_cache_$userId", null)?.let { json ->
                runCatching { gson.fromJson(json, DashboardToday::class.java) }.getOrNull()
            }?.takeIf { it.todayDate == todayBeijing }
            val cachedPair = prefs.getString("pair_cache_$userId", null)?.let { json ->
                runCatching { gson.fromJson(json, PairStatus::class.java) }.getOrNull()
            }
            val tasks = db.taskDao().getByUserId(userId)
            val rewards = db.rewardDao().getAll()
            val notifications = db.notificationDao().getByUserId(userId)
            val unread = db.notificationDao().unreadCount(userId)

            // 只把仍属于当前账号的快照写入 StateFlow。
            if (_currentUserId.value != userId) return@launch
            user?.let {
                _currentUsername.value = it.username
                _avatarUrl.value = it.avatarUrl
                _personalPoints.value = it.personalPoints
                _poolPoints.value = it.poolPoints
                _isVacation.value = it.isVacation
            }
            cachedDashboard?.let { dashboard ->
                _dashboard.value = dashboard
                _personalPoints.value = dashboard.myData.personalPoints
                _poolPoints.value = dashboard.myData.poolPoints
            }
            cachedPair?.let { status ->
                _pairStatus.value = status
                _partnerUsername.value = status.pair?.partnerUsername
                _partnerOnline.value = status.pair?.partnerOnline
            }
            _tasks.value = tasks
            _rewards.value = rewards
            _notifications.value = notifications
            _unreadCount.value = unread
            _dashboardLoaded.value = cachedDashboard != null
            _tasksLoaded.value = tasks.isNotEmpty()
            _rewardsLoaded.value = rewards.isNotEmpty()
            // 本地没有用户快照时也不要让“我的”页无限停留在骨架屏：
            // 登录态已经恢复，至少可以先展示已保存的用户名，网络返回后再补齐积分和头像。
            _profileLoaded.value = user != null || _currentUsername.value.isNotBlank()
        }
    }

    private fun saveDashboardCache(userId: String, dashboard: DashboardToday) {
        prefs.edit().putString("dashboard_cache_$userId", gson.toJson(dashboard)).apply()
    }

    private fun savePairCache(userId: String, status: PairStatus) {
        prefs.edit().putString("pair_cache_$userId", gson.toJson(status)).apply()
    }

    /** 解析非 2xx 响应 errorBody 里的 {code, message}，拿到服务端真实错误信息（body() 此时为 null） */
    private fun errorMessageOf(res: retrofit2.Response<*>): String? {
        val raw = try { res.errorBody()?.string() } catch (_: Exception) { null } ?: return null
        return runCatching { gson.fromJson(raw, ApiErrorBody::class.java).message }.getOrNull()
    }

    // ── 网络状态监听（网络恢复 → 补发离线打卡） ──

    private fun registerNetworkMonitor() {
        try {
            val cm = getApplication<Application>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    flushPendingCheckins()
                }
            })
        } catch (_: Exception) {}
    }

    // ── 离线待补打卡 ──

    private companion object {
        /** 离线补打卡对同一记录的最大重试次数（仅服务端 5xx 时累计），防止无限重试 */
        const val MAX_PENDING_ATTEMPTS = 5
    }

    private fun loadPendingCheckinState() {
        viewModelScope.launch {
            val uid = _currentUserId.value
            val list = if (uid.isBlank()) emptyList()
                else db.pendingCheckinDao().getAll().filter { it.userId == uid }
            _pendingCheckinCount.value = list.size
            _pendingCheckinTaskIds.value = list.map { it.taskId }.toSet()
        }
    }

    /** 补发离线打卡：成功删除 / 401 或业务错误(4xx)删除 / 5xx 计入重试次数，超过上限放弃 */
    fun flushPendingCheckins() {
        viewModelScope.launch {
            val uid = _currentUserId.value
            if (uid.isBlank()) return@launch
            val pendingList = db.pendingCheckinDao().getAll().filter { it.userId == uid }
            if (pendingList.isEmpty()) return@launch
            for (p in pendingList) {
                try {
                    val res = api.createCheckin(CheckinCreateRequest(
                        recordId = p.recordId,
                        taskId = p.taskId, userId = p.userId,
                        note = p.note, imageUrl = p.imageUrl,
                    ))
                    if (res.isSuccessful && res.body()?.code == 200) {
                        db.pendingCheckinDao().deleteById(p.id)
                        // 补发解锁成就也弹窗
                        val firstNew = res.body()!!.data?.newAchievements?.firstOrNull()
                        if (firstNew != null) _achievementUnlocked.value = firstNew
                    } else if (res.code() == 401) {
                        // token 已失效：保留只会无限重试，直接放弃（重新登录后如还在打卡窗口内可手动补）
                        db.pendingCheckinDao().deleteById(p.id)
                    } else if (res.code() in 400..499) {
                        // 业务错误（已打过/时间窗不符）：删除避免死循环
                        db.pendingCheckinDao().deleteById(p.id)
                        _error.value = errorMessageOf(res) ?: "补打卡失败（${res.code()}）"
                    } else {
                        // 5xx：服务端问题，计入重试次数，超过上限放弃，避免无限重试
                        if (p.attempts + 1 >= MAX_PENDING_ATTEMPTS) {
                            db.pendingCheckinDao().deleteById(p.id)
                        } else {
                            db.pendingCheckinDao().incrementAttempts(p.id)
                        }
                    }
                } catch (_: Exception) {
                    // 网络仍不通：停止本轮补发，等下次网络恢复
                    break
                }
            }
            loadPendingCheckinState()
            refreshDashboard()
        }
    }

    // ── 认证 ──

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val res = api.login(LoginRequest(username, password))
                if (res.isSuccessful && res.body()?.code == 200) {
                    val data = res.body()!!.data!!
                    RetrofitClient.saveToken(data.token, data.userId, data.username)
                    _currentUserId.value = data.userId
                    _currentUsername.value = data.username
                    _isLoggedIn.value = true
                    _avatarUrl.value = data.avatarUrl
                    // 缓存用户信息
                    db.userDao().insert(UserEntity(
                        userId = data.userId, username = data.username, avatarUrl = data.avatarUrl
                    ))
                    _profileLoaded.value = true
                    // WebSocket 连接
                    ws.connect(data.userId)
                    setupWsListeners()
                    refreshDashboard()
                    uploadCurrentFcmToken()
                    uploadJpushRegId()
                    refreshAchievements()
                    loadPendingCheckinState()
                    flushPendingCheckins()
                } else {
                    _error.value = errorMessageOf(res) ?: "登录失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val res = api.register(RegisterRequest(username, password))
                if (res.isSuccessful && res.body()?.code == 200) {
                    val data = res.body()!!.data!!
                    RetrofitClient.saveToken(data.token, data.userId, data.username)
                    _currentUserId.value = data.userId
                    _currentUsername.value = data.username
                    _isLoggedIn.value = true
                    db.userDao().insert(UserEntity(
                        userId = data.userId, username = data.username, avatarUrl = data.avatarUrl
                    ))
                    _profileLoaded.value = true
                    ws.connect(data.userId)
                    setupWsListeners()
                    refreshDashboard()
                    uploadCurrentFcmToken()
                    uploadJpushRegId()
                    refreshAchievements()
                } else {
                    _error.value = errorMessageOf(res) ?: "注册失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        ws.disconnect()
        RetrofitClient.clearToken()
        _isLoggedIn.value = false
        _currentUserId.value = ""
        _partnerOnline.value = null
        _dashboardLoaded.value = false
        _tasksLoaded.value = false
        _rewardsLoaded.value = false
        _profileLoaded.value = false
        _dashboard.value = null
        _pairStatus.value = null
        viewModelScope.launch {
            // 看板/搭档缓存按 userId 隔离存在 SharedPreferences 里，注销时全部清掉，
            // 避免下次登录（含换账号）先恢复到陈旧快照
            prefs.all.keys.filter { it.startsWith("dashboard_cache_") || it.startsWith("pair_cache_") }
                .forEach { prefs.edit().remove(it).apply() }
            db.userDao().clearAll()
            db.taskDao().clearAll()
            db.checkinDao().clearAll()
            db.rewardDao().clearAll()
            db.pendingCheckinDao().clearAll()
        }
        loadPendingCheckinState()
    }

    // ── 全量刷新（下拉刷新用） ──

    fun refreshAll() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val startTime = System.currentTimeMillis()
            try {
                refreshDashboard()
                refreshTasks()
                refreshRewards()
                refreshAchievements()
                getPairStatus()
                loadConfig()
                loadVacationState()
                refreshTransactions()
                loadMakeupCards()
                refreshNotifications()
                // 给用户一个稳定的刷新反馈，同时不再占用全局 isLoading 状态。
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 650) kotlinx.coroutines.delay(650 - elapsed)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ── 看板 ──

    fun refreshDashboard() {
        if (dashboardRefreshJob?.isActive == true) return
        dashboardRefreshJob = viewModelScope.launch {
            try {
                val res = api.getDashboard()
                if (res.isSuccessful && res.body()?.data != null) {
                    val data = res.body()!!.data!!
                    _dashboard.value = data
                    _personalPoints.value = data.myData.personalPoints
                    _poolPoints.value = data.myData.poolPoints
                    saveDashboardCache(_currentUserId.value, data)
                    // 同步更新用户快照，下一次启动可直接显示最新积分。
                    // 头像请求可能和看板请求并发，未返回前要保留数据库里的旧头像。
                    val existingUser = db.userDao().getById(_currentUserId.value)
                    db.userDao().insert(
                        UserEntity(
                            userId = _currentUserId.value,
                            username = _currentUsername.value.ifBlank { existingUser?.username ?: "" },
                            avatarUrl = _avatarUrl.value ?: existingUser?.avatarUrl,
                            personalPoints = data.myData.personalPoints,
                            poolPoints = data.myData.poolPoints,
                            isVacation = _isVacation.value,
                        )
                    )
                }
            } catch (_: Exception) {
                // 网络失败时保留已有 dashboard，不回退成空状态。
            } finally {
                _dashboardLoaded.value = true
                dashboardRefreshJob = null
            }
        }
    }

    fun refreshTasks(filter: String? = "mine") {
        viewModelScope.launch {
            try {
                val res = api.getTasks(filter)
                if (res.isSuccessful && res.body()?.data != null) {
                    val list = res.body()!!.data!!.tasks.map { it.toEntity() }
                    _tasks.value = list
                    // 只有 "mine"/"created"（与 Room 缓存语义一致）才写缓存；
                    // for_partner/from_partner/done 是子集数据，写入会把缓存写脏
                    if (filter == null || filter == "mine" || filter == "created") {
                        db.taskDao().clearAll()
                        db.taskDao().insertAll(list)
                    }
                }
            } catch (_: Exception) {
                // 离线或请求失败：只对 "mine" 和 "created" 使用缓存（其他过滤条件缓存不准确）
                if (filter == "mine" || filter == "created") {
                    _tasks.value = db.taskDao().getByUserId(_currentUserId.value)
                }
                // 其他过滤条件（for_partner/from_partner/done）失败时不覆盖列表，保持已有数据
            } finally {
                _tasksLoaded.value = true
            }
        }
    }

    fun createTask(name: String, assignTo: String? = null, frequency: String = "DAILY",
                   pointPerCheck: Int = 10, startTime: String? = null, endTime: String? = null,
                   editTaskId: String? = null, creatorId: String? = null,
                   requireApproval: Boolean = false,
                   onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("VM", "createTask: editTaskId=$editTaskId, creatorId=$creatorId, uid=${_currentUserId.value}")
                if (editTaskId != null && creatorId != null && creatorId != _currentUserId.value) {
                    android.util.Log.d("VM", "→ requestEditTask")
                    val res = api.requestEditTask(editTaskId, mapOf<String, Any>(
                        "taskId" to editTaskId!!, "name" to name, "frequency" to frequency,
                        "pointPerCheck" to pointPerCheck, "startTime" to (startTime ?: ""), "endTime" to (endTime ?: ""),
                        "requireApproval" to requireApproval,
                    ))
                    android.util.Log.d("VM", "editRes: code=${res.code()}")
                    _error.value = res.body()?.message ?: "编辑请求已发送"
                } else if (editTaskId != null) {
                    android.util.Log.d("VM", "→ updateTask direct")
                    api.updateTask(mapOf<String, Any>(
                        "taskId" to editTaskId!!, "name" to name, "frequency" to frequency,
                        "pointPerCheck" to pointPerCheck, "startTime" to (startTime ?: ""), "endTime" to (endTime ?: ""),
                        "requireApproval" to requireApproval,
                    ))
                } else {
                    val userId = assignTo ?: _currentUserId.value
                    api.createTask(TaskCreateRequest(
                        taskId = UUID.randomUUID().toString(), userId = userId, name = name,
                        frequency = frequency, pointPerCheck = pointPerCheck,
                        startTime = startTime, endTime = endTime,
                        requireApproval = requireApproval,
                    ))
                }
                refreshTasks()
            } catch (e: Exception) {
                _error.value = e.message
                android.util.Log.e("VM", "createTask error", e)
            } finally {
                _isLoading.value = false
                onDone?.invoke()
            }
        }
    }

    fun respondEditTask(taskId: String, accept: Boolean, filter: String? = null) {
        viewModelScope.launch {
            try { api.respondEditTask(mapOf("taskId" to taskId, "accept" to accept)); refreshTasks(filter) } catch (_: Exception) {}
        }
    }

    fun respondTaskDelete(taskId: String, accept: Boolean, filter: String? = null) {
        viewModelScope.launch {
            try {
                val res = api.respondDeleteTask(mapOf("taskId" to taskId, "accept" to accept))
                android.util.Log.d("VM", "respondTaskDelete: code=${res.code()}, msg=${res.body()?.message}")
                refreshTasks(filter)
            } catch (e: Exception) {
                android.util.Log.e("VM", "respondTaskDelete error", e)
            }
        }
    }

    fun deleteTask(taskId: String, creatorId: String? = null, filter: String? = null) {
        viewModelScope.launch {
            try {
                if (creatorId != null && creatorId != _currentUserId.value) {
                    // 搭档创建的任务，需要对方确认
                    api.requestDeleteTask(taskId)
                    refreshTasks(filter)
                    _error.value = null
                } else {
                    api.deleteTask(taskId)
                    refreshTasks(filter)
                }
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun respondDeleteTask(taskId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                api.respondDeleteTask(mapOf("taskId" to taskId, "accept" to accept))
                refreshTasks()
            } catch (_: Exception) {}
        }
    }

    fun reactivateTask(taskId: String, filter: String? = null) {
        viewModelScope.launch {
            try { api.reactivateTask(taskId); refreshTasks(filter) } catch (_: Exception) {}
        }
    }

    // ── 打卡 ──

    fun doCheckin(taskId: String, note: String? = null, imageUrl: String? = null) {
        // 乐观更新：点击立即显示"已完成"，不等网络返回；成功后随 dashboard 刷新自然对齐
        _checkingTaskIds.value = _checkingTaskIds.value + taskId
        _optimisticCheckin.value = _optimisticCheckin.value + taskId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val recordId = UUID.randomUUID().toString()
            try {
                val res = api.createCheckin(CheckinCreateRequest(
                    recordId = recordId,
                    taskId = taskId, userId = _currentUserId.value,
                    note = note, imageUrl = imageUrl,
                ))
                if (res.isSuccessful && res.body()?.code == 200) {
                    val result = res.body()!!.data
                    if (result?.status == "PENDING") {
                        _error.value = result.message ?: "打卡已提交，等待搭档确认"
                    }
                    // 新解锁成就弹窗
                    val firstNew = result?.newAchievements?.firstOrNull()
                    if (firstNew != null) {
                        _achievementUnlocked.value = firstNew
                    } else if (result?.status != "PENDING") {
                        // 打卡成功弹窗（无新成就、非待确认时）
                        val taskName = _dashboard.value?.myData?.todayTaskStatus
                            ?.firstOrNull { it.task.taskId == taskId }?.task?.name
                            ?: _tasks.value.firstOrNull { it.taskId == taskId }?.name
                            ?: ""
                        _checkinSuccess.value = CheckinSuccess(
                            taskName = taskName,
                            personalPoints = result?.personalPointsEarned ?: 0,
                            poolPoints = result?.poolPointsEarned ?: 0,
                            streak = _dashboard.value?.myData?.streak ?: 0,
                        )
                    }
                    refreshDashboard()
                } else {
                    // 非 2xx 时 body() 为 null，真实原因（401 登录过期 / 409 已打卡 / 400 时间窗）在 errorBody 里
                    _error.value = if (res.code() == 401) "登录已过期，请重新登录"
                        else errorMessageOf(res) ?: "打卡失败"
                    // 打卡失败：回滚乐观状态，按钮恢复
                    _optimisticCheckin.value = _optimisticCheckin.value - taskId
                }
            } catch (e: IOException) {
                // 网络异常：存入本地队列，网络恢复后自动补打，打卡不丢失（保留乐观"已完成"态，待补发）
                db.pendingCheckinDao().insert(PendingCheckinEntity(
                    recordId = recordId,
                    taskId = taskId, userId = _currentUserId.value,
                    note = note, imageUrl = imageUrl,
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                ))
                loadPendingCheckinState()
                _error.value = "网络异常，打卡已保存，联网后自动补打"
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
                _optimisticCheckin.value = _optimisticCheckin.value - taskId
            } finally {
                _checkingTaskIds.value = _checkingTaskIds.value - taskId
                _isLoading.value = false
            }
        }
    }

    // ── 打卡确认/拒绝（创建者对搭档的打卡） ──

    fun approveCheckin(recordId: String) {
        viewModelScope.launch {
            try {
                val res = api.approveCheckin(recordId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    refreshDashboard()
                    refreshNotifications()
                }
            } catch (_: Exception) {}
        }
    }

    fun rejectCheckin(recordId: String) {
        viewModelScope.launch {
            try {
                val res = api.rejectCheckin(recordId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    refreshDashboard()
                }
            } catch (_: Exception) {}
        }
    }

    // ── 奖励 ──

    private suspend fun fetchRewards() {
        try {
            val res = api.getRewards()
            android.util.Log.d("VM", "fetchRewards: code=${res.code()}, hasData=${res.body()?.data != null}")
            if (res.isSuccessful && res.body()?.data != null) {
                val rl = res.body()!!.data!!
                val newList = rl.rewards.map {
                    RewardEntity(it.id, it.creatorId, it.name, it.requiredPoints, it.expiresAt,
                        it.status, it.applicantId, it.createdAt, it.pendingDeleteBy, it.claimRequestedBy)
                }
                newList.forEach { android.util.Log.d("VM", "reward: ${it.name} status=${it.status} pendingDel=${it.pendingDeleteBy}") }
                _rewards.value = newList
                _poolPoints.value = rl.poolPoints
            }
        } catch (e: Exception) {
            android.util.Log.e("VM", "fetchRewards error", e)
        } finally {
            _rewardsLoaded.value = true
        }
    }

    fun notifyRewardsChanged() {
        _rewardVersion.value++
    }

    fun refreshRewards() {
        viewModelScope.launch { fetchRewards() }
    }

    fun createReward(name: String, points: Int) {
        viewModelScope.launch {
            try {
                api.createReward(RewardCreateRequest(name = name, requiredPoints = points))
                refreshRewards()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun exchangeReward(rewardId: String) {
        viewModelScope.launch {
            try {
                api.exchangeReward(rewardId)
                refreshRewards()
                refreshDashboard()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun confirmExchange(rewardId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                api.confirmExchange(rewardId, mapOf("accept" to accept))
                refreshRewards()
                refreshDashboard()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun claimReward(rewardId: String) {
        viewModelScope.launch {
            try {
                api.claimRequestReward(rewardId)
                refreshRewards()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun respondClaimReward(rewardId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                api.claimRespondReward(rewardId, mapOf("accept" to accept))
                refreshRewards()
            } catch (_: Exception) {}
        }
    }

    fun cancelExchange(rewardId: String) {
        viewModelScope.launch {
            try { api.cancelExchange(rewardId); refreshRewards() } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteReward(rewardId: String) {
        viewModelScope.launch {
            try {
                api.requestDeleteReward(rewardId)
                refreshRewards()
            } catch (_: Exception) {}
        }
    }

    fun respondDeleteReward(rewardId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                val res = api.respondDeleteReward(rewardId, mapOf("accept" to accept))
                android.util.Log.d("VM", "respondDeleteReward: accept=$accept, code=${res.code()}")
                fetchRewards()
                notifyRewardsChanged()
            } catch (e: Exception) {
                android.util.Log.e("VM", "respondDeleteReward error", e)
                _error.value = e.message
            }
        }
    }

    // ── 搭档 ──

    fun getPairStatus() {
        viewModelScope.launch {
            try {
                val res = api.getPairStatus()
                if (res.isSuccessful && res.body()?.data != null) {
                    val status = res.body()!!.data!!
                    _pairStatus.value = status
                    _partnerUsername.value = status.pair?.partnerUsername
                    _partnerOnline.value = status.pair?.partnerOnline
                    savePairCache(_currentUserId.value, status)
                }
            } catch (_: Exception) {}
        }
    }

    fun searchUser(query: String) {
        viewModelScope.launch {
            try {
                val res = api.searchUser(query)
                if (res.isSuccessful && res.body()?.data != null) {
                    _searchResults.value = res.body()!!.data!!.users
                }
            } catch (_: Exception) {}
        }
    }

    fun sendPairRequest(targetUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                api.sendPairRequest(PairRequest(targetUserId))
                getPairStatus()
                _searchResults.value = emptyList()
            } catch (e: Exception) { _error.value = e.message }
            finally { _isLoading.value = false }
        }
    }

    fun cancelPairRequest(targetUserId: String) {
        viewModelScope.launch {
            try { api.cancelPairRequest(PairRequest(targetUserId)); getPairStatus() }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun respondPair(pairId: String, accept: Boolean) {
        viewModelScope.launch {
            try { api.respondPair(PairRespond(pairId, accept)); getPairStatus() }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun unbindRequest() {
        viewModelScope.launch {
            try { api.unbindRequest(); getPairStatus() }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun unbindRespond(accept: Boolean) {
        viewModelScope.launch {
            try { api.unbindRespond(mapOf("accept" to accept)); getPairStatus() }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    // ── WebSocket ──

    private fun setupWsListeners() {
        ws.on("partner:checkin") {
            refreshDashboard()
            refreshNotifications()
        }
        ws.on("points:update") {
            refreshDashboard()
        }
        ws.on("reward:sync") {
            refreshRewards()
            refreshNotifications()
        }
        ws.on("pair:notification") {
            getPairStatus()
            refreshDashboard()
            refreshNotifications()
        }
        ws.on("task:sync") {
            refreshTasks()
            refreshNotifications()
        }
        ws.on("task:delete") {
            refreshTasks()
            refreshNotifications()
        }
        // 搭档上下线实时事件：{ userId, online, timestamp }
        ws.on("partner:online") { payload ->
            try {
                val obj = org.json.JSONObject(payload)
                _partnerOnline.value = obj.optBoolean("online")
            } catch (_: Exception) {}
        }
    }

    fun clearError() { _error.value = null }

    // ── FCM Token 主动上传 ──

    /**
     * 主动获取当前设备的 FCM Token 并上传到后端。
     * FirebaseMessaging.getInstance().token 可能在 App 启动后才就绪，
     * 因此放在 IO 线程 await，异常静默处理（如 Google Play 服务不可用）。
     */
    private fun uploadCurrentFcmToken() {
        viewModelScope.launch {
            try {
                val task = FirebaseMessaging.getInstance().token
                val fcmToken = withContext(Dispatchers.IO) { Tasks.await(task) }
                if (fcmToken.isNotEmpty()) {
                    api.uploadFcmToken(FcmTokenRequest(fcmToken))
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * 主动获取 JPush Registration ID 并上传到后端。
     * JPush 成功注册后会持久化到本地，直接读取可能还没就绪，多次重试。
     */
    private fun uploadJpushRegId() {
        viewModelScope.launch {
            try {
                var regId = JPushInterface.getRegistrationID(getApplication())
                android.util.Log.d("VM", "uploadJpushRegId: firstTry=$regId")
                var attempt = 0
                // 最多重试 15 次，每次间隔 2 秒（共 30 秒），JPush 注册通常在这个时间内完成
                while (regId.isNullOrEmpty() && attempt < 15) {
                    kotlinx.coroutines.delay(2000)
                    regId = JPushInterface.getRegistrationID(getApplication())
                    android.util.Log.d("VM", "uploadJpushRegId: attempt=${++attempt} regId=$regId")
                }
                if (!regId.isNullOrEmpty()) {
                    android.util.Log.d("VM", "uploadJpushRegId: uploading regId=$regId")
                    api.uploadJpushRegId(JpushRegIdRequest(regId))
                } else {
                    android.util.Log.w("VM", "uploadJpushRegId: failed after 15 retries")
                }
            } catch (e: Exception) {
                android.util.Log.e("VM", "uploadJpushRegId: error=${e.message}")
            }
        }
    }

    // ── 头像 ──

    /**
     * 从服务器加载当前用户头像 URL
     */
    private fun loadAvatar() {
        viewModelScope.launch {
            try {
                val res = api.getProfile()
                if (res.isSuccessful && res.body()?.data != null) {
                    val newAvatarUrl = res.body()!!.data!!.avatarUrl
                    _avatarUrl.value = newAvatarUrl
                    db.userDao().getById(_currentUserId.value)?.let { user ->
                        db.userDao().insert(user.copy(avatarUrl = newAvatarUrl))
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * 从本地 URI 选择图片 → 上传到服务器 → 更新头像
     */
    fun updateAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<CheckinApp>()
                val inputStream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                } ?: throw Exception("无法读取图片")
                val bytes = withContext(Dispatchers.IO) { inputStream.readBytes() }
                inputStream.close()

                val requestBody = RequestBody.create("image/*".toMediaType(), bytes)
                val imagePart = MultipartBody.Part.createFormData("image", "avatar.jpg", requestBody)

                // 1. 上传图片获取 URL
                val uploadRes = api.uploadImage(imagePart)
                if (!uploadRes.isSuccessful || uploadRes.body()?.code != 200) {
                    _error.value = uploadRes.body()?.message ?: "图片上传失败"
                    return@launch
                }
                val imageUrl = uploadRes.body()!!.data!!.url
                // 拼接完整 URL（服务器返回的是相对路径 /uploads/xxx）
                val fullUrl = RetrofitClient.getBaseUrl().trimEnd('/') + "/" + imageUrl.trimStart('/')

                // 2. 更新头像 URL
                val avatarRes = api.updateAvatar(mapOf("avatarUrl" to fullUrl))
                if (avatarRes.isSuccessful && avatarRes.body()?.code == 200) {
                    _avatarUrl.value = fullUrl
                    db.userDao().getById(_currentUserId.value)?.let { user ->
                        db.userDao().insert(user.copy(avatarUrl = fullUrl))
                    }
                } else {
                    _error.value = avatarRes.body()?.message ?: "头像更新失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "上传失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── 通知 ──

    fun refreshNotifications(showAlert: Boolean = false) {
        if (showAlert) notificationAlertPending = true
        if (notificationRefreshJob?.isActive == true) return
        notificationRefreshJob = viewModelScope.launch {
            val shouldShowAlert = notificationAlertPending
            notificationAlertPending = false
            try {
                val res = api.getNotifications()
                if (res.isSuccessful && res.body()?.data != null) {
                    val data = res.body()!!.data!!
                    val newList = data.notifications.map {
                        NotificationEntity(
                            id = it.id, userId = it.userId, type = it.type,
                            title = it.title, body = it.body, relatedId = it.relatedId,
                            isRead = it.isRead != 0, createdAt = it.createdAt,
                        )
                    }
                    if (shouldShowAlert) {
                        val oldIds = _notifications.value.map { it.id }.toSet()
                        newList.filter { notif ->
                            !notif.isRead &&
                            notif.id !in oldIds &&
                            notif.id !in shownNotificationIds
                        }.forEach { notif ->
                            showLocalNotification(notif.id, notif.title, notif.body ?: "", notif.type, notif.relatedId)
                            shownNotificationIds.add(notif.id)
                        }
                        saveShownIds()
                    }
                    _notifications.value = newList
                    _unreadCount.value = data.unreadCount
                    db.notificationDao().clearAll()
                    db.notificationDao().insertAll(newList)
                }
            } catch (e: Exception) {
                android.util.Log.e("VM", "refreshNotifications error", e)
            } finally {
                notificationRefreshJob = null
            }
        }
    }

    private fun showLocalNotification(notifId: Long, title: String, body: String, type: String = "", relatedId: String? = null) {
        try {
            val context = getApplication<CheckinApp>()

            // 根据通知类型决定点击后跳转的页面
            val navRoute = when (type) {
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
                "achievement_unlock" -> "profile/achievements"
                else -> "profile/notifications"
            }

            val intent = android.content.Intent(context, com.checkin.partner.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("nav_route", navRoute)
                putExtra("notif_id", notifId)
            }
            val notifTag = notifId.toInt()
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, notifTag, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // 滑出删除时标记已读（使用 BroadcastReceiver，不启动 Activity）
            val deleteIntent = android.content.Intent(context, com.checkin.partner.network.fcm.NotificationDismissReceiver::class.java).apply {
                putExtra("notif_id", notifId)
            }
            val deletePendingIntent = android.app.PendingIntent.getBroadcast(
                context, notifTag + 10000, deleteIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val channelId = when {
                title.contains("打卡") || title.contains("评论") -> CheckinApp.CHANNEL_CHECKIN
                title.contains("奖励") || title.contains("兑换") -> CheckinApp.CHANNEL_REWARD
                else -> CheckinApp.CHANNEL_PAIR
            }
            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .build()
            androidx.core.app.NotificationManagerCompat.from(context).notify(
                notifTag, notification
            )
        } catch (_: Exception) {}
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            try { api.markNotificationRead(id); refreshNotifications() } catch (_: Exception) {}
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            try { api.markAllRead(); refreshNotifications() } catch (_: Exception) {}
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                api.clearNotifications()
                // 不清 shownNotificationIds：已弹过的通知不再重复弹
                refreshNotifications()
            } catch (_: Exception) {}
        }
    }

    // ── 设置 ──
    private val _userConfig = MutableStateFlow<UserConfigData?>(null)
    val userConfig: StateFlow<UserConfigData?> = _userConfig.asStateFlow()

    // ── 导航（通知点击） ──

    fun navigateTo(route: String) {
        _navigationEvent.tryEmit(route)
    }

    fun loadConfig() {
        viewModelScope.launch {
            try {
                val res = api.getConfig()
                if (res.isSuccessful && res.body()?.data != null) {
                    _userConfig.value = res.body()!!.data
                }
            } catch (_: Exception) {}
        }
    }

    fun updateConfig(config: UserConfigData) {
        viewModelScope.launch {
            try { api.updateConfig(config); _userConfig.value = config } catch (_: Exception) {}
        }
    }

    fun changePassword(oldPwd: String, newPwd: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = api.changePassword(mapOf("oldPassword" to oldPwd, "newPassword" to newPwd))
                onResult(res.isSuccessful, res.body()?.message ?: "修改失败")
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    // ── 请假 ──
    fun loadVacationState() {
        viewModelScope.launch {
            try {
                val res = api.getProfile()
                if (res.isSuccessful && res.body()?.data != null) {
                    _isVacation.value = res.body()!!.data!!.isVacation
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleVacation() {
        viewModelScope.launch {
            try {
                if (_isVacation.value) { api.vacationEnd() } else { api.vacationStart() }
                _isVacation.value = !_isVacation.value
            } catch (_: Exception) {}
        }
    }

    fun deleteAccount(password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = api.deleteAccount(mapOf("password" to password))
                if (res.isSuccessful) {
                    logout()
                    onResult(true, "账号已注销")
                } else {
                    onResult(false, res.body()?.message ?: "注销失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    // ── 积分流水 ──

    fun refreshTransactions() {
        viewModelScope.launch {
            try {
                val res = api.getTransactions()
                if (res.isSuccessful && res.body()?.data != null) {
                    _transactions.value = res.body()!!.data!!.transactions.map {
                        PointTransactionEntity(
                            transactionId = it.transactionId,
                            userId = it.userId,
                            amount = it.amount,
                            type = it.type,
                            category = it.category,
                            description = it.description,
                            createdAt = it.createdAt,
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VM", "refreshTransactions error", e)
            }
        }
    }

    // ── 补签卡 ──
    private val _makeupRemaining = MutableStateFlow(0)
    val makeupRemaining: StateFlow<Int> = _makeupRemaining.asStateFlow()

    fun loadMakeupCards() {
        viewModelScope.launch {
            try {
                val res = api.getMakeupCards()
                if (res.isSuccessful && res.body()?.data != null) {
                    _makeupRemaining.value = res.body()!!.data!!.remaining
                }
            } catch (_: Exception) {}
        }
    }

    // ── 成就 ──

    fun refreshAchievements() {
        viewModelScope.launch {
            try {
                val res = api.getAchievements()
                if (res.isSuccessful && res.body()?.data != null) {
                    _achievements.value = res.body()!!.data!!.achievements
                }
            } catch (_: Exception) { }
        }
    }

    fun doMakeup(taskId: String, date: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = api.makeupCheckin(mapOf("taskId" to taskId, "userId" to _currentUserId.value, "checkinDate" to date))
                if (res.isSuccessful && res.body()?.code == 200) {
                    loadMakeupCards()
                    refreshDashboard()
                    onResult(true, "补签成功")
                } else {
                    onResult(false, if (res.code() == 401) "登录已过期，请重新登录"
                        else errorMessageOf(res) ?: "补签失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
}

/** 打卡成功弹窗数据 */
data class CheckinSuccess(
    val taskName: String,
    val personalPoints: Int,
    val poolPoints: Int,
    val streak: Int,
)
