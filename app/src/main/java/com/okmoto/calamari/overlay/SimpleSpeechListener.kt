package com.okmoto.calamari.overlay

import android.os.Bundle
import android.speech.RecognitionListener
import android.util.Log

/**
 * Convenience class to hide away unused methods in [RecognitionListener].
 */
interface SimpleSpeechListener : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}