package ru.bookmaster.client.util

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.bookmaster.client.data.api.RetrofitClient

class ClientMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("client_reminders", "Напоминания", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onNewToken(token: String) {
        getSharedPreferences("client_prefs", MODE_PRIVATE).edit { putString("fcm_token", token) }

        // Отправляем новый токен на сервер, если есть верифицированный телефон
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Сначала пробуем data-поля (сервер отправляет только data, без Notification)
        val title = message.data["title"] ?: message.notification?.title ?: "BookMaster"
        val body = message.data["body"] ?: message.notification?.body ?: ""

        val n = android.app.Notification.Builder(this, "client_reminders")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), n)
    }

    private fun sendTokenToServer(fcmToken: String) {
        val prefs = getSharedPreferences("verify_prefs", MODE_PRIVATE)
        val phone = prefs.getString("phone", null) ?: return
        val isVerified = prefs.getBoolean("is_verified", false)
        if (!isVerified || phone.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.registerClientToken(
                    mapOf("token" to fcmToken, "phone" to phone)
                )
            } catch (_: Exception) { }
        }
    }
}
