/**
 * Background verifier for Calamari-created events.
 *
 * It periodically checks stored `eventId`s against `CalendarContract.Events` and updates
 * the Room table so the UI can flag and disable taps for events deleted externally.
 */
package com.okmoto.calamari.calendar

import android.content.Context
import android.provider.CalendarContract
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreatedEventsVerifierWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val createdEventsDao: CreatedCalamariEventsDao by lazy {
        Room.databaseBuilder(
            applicationContext,
            CalamariEventsDatabase::class.java,
            "calamari_events.db",
        ).build().createdCalamariEventsDao()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val eventIds = createdEventsDao.getAllEventIds()
        if (eventIds.isEmpty()) return@withContext Result.success()

        val verifiedAtMillis = System.currentTimeMillis()
        val existingIds = findExistingEventIds(eventIds)
        val existingSet = existingIds.toHashSet()

        val missingIds = eventIds.filterNot { it in existingSet }
        val foundIds = eventIds.filter { it in existingSet }

        if (foundIds.isNotEmpty()) {
            createdEventsDao.markExists(foundIds, verifiedAtMillis)
        }
        if (missingIds.isNotEmpty()) {
            createdEventsDao.markDeleted(missingIds, verifiedAtMillis)
        }

        // Keep DB bounded even if many deletions happen.
        createdEventsDao.prune(maxRows = 250)
        Result.success()
    }

    private fun findExistingEventIds(eventIds: List<Long>): List<Long> {
        val projection = arrayOf(CalendarContract.Events._ID)
        val resolver = applicationContext.contentResolver

        // Avoid overly-large IN clauses.
        val chunkSize = 500
        val existing = mutableSetOf<Long>()

        for (chunk in eventIds.chunked(chunkSize)) {
            val placeholders = List(chunk.size) { "?" }.joinToString(separator = ",")
            val selection = "${CalendarContract.Events._ID} IN ($placeholders)"
            val selectionArgs = chunk.map { it.toString() }.toTypedArray()

            val cursor = resolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )
            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                while (it.moveToNext()) {
                    existing.add(it.getLong(idIdx))
                }
            }
        }

        return existing.toList()
    }
}

