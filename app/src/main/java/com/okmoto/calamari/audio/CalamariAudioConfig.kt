/**
 * Audio configuration types for the on-device Calamari speech pipeline.
 *
 * This file is a lightweight container for keyword/model paths and sensitivities so the
 * underlying engine can be configured without hard-coding those values in runtime logic.
 */
package com.okmoto.calamari.audio

/**
 * Configuration for Calamari's on-device audio pipeline.
 *
 * Hot words and language models are provided via asset or file paths.
 * No phrases or keywords are hard-coded here.
 */
data class CalamariAudioConfig(
    val porcupineKeywordPaths: List<String>,
    val rhinoContextPath: String,
    val porcupineSensitivities: List<Float> = emptyList(),
    val rhinoModelPath: String? = null,
)

