package com.okmoto.calamari.permissions.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.okmoto.calamari.permissions.viewmodels.NotificationsPermissionViewModel
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun NotificationsPermissionScreen(
    onPermissionSatisfied: () -> Unit,
) {
    val vm: NotificationsPermissionViewModel = viewModel()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onPermissionSatisfied()
        return
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onPermissionSatisfied()
        },
    )

    PermissionScreenScaffold(
        title = vm.title,
        description = vm.description,
        primaryActionText = "Allow notifications",
        onPrimaryAction = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
    )
}

@Preview(showBackground = true)
@Composable
fun NotificationsPermissionScreenPreview() {
    CalamariTheme {
        PermissionScreenScaffold(
            title = "Enable notifications",
            description = "Calamari uses notifications so the floating bubble can run reliably in the background and keep you informed.",
            primaryActionText = "Allow notifications",
            onPrimaryAction = {},
        )
    }
}

