package com.checkin.partner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// 整页骨架共享的扫光偏移，保证所有色块同步动画（全页只创建一个 infinite transition）
private val LocalShimmerOffset = staticCompositionLocalOf<State<Float>?> { null }

@Composable
private fun screenWidthPx(): Float = with(LocalDensity.current) {
    LocalConfiguration.current.screenWidthDp.dp.toPx()
}

/** 包裹整个骨架页，让所有色块共享同一条扫光动画 */
@Composable
fun ShimmerScope(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition()
    val widthPx = screenWidthPx()
    val offsetState = transition.animateFloat(
        initialValue = -widthPx,
        targetValue = widthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer"
    )
    CompositionLocalProvider(LocalShimmerOffset provides offsetState) {
        content()
    }
}

@Composable
fun Modifier.shimmer(): Modifier {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val widthPx = screenWidthPx()
    // 优先使用整页共享的动画；单独使用时（无 ShimmerScope）自己创建一个
    val offset = LocalShimmerOffset.current?.value ?: rememberInfiniteTransition().animateFloat(
        initialValue = -widthPx,
        targetValue = widthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer"
    ).value
    return background(
        Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(offset, 0f),
            end = Offset(offset + widthPx * 0.5f, widthPx * 0.5f),
        )
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier,
    shape: Shape,
) {
    Box(modifier.clip(shape).shimmer())
}

@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
        ) {
            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    ShimmerBox(Modifier.size(46.dp), androidx.compose.foundation.shape.CircleShape)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.36f).height(16.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.22f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    }
                    ShimmerBox(Modifier.size(38.dp), androidx.compose.foundation.shape.CircleShape)
                }
            }
            item { ShimmerBox(Modifier.fillMaxWidth().height(142.dp), androidx.compose.foundation.shape.RoundedCornerShape(22.dp)) }
            item { ShimmerBox(Modifier.fillMaxWidth().height(76.dp), androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) }
            item {
                androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(Modifier.fillMaxWidth(0.28f).height(18.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.46f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                }
            }
            item { SkeletonTaskCard() }
            item { SkeletonTaskCard() }
        }
    }
}

@Composable
fun TaskListSkeleton() {
    ShimmerScope {
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            item { ShimmerBox(Modifier.fillMaxWidth().height(72.dp), androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) }
            item {
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    repeat(4) { ShimmerBox(Modifier.width(74.dp).height(36.dp), androidx.compose.foundation.shape.RoundedCornerShape(50)) }
                }
            }
            item { SkeletonTaskCard() }
            item { SkeletonTaskCard() }
            item { SkeletonTaskCard() }
        }
    }
}

@Composable
fun RewardListSkeleton() {
    ShimmerScope {
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        ) {
            ShimmerBox(Modifier.fillMaxWidth().height(88.dp), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            SkeletonRewardCard()
            SkeletonRewardCard()
        }
    }
}

@Composable
fun ProfileSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = modifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        ) {
            item {
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                ) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        ShimmerBox(Modifier.size(66.dp), androidx.compose.foundation.shape.CircleShape)
                        androidx.compose.foundation.layout.Spacer(Modifier.width(14.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
                            ShimmerBox(Modifier.fillMaxWidth(0.2f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            ShimmerBox(Modifier.fillMaxWidth(0.45f).height(22.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            ShimmerBox(Modifier.fillMaxWidth(0.32f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        }
                    }
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    ) {
                        ShimmerBox(Modifier.weight(1f).height(48.dp), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        ShimmerBox(Modifier.weight(1f).height(48.dp), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    }
                }
            }
            item { ShimmerBox(Modifier.fillMaxWidth(0.25f).height(18.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) }
            item { SkeletonMenuCard() }
            item { SkeletonMenuCard() }
            item { SkeletonMenuCard() }
        }
    }
}

@Composable
private fun SkeletonTaskCard() {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().height(112.dp).padding(14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        ShimmerBox(Modifier.size(44.dp), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                ShimmerBox(Modifier.fillMaxWidth(0.46f).height(16.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                ShimmerBox(Modifier.width(62.dp).height(28.dp), androidx.compose.foundation.shape.RoundedCornerShape(50))
            }
            ShimmerBox(Modifier.fillMaxWidth(0.72f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.42f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        }
    }
}

@Composable
private fun SkeletonRewardCard() {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().height(110.dp).padding(14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        ShimmerBox(Modifier.size(46.dp), androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            ShimmerBox(Modifier.fillMaxWidth(0.52f).height(16.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.7f).height(12.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(1.dp), androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.3f).height(28.dp), androidx.compose.foundation.shape.RoundedCornerShape(50))
        }
    }
}

@Composable
private fun SkeletonMenuCard() {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        ShimmerBox(Modifier.size(40.dp), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)) {
            ShimmerBox(Modifier.fillMaxWidth(0.38f).height(15.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.62f).height(11.dp), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        }
        ShimmerBox(Modifier.size(18.dp), androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
    }
}
