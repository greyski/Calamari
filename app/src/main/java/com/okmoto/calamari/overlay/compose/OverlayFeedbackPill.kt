package com.okmoto.calamari.overlay.compose

import androidx.annotation.ColorRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.okmoto.calamari.R
import com.okmoto.calamari.ui.theme.CalamariTheme

enum class OverlayFeedbackStyle(
    @param:ColorRes val containerColor: Int,
    @param:ColorRes val contentColor: Int,
) {
    SUCCESS(
        containerColor = R.color.calamari_bubble_awaiting_send_green,
        contentColor = R.color.white,
    ),
    ERROR(
        containerColor = R.color.calamari_bubble_error_red,
        contentColor = R.color.white,
    ),
    INFO(
        containerColor = R.color.calamari_bubble_idle_title_blue,
        contentColor = R.color.white
    ),
}

data class OverlayFeedbackUiState(
    val message: String,
    val style: OverlayFeedbackStyle,
)

@Composable
fun OverlayFeedbackPill(
    state: OverlayFeedbackUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(percent = 50)),
        color = colorResource(state.style.containerColor),
        contentColor = colorResource(state.style.contentColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = when (state.style) {
                    OverlayFeedbackStyle.SUCCESS -> Icons.Default.CheckCircle
                    OverlayFeedbackStyle.ERROR -> Icons.Default.Warning
                    OverlayFeedbackStyle.INFO -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = state.message,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
fun OverlayFeedbackPillPreview(
    state: OverlayFeedbackUiState = OverlayFeedbackUiState(
        message = "Event \"Weekly planning\" created.",
        style = OverlayFeedbackStyle.SUCCESS,
    )
) {
    CalamariTheme {
        OverlayFeedbackPill(
            state = state,
        )
    }
}
