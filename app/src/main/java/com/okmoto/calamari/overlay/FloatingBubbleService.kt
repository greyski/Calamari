package com.okmoto.calamari.overlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import com.okmoto.calamari.MainActivity
import com.okmoto.calamari.R
import com.okmoto.calamari.audio.AudioSessionManager
import com.okmoto.calamari.audio.CalamariAudioListener
import com.okmoto.calamari.audio.CalamariIntent
import com.okmoto.calamari.calendar.CalamariCalendarEvent
import com.okmoto.calamari.calendar.CalendarRepository
import com.okmoto.calamari.calendar.CalendarUtil
import com.okmoto.calamari.overlay.compose.BubbleOverlay
import com.okmoto.calamari.overlay.compose.EventPromptOverlay
import com.okmoto.calamari.overlay.compose.OverlayFeedbackStyle
import com.okmoto.calamari.overlay.compose.PromptUiState
import com.okmoto.calamari.ui.theme.CalamariTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@AndroidEntryPoint
class FloatingBubbleService : Service(), SimpleSpeechListener {

    private var windowManager: WindowManager? = null
    private var bubbleView: ComposeView? = null
    private var promptView: ComposeView? = null
    private var promptBelowHost: Boolean? = null
    private val overlayOwners = OverlayViewTreeOwners()

    private val bubbleParams = defaultLayoutParams()

    private val promptParams = defaultLayoutParams()

    @Inject lateinit var calendarRepository: CalendarRepository
    @Inject lateinit var audioSessionManager: AudioSessionManager
    @Inject lateinit var listeningStateStore: ListeningStateStore
    @Inject lateinit var mainActivityForegroundStore: MainActivityForegroundStore
    @Inject lateinit var motionWakeController: MotionWakeController
    @Inject lateinit var bubbleFeedbackPlayer: BubbleFeedbackPlayer
    @Inject lateinit var bubbleNotificationController: BubbleNotificationController
    @Inject lateinit var bubbleOverlayFeedbackController: BubbleOverlayFeedbackController

    private var pendingEvent: CalamariCalendarEvent? = null

    // Listener for Picovoice intent & hot word detection.
    private val calamariAudioListener: CalamariAudioListener by lazy {
        object : CalamariAudioListener {
            override fun onWakeWordDetected() {
                bubbleFeedbackPlayer.playWakeFeedback(this@FloatingBubbleService)
                setListeningState(ListeningState.AWAITING_EVENT)
            }

            override fun onIntentFound(intent: CalamariIntent) {
                handleCalendarIntent(intent)
            }

            override fun onIntentMiss() {
                audioSessionManager.stopSession()
                setListeningState(ListeningState.AWAKE)
            }

            override fun onError(error: Throwable) {
                error.printStackTrace()
                setListeningState(ListeningState.ERROR)
            }
        }
    }

    // Idle timers never run at the same time, so we can reuse a single timer.
    private val idleHandler = IdleHandler()

    private val promptUiStateFlow = MutableStateFlow<PromptUiState?>(null)

