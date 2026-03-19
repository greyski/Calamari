package com.okmoto.calamari.permissions.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class NotificationsPermissionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val title = "Enable notifications"
    val description =
        "Calamari uses notifications so the floating bubble can run reliably in the background " +
            "and keep you informed about listening status."
}

