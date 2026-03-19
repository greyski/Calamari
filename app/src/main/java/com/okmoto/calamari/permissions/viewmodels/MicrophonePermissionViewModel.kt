package com.okmoto.calamari.permissions.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MicrophonePermissionViewModel @Inject constructor() : ViewModel() {
    val title = "Allow microphone access"
    val description =
        "Calamari needs microphone access to listen for the \"Calamari\" hot word and process " +
            "your commands."
}

