package com.okmoto.calamari.overlay.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okmoto.calamari.R
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun EventPromptOverlay(
    state: PromptUiState,
    modifier: Modifier = Modifier,
    onAddTitleClicked: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(12.dp)
                .background(Color.White)
                .clip(RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .clickable(
                        onClick = {
                            when (state.titleMode) {
                                is PromptUiState.TitleMode.AddEventName -> onAddTitleClicked()
                                is PromptUiState.TitleMode.Listening -> Unit
                                is PromptUiState.TitleMode.Captured -> onRetryClicked()
                                is PromptUiState.TitleMode.TryAgain -> onAddTitleClicked()
                            }
                        }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.titleMode != PromptUiState.TitleMode.Listening) {
                    Icon(
                        imageVector = when (state.titleMode) {
                            is PromptUiState.TitleMode.AddEventName -> Icons.Default.Add
                            is PromptUiState.TitleMode.Captured -> Icons.Default.Refresh
                            is PromptUiState.TitleMode.TryAgain -> Icons.Default.Refresh
                            else -> Icons.Default.Build
                        },
                        contentDescription = null,
                        tint = Color.Black,
                    )
                }
                Text(
                    text = state.titleMode.header,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                )
            }
            Text(
                text = state.dayLabel,
                fontSize = 18.sp,
                color = Color.Black,
            )
            if (state.timeLabel.isNotBlank()) {
                Text(
                    text = state.timeLabel,
                    fontSize = 18.sp,
                    color = Color.Black,
                )
            }
            state.idleTimer?.let { idle ->
                key(idle.startedAtUptimeMs, idle.durationMs) {
                    val color = colorResource(
                        when (idle.action) {
                            PromptUiState.TimerAction.SEND -> R.color.calamari_bubble_awaiting_send_green
                            PromptUiState.TimerAction.DELETE -> R.color.calamari_bubble_error_red
                        }
                    )
                    val progress = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        progress.snapTo(0f)
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = idle.durationMs.toInt(),
                                easing = LinearEasing,
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = when (idle.action) {
                                PromptUiState.TimerAction.SEND -> Icons.Default.Done
                                PromptUiState.TimerAction.DELETE -> Icons.Default.Close
                            },
                            contentDescription = null,
                            tint = color,
                        )
                        Box(Modifier.weight(1F)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
        Arrow(
            modifier = when (state.arrowPosition) {
                PromptUiState.ArrowPosition.ABOVE_CONTENT -> Modifier.align(Alignment.TopCenter)
                PromptUiState.ArrowPosition.BELOW_CONTENT -> Modifier.align(Alignment.BottomCenter)
            }.offset(
                y = when (state.arrowPosition) {
                    PromptUiState.ArrowPosition.ABOVE_CONTENT -> 6.dp
                    PromptUiState.ArrowPosition.BELOW_CONTENT -> (-6).dp
                }
            )
        )
    }
}

@Composable
private fun Arrow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .rotate(45f)
            .background(Color.White),
    )
}

@Preview
@Composable
fun EventPromptOverlayPreview(
    state: PromptUiState = PromptUiState(
        dayLabel = "Friday February 14th",
        timeLabel = "12:30pm",
        arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
        titleMode = PromptUiState.TitleMode.AddEventName
    )
) {
    CalamariTheme {
        EventPromptOverlay(
            state = state
        )
    }
}

