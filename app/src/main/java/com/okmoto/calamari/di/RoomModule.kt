/**
 * Hilt wiring for Room-based persistence.
 *
 * Provides a singleton `CalamariEventsDatabase` instance and the DAO used by:
 * - [com.okmoto.calamari.calendar.CalendarRepository]
 * - [com.okmoto.calamari.calendar.CreatedEventsVerifierWorker]
 */
package com.okmoto.calamari.di

import android.content.Context
import androidx.room.Room
import com.okmoto.calamari.calendar.CalamariEventsDatabase
import com.okmoto.calamari.calendar.CreatedCalamariEventsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    private const val DATABASE_NAME = "calamari_created_events.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext appContext: Context,
    ): CalamariEventsDatabase {
        return Room.databaseBuilder(
            appContext,
            CalamariEventsDatabase::class.java,
            DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCreatedEventsDao(
        database: CalamariEventsDatabase,
    ): CreatedCalamariEventsDao = database.createdCalamariEventsDao()
}

