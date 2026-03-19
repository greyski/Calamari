package com.okmoto.calamari.overlay.compose

import com.okmoto.calamari.overlay.compose.base.BaseSnapshotTest
import org.junit.Test

class EventPromptOverlaySnapshotTest : BaseSnapshotTest() {

    @Test
    fun eventPromptOverlay_addEventName() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
                    titleMode = PromptUiState.TitleMode.AddEventName,
                )
            )
        }
    }

    @Test
    fun eventPromptOverlay_listening() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
                    titleMode = PromptUiState.TitleMode.Listening,
                )
            )
        }
    }

    @Test
    fun eventPromptOverlay_tryAgain() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
                    titleMode = PromptUiState.TitleMode.TryAgain,
                )
            )
        }
    }

    @Test
    fun eventPromptOverlay_captured() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
                    titleMode = PromptUiState.TitleMode.Captured("Weekly planning"),
                )
            )
        }
    }

    @Test
    fun eventPromptOverlay_arrowAboveContent() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.ABOVE_CONTENT,
                    titleMode = PromptUiState.TitleMode.AddEventName,
                )
            )
        }
    }

    @Test
    fun eventPromptOverlay_timerSend() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
                    titleMode = PromptUiState.TitleMode.Captured("Weekly planning"),
                    idleTimer = PromptUiState.IdleTimer(
                        durationMs = 1,
                        startedAtUptimeMs = 0,
                        action = PromptUiState.TimerAction.SEND,
                    ),
                )
            )
        }
    }

    @Test
    fun eventPromptOverlay_timerDelete() {
        captureImage {
            EventPromptOverlayPreview(
                state = PromptUiState(
                    dayLabel = "Friday February 14th",
                    timeLabel = "12:30pm",
                    arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT,
                    titleMode = PromptUiState.TitleMode.TryAgain,
                    idleTimer = PromptUiState.IdleTimer(
                        durationMs = 1,
                        startedAtUptimeMs = 0,
                        action = PromptUiState.TimerAction.DELETE,
                    ),
                )
            )
        }
    }
}
