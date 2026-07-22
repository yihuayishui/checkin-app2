package com.checkin.partner.ui.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinDetailScreen(navController: NavController, viewModel: AppViewModel, recordId: String) {
    Scaffold(
        topBar = { TopAppBar(
            title = { Text("打卡详情", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            )
        )}
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("打卡详情", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("记录ID: $recordId", style = MaterialTheme.typography.bodyMedium)
                    Text("详情查看与留言互动功能开发中...", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
