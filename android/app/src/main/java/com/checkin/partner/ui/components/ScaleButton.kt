package com.checkin.partner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** 带 0.96 缩放动效的按钮 */
@Composable
fun ScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    var animating by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animating) 0.96f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "btnScale"
    )
    Button(
        onClick = {
            animating = true
            onClick()
        },
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}
