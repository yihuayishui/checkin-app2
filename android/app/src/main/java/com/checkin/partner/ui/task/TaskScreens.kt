package com.checkin.partner.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.checkin.partner.data.entity.TaskEntity
import com.checkin.partner.ui.components.TaskListSkeleton
import com.checkin.partner.ui.theme.PinkContainer
import com.checkin.partner.ui.theme.PinkPrimary
import com.checkin.partner.viewmodel.AppViewModel

val taskTabs = listOf(
    "mine" to "我的任务",
    "created" to "我创建的",
    "for_partner" to "我给搭档的",
    "from_partner" to "搭档给我的",
    "done" to "已完成",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController, viewModel: AppViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val tasksLoaded by viewModel.tasksLoaded.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val currentFilter = taskTabs[selectedTab].first

    // 切 tab 或从创建/编辑页返回时（route 重新回到 task/list），按当前 filter 刷新，
    // 避免 createTask/deleteTask 内部默认刷新成 "mine" 数据导致其他 tab 显示错乱
    val currentBackStack by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStack?.destination?.route, selectedTab) {
        if (currentBackStack?.destination?.route == "task/list") {
            viewModel.refreshTasks(currentFilter)
        }
    }
    val sortedTasks = tasks.sortedBy { if (it.status == "DONE") 1 else 0 }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("任务管理", fontWeight = FontWeight.Bold)
                        Text("把重要的事，一件件完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("task/create") },
                containerColor = PinkPrimary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, "创建任务")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!tasksLoaded) {
                TaskListSkeleton()
            } else {
                TaskSummaryCard(
                taskCount = sortedTasks.size,
                selectedLabel = taskTabs[selectedTab].second,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                taskTabs.forEachIndexed { index, pair ->
                    Surface(
                        modifier = Modifier.clickable { selectedTab = index },
                        shape = RoundedCornerShape(50),
                        color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = if (selectedTab == index) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    ) {
                        Text(
                            pair.second,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            if (sortedTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedTasks) { task ->
                        TaskCard(task, currentUserId,
                            onEdit = { navController.navigate("task/create?taskId=${task.taskId}") },
                            onDelete = { viewModel.deleteTask(task.taskId, task.creatorId, currentFilter) },
                            onReactivate = { viewModel.reactivateTask(task.taskId, currentFilter) },
                            onRespondEdit = { accept -> viewModel.respondEditTask(task.taskId, accept, currentFilter) },
                            onRespondDelete = { accept -> viewModel.respondTaskDelete(task.taskId, accept, currentFilter) }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun TaskSummaryCard(taskCount: Int, selectedLabel: String) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Checklist, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("把今天安排好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("$selectedLabel · $taskCount 个任务", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
            }
            Text("${taskCount}项", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, currentUserId: String,
             onEdit: () -> Unit, onDelete: () -> Unit, onReactivate: () -> Unit,
             onRespondEdit: ((Boolean) -> Unit)? = null,
             onRespondDelete: ((Boolean) -> Unit)? = null) {
    val isDone = task.status == "DONE"
    val creatorLabel = when {
        isDone -> "已完成"
        task.creatorId == currentUserId && task.userId == currentUserId -> "我创建的"
        task.creatorId == currentUserId && task.userId != currentUserId -> "我给搭档的"
        task.creatorId != currentUserId && task.userId == currentUserId -> "搭档给我的"
        else -> ""
    }
    val (frequencyLabel, frequencyColor, frequencyIcon) = when (task.frequency) {
        "DAILY" -> Triple("每日", MaterialTheme.colorScheme.primary, Icons.Filled.Today)
        "WEEKLY" -> Triple("每周", MaterialTheme.colorScheme.secondary, Icons.Filled.DateRange)
        "ONCE" -> Triple("一次性", MaterialTheme.colorScheme.tertiary, Icons.Filled.Flag)
        else -> Triple(task.frequency, MaterialTheme.colorScheme.tertiary, Icons.Filled.TaskAlt)
    }
    val statusLabel = when {
        task.pendingEditBy != null -> "编辑待确认"
        task.pendingDeleteBy != null -> "删除待确认"
        isDone -> "已完成"
        else -> "进行中"
    }
    val statusColor = when {
        task.pendingEditBy != null -> MaterialTheme.colorScheme.secondary
        task.pendingDeleteBy != null -> MaterialTheme.colorScheme.error
        isDone -> MaterialTheme.colorScheme.tertiary
        else -> frequencyColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(frequencyColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(frequencyIcon, contentDescription = null, tint = frequencyColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(task.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    AssistChip(
                        onClick = {},
                        label = { Text(statusLabel) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = statusColor.copy(alpha = 0.14f),
                            labelColor = statusColor,
                        ),
                        border = null,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("+${task.pointPerCheck}分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(creatorLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AssistChip(
                        onClick = {},
                        label = { Text(frequencyLabel) },
                        leadingIcon = { Icon(frequencyIcon, contentDescription = null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = frequencyColor.copy(alpha = 0.1f), labelColor = frequencyColor, leadingIconContentColor = frequencyColor),
                        border = null,
                    )
                }
                if (task.startTime != null || task.endTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${task.startTime ?: "不限"} — ${task.endTime ?: "不限"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when {
                        task.pendingEditBy != null && task.creatorId == currentUserId -> {
                            TextButton(onClick = { onRespondEdit?.invoke(true) }) { Text("同意编辑") }
                            TextButton(onClick = { onRespondEdit?.invoke(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                        }
                        task.pendingDeleteBy != null && task.creatorId == currentUserId -> {
                            TextButton(onClick = { onRespondDelete?.invoke(true) }) { Text("同意删除") }
                            TextButton(onClick = { onRespondDelete?.invoke(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                        }
                        isDone -> {
                            TextButton(onClick = onReactivate) { Text("重新激活") }
                            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                        }
                        else -> {
                            TextButton(onClick = onEdit) { Text("编辑") }
                            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateScreen(navController: NavController, viewModel: AppViewModel, editTaskId: String?, assignTo: String?) {
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("DAILY") }
    var pointPerCheck by remember { mutableStateOf("10") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("22:00") }

    val isLoading by viewModel.isLoading.collectAsState()
    val pairStatus by viewModel.pairStatus.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val tasksLoaded by viewModel.tasksLoaded.collectAsState()
    val editTask = tasks.find { it.taskId == editTaskId }
    val partnerId = pairStatus?.pair?.partnerId

    LaunchedEffect(editTaskId) { if (editTaskId != null) viewModel.refreshTasks() }
    var assignToPartner by remember { mutableStateOf(false) }
    var requireApproval by remember { mutableStateOf(false) }
    LaunchedEffect(assignTo) { if (assignTo != null) assignToPartner = true }
    LaunchedEffect(editTask) {
        editTask?.let {
            name = it.name
            frequency = it.frequency
            pointPerCheck = it.pointPerCheck.toString()
            startTime = it.startTime ?: ""
            endTime = it.endTime ?: ""
            requireApproval = it.requireApproval
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(if (editTaskId == null) "创建任务" else "编辑任务", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("任务名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (partnerId != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("分配给搭档 (${pairStatus?.pair?.partnerUsername ?: ""})")
                    Switch(checked = assignToPartner, onCheckedChange = { assignToPartner = it })
                }
            }

            // 分配给搭档时，显示"需要我确认"开关
            if (assignToPartner) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("需要我确认后才能完成打卡", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = requireApproval, onCheckedChange = { requireApproval = it })
                }
            }

            Text("打卡频次", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DAILY" to "每日", "WEEKLY" to "每周", "ONCE" to "一次性").forEach { (v, label) ->
                    FilterChip(selected = frequency == v, onClick = { frequency = v }, label = { Text(label) })
                }
            }

            OutlinedTextField(value = pointPerCheck, onValueChange = { pointPerCheck = it },
                label = { Text("每次积分") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = startTime, onValueChange = { startTime = it },
                label = { Text("开始时间 (如 08:00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("留空则不限制") })
            OutlinedTextField(value = endTime, onValueChange = { endTime = it },
                label = { Text("结束时间 (如 22:00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("留空则不限制") })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.createTask(name, if (assignToPartner) partnerId else null, frequency,
                        pointPerCheck.toIntOrNull() ?: 10, startTime.ifBlank { null }, endTime.ifBlank { null },
                        editTaskId, editTask?.creatorId,
                        requireApproval = assignToPartner && requireApproval,
                        onDone = { navController.popBackStack() })
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && name.isNotBlank()
            ) {
                Text("保  存", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
