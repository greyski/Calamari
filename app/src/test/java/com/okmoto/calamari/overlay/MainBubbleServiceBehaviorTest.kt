package com.okmoto.calamari.overlay

import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class MainBubbleServiceBehaviorTest {

    @Test
    fun setListeningState_awake_startsAudioAndStopsMotion() {
        val fixture = serviceFixture()

        fixture.invokeSetListeningState(ListeningState.AWAKE)

        assertEquals(ListeningState.AWAKE, fixture.listeningStateStore.state.value)
        assertEquals(1, fixture.audioSessionManager.startCount)
        assertEquals(1, fixture.motionWakeController.stopCount)
    }

    @Test
    fun setListeningState_awaitingEvent_timeoutTransitionsBackToAwake() {
        val fixture = serviceFixture()

        fixture.invokeSetListeningState(ListeningState.AWAITING_EVENT)
        shadowOf(Looper.getMainLooper()).idleFor(AWAIT_DELAY + 100, java.util.concurrent.TimeUnit.MILLISECONDS)

        assertEquals(ListeningState.AWAKE, fixture.listeningStateStore.state.value)
        assertEquals(1, fixture.audioSessionManager.stopCount)
        assertEquals(1, fixture.audioSessionManager.startCount)
    }

    @Test
    fun onBubbleDoubleTap_whileAwake_requestsImmediateIntent_thenIdlesToSleep() {
        val fixture = serviceFixture()
        fixture.invokeSetListeningState(ListeningState.AWAKE)

        fixture.invokeOnBubbleDoubleTap()
        shadowOf(Looper.getMainLooper()).idleFor(10_100, java.util.concurrent.TimeUnit.MILLISECONDS)

        assertEquals(1, fixture.audioSessionManager.requestImmediateIntentCount)
        assertEquals(ListeningState.IDLE, fixture.listeningStateStore.state.value)
        assertEquals(1, fixture.audioSessionManager.stopCount)
    }

    private fun serviceFixture(): ServiceFixture {
        val service = MainBubbleService()
        val fixture = ServiceFixture(
            service = service,
            listeningStateStore = FakeListeningStateStore(),
            audioSessionManager = FakeAudioSessionManager(),
            motionWakeController = FakeMotionWakeController(),
            bubbleFeedbackPlayer = FakeBubbleFeedbackPlayer(),
            bubbleNotificationController = FakeBubbleNotificationController(),
            mainActivityForegroundStore = FakeMainActivityForegroundStore(),
        )

        service.setPrivateField("listeningStateStore", fixture.listeningStateStore)
        service.setPrivateField("audioSessionManager", fixture.audioSessionManager)
        service.setPrivateField("motionWakeController", fixture.motionWakeController)
        service.setPrivateField("bubbleFeedbackPlayer", fixture.bubbleFeedbackPlayer)
        service.setPrivateField("bubbleNotificationController", fixture.bubbleNotificationController)
        service.setPrivateField("mainActivityForegroundStore", fixture.mainActivityForegroundStore)

        return fixture
    }

    private data class ServiceFixture(
        val service: MainBubbleService,
        val listeningStateStore: FakeListeningStateStore,
        val audioSessionManager: FakeAudioSessionManager,
        val motionWakeController: FakeMotionWakeController,
        val bubbleFeedbackPlayer: FakeBubbleFeedbackPlayer,
        val bubbleNotificationController: FakeBubbleNotificationController,
        val mainActivityForegroundStore: FakeMainActivityForegroundStore,
    ) {
        fun invokeSetListeningState(newState: ListeningState) {
            service.callPrivate(
                name = "setListeningState",
                parameterTypes = arrayOf(ListeningState::class.java),
                args = arrayOf(newState),
            )
        }

        fun invokeOnBubbleDoubleTap() {
            service.callPrivate(name = "onBubbleDoubleTap")
        }
    }
}

private class FakeAudioSessionManager : com.okmoto.calamari.audio.AudioSessionManager {
    var startCount = 0
    var stopCount = 0
    var requestImmediateIntentCount = 0

    override fun startHotwordAndCommandSession(listener: com.okmoto.calamari.audio.CalamariAudioListener) {
        startCount++
    }

    override fun requestImmediateIntent() {
        requestImmediateIntentCount++
    }

    override fun stopSession() {
        stopCount++
    }
}

private class FakeListeningStateStore : ListeningStateStore {
    private val mutableState = MutableStateFlow(ListeningState.IDLE)
    override val state: StateFlow<ListeningState> = mutableState

    override fun setState(newState: ListeningState): Boolean {
        if (mutableState.value == newState) return false
        mutableState.value = newState
        return true
    }
}

private class FakeMainActivityForegroundStore : MainActivityForegroundStore {
    private val mutableState = MutableStateFlow(false)
    override val mainActivityResumed: StateFlow<Boolean> = mutableState

    override fun setMainActivityResumed(resumed: Boolean) {
        mutableState.value = resumed
    }
}

private class FakeMotionWakeController : MotionWakeController {
    var startCount = 0
    var stopCount = 0

    override fun start(context: android.content.Context, onMotion: () -> Unit) {
        startCount++
    }

    override fun stop() {
        stopCount++
    }
}

private class FakeBubbleFeedbackPlayer : BubbleFeedbackPlayer {
    override fun playWakeFeedback(context: android.content.Context) = Unit
    override fun playTitleCapturedFeedback(context: android.content.Context) = Unit
}

private class FakeBubbleNotificationController : BubbleNotificationController {
    override fun startInForeground(service: android.app.Service, text: String) = Unit
    override fun updateNotification(context: android.content.Context, text: String) = Unit
}

private fun Any.setPrivateField(name: String, value: Any) {
    val field = javaClass.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
}

private fun Any.callPrivate(
    name: String,
    parameterTypes: Array<Class<*>> = emptyArray(),
    args: Array<Any> = emptyArray(),
) {
    val method = javaClass.getDeclaredMethod(name, *parameterTypes)
    method.isAccessible = true
    method.invoke(this, *args)
}
