package com.checkin.partner.ui.reward

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.checkin.partner.data.entity.RewardEntity
import com.checkin.partner.ui.components.RewardListSkeleton
import com.checkin.partner.ui.components.ScaleButton
import com.checkin.partner.ui.theme.PinkContainer
import com.checkin.partner.ui.theme.PinkPrimary
import com.checkin.partner.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardListScreen(navController: NavController, viewModel: AppViewModel) {
    val rewards by viewModel.rewards.collectAsState()
    val rewardsLoaded by viewModel.rewardsLoaded.collectAsState()
    val poolPoints by viewModel.poolPoints.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val rewardVersion by viewModel.rewardVersion.collectAsState()

    LaunchedEffect(Unit, rewardVersion) { viewModel.refreshRewards() }

    key(rewardVersion) {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("奖励兑换", fontWeight = FontWeight.Bold)
                            Text("把坚持换成期待", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("reward/create") },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, "上架奖励")
                }
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!rewardsLoaded) {
                    item { RewardListSkeleton() }
                } else {
                    item { RewardPoolCard(poolPoints, rewards.size) }

                    if (rewards.isEmpty()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("还没有奖励", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("上架一个小心愿，让每次打卡都有期待", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        }
                    } else {
                        items(rewards, key = { "${it.id}_${it.status}_${it.pendingDeleteBy}_${it.claimRequestedBy}" }) { reward ->
                        RewardCard(
                            reward, poolPoints, currentUserId,
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
}

@Composable
private fun RewardPoolCard(poolPoints: Int, rewardCount: Int) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
            ).padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("奖励池积分", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("给努力一点甜头 · $rewardCount 个奖励", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("$poolPoints", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("积分", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
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
        "ACTIVE" -> "可兑换"
        "PENDING" -> "待同意"
        "CONFIRMED" -> "待兑现"
        "CLAIMED" -> "已兑现"
        else -> reward.status
    }
    val canExchange = reward.status == "ACTIVE" && poolPoints >= reward.requiredPoints

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(statusColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = statusColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(reward.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Filled.Stars, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("需要 ${reward.requiredPoints} 奖励池积分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text(statusText) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = statusColor.copy(alpha = 0.14f), labelColor = statusColor),
                        border = null,
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    val pendingDelete = reward.pendingDeleteBy
                    if (pendingDelete != null && pendingDelete != currentUserId) {
                        TextButton(onClick = { onRespondDelete(true) }) { Text("同意删除") }
                        TextButton(onClick = { onRespondDelete(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                    } else if (pendingDelete == currentUserId) {
                        TextButton(onClick = { onRespondDelete(false) }) { Text("撤回删除") }
                    } else when {
                        reward.status == "ACTIVE" && canExchange -> {
                            ScaleButton(onClick = onExchange, modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("兑换") }
                        }
                        reward.status == "ACTIVE" && !canExchange -> {
                            Text("还差 ${reward.requiredPoints - poolPoints} 积分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        reward.status == "PENDING" && reward.applicantId == currentUserId -> {
                            TextButton(onClick = onCancelExchange) { Text("撤回申请") }
                        }
                        reward.status == "PENDING" && reward.applicantId != currentUserId -> {
                            TextButton(onClick = { onConfirm(true) }) { Text("同意") }
                            TextButton(onClick = { onConfirm(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                        }
                        reward.status == "CONFIRMED" && reward.claimRequestedBy == null -> {
                            ScaleButton(onClick = onClaim, modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("申请兑现") }
                        }
                        reward.status == "CONFIRMED" && reward.claimRequestedBy == currentUserId -> {
                            TextButton(onClick = { onRespondClaim(false) }) { Text("撤回兑现") }
                        }
                        reward.status == "CONFIRMED" && reward.claimRequestedBy != null && reward.claimRequestedBy != currentUserId -> {
                            TextButton(onClick = { onRespondClaim(true) }) { Text("同意兑现") }
                            TextButton(onClick = { onRespondClaim(false) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("拒绝") }
                        }
                        reward.status == "CLAIMED" -> {
                            TextButton(onClick = onRequestDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("请求删除") }
                        }
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
