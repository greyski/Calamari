/**
 * Room database for Calamari-created event history.
 *
 * This database is intentionally small and bounded by DAO pruning rules so it remains
 * efficient for UI display and periodic verification jobs.
 */
package com.okmoto.calamari.calendar

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CreatedCalamariEventEntity::class],
    version = 1,
)
abstract class CalamariEventsDatabase : RoomDatabase() {
    abstract fun createdCalamariEventsDao(): CreatedCalamariEventsDao
}

