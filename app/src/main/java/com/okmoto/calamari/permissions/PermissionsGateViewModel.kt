package com.okmoto.calamari.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.okmoto.calamari.core.REQUIRED_PERMISSIONS_FOR_HOME
import com.okmoto.calamari.core.isGranted

class PermissionsGateViewModel(
    application: Application,
) : AndroidViewModel(application) {

    fun firstMissingPermission(): com.okmoto.calamari.core.CalamariPermission? {
        val context = getApplication<Application>().applicationContext
        return REQUIRED_PERMISSIONS_FOR_HOME.firstOrNull { !it.isGranted(context) }
    }
}

