package ru.bookmaster.client

import android.Manifest
import android.content.Context
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
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
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
                    ClientScreen(verifiedPhone = verifiedPhone)
                }
            }
        }
    }
}