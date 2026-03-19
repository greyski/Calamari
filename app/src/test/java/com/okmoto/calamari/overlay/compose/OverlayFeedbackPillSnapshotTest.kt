package com.okmoto.calamari.overlay.compose

import com.okmoto.calamari.overlay.compose.base.BaseSnapshotTest
import org.junit.Test

class OverlayFeedbackPillSnapshotTest : BaseSnapshotTest() {

    @Test
    fun overlayFeedbackPill_success() {
        captureImage {
            OverlayFeedbackPillPreview(
                state = OverlayFeedbackUiState(
                    message = "Event \"Weekly planning\" created.",
                    style = OverlayFeedbackStyle.SUCCESS,
                )
            )
        }
    }

    @Test
    fun overlayFeedbackPill_error() {
        captureImage {
            OverlayFeedbackPillPreview(
                state = OverlayFeedbackUiState(
                    message = "Unable to create event. Try again.",
                    style = OverlayFeedbackStyle.ERROR,
                )
            )
        }
    }

    @Test
    fun overlayFeedbackPill_info() {
        captureImage {
            OverlayFeedbackPillPreview(
                state = OverlayFeedbackUiState(
                    message = "Listening for commands.",
                    style = OverlayFeedbackStyle.INFO,
                )
            )
        }
    }
}
