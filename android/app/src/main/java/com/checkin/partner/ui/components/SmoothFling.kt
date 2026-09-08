package com.checkin.partner.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * 顺滑惯性滚动：摩擦系数低于系统默认，松手后滑得更远、停得更柔。
 *
 * 系统默认 fling 用高摩擦 spline，手指一松很快停住，主观感受是"涩、跟手差"；
 * 这里用低摩擦指数衰减，初速保留更好，接近 Telegram / iOS 列表的顺滑手感。
 * 注意：只改变减速曲线，不改变内容，手感差异纯主观，真机验证为准。
 */
class SmoothFlingBehavior(
    private val decay: DecayAnimationSpec<Float>,
    private val minVelocity: Float = 80f,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < minVelocity) return initialVelocity
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(decay) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue += consumed
            // 滚不动了（到边界）就停
            if (consumed != delta) cancelAnimation()
        }
        return 0f
    }
}

/** 全 App 列表用的顺滑 fling（remember 缓存，跨重组复用）：指数衰减 + 低摩擦 */
@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decay = remember { exponentialDecay<Float>(frictionMultiplier = 0.25f) }
    return remember(decay) { SmoothFlingBehavior(decay) }
}
