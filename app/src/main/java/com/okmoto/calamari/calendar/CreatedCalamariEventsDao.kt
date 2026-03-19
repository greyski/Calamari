/**
 * DAO for persisted Calamari-created event history.
 *
 * Methodology:
 * - Provides a reactive stream ordered by `createdAtMillis DESC`.
 * - Supports bounded pruning to keep history lightweight (keep most recent N rows).
 * - Supports flagging events as present/missing in the system calendar.
 */
package com.okmoto.calamari.calendar

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreatedCalamariEventsDao {

    @Query(
        """
        SELECT *
        FROM calamari_created_events
        ORDER BY createdAtMillis DESC
        """
    )
    fun observeCreatedEvents(): Flow<List<CreatedCalamariEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CreatedCalamariEventEntity)

    /**
     * Keeps the most recent [maxRows] by `createdAtMillis` and deletes the oldest rows.
     */
    @Query(
        """
        DELETE FROM calamari_created_events
        WHERE eventId NOT IN (
            SELECT eventId
            FROM calamari_created_events
            ORDER BY createdAtMillis DESC
            LIMIT :maxRows
        )
        """
    )
    suspend fun prune(maxRows: Int)

    @Query("SELECT eventId FROM calamari_created_events")
    suspend fun getAllEventIds(): List<Long>

    @Query(
        """
        UPDATE calamari_created_events
        SET existsInSystem = 1,
            lastVerifiedAtMillis = :verifiedAtMillis
        WHERE eventId IN (:eventIds)
        """
    )
    suspend fun markExists(
        eventIds: List<Long>,
        verifiedAtMillis: Long,
    )

    @Query(
        """
        UPDATE calamari_created_events
        SET existsInSystem = 0,
            lastVerifiedAtMillis = :verifiedAtMillis
        WHERE eventId IN (:eventIds)
        """
    )
    suspend fun markDeleted(
        eventIds: List<Long>,
        verifiedAtMillis: Long,
    )
}

