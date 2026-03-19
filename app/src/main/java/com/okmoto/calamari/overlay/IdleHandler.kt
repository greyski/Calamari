/**
 * Idle/timer scheduling helper for overlay timeouts.
 *
 * Methodology:
 * - Wraps a single main-thread `Handler` runnable slot.
 * - Ensures timeout transitions are cancellable and only the latest idle
 *   callback can fire.
 */
package com.okmoto.calamari.overlay

import android.os.Handler
import android.os.Looper

const val AWAIT_DELAY = 5_000L

/**
 * Convenience wrapper to consolidate both the Handler and Runnable APIs used for idle timers.
 */
class IdleHandler : Handler(Looper.getMainLooper()) {

    private var idleRunnable: Runnable? = null

    fun scheduleIdle(delayMillis: Long = AWAIT_DELAY, block: () -> Unit = {}) {
        cancelIdle()
        val runnable = Runnable {
            idleRunnable = null
            block()
        }
        idleRunnable = runnable
        postDelayed(runnable, delayMillis)
    }

    fun cancelIdle() {
        idleRunnable?.let { removeCallbacks(it) }
        idleRunnable = null
    }

}