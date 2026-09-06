package com.checkin.partner.ui.theme

import androidx.compose.ui.graphics.Color

// ── 主色系 ──
val PinkPrimary     = Color(0xFFE66B93)  // 主品牌柔粉
val PinkContainer   = Color(0xFFFFE5EE)  // 主色浅透底色 rgba(216,112,147,0.12) on white
val PinkOnContainer = Color(0xFFB64970)  // 主色容器上文字

// ── 辅助色系 ──
val CreamLight      = Color(0xFFFFEFD9)  // 辅助奶油浅黄 #FFE8C8 @70% on white
val CreamContainer  = Color(0xFFFFF4E4)  // 奶油色容器底

val Lavender         = Color(0xFF8575D8)
val LavenderContainer = Color(0xFFEDEAFF)

/** 首页统一使用的品牌渐变，主页面保持同一套视觉识别。 */
val BrandGradient = listOf(PinkPrimary, Lavender)

// ── 完成/成功色 ──
val MintGreen       = Color(0xFF95D5B2)  // 完成薄荷绿
val GreenContainer  = Color(0xFFE8F5EE)  // 绿色浅透底

// ── 页面底色 ──
val PageBg          = Color(0xFFFFF9FB)  // 页面底色纯白
val CardBg          = Color(0xFFFFFFFF)  // 通用卡片底色
val CardBgDark      = Color(0xFF2C2C2E)  // 深色模式卡片底

// ── 分割线 ──
val DividerColor    = Color(0xFFF0E5EA)  // rgba(203,213,224,0.4) on white

// ── 文字色 ──
val TextDark        = Color(0xFF2D2430)  // 标题深色文字
val TextGray        = Color(0xFF7F7280)  // 辅助中灰文字
val TextLight       = Color(0xFFC8BBC3)  // 次要浅灰文字

// ── 功能色 ──
val DangerRed       = Color(0xFFC53030)  // 危险色
val SuccessGreen    = Color(0xFF48BB78)  // 成功绿色

// ── 底部导航 ──
val BottomNavActive   = PinkPrimary
val BottomNavInactive = TextLight

// ═══════════════════════════════════════════
// 深色模式色值
// ═══════════════════════════════════════════
val DarkBg          = Color(0xFF141217)
val DarkSurface     = Color(0xFF211D24)
val DarkSurface2    = Color(0xFF2C2730)
val DarkTextPrimary = Color(0xFFF4ECF1)
val DarkTextGray    = Color(0xFFB9AFB8)
val DarkDivider     = Color(0xFF403943)

// 深色模式使用降低饱和度后的品牌色，避免粉、紫、绿同时高亮造成视觉跳脱。
val DarkPrimary             = Color(0xFFE58AA6)
val DarkPrimaryContainer    = Color(0xFF482533)
val DarkOnPrimaryContainer  = Color(0xFFFFD7E1)
val DarkSecondary           = Color(0xFFA99DDF)
val DarkSecondaryContainer  = Color(0xFF332F48)
val DarkOnSecondaryContainer = Color(0xFFE8E0FF)
val DarkTertiary            = Color(0xFF8BC9A6)
val DarkTertiaryContainer   = Color(0xFF213A2C)
val DarkOnTertiaryContainer = Color(0xFFB9EBCB)
val DarkError               = Color(0xFFF29A95)
val DarkErrorContainer      = Color(0xFF4A292F)
