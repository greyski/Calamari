package com.okmoto.calamari.overlay.compose

import androidx.annotation.ColorRes
import androidx.compose.runtime.Immutable

@Immutable
data class PromptUiState(
    val dayLabel: String,
    val timeLabel: String,
    val arrowPosition: ArrowPosition,
    val titleMode: TitleMode,
    val idleTimer: IdleTimer? = null,
) {
    @Immutable
    data class IdleTimer(
        val durationMs: Long,
        val startedAtUptimeMs: Long,
        val action: TimerAction,
    )

    enum class TimerAction {
        DELETE,
        SEND
    }

    enum class ArrowPosition {
        ABOVE_CONTENT,
        BELOW_CONTENT,
    }

    sealed class TitleMode(val header: String) {
        data object AddEventName : TitleMode("Add event name")

        data object Listening : TitleMode("Listening...")

        data object TryAgain : TitleMode("Try adding again")
        data class Captured(val title: String) : TitleMode(title)
    }
}

