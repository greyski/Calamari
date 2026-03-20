/**
 * ViewModel providing UI copy for the notifications permission request screen.
 */
package com.okmoto.calamari.permissions.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationsPermissionViewModel @Inject constructor() : ViewModel() {
    val title = "Enable notifications"
    val description =
        "Calamari uses notifications so the floating bubble can run reliably in the background " +
            "and keep you informed about listening status."
}

