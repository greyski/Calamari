package com.okmoto.calamari.permissions.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.okmoto.calamari.permissions.viewmodels.CalendarPermissionViewModel
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun CalendarPermissionScreen(
    onPermissionSatisfied: () -> Unit,
) {
    val vm: CalendarPermissionViewModel = viewModel()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            val hasRead = results[Manifest.permission.READ_CALENDAR] == true
            val hasWrite = results[Manifest.permission.WRITE_CALENDAR] == true
            if (hasRead && hasWrite) onPermissionSatisfied()
        },
    )

    PermissionScreenScaffold(
        title = vm.title,
        description = vm.description,
        primaryActionText = "Allow calendar access",
        onPrimaryAction = {
            launcher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                )
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
fun CalendarPermissionScreenPreview() {
    CalamariTheme {
        PermissionScreenScaffold(
            title = "Allow calendar access",
            description = "Calamari needs access to read and create events in your existing calendar apps on this device.",
            primaryActionText = "Allow calendar access",
            onPrimaryAction = {},
        )
    }
}

