package com.checkin.partner.ui.reward

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
import com.checkin.partner.data.entity.RewardEntity
import com.checkin.partner.ui.components.ScaleButton
import com.checkin.partner.ui.theme.MintGreen
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardListScreen(navController: NavController, viewModel: AppViewModel) {
    val rewards by viewModel.rewards.collectAsState()
    val poolPoints by viewModel.poolPoints.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val rewardVersion by viewModel.rewardVersion.collectAsState()

    LaunchedEffect(Unit, rewardVersion) { viewModel.refreshRewards() }

    key(rewardVersion) {

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TopAppBar(
            title = { Text("奖励兑换", fontWeight = FontWeight.Bold) },
            navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "返回") }
        },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            )
        )},
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("reward/create") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Filled.Add, "上架奖励")
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎁 奖励池积分", style = MaterialTheme.typography.titleMedium)
                        Text("$poolPoints", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (rewards.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("还没有奖励，点击右下角上架吧~", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(rewards, key = { "${it.id}_${it.status}_${it.pendingDeleteBy}_${it.claimRequestedBy}" }) { reward ->
                    RewardCard(reward, poolPoints, currentUserId,
                        onExchange = { viewModel.exchangeReward(reward.id) },
                        onCancelExchange = { viewModel.cancelExchange(reward.id) },
                        onConfirm = { accept -> viewModel.confirmExchange(reward.id, accept) },
                        onClaim = { viewModel.claimReward(reward.id) },
                        onRequestDelete = { viewModel.deleteReward(reward.id) },
                        onRespondDelete = { accept -> viewModel.respondDeleteReward(reward.id, accept) },
                        onRespondClaim = { accept -> viewModel.respondClaimReward(reward.id, accept) },
                    )
                }
            }
        }
    }
  }
}

@Composable
fun RewardCard(reward: RewardEntity, poolPoints: Int, currentUserId: String,
               onExchange: () -> Unit, onCancelExchange: () -> Unit,
               onConfirm: (Boolean) -> Unit, onClaim: () -> Unit,
               onRequestDelete: () -> Unit, onRespondDelete: (Boolean) -> Unit,
               onRespondClaim: (Boolean) -> Unit) {
    val statusColor = when (reward.status) {
        "ACTIVE" -> MaterialTheme.colorScheme.primary
        "PENDING" -> MaterialTheme.colorScheme.secondary
        "CONFIRMED" -> MaterialTheme.colorScheme.tertiary
        "CLAIMED" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline
    }
    val statusText = when (reward.status) {
        "ACTIVE" -> "已上架"
        "PENDING" -> "待同意"
        "CONFIRMED" -> "待兑现"
        "CLAIMED" -> "已兑现"
        else -> reward.status
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(reward.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(statusText) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.15f)))
            }
            Text("需要 ${reward.requiredPoints} 奖励池积分", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            val canExchange = reward.status == "ACTIVE" && poolPoints >= reward.requiredPoints

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // 删除请求：被请求方显示同意/拒绝
                val pendingDelete = reward.pendingDeleteBy
                if (pendingDelete != null && pendingDelete != currentUserId) {
                    TextButton(onClick = { onRespondDelete(true) }) { Text("同意删除") }
                    TextButton(onClick = { onRespondDelete(false) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                }
                // 删除请求：请求方显示待确认
                else if (pendingDelete == currentUserId) {
                    TextButton(onClick = { onRespondDelete(false) }) { Text("撤回删除") }
                }
                // 正常操作
                else when {
                    reward.status == "ACTIVE" && canExchange -> {
                        ScaleButton(onClick = onExchange, modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = MaterialTheme.colorScheme.onTertiary)) { Text("兑换") }
                    }
                    reward.status == "PENDING" && reward.applicantId == currentUserId -> {
                        TextButton(onClick = onCancelExchange) { Text("撤回申请") }
                    }
                    reward.status == "PENDING" && reward.applicantId != currentUserId -> {
                        TextButton(onClick = { onConfirm(true) }) { Text("同意") }
                        TextButton(onClick = { onConfirm(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                    }
                    // 待兑现：还没人申请
                reward.status == "CONFIRMED" && reward.claimRequestedBy == null -> {
                    ScaleButton(onClick = onClaim, modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = MaterialTheme.colorScheme.onTertiary)) { Text("申请兑现") }
                }
                // 待兑现：我在申请中
                reward.status == "CONFIRMED" && reward.claimRequestedBy == currentUserId -> {
                    TextButton(onClick = { onRespondClaim(false) }) { Text("撤回兑现") }
                }
                // 待兑现：搭档申请了，我来确认
                reward.status == "CONFIRMED" && reward.claimRequestedBy != null && reward.claimRequestedBy != currentUserId -> {
                    TextButton(onClick = { onRespondClaim(true) }) { Text("同意兑现") }
                    TextButton(onClick = { onRespondClaim(false) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                }
                    reward.status == "CLAIMED" -> {
                        TextButton(onClick = onRequestDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("请求删除") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardCreateScreen(navController: NavController, viewModel: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TopAppBar(
            title = { Text("上架奖励", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("奖励名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("如：一顿大餐、一件礼物...") })
            OutlinedTextField(value = points, onValueChange = { points = it },
                label = { Text("所需积分") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.createReward(name, points.toIntOrNull() ?: 0)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = name.isNotBlank() && (points.toIntOrNull() ?: 0) > 0
            ) { Text("上  架", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
