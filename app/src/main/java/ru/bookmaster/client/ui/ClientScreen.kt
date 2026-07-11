package ru.bookmaster.client.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    verifiedPhone: String = "",
    viewModel: SalonViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // После верификации — запрос имени
    LaunchedEffect(verifiedPhone) {
        if (verifiedPhone.isNotBlank()) {
            viewModel.onPhoneChange(verifiedPhone)
            viewModel.loadClientNameFromServer()
        }
    }

    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            val intent = Intent(context, context.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text("BookMaster") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, "Меню", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("📅 Мои записи") },
                                onClick = { menuExpanded = false; viewModel.loadMyAppointments() }
                            )
                            DropdownMenuItem(
                                text = { Text("👤 Профиль") },
                                onClick = { menuExpanded = false; viewModel.showProfile() }
                            )
                            DropdownMenuItem(
                                text = { Text("🚪 Выйти") },
                                onClick = {
                                    menuExpanded = false
                                    context.getSharedPreferences(
                                        "verify_prefs",
                                        Context.MODE_PRIVATE
                                    ).edit { clear() }
                                    val intent = Intent(context, context.javaClass)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            // Экран "Как вас зовут?"
            if (state.showNamePrompt) {
                Text(
                    "Как вас зовут?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    state.clientName, viewModel::onNameChange, label = { Text("Ваше имя") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    context.getSharedPreferences("client_info", Context.MODE_PRIVATE)
                        .edit { putString("name", state.clientName) }
                    viewModel.saveName()
                }, Modifier.fillMaxWidth()) { Text("Сохранить") }
                return@Column
            }

            // Мои записи
            if (state.showMyAppointments) {
                Text(
                    "Мои записи",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                if (state.myAppointments.isEmpty()) Text("Нет активных записей", color = Color.Gray)
                var showCancelDialog by remember { mutableStateOf<Long?>(null) }

                if (state.myAppointments.isNotEmpty()) {
                    state.myAppointments.forEach { a ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    a.salonName ?: "",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(a.serviceName, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Мастер: ${a.masterName}", color = Color(0xFF94A3B8))
                                Text(
                                    "Дата: ${formatDate(a.startTime)} ${
                                        a.startTime.substring(11, 16)
                                    }", color = Color(0xFF94A3B8)
                                )
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
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = {
                                    val sTime = a.startTime.take(10)
                                        .replace("-", "") + "T" + a.startTime.substring(11, 16)
                                        .replace(":", "") + "00"
                                    val eTime = (a.endTime ?: a.startTime).substring(0, 10)
                                        .replace("-", "") + "T" + (a.endTime ?: a.startTime).substring(
                                        11,
                                        16
                                    ).replace(":", "") + "00"
                                    val title = "${a.serviceName} у ${a.masterName}"
                                    val details = "Салон: ${a.salonName ?: ""}"
                                    val url =
                                        "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                                                "&text=${java.net.URLEncoder.encode(title, "UTF-8")}" +
                                                "&dates=$sTime/$eTime" +
                                                "&details=${
                                                    java.net.URLEncoder.encode(
                                                        details,
                                                        "UTF-8"
                                                    )
                                                }" +
                                                "&location=${
                                                    java.net.URLEncoder.encode(
                                                        a.salonName ?: "",
                                                        "UTF-8"
                                                    )
                                                }"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                }) {
                                    Text(
                                        "📅 В календарь",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp
                                    )
                                }
                                if (a.cancelled != true) {
                                    Spacer(Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showCancelDialog = a.id },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFFCA5A5)
                                        )
                                    ) { Text("Отменить") }
                                }
                            }
                        }
                    }

                    // Диалог подтверждения отмены — ВЫНЕСЕН ИЗ ЦИКЛА
                    if (showCancelDialog != null) {
                        AlertDialog(
                            onDismissRequest = { showCancelDialog = null },
                            title = { Text("Отменить запись?") },
                            text = { Text("Вы уверены, что хотите отменить эту запись?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showCancelDialog?.let { viewModel.cancelAppointment(it) }
                                    showCancelDialog = null
                                }) { Text("Отменить", color = Color(0xFFFCA5A5)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCancelDialog = null }) {
                                    Text("Назад")
                                }
                            }
                        )
                    }

                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.backToSalon() },
                    Modifier.fillMaxWidth()
                ) { Text("Назад") }
                return@Column
            }

            // Профиль
            if (state.showProfile) {
                Text(
                    "Профиль",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                // Аватар
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = state.clientName
                        .split(" ")
                        .take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    state.clientName, viewModel::onNameChange, label = { Text("Имя") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    state.clientPhone, viewModel::onPhoneChange, label = { Text("Телефон") },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    context.getSharedPreferences("client_info", Context.MODE_PRIVATE).edit {
                        putString("name", state.clientName)
                        putString("phone", state.clientPhone)
                    }
                    viewModel.saveProfile()
                }, Modifier.fillMaxWidth()) { Text("Сохранить") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(viewModel::hideProfile, Modifier.fillMaxWidth()) { Text("Назад") }
                Spacer(Modifier.height(16.dp))
                var showDeleteDialog by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5))
                ) { Text("🗑 Удалить аккаунт") }

                if (showDeleteDialog) {
                    // Сначала показываем подтверждение
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Удалить аккаунт?") },
                        text = { Text("Все ваши данные будут удалены.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                viewModel.checkBeforeDelete()
                            }) { Text("Удалить", color = Color(0xFFFCA5A5)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                        }
                    )
                }

