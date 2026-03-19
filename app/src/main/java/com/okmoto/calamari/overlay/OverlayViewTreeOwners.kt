package com.okmoto.calamari.overlay

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * ComposeView overlays attached via WindowManager don't have an Activity view tree,
 * so we provide minimal owners for Compose's lifecycle-aware recomposer.
 */
class OverlayViewTreeOwners : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    fun createComposeView(context: Context): ComposeView {
        return ComposeView(context).also { view ->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
            view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
    }
}