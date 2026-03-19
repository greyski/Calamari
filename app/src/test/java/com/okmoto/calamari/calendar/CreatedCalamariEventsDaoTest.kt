package com.okmoto.calamari.calendar

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class CreatedCalamariEventsDaoTest {

    private fun inMemoryDb(context: Context): CalamariEventsDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            CalamariEventsDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @Test
    fun prune_keepsMostRecent250Rows() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = inMemoryDb(context)
        val dao = db.createdCalamariEventsDao()

        val total = 260
        for (i in 0 until total) {
            dao.upsert(
                CreatedCalamariEventEntity(
                    eventId = i.toLong(),
                    calendarId = 1L,
                    title = "Event $i",
                    startMillis = i.toLong(),
                    endMillis = i.toLong() + 60_000L,
                    allDay = false,
                    createdAtMillis = i.toLong(),
                    existsInSystem = true,
                    lastVerifiedAtMillis = null,
                ),
            )
        }

        dao.prune(maxRows = 250)

        val eventIds = dao.getAllEventIds().toSet()
        assertEquals(250, eventIds.size)
        assertTrue(eventIds.contains((total - 1).toLong()))
        assertFalse(eventIds.contains(0L))

        db.close()
    }

    @Test
    fun markDeletedAndMarkExists_updateExistsInSystemFlag() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = inMemoryDb(context)
        val dao = db.createdCalamariEventsDao()

        val now = System.currentTimeMillis()
        dao.upsert(
            CreatedCalamariEventEntity(
                eventId = 1L,
                calendarId = 1L,
                title = "A",
                startMillis = 0L,
                endMillis = 0L,
                allDay = false,
                createdAtMillis = 1L,
                existsInSystem = true,
                lastVerifiedAtMillis = null,
            ),
        )
        dao.upsert(
            CreatedCalamariEventEntity(
                eventId = 2L,
                calendarId = 1L,
                title = "B",
                startMillis = 0L,
                endMillis = 0L,
                allDay = false,
                createdAtMillis = 2L,
                existsInSystem = true,
                lastVerifiedAtMillis = null,
            ),
        )

        dao.markDeleted(eventIds = listOf(2L), verifiedAtMillis = now)

        val afterDelete = dao.observeCreatedEvents().first()
        val entityById = afterDelete.associateBy { it.eventId }
        assertTrue(entityById[1L]?.existsInSystem == true)
        assertFalse(entityById[2L]?.existsInSystem == true)

        dao.markExists(eventIds = listOf(2L), verifiedAtMillis = now + 1)
        val afterRestore = dao.observeCreatedEvents().first()
        val entityById2 = afterRestore.associateBy { it.eventId }
        assertTrue(entityById2[2L]?.existsInSystem == true)

        db.close()
    }
}

