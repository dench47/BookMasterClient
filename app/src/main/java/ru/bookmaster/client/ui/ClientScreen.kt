package ru.bookmaster.client.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    verifiedPhone: String = "",
    viewModel: SalonViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Автозаполнение телефона после верификации
    LaunchedEffect(verifiedPhone) {
        if (verifiedPhone.isNotBlank()) {
            viewModel.onPhoneChange(verifiedPhone)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BookMaster") },
                actions = {
                    if (!state.showMyAppointments) {
                        IconButton(onClick = { viewModel.loadMyAppointments() }) {
                            Icon(Icons.AutoMirrored.Filled.List, "Мои записи")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp)
        ) {
            if (state.showMyAppointments) {
                Text("Мои записи", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                if (state.myAppointments.isEmpty()) Text("Нет активных записей", color = Color.Gray)
                state.myAppointments.forEach { a ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(a.salonName ?: "", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            Text(a.serviceName, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Мастер: ${a.masterName}", color = Color(0xFF94A3B8))
                            Text("Дата: ${formatDate(a.startTime)} ${a.startTime.substring(11, 16)}", color = Color(0xFF94A3B8))
                            Text(
                                when {
                                    a.cancelled == true -> "❌ Отменена"
                                    a.confirmed == true -> "✅ Подтверждена"
                                    else -> "⏳ Ожидает"
                                },
                                color = when {
                                    a.cancelled == true -> Color(0xFFFCA5A5)
                                    a.confirmed == true -> Color(0xFF86EFAC)
                                    else -> Color(0xFFFCD34D)
                                }
                            )
                            // Кнопка "В календарь"
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = {
                                val sTime = a.startTime.take(10).replace("-", "") + "T" + a.startTime.substring(11, 16).replace(":", "") + "00"
                                val eTime = (a.endTime ?: a.startTime).substring(0, 10).replace("-", "") + "T" + (a.endTime ?: a.startTime).substring(11, 16).replace(":", "") + "00"
                                val title = "${a.serviceName} у ${a.masterName}"
                                val details = "Салон: ${a.salonName ?: ""}"
                                val url = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                                        "&text=${java.net.URLEncoder.encode(title, "UTF-8")}" +
                                        "&dates=$sTime/$eTime" +
                                        "&details=${java.net.URLEncoder.encode(details, "UTF-8")}" +
                                        "&location=${java.net.URLEncoder.encode(a.salonName ?: "", "UTF-8")}"
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    url.toUri())
                                context.startActivity(intent)
                            }) {
                                Text("📅 В календарь", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }
                            // Кнопка "Отменить"
                            if (a.cancelled != true) {
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { viewModel.cancelAppointment(a.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                                ) {
                                    Text("Отменить")
                                }
                            }
                        }
                    }                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(viewModel::hideMyAppointments, Modifier.fillMaxWidth()) { Text("Назад") }
                return@Column
            }

            OutlinedTextField(state.salonId, viewModel::onSalonIdChange, label = { Text("Код салона") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp))
            Spacer(Modifier.height(8.dp))
            Button(viewModel::loadSalon, Modifier.fillMaxWidth()) { Text("Найти салон") }

            if (state.salonInfo != null) {
                Spacer(Modifier.height(16.dp))
                Text(state.salonInfo!!.companyName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("Услуга:", fontWeight = FontWeight.Bold, color = Color.White)
                state.salonInfo!!.services.forEach { s ->
                    val sel = state.selectedService == s
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surface),
                        onClick = { viewModel.selectService(s) }) {
                        Text("${s.name} — ${s.price} ₽ (${s.durationMinutes} мин)", Modifier.padding(10.dp), color = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Мастер:", fontWeight = FontWeight.Bold, color = Color.White)
                state.salonInfo!!.masters.forEach { m ->
                    val sel = state.selectedMaster == m
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surface),
                        onClick = { viewModel.selectMaster(m) }) {
                        Text(m.name + (m.specialization?.let { " — $it" } ?: ""), Modifier.padding(10.dp), color = Color.White)
                    }
                }

                if (state.selectedMaster != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Дата:", fontWeight = FontWeight.Bold, color = Color.White)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        viewModel.getNextDays().forEach { item ->
                            val parts = item.split("|")
                            FilterChip(state.selectedDate == parts[1], { viewModel.selectDate(parts[1]) },
                                label = { Text(parts[0]) }, modifier = Modifier.padding(end = 6.dp))
                        }
                    }
                }

                if (state.selectedDate != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Время:", fontWeight = FontWeight.Bold, color = Color.White)
                    val slots = viewModel.getTimeSlots()
                    if (slots.isEmpty()) Text("Нет свободного времени", color = Color.Gray)
                    else Row(Modifier.horizontalScroll(rememberScrollState())) {
                        slots.forEach { t ->
                            FilterChip(state.selectedTime == t, { viewModel.selectTime(t) },
                                label = { Text(t) }, modifier = Modifier.padding(end = 6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(state.clientName, viewModel::onNameChange, label = { Text("Имя") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(state.clientPhone, viewModel::onPhoneChange, label = { Text("Телефон") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(16.dp))
                Button(viewModel::book, Modifier.fillMaxWidth().height(50.dp),
                    enabled = !state.isLoading && state.selectedTime != null) {
                    Text("Записаться", fontSize = 18.sp)
                }
            }

            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
private fun formatDate(dateTime: String): String {
    val parts = dateTime.take(10).split("-")
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}