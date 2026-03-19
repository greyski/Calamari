package com.okmoto.calamari.permissions.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class CalendarPermissionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val title = "Allow calendar access"
    val description =
        "Calamari needs access to read and create events in your existing calendar apps on this device."
}

