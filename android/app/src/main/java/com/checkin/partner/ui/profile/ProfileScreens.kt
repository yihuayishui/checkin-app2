package com.checkin.partner.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkin.partner.ui.components.ProfileSkeleton
import com.checkin.partner.ui.components.rememberSmoothFlingBehavior
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: AppViewModel) {
    val username by viewModel.currentUsername.collectAsState()
    val personalPoints by viewModel.personalPoints.collectAsState()
    val poolPoints by viewModel.poolPoints.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val profileLoaded by viewModel.profileLoaded.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.updateAvatarFromUri(uri)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("我的", fontWeight = FontWeight.Bold)
                        Text("管理你的打卡与奖励", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { padding ->
        if (!profileLoaded) {
            ProfileSkeleton(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            flingBehavior = rememberSmoothFlingBehavior(),
        ) {
            item {
                ProfileHeaderCard(
                    username = username,
                    personalPoints = personalPoints,
                    poolPoints = poolPoints,
                    avatarUrl = avatarUrl,
                    onAvatarClick = { imagePicker.launch("image/*") },
                )
            }

            item {
                Text("快捷入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileMenuItem(Icons.Filled.Star, "成就徽章", "记录每一次坚持的里程碑") { navController.navigate("profile/achievements") }
                    ProfileMenuItem(Icons.Filled.ReceiptLong, "积分流水", "查看积分收入与兑换记录") { navController.navigate("profile/points") }
                    ProfileMenuItem(Icons.Filled.NotificationsActive, "通知中心", "及时查看搭档的新动态") { navController.navigate("profile/notifications") }
                    ProfileMenuItem(Icons.Filled.AutoFixHigh, "补签卡", "把错过的打卡补回来") { navController.navigate("checkin/makeup") }
                    ProfileMenuItem(Icons.Filled.Tune, "设置", "提醒、积分比例与通知偏好") { navController.navigate("profile/settings") }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth().clickable {
                        viewModel.logout()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("退出登录", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    username: String,
    personalPoints: Int,
    poolPoints: Int,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))).padding(18.dp)
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(66.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).clickable { onAvatarClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarUrl != null) {
                            AsyncImage(model = avatarUrl, contentDescription = "头像", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Filled.AccountCircle, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("你好，", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
                        Text(username.ifEmpty { "未知" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("点击头像更换照片", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ProfileStat("个人积分", personalPoints.toString(), Modifier.weight(1f), Color.White)
                    Box(Modifier.width(1.dp).height(34.dp).background(Color.White.copy(alpha = 0.35f)))
                    ProfileStat("奖励池", poolPoints.toString(), Modifier.weight(1f), Color.White)
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier, contentColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.82f))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: AppViewModel) {
    val config by viewModel.userConfig.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadConfig(); viewModel.loadVacationState() }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("设置", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            )
        )}
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), flingBehavior = rememberSmoothFlingBehavior()) {

            // ── 打卡提醒 ──
            item {
                Text("🔔 打卡提醒时间", fontWeight = FontWeight.Bold)
                var reminder by remember { mutableStateOf(config?.reminderTime ?: "") }
                OutlinedTextField(value = reminder, onValueChange = { reminder = it },
                    label = { Text("如 20:00") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(4.dp))
                Button(onClick = {
                    viewModel.updateConfig(config!!.copy(reminderTime = reminder.ifBlank { null }))
                    com.checkin.partner.utils.ReminderWorker.schedule(navController.context, reminder.ifBlank { null })
                }, modifier = Modifier.fillMaxWidth()) { Text("保存提醒时间") }
            }

            item { SettingsDivider() }

            // ── 积分比例 ──
            item {
                Text("💰 积分分配比例", fontWeight = FontWeight.Bold)
                val ratio = config?.personalRatio ?: 0.5f
                val poolRatio = 1f - ratio
                Text("个人: ${(ratio * 100).toInt()}%  |  奖励池: ${(poolRatio * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.3f to "3:7", 0.5f to "5:5", 0.7f to "7:3").forEach { (r, label) ->
                        FilterChip(selected = ratio == r, onClick = { viewModel.updateConfig(config!!.copy(personalRatio = r, poolRatio = 1f - r)) },
                            label = { Text(label) })
                    }
                }
            }

            item { SettingsDivider() }

            // ── 通知开关 ──
            item {
                Text("📢 通知偏好", fontWeight = FontWeight.Bold)
                SettingsSwitchRow("打卡通知", config?.notifyCheckin == true) { viewModel.updateConfig(config!!.copy(notifyCheckin = it)) }
                SettingsSwitchRow("奖励通知", config?.notifyReward == true) { viewModel.updateConfig(config!!.copy(notifyReward = it)) }
                SettingsSwitchRow("搭档通知", config?.notifyPair == true) { viewModel.updateConfig(config!!.copy(notifyPair = it)) }
            }

            item { SettingsDivider() }

            // ── 深色模式 ──
            item {
                Text("🌙 深色模式", fontWeight = FontWeight.Bold)
                var expanded by remember { mutableStateOf(false) }
                val current = config?.themeMode ?: "system"
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(when (current) { "light" -> "浅色"; "dark" -> "深色"; else -> "跟随系统" })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (v, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                viewModel.updateConfig(config!!.copy(themeMode = v)); expanded = false
                            })
                        }
                    }
                }
            }

            item { SettingsDivider() }

            // ── 改密码 ──
            item {
                Text("🔑 修改密码", fontWeight = FontWeight.Bold)
                var oldPwd by remember { mutableStateOf("") }
                var newPwd by remember { mutableStateOf("") }
                var msg by remember { mutableStateOf<String?>(null) }
                OutlinedTextField(value = oldPwd, onValueChange = { oldPwd = it },
                    label = { Text("原密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(value = newPwd, onValueChange = { newPwd = it },
                    label = { Text("新密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
                Button(onClick = {
                    viewModel.changePassword(oldPwd, newPwd) { ok, m -> msg = m; if (ok) { oldPwd = ""; newPwd = "" } }
                }, modifier = Modifier.fillMaxWidth(), enabled = oldPwd.isNotBlank() && newPwd.length >= 6) { Text("修改密码") }
                msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }

            item { SettingsDivider() }

            // ── 断签惩罚 ──
            item {
                Text("⚡ 断签惩罚", fontWeight = FontWeight.Bold)
                SettingsSwitchRow("开启断签惩罚", config?.streakPenaltyOn == true) {
                    viewModel.updateConfig(config!!.copy(streakPenaltyOn = it))
                }
                if (config?.streakPenaltyOn == true) {
                    var days by remember { mutableStateOf((config?.streakPenaltyDays ?: 3).toString()) }
                    OutlinedTextField(value = days, onValueChange = { days = it },
                        label = { Text("连续N天不打卡触发惩罚") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = {
                        viewModel.updateConfig(config!!.copy(streakPenaltyDays = days.toIntOrNull() ?: 3))
                    }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
                    Text("每次断签扣奖励池10分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            item { SettingsDivider() }

            // ── 请假 ──
            item {
                Text("🏖 请假模式", fontWeight = FontWeight.Bold)
                Button(
                    onClick = { viewModel.toggleVacation() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text(if (viewModel.isVacation.collectAsState().value) "关闭请假模式" else "开启请假模式") }
            }

            item { SettingsDivider() }

            // ── 注销 ──
            item {
                var pwd by remember { mutableStateOf("") }
                OutlinedButton(
                    onClick = { viewModel.deleteAccount(pwd) { ok, m -> if (ok) navController.navigate("login") { popUpTo(0) { inclusive = true } } } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = pwd.isNotBlank()
                ) { Text("注销账号") }
                OutlinedTextField(value = pwd, onValueChange = { pwd = it },
                    label = { Text("输入密码确认注销") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
                Text("注销后所有数据将被清除，此操作不可撤销", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsHistoryScreen(navController: NavController, viewModel: AppViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refreshTransactions() }

    val filtered = transactions.filter { tx ->
        val d = tx.createdAt.take(10)
        (startDate.isBlank() || d >= startDate) && (endDate.isBlank() || d <= endDate)
    }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("积分流水", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            )
        )}
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 日期区间筛选
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("开始日期") },
                    placeholder = { Text("2026-07-01") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("结束日期") },
                    placeholder = { Text("2026-07-06") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无积分记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), flingBehavior = rememberSmoothFlingBehavior()) {
                    items(filtered) { tx ->
                    val isEarn = tx.type == "EARN"
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isEarn) "+${tx.amount}" else "-${tx.amount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isEarn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tx.description ?: when (tx.category) {
                                    "CHECKIN" -> "打卡收入"
                                    "CHECKIN_POOL" -> "奖励池收入"
                                    "REWARD" -> "兑换消费"
                                    "REVERT" -> "撤回打卡"
                                    else -> tx.category
                                }, style = MaterialTheme.typography.bodyMedium)
                                val dateParts = tx.createdAt.take(10).split("-")
                                Text("${dateParts[0]}年${dateParts[1].toInt()}月${dateParts[2].toInt()}日",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, viewModel: AppViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshNotifications(showAlert = true) }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("通知中心", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            actions = {
            TextButton(onClick = { viewModel.markAllNotificationsRead() }) { Text("全部已读", style = MaterialTheme.typography.labelSmall) }
            TextButton(onClick = { viewModel.clearAllNotifications() }) { Text("清空", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
        })}
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无通知", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), flingBehavior = rememberSmoothFlingBehavior()) {
                items(notifications) { notif ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notif.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.Normal)
                                if (!notif.isRead) {
                                    Text("●", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (!notif.body.isNullOrBlank()) {
                                Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(notif.createdAt.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                if (!notif.isRead) {
                                    TextButton(onClick = { viewModel.markNotificationRead(notif.id) }) { Text("标为已读") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController, viewModel: AppViewModel) {
    val achievements by viewModel.achievements.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshAchievements() }

    val personal = achievements.filter { it.group == "personal" }
    val partner = achievements.filter { it.group == "partner" }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TopAppBar(
            title = { Text("成就徽章", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            )
        )}
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), flingBehavior = rememberSmoothFlingBehavior()) {
            if (achievements.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无成就数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (personal.isNotEmpty()) {
                item {
                    Text("🏃 个人打卡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(personal) { AchievementCard(it) }
            }

            if (partner.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text("💑 搭档同行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(partner) { AchievementCard(it) }
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: com.checkin.partner.network.api.AchievementData) {
    val alpha = if (achievement.unlocked) 1f else 0.35f
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            Modifier.padding(16.dp).alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(achievement.icon, style = MaterialTheme.typography.headlineMedium, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(achievement.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (achievement.unlocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(achievement.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (achievement.unlocked && achievement.unlockedAt != null) {
                    Text(
                        "达成于 ${achievement.unlockedAt.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!achievement.unlocked) {
                Icon(Icons.Filled.Lock, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
