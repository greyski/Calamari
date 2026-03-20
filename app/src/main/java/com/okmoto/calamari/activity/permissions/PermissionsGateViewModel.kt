/**
 * Permission gate logic for Calamari onboarding/navigation.
 *
 * Methodology:
 * - Centralizes “which permissions are required for Home” by iterating over
 *   [com.okmoto.calamari.core.REQUIRED_PERMISSIONS_FOR_HOME].
 * - Exposes the first missing permission so `MainActivity` can route deterministically.
 */
package com.okmoto.calamari.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import com.okmoto.calamari.core.CalamariPermission
import com.okmoto.calamari.core.REQUIRED_PERMISSIONS_FOR_HOME
import com.okmoto.calamari.core.isGranted
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class PermissionsGateViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    fun firstMissingPermission(): CalamariPermission? {
        return REQUIRED_PERMISSIONS_FOR_HOME.firstOrNull { !it.isGranted(appContext) }
    }
}

