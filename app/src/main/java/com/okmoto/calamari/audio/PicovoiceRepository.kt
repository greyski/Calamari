package com.okmoto.calamari.audio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val CALAMARI_HOT_WORD_PATH = "Calamari_en_android_v4_0_0.ppn"
private const val CALAMARI_CONTEXT_PATH = "Calamari_en_android_v4_0_0.rhn"

/**
 * Singleton that owns the Picovoice audio engine session.
 * Call init(context) from Application.onCreate().
 */
object PicovoiceRepository {

    private lateinit var appContext: Context
    private var engine: CalamariAudioEngine? = null

    private val mainListenerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun startHotwordAndCommandSession(listener: CalamariAudioListener) {
        if (!::appContext.isInitialized || engine != null) return

        val config = CalamariAudioConfig(
            porcupineKeywordPaths = listOf(CALAMARI_HOT_WORD_PATH),
            rhinoContextPath = CALAMARI_CONTEXT_PATH,
            porcupineSensitivities = listOf(0.6f),
            rhinoModelPath = null,
        )

        val uiListener = object : CalamariAudioListener {
            override fun onWakeWordDetected() {
                mainListenerScope.launch { listener.onWakeWordDetected() }
            }
            override fun onIntentFound(intent: CalamariIntent) {
                mainListenerScope.launch { listener.onIntentFound(intent) }
            }
            override fun onIntentMiss() {
                mainListenerScope.launch { listener.onIntentMiss() }
            }
            override fun onError(error: Throwable) {
                mainListenerScope.launch { listener.onError(error) }
                stopSession()
            }
        }

        engine = CalamariAudioEngine(
            appContext = appContext,
            config = config,
            listener = uiListener,
        ).also { it.start(CalamariStartMode.HOTWORD_THEN_INTENT) }
    }

    fun requestImmediateIntent() {
        engine?.requestImmediateIntent()
    }

    fun stopSession() {
        engine?.stop()
        engine = null
    }
}
