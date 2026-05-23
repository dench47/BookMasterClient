package ru.bookmaster.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.bookmaster.client.ui.ClientScreen
import ru.bookmaster.client.ui.theme.ClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClientTheme { ClientScreen() }
        }
    }
}