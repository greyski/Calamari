/**
 * Audio session repository backed by Picovoice (hotword + intent engine).
 *
 * Methodology:
 * - Owns the lifecycle of [CalamariAudioEngine] and prevents duplicate initialization.
 * - Dispatches listener callbacks onto the main thread so services/UI state updates can
 *   happen without extra threading coordination.
 * - Exposes a small interface ([AudioSessionManager]) so the overlay service can treat
 *   audio processing as a black box.
 */
package com.okmoto.calamari.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val CALAMARI_HOT_WORD_PATH = "Calamari_en_android_v4_0_0.ppn"
private const val CALAMARI_CONTEXT_PATH = "Calamari_en_android_v4_0_0.rhn"

interface AudioSessionManager {
    fun startHotwordAndCommandSession(listener: CalamariAudioListener)
    fun requestImmediateIntent()
    fun stopSession()
}

@Singleton
class PicovoiceRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : AudioSessionManager {
    private var engine: CalamariAudioEngine? = null

    private val mainListenerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    override fun startHotwordAndCommandSession(listener: CalamariAudioListener) {
        if (engine != null) return

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

    override fun requestImmediateIntent() {
        engine?.requestImmediateIntent()
    }

    override fun stopSession() {
        engine?.stop()
        engine = null
    }
}
