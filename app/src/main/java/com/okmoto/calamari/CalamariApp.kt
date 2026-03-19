/**
 * Application entry point.
 *
 * Enables Hilt via [HiltAndroidApp]. Calamari relies on DI to provide the overlay service,
 * audio/motion/calendar integrations, and repositories to UI/service components.
 */
package com.okmoto.calamari

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CalamariApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
