package ru.bookmaster.client.util

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ClientMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("client_reminders", "Напоминания", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onNewToken(token: String) {
        getSharedPreferences("client_prefs", MODE_PRIVATE).edit { putString("fcm_token", token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        message.notification?.let {
            val n = android.app.Notification.Builder(this, "client_reminders")
                .setContentTitle(it.title).setContentText(it.body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true).build()
            getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), n)
        }
    }
}