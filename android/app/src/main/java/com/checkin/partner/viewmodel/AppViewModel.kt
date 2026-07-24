package com.checkin.partner.viewmodel

import android.app.Application
import android.content.Context
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
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    // ── 看板 ──
    private val _dashboard = MutableStateFlow<DashboardToday?>(null)
    val dashboard: StateFlow<DashboardToday?> = _dashboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    // ── 搭档 ──
    private val _pairStatus = MutableStateFlow<PairStatus?>(null)
    val pairStatus: StateFlow<PairStatus?> = _pairStatus.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchUser>>(emptyList())
    val searchResults: StateFlow<List<SearchUser>> = _searchResults.asStateFlow()

    private val _partnerUsername = MutableStateFlow<String?>(null)
    val partnerUsername: StateFlow<String?> = _partnerUsername.asStateFlow()

    // ── 持久化已通知到系统通知栏的 ID，避免重复弹窗 ──
    private val prefs = application.getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE)
    private val shownNotificationIds: MutableSet<Long> = loadShownIds()

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
        // 恢复登录状态
        val savedToken = RetrofitClient.token
        if (!savedToken.isNullOrEmpty()) {
            _isLoggedIn.value = true
            _currentUserId.value = RetrofitClient.getSavedUserId() ?: ""
            _currentUsername.value = RetrofitClient.getSavedUsername() ?: ""

            // 冷启动：连接 WebSocket + 拉取通知（弹系统通知栏）+ 上传 FCM/JPush Token + 加载头像
            val uid = _currentUserId.value
            if (uid.isNotBlank()) {
                ws.connect(uid)
                setupWsListeners()
                refreshNotifications(showAlert = true)
                uploadCurrentFcmToken()
                uploadJpushRegId()
                loadAvatar()
                refreshAchievements()
            }
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
                    // WebSocket 连接
                    ws.connect(data.userId)
                    setupWsListeners()
                    refreshDashboard()
                    uploadCurrentFcmToken()
                    uploadJpushRegId()
                    refreshAchievements()
                } else {
                    _error.value = res.body()?.message ?: "登录失败"
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
                    ws.connect(data.userId)
                    setupWsListeners()
                    refreshDashboard()
                    uploadCurrentFcmToken()
                    uploadJpushRegId()
                    refreshAchievements()
                } else {
                    _error.value = res.body()?.message ?: "注册失败"
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
        viewModelScope.launch {
            db.userDao().clearAll()
            db.taskDao().clearAll()
            db.checkinDao().clearAll()
            db.rewardDao().clearAll()
        }
    }

    // ── 全量刷新（下拉刷新用） ──

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            refreshDashboard()
            refreshTasks()
            refreshRewards()
            refreshAchievements()
            getPairStatus()
            loadConfig()
            loadVacationState()
            refreshTransactions()
            loadMakeupCards()
            refreshNotifications(showAlert = true)
            // 至少显示 1.5 秒刷新指示器，让用户感知到刷新完成
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1500) {
                kotlinx.coroutines.delay(1500 - elapsed)
            }
            _isLoading.value = false
        }
    }

    // ── 看板 ──

    fun refreshDashboard() {
        viewModelScope.launch {
            try {
                val res = api.getDashboard()
                if (res.isSuccessful && res.body()?.data != null) {
                    _dashboard.value = res.body()!!.data
                    val d = res.body()!!.data!!
                    _personalPoints.value = d.myData.personalPoints
                    _poolPoints.value = d.myData.poolPoints
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshTasks(filter: String? = "mine") {
        viewModelScope.launch {
            try {
                val res = api.getTasks(filter)
                if (res.isSuccessful && res.body()?.data != null) {
                    val list = res.body()!!.data!!.tasks.map { it.toEntity() }
                    _tasks.value = list
                    db.taskDao().clearAll()
                    db.taskDao().insertAll(list)
                }
            } catch (_: Exception) {
                // 离线或请求失败：只对 "mine" 和 "created" 使用缓存（其他过滤条件缓存不准确）
                if (filter == "mine" || filter == "created") {
                    _tasks.value = db.taskDao().getByUserId(_currentUserId.value)
                }
                // 其他过滤条件（for_partner/from_partner/done）失败时不覆盖列表，保持已有数据
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

    fun deleteTask(taskId: String, creatorId: String? = null) {
        viewModelScope.launch {
            try {
                if (creatorId != null && creatorId != _currentUserId.value) {
                    // 搭档创建的任务，需要对方确认
                    api.requestDeleteTask(taskId)
                    refreshTasks()
                    _error.value = null
                } else {
                    api.deleteTask(taskId)
                    refreshTasks()
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

    fun reactivateTask(taskId: String) {
        viewModelScope.launch {
            try { api.reactivateTask(taskId); refreshTasks() } catch (_: Exception) {}
        }
    }

    // ── 打卡 ──

    fun doCheckin(taskId: String, note: String? = null, imageUrl: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val res = api.createCheckin(CheckinCreateRequest(
                    recordId = UUID.randomUUID().toString(),
                    taskId = taskId, userId = _currentUserId.value,
                    note = note, imageUrl = imageUrl,
                ))
                if (res.isSuccessful && res.body()?.code == 200) {
                    val result = res.body()!!.data
                    if (result?.status == "PENDING") {
                        // 需创建者确认，显示提示
                        _error.value = result.message ?: "打卡已提交，等待搭档确认"
                    }
                    refreshDashboard()
                } else {
                    _error.value = res.body()?.message ?: "打卡失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
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
            refreshNotifications(showAlert = true)
        }
        ws.on("points:update") {
            refreshDashboard()
        }
        ws.on("reward:sync") {
            refreshRewards()
            refreshNotifications(showAlert = true)
        }
        ws.on("pair:notification") {
            getPairStatus()
            refreshDashboard()
            refreshNotifications(showAlert = true)
        }
        ws.on("task:sync") {
            refreshTasks()
            refreshNotifications(showAlert = true)
        }
        ws.on("task:delete") {
            refreshTasks()
            refreshNotifications(showAlert = true)
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
                    _avatarUrl.value = res.body()!!.data!!.avatarUrl
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
        viewModelScope.launch {
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
                    if (showAlert) {
                        val oldIds = _notifications.value.map { it.id }.toSet()
                        newList.filter { notif ->
                            !notif.isRead &&
                            notif.id !in oldIds &&
                            notif.id !in shownNotificationIds
                        }.forEach { notif ->
                            showLocalNotification(notif.id, notif.title, notif.body ?: "", notif.type, notif.relatedId)
                            shownNotificationIds.add(notif.id)
                        }
                        // 持久化已通知 ID 列表
                        saveShownIds()
                    }
                    _notifications.value = newList
                    _unreadCount.value = data.unreadCount
                }
            } catch (e: Exception) {
                android.util.Log.e("VM", "refreshNotifications error", e)
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
                "task_delete_request", "task_edit_request" -> "task/list"
                "password_reset_request", "password_reset_done" -> "profile"
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
    private val _isVacation = MutableStateFlow(false)
    val isVacation: StateFlow<Boolean> = _isVacation.asStateFlow()

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
                    onResult(false, res.body()?.message ?: "补签失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
}
