/**
 * Implementation of the on-device audio pipeline using Picovoice.
 *
 * Methodology:
 * - Captures microphone audio on a background coroutine.
 * - Optionally runs hotword detection before streaming frames to intent inference.
 * - Reports results through [CalamariAudioListener] callbacks.
 */
package com.okmoto.calamari.audio

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.rhino.Rhino
import ai.picovoice.rhino.RhinoInference
import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.okmoto.calamari.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class CalamariIntentResult(val intent: CalamariIntent?) {
    constructor(inference: RhinoInference) : this(
        intent = inference.intent?.let { type ->
            CalamariIntent.create(
                type = type,
                slots = inference.slots.orEmpty(),
            )
        },
    )
}

enum class CalamariStartMode {
    HOTWORD_THEN_INTENT,
    INTENT_ONLY,
}

interface CalamariAudioListener {
    fun onWakeWordDetected()
    fun onIntentFound(intent: CalamariIntent)
    fun onIntentMiss()
    fun onError(error: Throwable)
}

/**
 * On-device audio pipeline using Picovoice (Porcupine + Rhino).
 * Captures microphone audio, optionally listens for hotword, then streams
 * frames into Rhino for speech-to-intent. All work is done off the main thread.
 */
class CalamariAudioEngine(
    private val appContext: Context,
    private val config: CalamariAudioConfig,
    private val listener: CalamariAudioListener,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var rhino: Rhino? = null
    private var porcupine: Porcupine? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private var startMode: CalamariStartMode = CalamariStartMode.INTENT_ONLY

    @Volatile
    private var forceNextIntent = false

    fun start(mode: CalamariStartMode) {
        if (recordingJob != null) return
        startMode = mode

        recordingJob = scope.launch {
            try {
                initEngines()
                coroutineContext[Job]?.let { job ->
                    runLoop(job)
                }
            } catch (s: SecurityException) {
                listener.onError(s)
            } catch (t: Throwable) {
                listener.onError(t)
            } finally {
                releaseEngines()
            }
        }
    }

    fun requestImmediateIntent() {
        forceNextIntent = true
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
    }

    private fun initEngines() {
        val accessKey = BuildConfig.PICOVOICE_ACCESS_KEY

        if (config.porcupineKeywordPaths.isNotEmpty()) {
            porcupine = Porcupine.Builder()
                .setAccessKey(accessKey)
                .setKeywordPaths(config.porcupineKeywordPaths.toTypedArray())
                .apply {
                    if (config.porcupineSensitivities.isNotEmpty()) {
                        setSensitivities(config.porcupineSensitivities.toFloatArray())
                    }
                }
                .build(appContext)
        }

        rhino = Rhino.Builder()
            .setAccessKey(accessKey)
            .setContextPath(config.rhinoContextPath)
            .apply {
                config.rhinoModelPath?.let { setModelPath(it) }
            }
            .build(appContext)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun runLoop(job: Job) {
        val rhinoLocal = rhino ?: return
        val porcupineLocal = porcupine ?: return

        val hasHotword = startMode == CalamariStartMode.HOTWORD_THEN_INTENT

        val frameLength = when {
            hasHotword -> porcupineLocal.frameLength
            else -> rhinoLocal.frameLength
        }
        val sampleRate = when {
            hasHotword -> porcupineLocal.sampleRate
            else -> rhinoLocal.sampleRate
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = minBufferSize.coerceAtLeast(frameLength * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        ).also { it.startRecording() }

        val frameBuffer = ShortArray(frameLength)
        var inIntentPhase = !hasHotword

        while (job.isActive) {
            val read = audioRecord?.read(frameBuffer, 0, frameLength) ?: break
            if (read <= 0) continue

            if (!inIntentPhase) {
                if (forceNextIntent) {
                    listener.onWakeWordDetected()
                    inIntentPhase = true
                } else {
                    val keywordIndex = porcupineLocal.process(frameBuffer)
                    if (keywordIndex >= 0) {
                        listener.onWakeWordDetected()
                        inIntentPhase = true
                    }
                    continue
                }
            }
            val isFinalized = rhinoLocal.process(frameBuffer)
            if (isFinalized) {
                val inference = rhinoLocal.inference
                val result = CalamariIntentResult(inference)
                result.intent?.let {
                    listener.onIntentFound(it)
                } ?: run {
                    listener.onIntentMiss()
                }

                if (startMode == CalamariStartMode.HOTWORD_THEN_INTENT && hasHotword) {
                    try {
                        rhinoLocal.reset()
                    } catch (e: Throwable) {
                        listener.onError(e)
                    }
                    inIntentPhase = false
                    forceNextIntent = false
                } else {
                    break
                }
            }
        }
    }

    private fun releaseEngines() {
        try {
            audioRecord?.stop()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        audioRecord?.release()
        audioRecord = null

        rhino?.delete()
        rhino = null

        porcupine?.delete()
        porcupine = null
    }
}
