package com.okmoto.calamari.home

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.okmoto.calamari.calendar.CreatedCalamariCalendarEvent
import com.okmoto.calamari.overlay.ListeningState
import com.okmoto.calamari.ui.theme.CalamariTheme

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    listeningState: ListeningState,
    createdEvents: List<CreatedCalamariCalendarEvent>,
) {
    var isEventsSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isEventsSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isEventsSheetOpen = false },
            sheetState = sheetState,
        ) {
            EventsSheet(
                events = createdEvents,
            )
        }
    }

    Scaffold { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                modifier = Modifier.align(End).clickable {
                    isEventsSheetOpen = true
                },
                imageVector = Icons.Default.DateRange,
                contentDescription = "Your Calamari events",
                tint = MaterialTheme.colorScheme.primary,
            )
            Crossfade(listeningState) { state ->
                when (state) {
                    ListeningState.IDLE -> Text(
                        text = "Drag or tap to wake me up!",
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
                        text = "Sending your event to your calendar!",
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
            createdEvents = emptyList(),
        )
    }
}

