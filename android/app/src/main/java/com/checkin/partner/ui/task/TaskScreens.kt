package com.checkin.partner.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.checkin.partner.data.entity.TaskEntity
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
    val currentUserId by viewModel.currentUserId.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val currentFilter = taskTabs[selectedTab].first

    LaunchedEffect(currentFilter) { viewModel.refreshTasks(currentFilter) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务管理", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("task/create") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, "创建任务")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 8.dp,
                divider = { },
                indicator = { }
            ) {
                taskTabs.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(pair.second, maxLines = 1,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )
                }
            }

            val sortedTasks = tasks.sortedBy { if (it.status == "DONE") 1 else 0 }

            if (sortedTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedTasks) { task ->
                        TaskCard(task, currentUserId,
                            onEdit = { navController.navigate("task/create?taskId=${task.taskId}") },
                            onDelete = { viewModel.deleteTask(task.taskId, task.creatorId) },
                            onReactivate = { viewModel.reactivateTask(task.taskId) },
                            onRespondEdit = { accept -> viewModel.respondEditTask(task.taskId, accept, currentFilter) },
                            onRespondDelete = { accept -> viewModel.respondTaskDelete(task.taskId, accept, currentFilter) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, currentUserId: String,
             onEdit: () -> Unit, onDelete: () -> Unit, onReactivate: () -> Unit,
             onRespondEdit: ((Boolean) -> Unit)? = null,
             onRespondDelete: ((Boolean) -> Unit)? = null) {
    val isMine = task.userId == currentUserId
    val isDone = task.status == "DONE"
    val creatorLabel = when {
        isDone -> "已完成"
        task.creatorId == currentUserId && task.userId == currentUserId -> "我创建的"
        task.creatorId == currentUserId && task.userId != currentUserId -> "我给搭档的"
        task.creatorId != currentUserId && task.userId == currentUserId -> "搭档给我的"
        else -> ""
    }

    val color = when (task.frequency) {
        "DAILY" -> MaterialTheme.colorScheme.primary
        "WEEKLY" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(task.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isDone) {
                        AssistChip(onClick = {}, label = { Text("已休眠") },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant))
                    }
                    if (task.pendingEditBy != null) {
                        AssistChip(onClick = {}, label = { Text("编辑待确认") },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)))
                    }
                    if (task.pendingDeleteBy != null) {
                        AssistChip(onClick = {}, label = { Text("删除待确认") },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)))
                    }
                    AssistChip(onClick = {}, label = { Text(task.frequency) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.15f)))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("+${task.pointPerCheck}分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(creatorLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (task.startTime != null || task.endTime != null) {
                Text("⏰ ${task.startTime ?: "不限"}~${task.endTime ?: "不限"}", style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (task.pendingEditBy != null && task.creatorId == currentUserId) {
                    TextButton(onClick = { onRespondEdit?.invoke(true) }) { Text("同意编辑") }
                    TextButton(onClick = { onRespondEdit?.invoke(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                } else if (task.pendingDeleteBy != null && task.creatorId == currentUserId) {
                    TextButton(onClick = { android.util.Log.d("VM", "deleteAgree clicked"); onRespondDelete?.invoke(true) }) { Text("同意删除") }
                    TextButton(onClick = { android.util.Log.d("VM", "deleteReject clicked"); onRespondDelete?.invoke(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝删除") }
                } else if (isDone) {
                    TextButton(onClick = onReactivate) { Text("重新激活") }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                } else {
                    TextButton(onClick = onEdit) { Text("编辑") }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
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
    val editTask = tasks.find { it.taskId == editTaskId }
    val partnerId = pairStatus?.pair?.partnerId

    LaunchedEffect(editTaskId) { if (editTaskId != null) viewModel.refreshTasks() }
    LaunchedEffect(editTask) {
        editTask?.let {
            name = it.name
            frequency = it.frequency
            pointPerCheck = it.pointPerCheck.toString()
            startTime = it.startTime ?: ""
            endTime = it.endTime ?: ""
        }
    }
    var assignToPartner by remember { mutableStateOf(false) }
    LaunchedEffect(assignTo) { if (assignTo != null) assignToPartner = true }

    Scaffold(
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
