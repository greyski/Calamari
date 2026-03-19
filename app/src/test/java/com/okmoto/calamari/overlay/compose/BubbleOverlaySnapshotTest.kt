package com.okmoto.calamari.overlay.compose

import com.okmoto.calamari.overlay.ListeningState
import com.okmoto.calamari.overlay.compose.base.BaseSnapshotTest
import org.junit.Test

class BubbleOverlaySnapshotTest : BaseSnapshotTest() {

    @Test
    fun bubbleOverlay_idle() {
        captureImage {
            BubbleOverlayPreview(state = ListeningState.IDLE)
        }
    }

//    @Test // TODO Roborazzi does not support screenshots when animations exist in compose
//    fun bubbleOverlay_awake() {
//        captureImage {
//            BubbleOverlayPreview(state = ListeningState.AWAKE)
//        }
//    }
//
//    @Test
//    fun bubbleOverlay_awaitingEvent() {
//        captureImage {
//            BubbleOverlayPreview(state = ListeningState.AWAITING_EVENT)
//        }
//    }
//
//    @Test
//    fun bubbleOverlay_idleTitle() {
//        captureImage {
//            BubbleOverlayPreview(state = ListeningState.IDLE_TITLE)
//        }
//    }
//
//    @Test
//    fun bubbleOverlay_awaitingTitle() {
//        captureImage {
//            BubbleOverlayPreview(state = ListeningState.AWAITING_TITLE)
//        }
//    }
//
//    @Test
//    fun bubbleOverlay_idleSend() {
//        captureImage {
//            BubbleOverlayPreview(state = ListeningState.IDLE_SEND)
//        }
//    }
//
//    @Test
//    fun bubbleOverlay_error() {
//        captureImage {
//            BubbleOverlayPreview(state = ListeningState.ERROR)
//        }
//    }
}
