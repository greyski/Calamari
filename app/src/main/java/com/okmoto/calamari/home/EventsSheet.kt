/**
 * Bottom sheet UI that lists calendar events created by Calamari.
 *
 * Methodology:
 * - Events are rendered in a `LazyColumn`, ordered by `createdAtMillis` (most recent first).
 * - Clicking an event follows the 24-hour rule:
 *   - <24h old: open the default calendar view (no event deep link).
 *   - >=24h old: open the specific event using `CalendarContract.Events` + `eventId`.
 */
package com.okmoto.calamari.home

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.okmoto.calamari.calendar.CreatedCalamariCalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventsSheet(
    events: List<CreatedCalamariCalendarEvent>,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Your events",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 8.dp),
        )

        if (events.isEmpty()) {
            Text(
                text = "No events yet. Ask Calamari to add one.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val orderedEvents = events.sortedByDescending { it.createdAtMillis }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(orderedEvents) { created ->
                    val event = created.event
                    val title = event.title?.takeIf { it.isNotBlank() } ?: "Untitled event"
                    val dateLabel = event.startMillis.formatDateLabel()
                    val baseSubtitle = if (event.allDay) {
                        "$dateLabel • All day"
                    } else {
                        "$dateLabel • ${event.startMillis.formatTimeLabel()}"
                    }
                    val isDeleted = !created.existsInSystem
                    val subtitle = if (isDeleted) {
                        "$baseSubtitle • Deleted"
                    } else {
                        baseSubtitle
                    }
                    val subtitleColor = if (isDeleted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Column(
                        modifier = Modifier.clickable(
                            enabled = created.existsInSystem,
                            onClick = { context.openCalendarForCreatedEvent(created) },
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = subtitleColor,
                        )
                    }
                }
            }
        }
    }
}

private fun Long.formatDateLabel(): String {
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return dateFormat.format(Date(this))
}

private fun Long.formatTimeLabel(): String {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return timeFormat.format(Date(this))
}

private fun Context.openCalendarForCreatedEvent(event: CreatedCalamariCalendarEvent) {
    if (!event.existsInSystem) return

    val twentyFourHoursMillis = 24L * 60L * 60L * 1000L
    val eventAgeMillis = System.currentTimeMillis() - event.createdAtMillis
    val shouldOpenByEventId = eventAgeMillis >= twentyFourHoursMillis

    val intent = if (shouldOpenByEventId) {
        Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId)
        }
    } else {
        Intent(Intent.ACTION_VIEW).apply {
            data = CalendarContract.CONTENT_URI
        }
    }

    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    safeStartActivity(intent)
}

private fun Context.safeStartActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Ignore: no calendar app available.
    } catch (_: SecurityException) {
        // Ignore: calendar provider/viewer not available to this app.
    }
}
