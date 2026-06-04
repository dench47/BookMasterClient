package ru.bookmaster.client.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VerifyScreen(
    onVerified: (String) -> Unit,
    viewModel: VerifyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) {
            onVerified(state.phone)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Верификация номера", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(24.dp))

        if (!state.isCalling) {
            OutlinedTextField(
                state.phone, viewModel::onPhoneChange,
                label = { Text("Номер телефона") },
                placeholder = { Text("+79001234567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.requestVerification() },
                Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Верифицировать")
            }
        } else {
            Text("Позвоните на номер", color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(state.callPhone, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF38BDF8))
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${state.callPhone}"))
                    context.startActivity(intent)
                },
                Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("Позвонить")
            }

            Spacer(Modifier.height(12.dp))
            Text("После звонка вернитесь в приложение", color = Color.Gray, fontSize = 13.sp)

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { viewModel.startChecking() },
                Modifier.fillMaxWidth()
            ) {
                Text("Начать проверку")
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}