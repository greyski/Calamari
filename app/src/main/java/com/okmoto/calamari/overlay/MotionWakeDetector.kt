package com.okmoto.calamari.overlay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small helper responsible for listening to accelerometer changes and invoking
 * a callback when the device has clearly moved.
 *
 * Used by [FloatingBubbleService] to move from IDLE → AWAKE without keeping
 * Picovoice running all the time.
 */
interface MotionWakeController {
    fun start(context: Context, onMotion: () -> Unit)
    fun stop()
}

@Singleton
class MotionWakeDetector @Inject constructor() : MotionWakeController {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var listenerRegistered: Boolean = false
    private var onMotion: (() -> Unit)? = null

    private val sensorListener = object : SensorEventListener {
        private var lastMagnitude: Float? = null

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val mag = sqrt(x * x + y * y + z * z)
            val last = lastMagnitude
            lastMagnitude = mag

            last?.let {
                val delta = abs(mag - it)
                // Threshold tuned to ignore tiny jitter but detect real motion.
                if (delta > 1.5f) {
                    onMotion?.invoke()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /**
     * Begins listening for motion if not already registered. The [onMotion]
     * callback will be invoked the first time significant motion is detected.
     */
    override fun start(context: Context, onMotion: () -> Unit) {
        if (listenerRegistered) return
        val mgr = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val accel = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        sensorManager = mgr
        accelerometer = accel
        this.onMotion = {
            // Only fire once per registration to avoid duplicate wake attempts.
            stop()
            onMotion()
        }

        listenerRegistered = mgr.registerListener(
            sensorListener,
            accel,
            SensorManager.SENSOR_DELAY_NORMAL,
        )
    }

    /**
     * Stops listening for motion if currently registered.
     */
    override fun stop() {
        if (!listenerRegistered) return
        sensorManager?.unregisterListener(sensorListener)
        listenerRegistered = false
        onMotion = null
    }
}

