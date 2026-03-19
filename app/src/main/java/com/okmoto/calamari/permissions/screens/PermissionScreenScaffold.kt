/**
 * Shared scaffold used by permission request screens.
 *
 * Methodology:
 * - Standardizes layout (title, description, primary button, optional secondary button).
 * - Keeps screens focused on permission mechanics while the scaffold stays purely UI.
 */
package com.okmoto.calamari.permissions.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreenScaffold(
    title: String,
    description: String,
    primaryActionText: String,
    onPrimaryAction: () -> Unit,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(text = description, style = MaterialTheme.typography.bodyLarge)

            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = onPrimaryAction,
            ) {
                Text(primaryActionText)
            }

            if (secondaryActionText != null && onSecondaryAction != null) {
                Button(
                    onClick = onSecondaryAction,
                ) {
                    Text(secondaryActionText)
                }
            }
        }
    }
}

