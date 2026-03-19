/**
 * Permission model and helper functions for Calamari.
 *
 * Methodology:
 * - Encapsulates the mapping between Calamari needs (overlay, audio, calendar) and
 *   Android permission strings.
 * - Provides utilities to decide whether each permission is required on this device
 *   and whether it is already granted.
 */
package com.okmoto.calamari.core

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

enum class CalamariPermission(val androidPermission: String?) {
    POST_NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS),
    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO),
    READ_CALENDAR(Manifest.permission.READ_CALENDAR),
    WRITE_CALENDAR(Manifest.permission.WRITE_CALENDAR),
    // SYSTEM_ALERT_WINDOW and foreground service are special-cased intents / declarations
    OVERLAY(null),
}

fun CalamariPermission.isRequiredOnThisDevice(): Boolean {
    return when (this) {
        CalamariPermission.POST_NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        else -> true
    }
}

fun CalamariPermission.isGranted(context: Context): Boolean {
    return when (this) {
        CalamariPermission.OVERLAY -> Settings.canDrawOverlays(context)
        CalamariPermission.POST_NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
            else ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        else -> {
            val perm = androidPermission ?: return true
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * Permissions required for the user to reach the main Home screen.
 *
 * Order matters: we navigate to the first missing permission screen.
 */
val REQUIRED_PERMISSIONS_FOR_HOME: List<CalamariPermission> = listOf(
    CalamariPermission.POST_NOTIFICATIONS,
    CalamariPermission.RECORD_AUDIO,
    CalamariPermission.READ_CALENDAR,
    CalamariPermission.WRITE_CALENDAR,
    CalamariPermission.OVERLAY,
).filter { it.isRequiredOnThisDevice() }

