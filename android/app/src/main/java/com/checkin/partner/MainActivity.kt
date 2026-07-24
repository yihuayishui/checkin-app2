package com.checkin.partner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.checkin.partner.ui.CheckinNavGraph
import com.checkin.partner.ui.theme.CheckinPartnerTheme
import com.checkin.partner.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    // Android 13+ 通知权限请求
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // 不需要额外处理，JPush/FCM 自己会按权限状态决定行为
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 处理从系统通知栏点击进入的导航 + 标记已读
        handleNotificationIntent(intent)

        // Android 13+ 需运行时请求通知权限
        requestNotificationPermissionIfNeeded()

        setContent {
            CheckinPartnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CheckinNavGraph(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App 已在前台时点击通知，走这里
        handleNotificationIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val isDismiss = intent.getStringExtra("action") == "dismiss"
        val notifId = intent.getLongExtra("notif_id", -1L)

        // 滑出删除 → 只标记已读，不跳转
        if (isDismiss) {
            if (notifId > 0) {
                viewModel.markNotificationRead(notifId)
            }
            return
        }

        // 点击通知 → 跳转 + 标记已读
        intent.getStringExtra("nav_route")?.let { route ->
            viewModel.navigateTo(route)
        }
        if (notifId > 0) {
            viewModel.markNotificationRead(notifId)
        }
    }
}
