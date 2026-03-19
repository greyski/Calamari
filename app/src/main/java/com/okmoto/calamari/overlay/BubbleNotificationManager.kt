package com.okmoto.calamari.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.okmoto.calamari.R
import com.okmoto.calamari.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper responsible for creating and updating the foreground notification used
 * by [FloatingBubbleService] while it is running as a foreground service.
 *
 * This object encapsulates all knowledge of channels, IDs and text mapping so
 * that the service can focus purely on behavior and state transitions.
 */
interface BubbleNotificationController {
    fun startInForeground(service: Service, text: String)
    fun updateNotification(context: Context, text: String)
}

@Singleton
class BubbleNotificationManager @Inject constructor() : BubbleNotificationController {
    private val channelId = "calamari_bubble_channel"
    private val channelName = "Calamari Listening"
    private val notificationId = 1

    override fun startInForeground(service: Service, text: String) {
        ensureChannel(service)
        val notification = buildNotification(service, text)
        service.startForeground(notificationId, notification)
    }

    /**
     * Ensures the notification channel required for the foreground service
     * exists on API 26+.
     */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) return

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the foreground notification corresponding to the given
     * [ListeningState].
     */
    private fun buildNotification(context: Context, text: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Updates the existing foreground notification to reflect a new
     * [ListeningState].
     */
    override fun updateNotification(context: Context, text: String) {
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, buildNotification(context, text))
    }
}

