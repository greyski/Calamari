package com.okmoto.calamari.overlay.compose

import com.okmoto.calamari.overlay.ListeningState
import com.okmoto.calamari.overlay.compose.base.BaseSnapshotTest
import org.junit.Ignore
import org.junit.Test

class BubbleOverlaySnapshotTest : BaseSnapshotTest() {

    @Test
    fun bubbleOverlay_idle() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.IDLE)
        }
    }

    @Ignore("Roborazzi does not support screenshots when animations exist in compose")
    @Test
    fun bubbleOverlay_awake() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.AWAKE)
        }
    }

    @Ignore("Roborazzi does not support screenshots when animations exist in compose")
    @Test
    fun bubbleOverlay_awaitingEvent() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.AWAITING_EVENT)
        }
    }

    @Ignore("Roborazzi does not support screenshots when animations exist in compose")
    @Test
    fun bubbleOverlay_idleTitle() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.IDLE_TITLE)
        }
    }

    @Ignore("Roborazzi does not support screenshots when animations exist in compose")
    @Test
    fun bubbleOverlay_awaitingTitle() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.AWAITING_TITLE)
        }
    }

    @Ignore("Roborazzi does not support screenshots when animations exist in compose")
    @Test
    fun bubbleOverlay_idleSend() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.IDLE_SEND)
        }
    }

    @Ignore("Roborazzi does not support screenshots when animations exist in compose")
    @Test
    fun bubbleOverlay_error() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.ERROR)
        }
    }
}
