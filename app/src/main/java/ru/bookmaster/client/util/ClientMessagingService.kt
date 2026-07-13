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
        val data = message.data
        val type = data["type"] ?: ""
        val clientPhone = data["clientPhone"] ?: ""

        // Проверка принадлежности уведомления текущему пользователю
        if (clientPhone.isNotBlank() && (type == "CLIENT_APPOINTMENT" || type == "WAITING_LIST_OFFER")) {
            val currentPhone = getSharedPreferences("verify_prefs", MODE_PRIVATE)
                .getString("phone", "")?.replace(Regex("[^0-9+]"), "") ?: ""
            val msgPhone = clientPhone.replace(Regex("[^0-9+]"), "")
            if (currentPhone.isBlank() || currentPhone != msgPhone) {
                return // Уведомление не для текущего пользователя
            }
        }

        // Для WAITING_LIST_OFFER — сохраняем данные о предложении и показываем уведомление
        if (type == "WAITING_LIST_OFFER") {
            val entryId = data["entryId"]?.toLongOrNull() ?: return
            val offerDate = data["date"] ?: ""
            val offerTime = data["time"] ?: ""
            val serviceName = data["serviceName"] ?: ""
            val masterName = data["masterName"] ?: ""

            // Сохраняем данные предложения для последующего подтверждения в UI
            getSharedPreferences("waiting_offer", MODE_PRIVATE).edit {
                putLong("entryId", entryId)
                putString("date", offerDate)
                putString("time", offerTime)
                putString("serviceName", serviceName)
                putString("masterName", masterName)
            }

            val title = data["title"] ?: "📋 Появилось свободное время!"
            val body = data["body"] ?: "$serviceName у $masterName • $offerDate $offerTime"

            val intent = android.content.Intent(this, ru.bookmaster.client.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("showWaitingOffer", true)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val n = android.app.Notification.Builder(this, "client_reminders")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), n)
            return
        }

        val title = data["title"] ?: message.notification?.title ?: "BookMaster"
        val body = data["body"] ?: message.notification?.body ?: ""

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
