package com.okmoto.calamari.overlay

import android.content.Context
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.okmoto.calamari.overlay.compose.OverlayFeedbackPill
import com.okmoto.calamari.overlay.compose.OverlayFeedbackStyle
import com.okmoto.calamari.overlay.compose.OverlayFeedbackUiState
import com.okmoto.calamari.ui.theme.CalamariTheme
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface BubbleOverlayFeedbackController {
    fun setup(
        context: Context,
        windowManager: WindowManager,
        attachOverlayOwners: (ComposeView) -> Unit,
        bubbleParams: WindowManager.LayoutParams,
        bubbleViewProvider: () -> ComposeView?,
    )

    fun show(
        message: String,
        style: OverlayFeedbackStyle,
        durationMs: Long = 2_000L,
    )

    fun hide()
    fun onAnchorMoved()
    fun teardown()
}

@Singleton
class BubbleOverlayFeedbackManager @Inject constructor() : BubbleOverlayFeedbackController {
    private var context: Context? = null
    private var windowManager: WindowManager? = null
    private var attachOverlayOwners: ((ComposeView) -> Unit)? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleViewProvider: (() -> ComposeView?)? = null

    private var feedbackView: ComposeView? = null
    private val feedbackParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        android.graphics.PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = android.view.Gravity.CENTER
    }
    private val feedbackUiStateFlow = MutableStateFlow<OverlayFeedbackUiState?>(null)
    private val feedbackHandler = IdleHandler()

    override fun setup(
        context: Context,
        windowManager: WindowManager,
        attachOverlayOwners: (ComposeView) -> Unit,
        bubbleParams: WindowManager.LayoutParams,
        bubbleViewProvider: () -> ComposeView?,
    ) {
        this.context = context
        this.windowManager = windowManager
        this.attachOverlayOwners = attachOverlayOwners
        this.bubbleParams = bubbleParams
        this.bubbleViewProvider = bubbleViewProvider
    }

    override fun show(message: String, style: OverlayFeedbackStyle, durationMs: Long) {
        hide()
        val currentContext = context ?: return
        val currentWindowManager = windowManager ?: return
        val currentAttachOverlayOwners = attachOverlayOwners ?: return
        feedbackUiStateFlow.value = OverlayFeedbackUiState(message = message, style = style)
        feedbackView = ComposeView(currentContext).apply {
            currentAttachOverlayOwners(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            visibility = View.INVISIBLE
            setContent {
                val state by feedbackUiStateFlow.collectAsState()
                state?.let { s ->
                    CalamariTheme {
                        OverlayFeedbackPill(state = s)
                    }
                }
            }
            addToWindow(feedbackParams, currentWindowManager)
            post {
                positionFeedback()
                visibility = View.VISIBLE
            }
        }
        feedbackHandler.scheduleIdle(durationMs) { hide() }
    }

    override fun hide() {
        feedbackHandler.cancelIdle()
        feedbackView?.removeFromWindow(windowManager)
        feedbackView = null
        feedbackUiStateFlow.value = null
    }

    override fun onAnchorMoved() {
        positionFeedback()
    }

    override fun teardown() {
        hide()
        context = null
        windowManager = null
        attachOverlayOwners = null
        bubbleParams = null
        bubbleViewProvider = null
    }

    private fun positionFeedback() {
        val wm = windowManager ?: return
        val view = feedbackView ?: return
        val params = bubbleParams ?: return
        val bubble = bubbleViewProvider?.invoke()
        val bubbleWidth = bubble?.width ?: 0
        val bubbleHeight = bubble?.height ?: 0
        val feedbackWidth = view.measuredWidth.takeIf { it > 0 } ?: view.minimumWidth
        val density = context?.resources?.displayMetrics?.density ?: return
        val margin = (16 * density).toInt()
        feedbackParams.x = params.x + bubbleWidth / 2 - feedbackWidth / 2
        feedbackParams.y = params.y - bubbleHeight - margin
        try {
            wm.updateViewLayout(view, feedbackParams)
        } catch (_: Exception) {
            // Ignore if the view is detached while dismissing.
        }
    }
}
