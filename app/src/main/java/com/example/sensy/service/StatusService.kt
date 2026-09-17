package com.example.sensy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.sensy.MainActivity
import com.example.sensy.data.StatusRepository

class StatusService : Service() {

    private lateinit var statusRepository: StatusRepository

    override fun onCreate() {
        super.onCreate()
        statusRepository = StatusRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            statusRepository.clearStatus()
            stopSelf()
            return START_NOT_STICKY
        }

        val activeStatus = statusRepository.getActiveStatus()
        if (activeStatus == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification(activeStatus.statusText)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotification(statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StatusService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sensy Active")
            .setContentText("Status: $statusText")
            // A typical placeholder icon
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Clear Status", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sensy Status Channel"
            val descriptionText = "Shows the currently active Sensy status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val CHANNEL_ID = "sensy_status_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, StatusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StatusService::class.java)
            context.stopService(intent)
        }
    }
}
