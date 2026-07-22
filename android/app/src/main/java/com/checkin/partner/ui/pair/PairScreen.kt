package com.checkin.partner.ui.pair

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
import com.checkin.partner.network.dto.PairStatus
import com.checkin.partner.network.dto.SearchUser
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(navController: NavController, viewModel: AppViewModel) {
    val pairStatus by viewModel.pairStatus.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.getPairStatus() }
    LaunchedEffect(error) { error?.let { viewModel.clearError() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的搭档", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val status = pairStatus?.status ?: "NONE"

            when (status) {
                "BOUND" -> {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Favorite, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("你和 ${pairStatus?.pair?.partnerUsername ?: "搭档"} 已绑定", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("绑定于 ${pairStatus?.pair?.createdAt?.take(10) ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        if (pairStatus?.pair?.unbindRequestedBy != null) {
                            val isMine = pairStatus!!.pair!!.unbindRequestedBy == viewModel.currentUserId.value
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(if (isMine) "你已发起解绑，等待对方确认…" else "搭档请求解绑",
                                        style = MaterialTheme.typography.bodyMedium)
                                    if (!isMine) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { viewModel.unbindRespond(true) }) { Text("同意") }
                                            Button(onClick = { viewModel.unbindRespond(false) }) { Text("拒绝") }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.unbindRequest() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("解除搭档关系") }
                        }
                    }
                }

                "PENDING" -> {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("搭档请求处理中…", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                val isRequested = pairStatus?.pair?.requestedBy == viewModel.currentUserId.value
                                if (isRequested) {
                                    Text("等待 ${pairStatus?.pair?.partnerUsername ?: "对方"} 回应", style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = {
                                        pairStatus?.pair?.partnerId?.let { viewModel.cancelPairRequest(it) }
                                    }) { Text("撤回请求") }
                                } else {
                                    Text("${pairStatus?.pair?.partnerUsername ?: "对方"} 向你发来搭档请求")
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { pairStatus?.pair?.pairId?.let { viewModel.respondPair(it, true) } }) { Text("同意") }
                                        OutlinedButton(onClick = { pairStatus?.pair?.pairId?.let { viewModel.respondPair(it, false) } }) { Text("拒绝") }
                                    }
                                }
                            }
                        }
                    }
                }

                "NONE" -> {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.PersonAdd, null, Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("还没有搭档", fontWeight = FontWeight.Bold)
                                Text("搜索用户名添加搭档吧", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 搜索
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("搜索用户名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { if (searchQuery.isNotBlank()) viewModel.searchUser(searchQuery.trim()) }) {
                                    Icon(Icons.Filled.Search, "搜索")
                                }
                            }
                        )
                    }

                    if (searchResults.isNotEmpty()) {
                        item { Text("搜索结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                        items(searchResults) { user ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(user.username, fontWeight = FontWeight.Bold)
                                        Text("ID: ${user.userId.take(8)}…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(onClick = { viewModel.sendPairRequest(user.userId) }) { Text("添加") }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            if (error != null) {
                item { Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
