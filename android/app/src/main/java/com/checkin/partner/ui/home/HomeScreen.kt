package com.checkin.partner.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.checkin.partner.network.dto.DashboardToday
import com.checkin.partner.network.dto.TaskWithStatus
import com.checkin.partner.ui.theme.MintGreen
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel) {
    val dashboard by viewModel.dashboard.collectAsState()
    val personalPoints by viewModel.personalPoints.collectAsState()
    val poolPoints by viewModel.poolPoints.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isVacation by viewModel.isVacation.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()

    // 请求通知权限（Android 13+）
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDashboard()
        viewModel.refreshNotifications(showAlert = true)
    }

    // 错误提示
    LaunchedEffect(error) {
        error?.let { msg ->
            android.widget.Toast.makeText(navController.context, msg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.refreshAll() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打卡搭档${if (isVacation) " 🏖️" else ""}", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "我的",
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.AccountCircle, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile/achievements") }) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { navController.navigate("profile/notifications") }) {
                        BadgedBox(badge = { if (unreadCount > 0) Badge { Text("$unreadCount") } }) {
                            Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 积分卡片
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⭐ 个人积分", style = MaterialTheme.typography.labelMedium)
                                Text("$personalPoints", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎁 奖励池", style = MaterialTheme.typography.labelMedium)
                                Text("$poolPoints", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // 搭档入口
                item {
                    PartnerEntryCard(navController, viewModel)
                }

                // 连续打卡
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥 我的连续", style = MaterialTheme.typography.labelSmall)
                                Text("${dashboard?.myData?.streak ?: 0}天", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                            if (dashboard?.partnerData != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔥 搭档连续", style = MaterialTheme.typography.labelSmall)
                                    Text("${dashboard?.partnerData?.streak ?: 0}天", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }

                // 今日任务（一键打卡）
                item {
                    Text("📋 今日任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                val todayTasks = dashboard?.myData?.todayTaskStatus ?: emptyList()
                if (todayTasks.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("今天没有待打卡的任务~", modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(todayTasks) { ts ->
                        TodayTaskCard(ts, currentUserId,
                            onCheckin = { viewModel.doCheckin(ts.task.taskId) },
                            onDetail = { navController.navigate("checkin/detail/${ts.task.taskId}") })
                    }
                }

                // 搭档今日进度
                item { Spacer(Modifier.height(8.dp)) }

                if (dashboard?.partnerData != null) {
                    item {
                        Text("💑 ${dashboard?.partnerData?.username ?: "搭档"} 今日进度${if (dashboard?.partnerData?.isVacation == true) " 🏖️请假中" else ""}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val partnerTasks = dashboard?.partnerData?.todayTaskStatus ?: emptyList()
                    if (partnerTasks.isEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Text("搭档今天没有任务~", modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(partnerTasks) { ts ->
                            PartnerTaskCard(ts, viewModel, currentUserId)
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun TodayTaskCard(ts: TaskWithStatus, currentUserId: String, onCheckin: () -> Unit, onDetail: () -> Unit) {
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
    Card(Modifier.fillMaxWidth().clickable { onDetail() }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ts.task.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("+${ts.task.pointPerCheck}分 · $freqLabel · $creatorLabel",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ts.task.startTime != null || ts.task.endTime != null) {
                    Text("⏰ ${ts.task.startTime ?: "不限"}~${ts.task.endTime ?: "不限"}",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (ts.pendingApproval != null) {
                Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary, disabledContainerColor = MaterialTheme.colorScheme.tertiary)) {
                    Text("⏳ 等待确认", color = MaterialTheme.colorScheme.onTertiary)
                }
            } else if (ts.checkedIn) {
                Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(
                    containerColor = MintGreen, disabledContainerColor = MintGreen)) {
                    Icon(Icons.Filled.Check, null); Text("已打卡")
                }
            } else {
                Button(onClick = onCheckin) {
                    Text("打卡")
                }
            }
        }
    }
}

@Composable
fun PartnerTaskCard(ts: TaskWithStatus, viewModel: AppViewModel, currentUserId: String) {
    val isMyCreation = ts.task.creatorId == currentUserId
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = if (ts.pendingApproval != null) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.secondaryContainer
    )) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ts.task.name, style = MaterialTheme.typography.bodyMedium)
                if (ts.pendingApproval != null && isMyCreation) {
                    Text("⏳ 等待确认", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else if (ts.pendingApproval != null) {
                    Text("⏳ 待搭档确认", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (ts.pendingApproval != null && isMyCreation) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { viewModel.approveCheckin(ts.pendingApproval.recordId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("✓ 同意", fontWeight = FontWeight.Bold) }
                    TextButton(
                        onClick = { viewModel.rejectCheckin(ts.pendingApproval.recordId) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("✗ 拒绝") }
                }
            } else {
                Text(if (ts.checkedIn) "✅" else "⏳", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun PartnerEntryCard(navController: NavController, viewModel: AppViewModel) {
    val pairStatus by viewModel.pairStatus.collectAsState()
    LaunchedEffect(Unit) { viewModel.getPairStatus() }

    Card(
        Modifier.fillMaxWidth().clickable { navController.navigate("pair") },
        colors = CardDefaults.cardColors(
            containerColor = if (pairStatus?.status == "BOUND") MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            when (pairStatus?.status) {
                "BOUND" -> {
                    Icon(Icons.Filled.Favorite, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("搭档: ${pairStatus?.pair?.partnerUsername ?: "未知"}", fontWeight = FontWeight.Bold)
                        Text("点击查看搭档详情", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "PENDING" -> {
                    Icon(Icons.Filled.HourglassEmpty, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Text("搭档请求处理中…", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Icon(Icons.Filled.PersonAdd, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text("添加搭档", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
