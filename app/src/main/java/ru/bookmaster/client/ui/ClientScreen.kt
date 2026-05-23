package ru.bookmaster.client.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ClientScreen(viewModel: SalonViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            Text("BookMaster Клиент", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            if (state.salonInfo == null) {
                OutlinedTextField(state.salonId, viewModel::onSalonIdChange, label = { Text("Код салона") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp))
                Spacer(Modifier.height(8.dp))
                Button(viewModel::loadSalon, Modifier.fillMaxWidth()) { Text("Найти салон") }
            } else {
                Text(state.salonInfo!!.companyName, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                // Услуги
                Text("Выберите услугу:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                state.salonInfo!!.services.forEach { s ->
                    val sel = state.selectedService == s
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surface),
                        onClick = { viewModel.selectService(s) }) {
                        Text("${s.name} — ${s.price} ₽ (${s.durationMinutes} мин)", Modifier.padding(10.dp))
                    }
                }

                // Мастера
                Spacer(Modifier.height(12.dp))
                Text("Выберите мастера:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                state.salonInfo!!.masters.forEach { m ->
                    val sel = state.selectedMaster == m
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surface),
                        onClick = { viewModel.selectMaster(m) }) {
                        Text(m.name + (m.specialization?.let { " — $it" } ?: ""), Modifier.padding(10.dp))
                    }
                }

                // Календарь
                if (state.selectedMaster != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Дата:", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        viewModel.getNextDays().forEach { item ->
                            val parts = item.split("|")
                            val sel = state.selectedDate == parts[1]
                            FilterChip(sel, { viewModel.selectDate(parts[1]) },
                                label = { Text(parts[0]) },
                                modifier = Modifier.padding(end = 6.dp))
                        }
                    }
                }

                // Время
                if (state.selectedDate != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Время:", fontWeight = FontWeight.Bold)
                    val slots = viewModel.getTimeSlots()
                    if (slots.isEmpty()) Text("Нет свободного времени", color = Color.Gray)
                    else {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            slots.forEach { t ->
                                val sel = state.selectedTime == t
                                FilterChip(sel, { viewModel.selectTime(t) },
                                    label = { Text(t) },
                                    modifier = Modifier.padding(end = 6.dp))
                            }
                        }
                    }
                }

                // Имя, телефон
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(state.clientName, viewModel::onNameChange, label = { Text("Ваше имя") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(state.clientPhone, viewModel::onPhoneChange, label = { Text("Телефон") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(16.dp))

                Button(viewModel::book, Modifier.fillMaxWidth().height(50.dp),
                    enabled = !state.isLoading && state.selectedService != null && state.selectedMaster != null && state.selectedTime != null) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Записаться", fontSize = 18.sp)
                }
            }

            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            if (state.isSuccess) Text("✅ Вы записаны!", color = Color(0xFF86EFAC), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}