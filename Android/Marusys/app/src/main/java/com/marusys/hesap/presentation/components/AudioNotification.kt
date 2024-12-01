package com.marusys.hesap.presentation.components

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.marusys.hesap.MainActivity
import com.marusys.hesap.R

// 알림창
class AudioNotification(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    init {
        createNotificationChannel()
        notificationBuilder = createNotificationBuilder()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "음성 인식을 위한 서비스입니다"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("마르시스")
            .setContentText("음성 인식 대기 중...")
            .setSmallIcon(R.drawable.marusys_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // 즉시 알림 표시
    }
    fun getNotification(): Notification = notificationBuilder.build()

    fun updateNotification(text: String) {
        notificationBuilder.setContentText(text)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    companion object {
        private const val CHANNEL_ID = "AudioServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}

