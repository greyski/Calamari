package com.okmoto.calamari.permissions.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CalendarPermissionViewModel @Inject constructor() : ViewModel() {
    val title = "Allow calendar access"
    val description =
        "Calamari needs access to read and create events in your existing calendar apps on this device."
}

