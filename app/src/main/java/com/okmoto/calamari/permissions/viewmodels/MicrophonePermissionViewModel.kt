package com.okmoto.calamari.permissions.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MicrophonePermissionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val title = "Allow microphone access"
    val description =
        "Calamari needs microphone access to listen for the \"Calamari\" hot word and process " +
            "your commands."
}

