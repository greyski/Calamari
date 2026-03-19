package com.okmoto.calamari.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.okmoto.calamari.ui.theme.CalamariTheme
import com.okmoto.calamari.overlay.ListeningState

@Composable
fun HomeScreen(
    listeningState: ListeningState,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Crossfade(listeningState) { state ->
                when (state) {
                    ListeningState.IDLE -> Text(
                        text = "Drag or tap me so I wake up!",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ListeningState.AWAKE -> Text(
                        text = "Say \"Calamari\" or double-tap me!",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ListeningState.AWAITING_EVENT -> Text(
                        text = "Say \"Add an event for August 5th\"",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ListeningState.IDLE_TITLE -> Text(
                        text = "Add a title by tapping the speech bubble.",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ListeningState.AWAITING_TITLE -> Text(
                        text = "Say \"Calamari rules!\"",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ListeningState.IDLE_SEND -> Text(
                        text = "I'll create your event!\nTap the refresh icon to update the title",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    ListeningState.ERROR -> Text(
                        text = "Something went wrong!\n",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CalamariTheme {
        HomeScreen(
            listeningState = ListeningState.IDLE,
        )
    }
}

