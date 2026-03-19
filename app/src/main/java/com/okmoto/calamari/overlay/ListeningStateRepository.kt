/**
 * In-memory store for the current overlay [ListeningState].
 *
 * Methodology:
 * - Backed by a `MutableStateFlow` so services and Compose UIs can observe changes.
 * - `setState(...)` returns whether the state actually changed, allowing callers
 *   to avoid redundant work.
 */
package com.okmoto.calamari.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the current floating bubble listening state.
 *
 * Both the overlay service and the UI can observe this.
 */
interface ListeningStateStore {
    val state: StateFlow<ListeningState>
    fun setState(newState: ListeningState): Boolean
}

@Singleton
class ListeningStateRepository @Inject constructor() : ListeningStateStore {
    private val mutableState = MutableStateFlow(ListeningState.IDLE)
    override val state: StateFlow<ListeningState> = mutableState.asStateFlow()

    override fun setState(newState: ListeningState): Boolean {
        if (mutableState.value == newState) return false
        mutableState.value = newState
        return true
    }
}

