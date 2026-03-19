/**
 * Repository tracking whether `MainActivity` is currently resumed.
 *
 * Methodology:
 * - Allows overlay Compose UI to adapt behavior when the foreground activity is visible.
 * - Uses a `StateFlow<Boolean>` so both services and Compose can observe it.
 */
package com.okmoto.calamari.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether [com.okmoto.calamari.MainActivity] is in the resumed (foreground) lifecycle state.
 *
 * Updated from the activity so the floating bubble can lower opacity when the user is in
 * another app or system UI, without blocking their view as much.
 */
interface MainActivityForegroundStore {
    val mainActivityResumed: StateFlow<Boolean>
    fun setMainActivityResumed(resumed: Boolean)
}

@Singleton
class MainActivityForegroundRepository @Inject constructor() : MainActivityForegroundStore {
    private val mutableMainActivityResumed = MutableStateFlow(false)
    override val mainActivityResumed: StateFlow<Boolean> = mutableMainActivityResumed.asStateFlow()

    override fun setMainActivityResumed(resumed: Boolean) {
        if (mutableMainActivityResumed.value == resumed) return
        mutableMainActivityResumed.value = resumed
    }
}
