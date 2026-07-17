package ru.bookmaster.client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import ru.bookmaster.client.ui.ClientScreen
import ru.bookmaster.client.ui.VerifyScreen
import ru.bookmaster.client.ui.theme.ClientTheme

class MainActivity : ComponentActivity() {
    // Версия — при каждом FCM увеличивается, чтобы LaunchedEffect перезапускался
    private var pendingShowWaitingOfferVersion by mutableStateOf(0)

    companion object {
        @Volatile
        var isForeground = false
            private set
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Восстанавливаем версию после поворота экрана
        pendingShowWaitingOfferVersion = savedInstanceState?.getInt("offer_version", 0) ?: 0
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.CALL_PHONE)
        }

        // Проверяем исходный intent при первом запуске
        handleIntent(intent)

        setContent {
            ClientTheme {
                var isVerified by remember { mutableStateOf(false) }
                var verifiedPhone by remember { mutableStateOf("") }
                val prefs = getSharedPreferences("verify_prefs", MODE_PRIVATE)
                val alreadyVerified = prefs.getBoolean("is_verified", false)
                val savedPhone = prefs.getString("phone", "") ?: ""

                if (alreadyVerified && savedPhone.isNotBlank()) {
                    isVerified = true
                    verifiedPhone = savedPhone
                }

                if (!isVerified) {
                    VerifyScreen(
                        onVerified = { phone ->
                            verifiedPhone = phone
                            isVerified = true
                        }
                    )
                } else {
                    ClientScreen(verifiedPhone = verifiedPhone, showWaitingOfferVersion = pendingShowWaitingOfferVersion)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("offer_version", pendingShowWaitingOfferVersion)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("showWaitingOffer", false) == true) {
            pendingShowWaitingOfferVersion++
            intent.removeExtra("showWaitingOffer") // чтобы при повороте экрана не срабатывало повторно
        }
    }
}
