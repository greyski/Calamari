/**
 * Placeholder ViewModel for the Home screen.
 *
 * Today Home state is driven directly by repositories/stores (e.g. `ListeningStateStore`)
 * rather than Home-specific state, but keeping a ViewModel boundary makes it easier
 * to evolve the Home screen without inflating `HomeScreen`.
 */
package com.okmoto.calamari.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel()
