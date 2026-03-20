/**
 * Compose screen for ensuring overlay (“display over other apps”) permission.
 *
 * Methodology:
 * - Checks `Settings.canDrawOverlays(...)` and immediately advances if already granted.
 * - Otherwise shows a scaffold that opens the system overlay settings via
 *   [com.okmoto.calamari.permissions.viewmodels.OverlayPermissionViewModel].
 */
package com.okmoto.calamari.permissions.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.okmoto.calamari.permissions.viewmodels.OverlayPermissionViewModel
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun OverlayPermissionScreen(
    onPermissionSatisfied: () -> Unit,
) {
    val context = LocalContext.current
    val vm: OverlayPermissionViewModel = hiltViewModel()

    DisposableEffect(Unit) {
        if (Settings.canDrawOverlays(context)) {
            onPermissionSatisfied()
        }
        onDispose { }
    }

    PermissionScreenScaffold(
        title = vm.title,
        description = vm.description,
        primaryActionText = "Open overlay settings",
        onPrimaryAction = { context.safeStartActivity(vm.buildOverlaySettingsIntent()) },
        secondaryActionText = "I've enabled it",
        onSecondaryAction = onPermissionSatisfied,
    )
}

private fun Context.safeStartActivity(intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun OverlayPermissionScreenPreview() {
    CalamariTheme {
        PermissionScreenScaffold(
            title = "Display over other apps",
            description = "Calamari uses a small floating bubble that appears over other apps so you can trigger voice commands from anywhere.",
            primaryActionText = "Open overlay settings",
            onPrimaryAction = {},
            secondaryActionText = "I've enabled it",
            onSecondaryAction = {},
        )
    }
}

