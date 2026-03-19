/**
 * Application entry point.
 *
 * Enables Hilt via [HiltAndroidApp]. Calamari relies on DI to provide the overlay service,
 * audio/motion/calendar integrations, and repositories to UI/service components.
 */
package com.okmoto.calamari

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CalamariApp : Application()
