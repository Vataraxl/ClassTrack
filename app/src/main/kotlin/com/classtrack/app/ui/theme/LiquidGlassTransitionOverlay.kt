package com.classtrack.app.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classtrack.app.data.model.UiStyle
import com.classtrack.app.ui.main.UiStyleTransitionEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 零延迟跟手质感切换容器：
 * 点击瞬间第 0 帧即切换目标材质（消除 80ms 阻塞等待），配合高刚度微弹性阻尼弹簧，
 * 确保材质切换瞬间响应、不掉帧。
 */
@Composable
fun UiStyleOpticalTransitionContainer(
    targetUiStyle: UiStyle,
    event: UiStyleTransitionEvent?,
    onTransitionFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (displayedUiStyle: UiStyle) -> Unit
) {
    val view = LocalView.current
    var displayedStyle by remember { mutableStateOf(targetUiStyle) }

    LaunchedEffect(targetUiStyle) {
        displayedStyle = targetUiStyle
    }

    val contentScale = remember { Animatable(1f) }

    LaunchedEffect(event?.timestamp) {
        val currentEvent = event ?: return@LaunchedEffect
        displayedStyle = currentEvent.toStyle

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        launch {
            contentScale.snapTo(0.986f)
            contentScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 550f
                )
            )
        }

        delay(180L)
        onTransitionFinished()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                }
        ) {
            content(displayedStyle)
        }
    }
}
