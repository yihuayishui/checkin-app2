package com.checkin.partner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * 打卡搭档 — 设计规范主题
 * 参考: 打卡搭档APP-UI切图标注规范.md
 */

// ── 浅色主题配色 ──
private val LightColorScheme = lightColorScheme(
    // 主色
    primary = PinkPrimary,
    onPrimary = PageBg,
    primaryContainer = PinkContainer,
    onPrimaryContainer = PinkOnContainer,
    // 辅助色（用容器色模拟奶油浅黄）
    secondary = TextGray,
    onSecondary = PageBg,
    secondaryContainer = CreamLight.copy(alpha = 0.7f),  // 规范：搭档卡片 70% 透明
    onSecondaryContainer = TextDark,
    // 第三色（薄荷绿）
    tertiary = MintGreen,
    onTertiary = PageBg,
    tertiaryContainer = GreenContainer,
    onTertiaryContainer = TextDark,
    // 页面底色
    background = PageBg,
    onBackground = TextDark,
    surface = CardBg,
    onSurface = TextDark,
    surfaceVariant = CardBg.copy(alpha = 0.75f),  // 规范：通用卡片 75% 透明
    onSurfaceVariant = TextGray,
    // 功能色
    error = DangerRed,
    onError = PageBg,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = DangerRed,
    // 轮廓/分割线
    outline = DividerColor,
    outlineVariant = TextLight,
)

// ── 深色主题配色 ──
private val DarkColorScheme = darkColorScheme(
    primary = PinkPrimary,
    onPrimary = DarkBg,
    primaryContainer = Color(0xFF3A1A2A),
    onPrimaryContainer = Color(0xFFF0C0D0),
    secondary = TextGray,
    onSecondary = DarkBg,
    secondaryContainer = Color(0xFF3A3020),
    onSecondaryContainer = CreamLight,
    tertiary = MintGreen,
    onTertiary = DarkBg,
    tertiaryContainer = Color(0xFF1A3A2A),
    onTertiaryContainer = Color(0xFFB0E0C0),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTextGray,
    error = Color(0xFFE06060),
    onError = DarkBg,
    errorContainer = Color(0xFF4A2020),
    onErrorContainer = Color(0xFFE06060),
    outline = DarkDivider,
    outlineVariant = DarkDivider,
)

// ── 自定义排版（对应 UI 规范） ──
// 规范: 页面大标题 18sp Bold 24sp行高
//       模块标题 16sp Bold 22sp行高
//       正文 15sp Regular 20sp行高
//       标签/Tab 13sp Regular 18sp行高
//       辅助小字 12sp Regular 16sp行高
//       核心数据 26sp Bold 32sp行高

private val AppTypography = Typography(
    // 页面大标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    // 模块标题
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    // 正文内容
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // 标签/Tab文字
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // 辅助小字
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // 核心数据（用于积分、天数等）
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    // 按钮文字
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // 标签小字
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

// ── 自定义圆角（对应 UI 规范） ──
// 大模块卡片 12dp, 任务条目/按钮 10dp, 状态标签 6dp
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // 状态标签/小标识
    small = RoundedCornerShape(10.dp),       // 任务条目/按钮/商品卡片
    medium = RoundedCornerShape(12.dp),      // 大模块卡片
    large = RoundedCornerShape(16.dp),       // 弹窗等
    extraLarge = RoundedCornerShape(28.dp),  // 底部Sheet等
)

@Composable
fun CheckinPartnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
