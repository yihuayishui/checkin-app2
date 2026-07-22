package com.checkin.partner.ui.checkin

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.checkin.partner.data.entity.TaskEntity
import com.checkin.partner.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeupScreen(navController: NavController, viewModel: AppViewModel) {
    val remaining by viewModel.makeupRemaining.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var targetDate by remember { mutableStateOf("") }
    var resultMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMakeupCards()
        viewModel.refreshTasks()
    }

    val activeTasks = tasks.filter { it.status == "ACTIVE" && it.userId == currentUserId }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("补签卡", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            )
        )}
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("🎫", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("剩余补签卡", style = MaterialTheme.typography.labelSmall)
                            Text("$remaining 张", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { Text("选择任务", fontWeight = FontWeight.Bold) }

            items(activeTasks) { task ->
                Card(Modifier.fillMaxWidth().clickable { selectedTask = task }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedTask?.taskId == task.taskId, onClick = { selectedTask = task })
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(task.name, fontWeight = FontWeight.Bold)
                            Text("+${task.pointPerCheck}分 · ${task.frequency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("补签日期 (如 2026-07-15)") },
                    placeholder = { Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                Button(
                    onClick = {
                        val task = selectedTask
                        if (task != null && targetDate.isNotBlank()) {
                            viewModel.doMakeup(task.taskId, targetDate) { ok, msg ->
                                resultMsg = msg
                                if (ok) {
                                    selectedTask = null
                                    targetDate = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = selectedTask != null && targetDate.isNotBlank() && remaining > 0
                ) { Text("使用补签卡", style = MaterialTheme.typography.titleMedium) }
            }

            resultMsg?.let {
                item { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
