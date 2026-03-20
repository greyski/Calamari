/**
 * ViewModel providing UI copy and system intent wiring for overlay permission.
 *
 * Methodology:
 * - Generates an intent pointing to the system overlay permission page for this app.
 * - Keeps screen composables free from intent construction details.
 */
package com.okmoto.calamari.permissions.viewmodels

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.content.Context

@HiltViewModel
class OverlayPermissionViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    val title = "Display over other apps"
    val description =
        "Calamari uses a small floating bubble that appears over other apps so you can trigger voice commands from anywhere."

    fun buildOverlaySettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${appContext.packageName}".toUri(),
        )
    }
}