    private var recognizer: SpeechRecognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlayOwners.onCreate()
        overlayOwners.onResume()
        bubbleNotificationController.startInForeground(this, listeningStateStore.state.value.notificationText)
        addBubble()
        windowManager?.let { wm ->
            bubbleOverlayFeedbackController.setup(
                context = this,
                windowManager = wm,
                attachOverlayOwners = { view -> overlayOwners.attachTo(view) },
                bubbleParams = bubbleParams,
                bubbleViewProvider = { bubbleView },
            )
        }
        startAudioPipeline()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
            recognizer.setRecognitionListener(this)
        }
    }

    override fun onError(error: Int) {
        Log.e("onError", "$error")
        restartSpeechRecognizer()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.e("onReadyForSpeech", "onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        Log.e("onBeginningOfSpeech", "onBeginningOfSpeech")
    }

    override fun onEndOfSpeech() {
        Log.e("onEndOfSpeech", "onEndOfSpeech")
    }

    override fun onPartialResults(partialResults: Bundle) {
        val matches: ArrayList<String?>? =
            partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            Log.e("onPartialResults", matches.toString())
        }
    }

    override fun onResults(results: Bundle) {
        val matches = results.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION,
        )
        matches?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { raw ->
            val title = raw.replaceFirstChar { it.uppercase() }
            Log.e("onResults", title)
            onTitleCaptured(title)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
        bubbleOverlayFeedbackController.teardown()
        audioSessionManager.stopSession()
        motionWakeController.stop()
        overlayOwners.onDestroy()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        listeningStateStore.setState(ListeningState.IDLE)
    }

    private fun restartSpeechRecognizer() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
            recognizer.setRecognitionListener(this)
        }
        setListeningState(ListeningState.IDLE_TITLE)
        promptUiStateFlow.update { state ->
            state?.copy(
                titleMode = PromptUiState.TitleMode.TryAgain
            )
        }
        schedulePromptIdle(PromptUiState.TimerAction.DELETE) { dismissPromptWithoutSaving() }
    }

    private fun addBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleView = ComposeView(this).apply {
            overlayOwners.attachTo(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setOnTouchListener(BubbleTouchListener())
            setContent {
                val state by listeningStateStore.state.collectAsState()
                val mainActivityResumed by mainActivityForegroundStore.mainActivityResumed.collectAsState()
                val monthAndDate = CalendarUtil.getMonthAndDateToday()
                CalamariTheme {
                    BubbleOverlay(
                        state = state,
                        mainActivityResumed = mainActivityResumed,
                        month = monthAndDate.first,
                        day = monthAndDate.second,
                    )
                }
            }
            addToWindow(bubbleParams, windowManager)
        }
    }

    private fun startAudioPipeline() {
        setListeningState(ListeningState.AWAKE)
    }

    private fun handleCalendarIntent(intent: CalamariIntent) {
        // Compute event timing info from the intent and store as pending event.
        CalendarUtil.buildEventTimesFromIntent(intent)?.let { (startMillis, endMillis, allDay) ->
            pendingEvent = CalamariCalendarEvent(
                startMillis = startMillis,
                endMillis = endMillis,
                allDay = allDay,
            )
            // We have an event ready, waiting for the user to tap "Enter title".
            setListeningState(ListeningState.IDLE_TITLE)
            showEventPrompt(startMillis, allDay)
        }
    }

    private fun showEventPrompt(startMillis: Long, allDay: Boolean) {
        // Cancel any existing prompt (do not restart hot word; we're about to show a new prompt
        // and stop the session).
        hideEventPrompt(false)

        // Release the mic from Picovoice so SpeechRecognizer can use it when the user adds an
        // event name
        audioSessionManager.stopSession()

        val dayLabel = CalendarUtil.formatDayLabel(startMillis)
        val timeLabel = if (allDay) "" else CalendarUtil.formatTimeLabel(startMillis)

        promptUiStateFlow.value = PromptUiState(
            dayLabel = dayLabel,
            timeLabel = timeLabel,
            arrowPosition = PromptUiState.ArrowPosition.ABOVE_CONTENT,
            titleMode = PromptUiState.TitleMode.AddEventName,
        )

        promptView = ComposeView(this).apply {
            overlayOwners.attachTo(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            visibility = View.INVISIBLE
            setContent {
                val state by promptUiStateFlow.collectAsState()
                state?.let { s ->
                    CalamariTheme {
                        EventPromptOverlay(
                            state = s,
                            onAddTitleClicked = {
                                setListeningState(ListeningState.AWAITING_TITLE)
                            },
                            onRetryClicked = {
                                cancelPromptIdle()
                                pendingEvent = pendingEvent?.copy(title = null)
                                promptUiStateFlow.value = promptUiStateFlow.value?.copy(
                                    titleMode = PromptUiState.TitleMode.AddEventName,
                                )
                                setListeningState(ListeningState.AWAITING_TITLE)

                            },
                        )
                    }
                }
            }
            addToWindow(promptParams, windowManager)
            post {
                positionPromptRelativeToBubble(windowManager, bubbleView, this)
                visibility = View.VISIBLE
            }
        }

        // If the user doesn't tap "Add event name" within 10s, dismiss the prompt without saving
        // and restart Picovoice.
        schedulePromptIdle(PromptUiState.TimerAction.DELETE) {
            dismissPromptWithoutSaving()
        }
    }

    /** Dismisses the prompt without saving the event and restarts the Picovoice hotword session. */
    private fun dismissPromptWithoutSaving() {
        cancelPromptIdle()
        pendingEvent = null
        hideEventPrompt(true)
        setListeningState(ListeningState.AWAKE)
    }

    /**
     * Hides the event prompt and optionally restarts the Picovoice hotword session.
     * @param restartHotword true to restart listening for "Calamari" after hiding
     *  (e.g. after submit); false when tearing down or replacing the prompt.
     */
    private fun hideEventPrompt(restartHotword: Boolean) {
        cancelPromptIdle()

        val view = promptView
        if (view != null) {
            promptView = null
            view.removeFromWindow(windowManager)
        }
        promptUiStateFlow.value = null

        if (restartHotword) {
            audioSessionManager.startHotwordAndCommandSession(calamariAudioListener)
        }
    }

    private fun startTitleCapture() {
        cancelPromptIdle()
        promptUiStateFlow.update { state ->
            state?.copy(
                titleMode = PromptUiState.TitleMode.Listening
            )
        }
        recognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(
                    RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    packageName
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
            }
        )
    }

    private fun onTitleCaptured(title: String) {
        pendingEvent = pendingEvent?.copy(title = title)

        promptUiStateFlow.update { state ->
            state?.copy(
                titleMode = PromptUiState.TitleMode.Captured(title),
            )
        }

        bubbleFeedbackPlayer.playTitleCapturedFeedback(this)
        setListeningState(ListeningState.IDLE_SEND)
        scheduleAutoSubmit()
    }

    private fun scheduleAutoSubmit() {
        schedulePromptIdle(PromptUiState.TimerAction.SEND) {
            pendingEvent?.let { event ->
                calendarRepository.submitEvent(
                    event = event,
                    onSuccess = {
                        bubbleOverlayFeedbackController.show(
                            message = "Event \"${event.title}\" created!",
                            style = OverlayFeedbackStyle.SUCCESS,
                        )
                        hideEventPrompt(true)
                        setListeningState(ListeningState.AWAKE)
                    },
                    onError = {
                        bubbleOverlayFeedbackController.show(
                            message = "We ran into an issue submitting your event.",
                            style = OverlayFeedbackStyle.ERROR,
                        )
                        hideEventPrompt(true)
                        setListeningState(ListeningState.ERROR)
                    }
                )
            }
        }
    }

    private fun setListeningState(newState: ListeningState) {
        if (listeningStateStore.setState(newState).not()) return
        // Update foreground notification text for clarity.
        bubbleNotificationController.updateNotification(this, newState.notificationText)
        when (newState) {
            ListeningState.AWAKE -> {
                audioSessionManager.startHotwordAndCommandSession(calamariAudioListener)
                markListeningActivity()
                motionWakeController.stop()
            }

            ListeningState.AWAITING_EVENT -> {
                idleHandler.scheduleIdle(AWAIT_DELAY) {
                    if (listeningStateStore.state.value == ListeningState.AWAITING_EVENT) {
                        audioSessionManager.stopSession()
                        setListeningState(ListeningState.AWAKE)
                    }
                }
            }

            ListeningState.IDLE -> {
                idleHandler.cancelIdle()
                motionWakeController.start(this) {
                    if (listeningStateStore.state.value == ListeningState.IDLE) {
                        setListeningState(ListeningState.AWAKE)
                    }
                }
            }

            ListeningState.AWAITING_TITLE -> {
                startTitleCapture()
            }

            else -> {
                idleHandler.cancelIdle()
            }
        }
    }

    private fun markListeningActivity() {
        if (listeningStateStore.state.value != ListeningState.AWAKE) return
        idleHandler.scheduleIdle {
            if (listeningStateStore.state.value == ListeningState.AWAKE) {
                audioSessionManager.stopSession()
                setListeningState(ListeningState.IDLE)
            }
        }
    }

    private fun schedulePromptIdle(
        action: PromptUiState.TimerAction,
        delayMs: Long = AWAIT_DELAY,
        block: () -> Unit,
    ) {
        val startedAt = SystemClock.uptimeMillis()
        promptUiStateFlow.update { state ->
            state?.copy(
                idleTimer = PromptUiState.IdleTimer(
                    durationMs = delayMs,
                    startedAtUptimeMs = startedAt,
                    action = action,
                )
            )
        }
        idleHandler.scheduleIdle(delayMs) {
            promptUiStateFlow.update { state -> state?.copy(idleTimer = null) }
            block()
        }
    }

    private fun cancelPromptIdle() {
        idleHandler.cancelIdle()
        promptUiStateFlow.update { state -> state?.copy(idleTimer = null) }
    }

    private fun positionPromptRelativeToBubble(
        wm: WindowManager?,
        bubble: ComposeView?,
        prompt: ComposeView,
    ) {
        val metrics = resources.displayMetrics
        val screenHeight = metrics.heightPixels
        val bubbleHeight = bubble?.height ?: 0
        val bubbleWidth = bubble?.width ?: 0
        val promptWidth = prompt.measuredWidth.takeIf { it > 0 } ?: prompt.minimumWidth

        val bubbleCenterY = bubbleParams.y + bubbleHeight / 2
        val margin = (16 * resources.displayMetrics.density).toInt()

        // Center prompt horizontally over the bubble.
        promptParams.x = bubbleParams.x + bubbleWidth / 2 - promptWidth / 2
        if (promptParams.x < 0) promptParams.x = 0

        // Decide once whether the prompt is shown below or above the host,
        // and keep that relationship stable for the life of this prompt.
        promptBelowHost = bubbleCenterY < screenHeight / 2

        when (promptBelowHost) {
            true -> {
                // Prompt is below the host bubble; arrow on top.
                promptParams.y = bubbleParams.y + bubbleHeight + margin
                promptUiStateFlow.update { state ->
                    state?.copy(arrowPosition = PromptUiState.ArrowPosition.ABOVE_CONTENT)
                }
            }

            false -> {
                // Prompt is above the host bubble; arrow on bottom.
                promptParams.y = bubbleParams.y - prompt.measuredHeight - margin
                promptUiStateFlow.update { state ->
                    state?.copy(arrowPosition = PromptUiState.ArrowPosition.BELOW_CONTENT)
                }
            }

            else -> Unit
        }

        try {
            wm?.updateViewLayout(prompt, promptParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeBubble() {
        bubbleOverlayFeedbackController.hide()
        hideEventPrompt(false)
        val view = bubbleView
        if (view != null) {
            bubbleView = null
            view.removeFromWindow(windowManager)
        }
    }

    /**
     * Invoked when the user double-taps the bubble.
     * - In [ListeningState.AWAKE], request immediate intent processing.
     * - In [ListeningState.ERROR], bring the user to [MainActivity] and recover to AWAKE.
     */
    private fun onBubbleDoubleTap() {
        when (listeningStateStore.state.value) {
            ListeningState.AWAKE -> {
                audioSessionManager.requestImmediateIntent()
                idleHandler.scheduleIdle {
                    if (listeningStateStore.state.value == ListeningState.AWAKE) {
                        audioSessionManager.stopSession()
                        setListeningState(ListeningState.IDLE)
                    }
                }
            }
            ListeningState.ERROR -> {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                )
                setListeningState(ListeningState.AWAKE)
            }
            else -> Unit
        }
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        private var initialX: Int = 0
        private var initialY: Int = 0
        private var initialTouchX: Float = 0f
        private var initialTouchY: Float = 0f
        private var lastTapTimeMs: Long = 0L
        private val doubleTapTimeoutMs: Long = 300L

        // Distance threshold to consider a touch a "tap" vs drag.
        private val tapSlopPx: Float =
            16f * resources.displayMetrics.density

        @SuppressLint("ClickableViewAccessibility") // Not clickable
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            // If the view is no longer attached, ignore further touch events to
            // avoid IllegalArgumentException from WindowManager.
            if (v.windowToken == null) return false

            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (listeningStateStore.state.value == ListeningState.IDLE) {
                        // User is interacting with the bubble; wake the listener.
                        setListeningState(ListeningState.AWAKE)
                    }
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()

                    // If a prompt is visible, clamp vertical movement so that
                    // the prompt cannot be dragged off-screen while staying
                    // attached above/below the host bubble.
                    promptView?.let { prompt ->
                        val promptHeight = prompt.height
                        bubbleView?.let { bubble ->
                            val bubbleHeight = bubble.height
                            val metrics = resources.displayMetrics
                            val screenHeight = metrics.heightPixels
                            val margin = (16 * resources.displayMetrics.density).toInt()
                            when (promptBelowHost) {
                                true -> {
                                    // Prompt is below the bubble; clamp so its
                                    // bottom never goes past the bottom of the screen.
                                    val maxBubbleY =
                                        screenHeight - promptHeight - bubbleHeight - margin
                                    if (bubbleParams.y > maxBubbleY) {
                                        bubbleParams.y = maxBubbleY
                                    }
                                }

                                false -> {
                                    // Prompt is above the bubble; clamp so its
                                    // top never goes above the top of the screen.
                                    val minBubbleY = promptHeight + margin
                                    if (bubbleParams.y < minBubbleY) {
                                        bubbleParams.y = minBubbleY
                                    }
                                }

                                else -> Unit
                            }
                        }
                    }
                    try {
                        bubbleView?.let { bubble ->
                            windowManager?.let { wm ->
                                wm.updateViewLayout(bubble, bubbleParams)
                                promptView?.let { prompt ->
                                    positionPromptRelativeToBubble(
                                        wm,
                                        bubble,
                                        prompt,
                                    )
                                }
                                bubbleOverlayFeedbackController.onAnchorMoved()
                            }
                        }
                        true
                    } catch (e: Exception) {
                        // View is not attached to window manager anymore; ignore.
                        e.printStackTrace()
                        false
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val distanceSq = dx * dx + dy * dy
                    val isTap = distanceSq <= tapSlopPx * tapSlopPx

                    val currentState = listeningStateStore.state.value
                    if (isTap && (currentState == ListeningState.AWAKE || currentState == ListeningState.ERROR)) {
                        val now = SystemClock.uptimeMillis()
                        if (now - lastTapTimeMs <= doubleTapTimeoutMs) {
                            // Detected a double-tap while active/error.
                            onBubbleDoubleTap()
                            lastTapTimeMs = 0L
                        } else {
                            lastTapTimeMs = now
                        }
                    }
                    false
                }

                else -> false
            }
        }
    }

    private fun defaultLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        // Start in the dead center of the screen.
        gravity = Gravity.CENTER
    }

}
