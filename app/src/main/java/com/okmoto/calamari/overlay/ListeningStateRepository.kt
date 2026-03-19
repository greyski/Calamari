package com.okmoto.calamari.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the current floating bubble listening state.
 *
 * Both the overlay service and the UI can observe this.
 */
object ListeningStateRepository {
    private val _state = MutableStateFlow(ListeningState.IDLE)
    val state: StateFlow<ListeningState> = _state.asStateFlow()

    fun setState(newState: ListeningState): Boolean {
        if (_state.value == newState) return false
        _state.value = newState
        return true
    }
}

