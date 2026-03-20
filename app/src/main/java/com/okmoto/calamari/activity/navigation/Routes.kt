/**
 * Navigation route constants and permission->route mapping helpers.
 *
 * Methodology:
 * - Keeps all routes in one place so `MainActivity` can deterministically route users
 *   based on the first missing permission.
 * - Implements `routeForPermission()` as a single source of truth for mapping
 *   [com.okmoto.calamari.core.CalamariPermission] to the corresponding destination.
 */
package com.okmoto.calamari.navigation

import com.okmoto.calamari.core.CalamariPermission

object Routes {
    const val Splash = "splash"
    const val Home = "home"

    const val PermissionNotifications = "permission/notifications"
    const val PermissionMicrophone = "permission/microphone"
    const val PermissionCalendar = "permission/calendar"
    const val CalendarSetup = "permission/calendar_setup"
    const val PermissionOverlay = "permission/overlay"
}

fun CalamariPermission.routeForPermission(): String {
    return when (this) {
        CalamariPermission.POST_NOTIFICATIONS -> Routes.PermissionNotifications
        CalamariPermission.RECORD_AUDIO -> Routes.PermissionMicrophone
        CalamariPermission.READ_CALENDAR,
        CalamariPermission.WRITE_CALENDAR -> Routes.PermissionCalendar
        CalamariPermission.OVERLAY -> Routes.PermissionOverlay
    }
}

