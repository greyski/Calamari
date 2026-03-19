package com.okmoto.calamari.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.okmoto.calamari.overlay.compose.OverlayFeedbackPill
import com.okmoto.calamari.overlay.compose.OverlayFeedbackStyle
import com.okmoto.calamari.overlay.compose.OverlayFeedbackUiState
import com.okmoto.calamari.ui.theme.CalamariTheme
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_DELAY = 5_000L

interface BubbleOverlayFeedbackController {
    fun setup(
        context: Context,
        windowManager: WindowManager,
    )

    fun show(
        message: String,
        style: OverlayFeedbackStyle,
        owners: OverlayViewTreeOwners,
        durationMs: Long = DEFAULT_DELAY,
    )

    fun hide()
    fun teardown()
}

@Singleton
class BubbleOverlayFeedbackManager @Inject constructor() : BubbleOverlayFeedbackController {
    private var context: Context? = null
    private var windowManager: WindowManager? = null
    private var feedbackView: ComposeView? = null
    private val feedbackParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.CENTER or Gravity.BOTTOM
    }
    private val feedbackUiStateFlow = MutableStateFlow<OverlayFeedbackUiState?>(null)
    private val feedbackHandler = IdleHandler()

    override fun setup(
        context: Context,
        windowManager: WindowManager,
    ) {
        this.context = context
        this.windowManager = windowManager
    }

    override fun show(
        message: String,
        style: OverlayFeedbackStyle,
        owners: OverlayViewTreeOwners,
        durationMs: Long
    ) {
        hide()
        val currentContext = context ?: return
        val currentWindowManager = windowManager ?: return
        feedbackUiStateFlow.value = OverlayFeedbackUiState(message = message, style = style)
        feedbackView = owners.createComposeView(currentContext).apply {
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

    override fun teardown() {
        hide()
        context = null
        windowManager = null
    }
}
