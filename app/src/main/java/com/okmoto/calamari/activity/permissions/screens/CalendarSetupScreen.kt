/**
 * Calendar setup screen shown when Calamari can't find a writable calendar.
 *
 * Methodology:
 * - Presents user guidance and a primary action that opens system settings.
 * - The “Skip for now” action returns control to the caller so Home navigation can continue.
 */
package com.okmoto.calamari.permissions.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun CalendarSetupScreen(
    onSkip: () -> Unit,
) {
    val context = LocalContext.current

    PermissionScreenScaffold(
        title = "Set up a calendar",
        description = "Calamari can create events, but your device doesn’t have any writable calendars available yet. Add a calendar account (Google, Exchange, etc.) and make sure calendars are enabled and visible.",
        primaryActionText = "Open system settings",
        onPrimaryAction = { context.safeStartActivity(Intent(Settings.ACTION_SETTINGS)) },
        secondaryActionText = "Skip for now",
        onSecondaryAction = onSkip,
    )
}

private fun Context.safeStartActivity(intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun CalendarSetupScreenPreview() {
    CalamariTheme {
        CalendarSetupScreen(onSkip = {})
    }
}

