package com.checkin.partner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Home, "首页") },
                        label = { Text("首页") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
                            indicatorColor = androidx.compose.ui.graphics.Color(0xFFE7F3D8).copy(alpha = 0.7f),
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "task/list",
                        onClick = { navController.navigate("task/list") { popUpTo("home"); launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Checklist, "任务") },
                        label = { Text("任务") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
                            indicatorColor = androidx.compose.ui.graphics.Color(0xFFE7F3D8).copy(alpha = 0.7f),
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "reward/list",
                        onClick = { navController.navigate("reward/list") { popUpTo("home"); launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.CardGiftcard, "奖励") },
                        label = { Text("奖励") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
                            indicatorColor = Color(0xFFE7F3D8).copy(alpha = 0.7f),
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") { popUpTo("home"); launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Person, "我的") },
                        label = { Text("我的") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
                            indicatorColor = Color(0xFFE7F3D8).copy(alpha = 0.7f),
                        )
                    )
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