// Если сервер вернул ошибку (активные записи)
                if (state.deleteError != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearDeleteError() },
                        title = { Text("Невозможно удалить") },
                        text = { Text(state.deleteError!!) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.clearDeleteError() }) { Text("Понятно") }
                        }
                    )
                }
                return@Column
            }

            // Экран подтверждения быстрой записи
            if (state.showConfirm) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚡ Ближайшая запись",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${state.selectedService?.name}. Специалист ${state.selectedMaster?.name}",
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            "${formatDate(state.selectedDate ?: "")} в ${state.selectedTime?.take(5)}",
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.hideConfirm() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Отмена") }
                            Button(
                                onClick = { viewModel.book() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Подтвердить") }
                        }
                    }
                }
                return@Column
            }

            // Основной экран
            OutlinedTextField(
                state.salonId, viewModel::onSalonIdChange, label = { Text("Код салона") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp)
            )
            Spacer(Modifier.height(8.dp))
            Button(viewModel::loadSalon, Modifier.fillMaxWidth()) { Text("Найти салон") }

            if (state.salonInfo != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    state.salonInfo!!.companyName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(Modifier.height(12.dp))
                Text("Услуга:", fontWeight = FontWeight.Bold, color = Color.White)
                state.salonInfo!!.services.forEach { s ->
                    val sel = state.selectedService == s
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFF38BDF8) else MaterialTheme.colorScheme.surface),
                        onClick = { viewModel.selectService(s) }) {
                        Text(
                            "${s.name} — ${s.price} ₽ (${s.durationMinutes} мин)",
                            Modifier.padding(10.dp),
                            color = Color.White
                        )
                    }
                }

                if (state.selectedService != null) {
                    Spacer(Modifier.height(8.dp))
                    val masterLabel =
                        if (state.selectedMaster != null) "к ${state.selectedMaster!!.name}" else ""
                    Button(
                        onClick = { viewModel.getNextSlot() },
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        if (state.isLoading) CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        else Text(
                            "⚡ Записаться на ближайшее $masterLabel",
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Мастер:", fontWeight = FontWeight.Bold, color = Color.White)
                state.salonInfo!!.masters
                    .filter { m ->
                        state.selectedService == null ||
                                m.serviceIds == null || m.serviceIds.split(",")
                            .contains(state.selectedService!!.id.toString())
                    }
                    .forEach { m ->
                        val sel = state.selectedMaster == m
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sel) Color(
                                    0xFF38BDF8
                                ) else MaterialTheme.colorScheme.surface
                            ),
                            onClick = { viewModel.selectMaster(m) }) {
                            Text(m.name + (m.specialization?.let { " — $it" } ?: ""),
                                Modifier.padding(10.dp),
                                color = Color.White)
                        }
                    }
                if (state.selectedMaster != null) {
                    TextButton(
                        onClick = { viewModel.selectMaster(null) },
                        Modifier.fillMaxWidth()
                    ) {
                        Text("↩ Любой мастер", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                }

                if (state.selectedMaster != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Дата:", fontWeight = FontWeight.Bold, color = Color.White)

                    if (state.isPremium) {
                        val monthNames = listOf(
                            "Январь",
                            "Февраль",
                            "Март",
                            "Апрель",
                            "Май",
                            "Июнь",
                            "Июль",
                            "Август",
                            "Сентябрь",
                            "Октябрь",
                            "Ноябрь",
                            "Декабрь"
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { viewModel.prevMonth() }) {
                                Text(
                                    "←",
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            Text(
                                "${monthNames[state.calendarMonth - 1]} ${state.calendarYear}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.nextMonth() }) {
                                Text(
                                    "→",
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { d ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(d, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        }
                        val days = viewModel.getCalendarDays()
                        for (i in days.indices step 7) {
                            val week = days.subList(i, minOf(i + 7, days.size))
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { day ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!day.empty) {
                                            if (day.enabled) {
                                                val isSelected = state.selectedDate == day.date
                                                Box(
                                                    Modifier
                                                        .size(36.dp)
                                                        .then(
                                                            if (isSelected) Modifier.background(
                                                                Color(0xFF38BDF8),
                                                                CircleShape
                                                            ) else Modifier
                                                        )
                                                        .clickable { viewModel.selectDate(day.date) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        day.label, fontSize = 13.sp, maxLines = 1,
                                                        color = if (isSelected) Color(0xFF0F172A) else Color.White
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    day.label,
                                                    color = Color.Gray,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            viewModel.getNextDays().forEach { item ->
                                val parts = item.split("|")
                                FilterChip(
                                    state.selectedDate == parts[1],
                                    { viewModel.selectDate(parts[1]) },
                                    label = { Text(parts[0]) },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (state.selectedDate != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Время:", fontWeight = FontWeight.Bold, color = Color.White)
                    if (state.loadingWorkHours) Text("Загрузка...", color = Color.Gray)
                    else {
                        val slots = viewModel.getTimeSlots()
                        if (slots.isEmpty()) Text("Нет свободного времени", color = Color.Gray)
                        else Row(Modifier.horizontalScroll(rememberScrollState())) {
                            slots.forEach { t ->
                                FilterChip(
                                    state.selectedTime == t, { viewModel.selectTime(t) },
                                    label = { Text(t) }, modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    viewModel::showConfirmation, Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !state.isLoading && state.selectedTime != null
                ) {
                    Text("Записаться", fontSize = 18.sp)
                }
            }

            if (state.error != null) Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatDate(dateTime: String): String {
    val parts = dateTime.take(10).split("-")
    return "${parts[2]}.${parts[1]}.${parts[0]}"
}