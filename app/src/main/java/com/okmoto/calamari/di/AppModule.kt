/**
 * Hilt dependency bindings for the Calamari app.
 *
 * Methodology:
 * - Declares interfaces -> implementations for audio/session, overlay feedback,
 *   calendar/event repository plumbing, and motion detection.
 * - Centralizes wiring so composables/services can remain implementation-agnostic.
 */
package com.okmoto.calamari.di

import com.okmoto.calamari.audio.AudioSessionManager
import com.okmoto.calamari.audio.PicovoiceRepository
import com.okmoto.calamari.overlay.BubbleFeedbackManager
import com.okmoto.calamari.overlay.BubbleFeedbackPlayer
import com.okmoto.calamari.overlay.BubbleOverlayFeedbackController
import com.okmoto.calamari.overlay.BubbleOverlayFeedbackManager
import com.okmoto.calamari.overlay.BubbleNotificationController
import com.okmoto.calamari.overlay.BubbleNotificationManager
import com.okmoto.calamari.overlay.ListeningStateRepository
import com.okmoto.calamari.overlay.ListeningStateStore
import com.okmoto.calamari.overlay.MainActivityForegroundRepository
import com.okmoto.calamari.overlay.MainActivityForegroundStore
import com.okmoto.calamari.overlay.MotionWakeController
import com.okmoto.calamari.overlay.MotionWakeDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAudioSessionManager(impl: PicovoiceRepository): AudioSessionManager

    @Binds
    @Singleton
    abstract fun bindListeningStateStore(impl: ListeningStateRepository): ListeningStateStore

    @Binds
    @Singleton
    abstract fun bindMainActivityForegroundStore(impl: MainActivityForegroundRepository): MainActivityForegroundStore

    @Binds
    @Singleton
    abstract fun bindMotionWakeController(impl: MotionWakeDetector): MotionWakeController

    @Binds
    @Singleton
    abstract fun bindBubbleFeedbackPlayer(impl: BubbleFeedbackManager): BubbleFeedbackPlayer

    @Binds
    @Singleton
    abstract fun bindBubbleNotificationController(impl: BubbleNotificationManager): BubbleNotificationController

    @Binds
    @Singleton
    abstract fun bindBubbleOverlayFeedbackController(
        impl: BubbleOverlayFeedbackManager
    ): BubbleOverlayFeedbackController
}
