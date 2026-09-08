package com.checkin.partner.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.checkin.partner.network.dto.TaskWithStatus
import com.checkin.partner.ui.components.ScaleButton
import com.checkin.partner.ui.components.HomeSkeleton
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val dashboardLoaded by viewModel.dashboardLoaded.collectAsStateWithLifecycle()
    val personalPoints by viewModel.personalPoints.collectAsStateWithLifecycle()
    val poolPoints by viewModel.poolPoints.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val currentUsername by viewModel.currentUsername.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isVacation by viewModel.isVacation.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val achievementUnlocked by viewModel.achievementUnlocked.collectAsStateWithLifecycle()
    val checkinSuccess by viewModel.checkinSuccess.collectAsStateWithLifecycle()
    val checkingTaskIds by viewModel.checkingTaskIds.collectAsStateWithLifecycle()

    // 只在 dashboard 变化时重新计算列表，滚动过程中不重复创建空列表和分组对象。
    val myData = dashboard?.myData
    val partnerData = dashboard?.partnerData
    val todayTasks = remember(myData) { myData?.todayTaskStatus.orEmpty() }
    val partnerTasks = remember(partnerData) { partnerData?.todayTaskStatus.orEmpty() }

    // 回调提升为稳定引用，避免 items lambda 每次重组重建导致 item 无法跳过重组
    val checkinAction = remember { { taskId: String -> viewModel.doCheckin(taskId) } }
    val detailAction = remember { { taskId: String -> navController.navigate("checkin/detail/$taskId") } }
    val approveAction = remember { { recordId: String -> viewModel.approveCheckin(recordId) } }
    val rejectAction = remember { { recordId: String -> viewModel.rejectCheckin(recordId) } }
    val openProfileAction = remember { { navController.navigate("profile") } }
    val openAchievementsAction = remember { { navController.navigate("profile/achievements") } }
    val openNotificationsAction = remember { { navController.navigate("profile/notifications") } }
    val openPairAction = remember { { navController.navigate("pair") } }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.refreshDashboard()
        viewModel.refreshNotifications(showAlert = true)
    }

    // 错误提示：用 collect 订阅，error 变化不驱动首页重组
    LaunchedEffect(Unit) {
        viewModel.error.collect { msg ->
            if (msg != null) {
                android.widget.Toast.makeText(navController.context, msg, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    achievementUnlocked?.let { ach ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAchievementUnlocked() },
            icon = { Text(ach.icon, style = MaterialTheme.typography.headlineLarge) },
            title = { Text("成就解锁", fontWeight = FontWeight.Bold) },
            text = { Text("恭喜获得「${ach.name}」成就") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAchievementUnlocked()
                    navController.navigate("profile/achievements")
                }) { Text("查看成就") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearAchievementUnlocked() }) { Text("好的") }
            }
        )
    }

    // 打卡成功弹窗（与成就弹窗互斥：有新成就时只弹成就窗，此处 checkinSuccess 为 null）
    checkinSuccess?.let { s ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCheckinSuccess() },
            icon = { Text("✅", style = MaterialTheme.typography.headlineLarge) },
            title = { Text("打卡成功", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    (if (s.taskName.isNotBlank()) "「${s.taskName}」" else "本次打卡") +
                        "完成啦！个人积分 +${s.personalPoints}，奖励池 +${s.poolPoints}" +
                        if (s.streak > 0) "，已连续打卡 ${s.streak} 天" else ""
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.clearCheckinSuccess() }) { Text("太棒了") }
            },
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshAll() }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // MainActivity 开启 edge-to-edge，首页没有 TopAppBar 自动消费状态栏 inset，
                // 需要显式留出状态栏高度，避免头像和用户名压到系统时间区域。
                .windowInsetsPadding(WindowInsets.statusBars)
                .pullRefresh(pullRefreshState)
        ) {
            if (!dashboardLoaded && dashboard == null) {
                HomeSkeleton(Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                item(key = "header", contentType = "header") {
                    HomeHeader(
                        username = currentUsername,
                        avatarUrl = avatarUrl,
                        unreadCount = unreadCount,
                        isVacation = isVacation,
                        onProfile = openProfileAction,
                        onAchievements = openAchievementsAction,
                        onNotifications = openNotificationsAction,
                    )
                }

                item(key = "points", contentType = "hero") {
                    PointsHeroCard(
                        personalPoints = personalPoints,
                        poolPoints = poolPoints,
                        myStreak = myData?.streak ?: 0,
                        partnerStreak = partnerData?.streak,
                    )
                }

                item(key = "partner", contentType = "hero") { PartnerEntryCard(openPairAction, viewModel) }

                item(key = "today_header", contentType = "section") {
                    SectionHeader(
                        title = "今日任务",
                        subtitle = "完成一点点，今天就很棒",
                        icon = Icons.Filled.CheckCircle,
                    )
                }

                if (todayTasks.isEmpty()) {
                    item(key = "today_empty", contentType = "empty") { EmptyStateCard("今天没有待打卡的任务", "去任务页安排一个小目标吧") }
                } else {
                    items(todayTasks, key = { it.task.taskId }, contentType = { "task" }) { ts ->
                        TodayTaskCard(
                            ts = ts,
                            currentUserId = currentUserId,
                            onCheckin = checkinAction,
                            onDetail = detailAction,
                            isChecking = ts.task.taskId in checkingTaskIds,
                        )
                    }
                }

                if (partnerData != null) {
                    item(key = "partner_header", contentType = "section") {
                        SectionHeader(
                            title = "${partnerData.username} 的进度",
                            subtitle = if (partnerData.isVacation) "今天请假中，好好休息" else "互相看见，也互相鼓励",
                            icon = Icons.Filled.Favorite,
                        )
                    }
                    if (partnerTasks.isEmpty()) {
                        item(key = "partner_empty", contentType = "empty") { EmptyStateCard("搭档今天没有任务", "一起安排一个轻松的小目标吧") }
                    } else {
                        items(partnerTasks, key = { it.task.taskId }, contentType = { "task" }) { ts ->
                            PartnerTaskCard(ts, currentUserId, approveAction, rejectAction)
                        }
                    }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeader(
    username: String,
    avatarUrl: String?,
    unreadCount: Int,
    isVacation: Boolean,
    onProfile: () -> Unit,
    onAchievements: () -> Unit,
    onNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onProfile),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    // 限制解码尺寸，避免大图加载卡顿
                    model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).size(96).build(),
                    contentDescription = "我的头像",
                    modifier = Modifier.size(46.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("早上好${if (isVacation) "，今天休息" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (username.isBlank()) "打卡搭档" else username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onAchievements) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "成就", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onNotifications) {
                BadgedBox(badge = { if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") } }) {
                    Icon(Icons.Filled.NotificationsNone, contentDescription = "通知", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PointsHeroCard(personalPoints: Int, poolPoints: Int, myStreak: Int, partnerStreak: Int?) {
    val primaryText = Color.White
    val secondaryText = Color.White.copy(alpha = 0.78f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("今天也一起加油", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryText)
                    Text("每一次完成，都是给彼此的小回应", style = MaterialTheme.typography.bodySmall, color = secondaryText)
                }
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = primaryText.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                HeroMetric("我的积分", "$personalPoints", Modifier.weight(1f), primaryText, secondaryText)
                HeroMetric("奖励池", "$poolPoints", Modifier.weight(1f), primaryText, secondaryText)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                StreakChip("我的连续", myStreak, Modifier.weight(1f), primaryText, secondaryText)
                if (partnerStreak != null) StreakChip("搭档连续", partnerStreak, Modifier.weight(1f), primaryText, secondaryText)
            }
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color,
    labelColor: Color,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = labelColor)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StreakChip(
    label: String,
    days: Int,
    modifier: Modifier = Modifier,
    valueColor: Color,
    labelColor: Color,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = valueColor, modifier = Modifier.size(17.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text("${days}天", style = MaterialTheme.typography.labelLarge, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TodayTaskCard(
    ts: TaskWithStatus,
    currentUserId: String,
    onCheckin: (String) -> Unit,
    onDetail: (String) -> Unit,
    isChecking: Boolean = false,
) {
    val freqLabel = when (ts.task.frequency) {
        "DAILY" -> "每日"
        "WEEKLY" -> "每周"
        "ONCE" -> "一次性"
        else -> ts.task.frequency
    }
    val creatorLabel = when {
        ts.task.creatorId == currentUserId && ts.task.userId == currentUserId -> "我创建的"
        ts.task.creatorId == currentUserId && ts.task.userId != currentUserId -> "我给搭档的"
        ts.task.creatorId != currentUserId && ts.task.userId == currentUserId -> "搭档给我的"
        else -> ""
    }
    // "是否已完成"只认服务端 dashboard 的 checkedIn，本地绝不提前写死，避免跨天/回滚错乱
    val done = ts.checkedIn
    val accent = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    // 已完成切换为"盖章"弹出动画，把用户注意力从"等了多久"转移到"完成了"
    val doneScale by animateFloatAsState(
        targetValue = if (done) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "doneStamp",
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = { onDetail(ts.task.taskId) }),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(ts.task.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("+${ts.task.pointPerCheck}分 · $freqLabel · $creatorLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ts.task.startTime != null || ts.task.endTime != null) {
                    Text("${ts.task.startTime ?: "不限"} — ${ts.task.endTime ?: "不限"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                ts.pendingApproval != null -> StatusPill("待确认", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                done -> Box(Modifier.graphicsLayer(scaleX = doneScale, scaleY = doneScale)) {
                    StatusPill("✓ 已完成", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
                else -> ScaleButton(
                    onClick = { onCheckin(ts.task.taskId) },
                    enabled = !isChecking,
                ) {
                    if (isChecking) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text("打卡中")
                        }
                    } else {
                        Text("打卡")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, background: Color, contentColor: Color) {
    Surface(color = background, contentColor = contentColor, shape = RoundedCornerShape(10.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PartnerTaskCard(ts: TaskWithStatus, currentUserId: String, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    val isMyCreation = ts.task.creatorId == currentUserId
    val accent = if (ts.checkedIn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(ts.task.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        ts.pendingApproval != null && isMyCreation -> "等待你的确认"
                        ts.pendingApproval != null -> "等待搭档确认"
                        ts.checkedIn -> "搭档已经完成啦"
                        else -> "还在努力中"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ts.pendingApproval != null && isMyCreation) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onApprove(ts.pendingApproval.recordId) }) { Text("同意", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { onReject(ts.pendingApproval.recordId) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                }
            } else {
                StatusPill(if (ts.checkedIn) "完成" else "进行中", if (ts.checkedIn) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant, if (ts.checkedIn) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PartnerEntryCard(onClick: () -> Unit, viewModel: AppViewModel) {
    val pairStatus by viewModel.pairStatus.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.getPairStatus() }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                when (pairStatus?.status) {
                    "BOUND" -> {
                        val partnerAvatarUrl = pairStatus?.pair?.partnerAvatarUrl
                        if (partnerAvatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(partnerAvatarUrl).size(96).build(),
                                contentDescription = "搭档头像",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    "PENDING" -> Icon(Icons.Filled.HourglassEmpty, null, tint = MaterialTheme.colorScheme.secondary)
                    else -> Icon(Icons.Filled.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    when (pairStatus?.status) {
                        "BOUND" -> "搭档：${pairStatus?.pair?.partnerUsername ?: "未知"}"
                        "PENDING" -> "搭档请求处理中"
                        else -> "添加你的搭档"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when (pairStatus?.status) {
                        "BOUND" -> "点击查看搭档详情"
                        "PENDING" -> "确认后就可以开始互相打卡啦"
                        else -> "邀请一个人，一起坚持更容易"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
