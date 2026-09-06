package com.checkin.partner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.checkin.partner.ui.auth.LoginScreen
import com.checkin.partner.ui.auth.RegisterScreen
import com.checkin.partner.ui.home.HomeScreen
import com.checkin.partner.ui.pair.PairScreen
import com.checkin.partner.ui.task.TaskListScreen
import com.checkin.partner.ui.task.TaskCreateScreen
import com.checkin.partner.ui.checkin.CheckinDetailScreen
import com.checkin.partner.ui.checkin.MakeupScreen
import com.checkin.partner.ui.reward.RewardListScreen
import com.checkin.partner.ui.reward.RewardCreateScreen
import com.checkin.partner.ui.profile.ProfileScreen
import com.checkin.partner.ui.profile.SettingsScreen
import com.checkin.partner.ui.profile.PointsHistoryScreen
import com.checkin.partner.ui.profile.NotificationsScreen
import com.checkin.partner.ui.profile.AchievementsScreen
import com.checkin.partner.viewmodel.AppViewModel

val bottomNavRoutes = listOf("home", "task/list", "reward/list", "profile")

@Composable
private fun BottomNavItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 62.dp, height = 56.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(23.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .size(if (selected) 6.dp else 4.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    CircleShape,
                )
        )
    }
}

@Composable
fun CheckinNavGraph(viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val navController = rememberNavController()
    val startDest = if (viewModel.isLoggedIn.value) "home" else "login"
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // 监听通知点击导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route ->
            try {
                android.util.Log.d("NavGraph", "Navigate to: $route")
                navController.navigate(route) {
                    // 避免重复入栈
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                android.util.Log.e("NavGraph", "Navigation failed: $route", e)
            }
        }
    }

    // 登录态失效（token 过期 401 / 手动退出）→ 清空回退栈回到登录页
    LaunchedEffect(Unit) {
        viewModel.isLoggedIn.collect { loggedIn ->
            if (!loggedIn && navController.currentDestination?.route != "login") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 32.dp, end = 32.dp, top = 10.dp)
                        .shadow(12.dp, RoundedCornerShape(36.dp)),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        BottomNavItem(
                            selected = currentRoute == "home",
                            icon = Icons.Filled.Home,
                            contentDescription = "首页",
                            onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } },
                        )
                        BottomNavItem(
                            selected = currentRoute == "task/list",
                            icon = Icons.Filled.Checklist,
                            contentDescription = "任务",
                            onClick = { navController.navigate("task/list") { popUpTo("home"); launchSingleTop = true } },
                        )
                        BottomNavItem(
                            selected = currentRoute == "reward/list",
                            icon = Icons.Filled.CardGiftcard,
                            contentDescription = "奖励",
                            onClick = { navController.navigate("reward/list") { popUpTo("home"); launchSingleTop = true } },
                        )
                        BottomNavItem(
                            selected = currentRoute == "profile",
                            icon = Icons.Filled.Person,
                            contentDescription = "我的",
                            onClick = { navController.navigate("profile") { popUpTo("home"); launchSingleTop = true } },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(padding)
        ) {
            // 认证
            composable("login") { LoginScreen(navController, viewModel) }
            composable("register") { RegisterScreen(navController, viewModel) }

            // 主页
            composable("home") { HomeScreen(navController, viewModel) }
            composable("task/list") { TaskListScreen(navController, viewModel) }
            composable("reward/list") { RewardListScreen(navController, viewModel) }
            composable("profile") { ProfileScreen(navController, viewModel) }
            composable("pair") { PairScreen(navController, viewModel) }

            // 子页面
            composable("task/create?taskId={taskId}&assignTo={assignTo}",
                arguments = listOf(navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                 navArgument("assignTo") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { TaskCreateScreen(navController, viewModel, it.arguments?.getString("taskId"), it.arguments?.getString("assignTo")) }

            composable("checkin/detail/{recordId}",
                arguments = listOf(navArgument("recordId") { type = NavType.StringType })) {
                CheckinDetailScreen(navController, viewModel, it.arguments?.getString("recordId") ?: "")
            }
            composable("checkin/makeup") { MakeupScreen(navController, viewModel) }

            composable("reward/create") { RewardCreateScreen(navController, viewModel) }
            composable("profile/settings") { SettingsScreen(navController, viewModel) }
            composable("profile/points") { PointsHistoryScreen(navController, viewModel) }
            composable("profile/notifications") { NotificationsScreen(navController, viewModel) }
            composable("profile/achievements") { AchievementsScreen(navController, viewModel) }
        }
    }
}
