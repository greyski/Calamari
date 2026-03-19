package com.okmoto.calamari.permissions.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.okmoto.calamari.permissions.viewmodels.MicrophonePermissionViewModel
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun MicrophonePermissionScreen(
    onPermissionSatisfied: () -> Unit,
) {
    val vm: MicrophonePermissionViewModel = hiltViewModel()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onPermissionSatisfied()
        },
    )

    PermissionScreenScaffold(
        title = vm.title,
        description = vm.description,
        primaryActionText = "Allow microphone",
        onPrimaryAction = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
    )
}

@Preview(showBackground = true)
@Composable
fun MicrophonePermissionScreenPreview() {
    CalamariTheme {
        PermissionScreenScaffold(
            title = "Allow microphone access",
            description = "Calamari needs microphone access to listen for the hot word and process your commands.",
            primaryActionText = "Allow microphone",
            onPrimaryAction = {},
        )
    }
}

