/**
 * Repository wrapper for calendar integration (Calamari-created events).
 *
 * Methodology:
 * - Owns inserts into `CalendarContract.Events`.
 * - Maintains an in-memory cache (`createdEvents`) of events created through this repo
 *   so the Home screen can display them without re-querying the system calendar.
 */
package com.okmoto.calamari.calendar

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Immutable value object describing a single calendar event in the user's calendar.
 *
 * This is the data shape that `CalendarRepository` reads and writes. It is intentionally
 * simple and mirrors the subset of `CalendarContract.Events` fields that we care about.
 */
data class CalamariCalendarEvent(
    /** Human-readable title for the event. */
    val title: String? = null,
    /** Event start time in epoch milliseconds (local time). */
    val startMillis: Long = 0L,
    /** Event end time in epoch milliseconds (local time). */
    val endMillis: Long = 0L,
    /** Target calendar row ID in `CalendarContract.Calendars`. */
    val calendarId: Long = 0L,
    /** Whether the event spans the whole day. */
    val allDay: Boolean = false,
)

/**
 * Represents an event that was successfully created via [CalendarRepository].
 *
 * This is a local cache record so the app can show "events created by Calamari"
 * without having to re-query the system calendar immediately.
 */
data class CreatedCalamariCalendarEvent(
    val eventId: Long,
    val event: CalamariCalendarEvent,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Thin repository wrapper around `CalendarContract` that owns all direct
 * interaction with the system calendar provider.
 *
 * Responsibilities:
 * - Finding a sensible default calendar to write to.
 * - Inserting events into `CalendarContract.Events`.
 * - Providing small convenience helpers around these operations.
 */
@Singleton
class CalendarRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val _createdEvents = MutableStateFlow<List<CreatedCalamariCalendarEvent>>(emptyList())

    /**
     * In-memory cache of events created through this repository (most-recent first).
     *
     * Note: this cache is process-lifetime only. If the app process dies, callers should
     * rehydrate by querying the provider (we can add that later when wiring up UI).
     */
    val createdEvents: StateFlow<List<CreatedCalamariCalendarEvent>> = _createdEvents.asStateFlow()

    companion object {
        /**
         * Returns true if the device appears to have at least one visible calendar that we can
         * likely write to (i.e. contributes events).
         *
         * This is used as an onboarding gate so we can prompt the user to set up a calendar
         * account (Google, Exchange, etc.) before they reach Home.
         */
        fun hasWritableCalendar(context: Context): Boolean {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            )

            // CAL_ACCESS_CONTRIBUTOR is the minimum for inserting events.
            val selection =
                "${CalendarContract.Calendars.VISIBLE}=? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?"
            val args = arrayOf(
                "1",
                CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString(),
            )

            return try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    selection,
                    args,
                    null,
                )?.use { cursor ->
                    cursor.moveToFirst()
                } == true
            } catch (_: SecurityException) {
                // If calendar perms aren't granted yet, treat as "not ready".
                false
            } catch (_: Throwable) {
                false
            }
        }
    }

    /**
     * Convenience entry point for callers that cannot directly call suspend
     * functions. This launches a short-lived coroutine on the main thread
     * which, in turn, calls [createEvent] and reports the result via callbacks.
     *
     * This is primarily used by the overlay service, which wants to kick off
     * calendar I/O and then update UI once the operation finishes without
     * having to manage its own repository coroutine scope.
     *
     * @param event event details without a calendar ID; the default calendar
     *              will be resolved internally.
     * @param onSuccess invoked on the main thread if an event ID is created.
     * @param onError invoked on the main thread if the insert fails.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun submitEvent(
        event: CalamariCalendarEvent,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            val title = event.title.orEmpty()
            val createdId = createEvent(
                title = title,
                startMillis = event.startMillis,
                endMillis = event.endMillis,
                allDay = event.allDay,
            )
            if (createdId != null) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    /**
     * Finds a writable calendar ID to use as default.
     *
     * Strategy:
     * - Prefer a calendar that is both primary and visible.
     * - If none match, fall back to the first row returned by the query.
     */
    private suspend fun getDefaultCalendarId(): Long? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE,
        )
        val uri = CalendarContract.Calendars.CONTENT_URI

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            pickDefaultCalendar(cursor)
        }
    }

    /**
     * Uses the current cursor row set to pick a suitable calendar ID.
     *
     * This is used internally by [getDefaultCalendarId] to encapsulate the
     * selection logic away from the query plumbing.
     */
    private fun pickDefaultCalendar(cursor: Cursor): Long? {
        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
        val visibleIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

        var fallbackId: Long? = null
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIdx)
            if (fallbackId == null) fallbackId = id

            val isPrimary = if (primaryIdx >= 0) cursor.getInt(primaryIdx) == 1 else false
            val isVisible = if (visibleIdx >= 0) cursor.getInt(visibleIdx) == 1 else true

            if (isPrimary && isVisible) return id
        }
        return fallbackId
    }

    /**
     * Inserts a fully specified [CalamariCalendarEvent] into the user's calendar.
     *
     * Callers are responsible for providing a valid `calendarId` and time range;
     * this method simply translates the data class into `ContentValues` and
     * performs the insert against `CalendarContract.Events`.
     *
     * @return the newly created event ID, or `null` if the insert failed.
     */
    private suspend fun insertEvent(event: CalamariCalendarEvent): Long? =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(Events.TITLE, event.title)
                put(Events.DTSTART, event.startMillis)
                put(Events.DTEND, event.endMillis)
                put(Events.CALENDAR_ID, event.calendarId)
                put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(Events.ALL_DAY, if (event.allDay) 1 else 0)
            }

            val uri = context.contentResolver.insert(Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        }

    /**
     * Creates a calendar event from already-resolved fields.
     *
     * This is a small convenience wrapper that:
     * - Locates the default calendar via [getDefaultCalendarId].
     * - Builds a [CalamariCalendarEvent] with the provided fields.
     * - Delegates to [insertEvent] to perform the actual write.
     *
     * @return the newly created event ID, or `null` if there is no writable
     *         calendar or the insert fails.
     */
    private suspend fun createEvent(
        title: String,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean,
    ): Long? = withContext(Dispatchers.IO) {
        val calendarId = getDefaultCalendarId() ?: return@withContext null
        val event = CalamariCalendarEvent(
            title = title,
            startMillis = startMillis,
            endMillis = endMillis,
            calendarId = calendarId,
            allDay = allDay,
        )
        val createdId = insertEvent(event)
        if (createdId != null) {
            _createdEvents.update { current ->
                listOf(
                    CreatedCalamariCalendarEvent(
                        eventId = createdId,
                        event = event,
                    )
                ) + current
            }
        }
        createdId
    }

}

