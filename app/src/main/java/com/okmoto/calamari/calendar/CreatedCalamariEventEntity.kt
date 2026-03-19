/**
 * Room entity describing Calamari-created calendar event history.
 *
 * This table is the source of truth for the Home events bottom sheet, including:
 * - `createdAtMillis` for the 24-hour click/deeplink rule
 * - `existsInSystem` so the UI can disable taps for events deleted externally
 */
package com.okmoto.calamari.calendar

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted metadata for a calendar event created via [CalendarRepository].
 *
 * We store `createdAtMillis` so Home can apply the 24-hour click/deeplink rule, and we store
 * `existsInSystem` so the UI can flag events that were deleted outside Calamari.
 */
@Entity(tableName = "calamari_created_events")
data class CreatedCalamariEventEntity(
    /**
     * System calendar event row id (`CalendarContract.Events._ID`).
     *
     * This is our primary key because it uniquely identifies the external event.
     */
    @PrimaryKey val eventId: Long,
    val calendarId: Long,
    val title: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val createdAtMillis: Long,
    val existsInSystem: Boolean = true,
    val lastVerifiedAtMillis: Long? = null,
)

