package com.okmoto.calamari.overlay.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.okmoto.calamari.overlay.ListeningState
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun BubbleOverlay(
    state: ListeningState,
    mainActivityResumed: Boolean,
    month: String,
    day: String,
    modifier: Modifier = Modifier,
) {
    val overlayAlpha = bubbleOverlayAlpha(
        mainActivityResumed = mainActivityResumed,
        state = state,
    )
    Column(
        modifier = modifier
            .alpha(overlayAlpha)
            .padding(4.dp)
            .size(96.dp)
            .animatedBadgeFor(
                state = state,
                backgroundColor = colorResource(state.backgroundColorId),
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.wrapContentSize(),
            text = month,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = colorResource(state.textColorId),
        )
        Text(
            modifier = Modifier.wrapContentSize(),
            text = day,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = colorResource(state.textColorId),
        )
    }
}

/**
 * Opacity for the bubble: lower when [MainActivity] is not in the foreground, and extra-low
 * in [ListeningState.IDLE] so the sleeping bubble blocks less of other apps / system UI.
 */
internal fun bubbleOverlayAlpha(mainActivityResumed: Boolean, state: ListeningState): Float {
    val idle = state == ListeningState.IDLE
    return when {
        mainActivityResumed && idle -> 0.5F
        mainActivityResumed && !idle -> 1F
        !mainActivityResumed && idle -> 0.25F
        else -> 0.5F
    }
}

@Preview
@Composable
fun BubbleOverlayPreview(state: ListeningState = ListeningState.AWAKE) {
    CalamariTheme {
        BubbleOverlay(
            state = state,
            mainActivityResumed = true,
            month = "FEB",
            day = "14",
        )
    }
}

