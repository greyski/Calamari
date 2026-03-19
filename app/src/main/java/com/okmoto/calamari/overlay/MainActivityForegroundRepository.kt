package com.okmoto.calamari.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether [com.okmoto.calamari.MainActivity] is in the resumed (foreground) lifecycle state.
 *
 * Updated from the activity so the floating bubble can lower opacity when the user is in
 * another app or system UI, without blocking their view as much.
 */
object MainActivityForegroundRepository {

    private val _mainActivityResumed = MutableStateFlow(false)
    val mainActivityResumed: StateFlow<Boolean> = _mainActivityResumed.asStateFlow()

    fun setMainActivityResumed(resumed: Boolean) {
        if (_mainActivityResumed.value == resumed) return
        _mainActivityResumed.value = resumed
    }
}
