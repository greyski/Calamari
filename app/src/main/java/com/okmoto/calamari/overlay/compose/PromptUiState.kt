/**
 * Immutable UI model for the event prompt overlay.
 *
 * Methodology:
 * - Encapsulates all text/visual state needed by `EventPromptOverlay` in one data object.
 * - Keeps the idle timeout as data (`IdleTimer`) so Compose can render progress deterministically
 *   while the service controls the actual timer scheduling/cancellation.
 */
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

