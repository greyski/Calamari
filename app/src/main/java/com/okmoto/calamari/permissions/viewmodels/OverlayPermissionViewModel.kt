package com.okmoto.calamari.permissions.viewmodels

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel

class OverlayPermissionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val title = "Display over other apps"
    val description =
        "Calamari uses a small floating bubble that appears over other apps so you can trigger voice commands from anywhere."

    fun buildOverlaySettingsIntent(): Intent {
        val context = getApplication<Application>().applicationContext
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri(),
        )
    }
}

