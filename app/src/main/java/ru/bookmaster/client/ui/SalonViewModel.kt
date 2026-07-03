package ru.bookmaster.client.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.bookmaster.client.data.api.RetrofitClient
import ru.bookmaster.client.data.model.AppointmentRequest
import ru.bookmaster.client.data.model.AppointmentResponse
import ru.bookmaster.client.data.model.MasterDto
import ru.bookmaster.client.data.model.SalonInfo
import ru.bookmaster.client.data.model.ServiceDto
import ru.bookmaster.client.data.model.SlotInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


data class ClientUiState(
    val salonId: String = "",
    val salonInfo: SalonInfo? = null,
    val selectedService: ServiceDto? = null,
    val selectedMaster: MasterDto? = null,
    val selectedDate: String? = null,
    val selectedTime: String? = null,
    val bookedSlots: List<SlotInfo> = emptyList(),
    val clientName: String = "",
    val clientPhone: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val showMyAppointments: Boolean = false,
    val myAppointments: List<AppointmentResponse> = emptyList(),
    val showConfirm: Boolean = false,
    val startTime: String = "",
    val workStart: String = "09:00",
    val workEnd: String = "18:00",
    val loadingWorkHours: Boolean = false,
    val isPremium: Boolean = false,
    val calendarMonth: Int = LocalDate.now().monthValue,
    val calendarYear: Int = LocalDate.now().year,
    val timeStep: Int = 30,
    val stickTime: Boolean = false,
    val bookingLimit: String = "none",
    val showProfile: Boolean = false,
    val showNamePrompt: Boolean = false,
    val accountDeleted: Boolean = false,
    val deleteError: String? = null,
    val isServerError: Boolean = false
)

class SalonViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitClient.instance
    private val quickApi = RetrofitClient.quickInstance  // ← для быстрых запросов

    private val app = application
    private val _state = MutableStateFlow(ClientUiState())
    val state = _state.asStateFlow()

    companion object {
        private const val CACHE_APPOINTMENTS_KEY = "cached_appointments"
    }

    init {
        val prefs = app.getSharedPreferences("client_info", Context.MODE_PRIVATE)
        val savedPhone = prefs.getString("phone", "")
        val savedName = prefs.getString("name", "")
        if (!savedPhone.isNullOrBlank()) {
            _state.value = _state.value.copy(clientPhone = savedPhone)
        }
        if (!savedName.isNullOrBlank()) {
            _state.value = _state.value.copy(clientName = savedName)
        }
    }

    private fun saveAppointmentsLocally(appointments: List<AppointmentResponse>) {
        val gson = Gson()
        val json = gson.toJson(appointments)
        app.getSharedPreferences("client_info", Context.MODE_PRIVATE).edit {
            putString(CACHE_APPOINTMENTS_KEY, json)
        }
    }

    private fun loadAppointmentsFromCache(): List<AppointmentResponse> {
        val prefs = app.getSharedPreferences("client_info", Context.MODE_PRIVATE)
        val json = prefs.getString(CACHE_APPOINTMENTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<AppointmentResponse>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun loadSavedClientInfo() {
        val prefs = app.getSharedPreferences("client_info", android.content.Context.MODE_PRIVATE)
        val savedName = prefs.getString("name", "")
        val savedPhone = prefs.getString("phone", "")
        if (!savedName.isNullOrBlank()) _state.value = _state.value.copy(clientName = savedName)
        if (!savedPhone.isNullOrBlank()) _state.value = _state.value.copy(clientPhone = savedPhone)
    }

    fun loadClientNameFromServer() {
        val phone = _state.value.clientPhone.replace(Regex("[^0-9+]"), "")
        if (phone.length < 10) return

        viewModelScope.launch {
            try {
                val r = api.getClientByPhone(phone)
                if (r.isSuccessful) {
                    val body = r.body()
                    val name = body?.get("name")?.toString() ?: ""
                    if (name.isNotBlank() && name != "Клиент") {
                        try {
                            val fcmToken = FirebaseMessaging.getInstance().token.await()
                            if (fcmToken != null) {
                                api.registerClientToken(
                                    mapOf(
                                        "token" to fcmToken,
                                        "phone" to phone
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                        _state.value = _state.value.copy(clientName = name)
                        app.getSharedPreferences("client_info", Context.MODE_PRIVATE).edit {
                            putString("name", name)
                        }
                    } else {
                        showNamePrompt()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun onSalonIdChange(id: String) {
        _state.value = _state.value.copy(salonId = id)
    }

    fun onNameChange(n: String) {
        _state.value = _state.value.copy(clientName = n)
    }

    fun onPhoneChange(p: String) {
        _state.value = _state.value.copy(clientPhone = p)
    }

    fun hideMyAppointments() {
        _state.value = _state.value.copy(showMyAppointments = false, error = null, isServerError = false)
    }

    fun loadMyAppointments() {
        val phone = _state.value.clientPhone.replace(Regex("[^0-9+]"), "")
        if (phone.length < 10) {
            _state.value = _state.value.copy(error = "Номер не верифицирован")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, isServerError = false)
            try {
                val r = quickApi.getMyAppointments(phone)
                if (r.isSuccessful) {
                    val apps = r.body()?.filter {
                        val dt = LocalDateTime.parse(it.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        dt.isAfter(LocalDateTime.now()) && it.cancelled != true
                    } ?: emptyList()

                    saveAppointmentsLocally(apps)

                    _state.value = _state.value.copy(
                        myAppointments = apps,
                        showMyAppointments = true,
                        isLoading = false,
                        error = null,
                        isServerError = false
                    )
                } else {
                    val cachedApps = loadAppointmentsFromCache()
                    if (cachedApps.isNotEmpty()) {
                        _state.value = _state.value.copy(
                            myAppointments = cachedApps,
                            showMyAppointments = true,
                            isLoading = false,
                            error = "⚠️ Сервер недоступен. Показаны сохранённые записи.",
                            isServerError = false
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Нет сохранённых записей и сервер недоступен"
                        )
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                val cachedApps = loadAppointmentsFromCache()
                if (cachedApps.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        myAppointments = cachedApps,
                        showMyAppointments = true,
                        isLoading = false,
                        error = "⚠️ Сервер не отвечает. Оффлайн режим",
                        isServerError = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isServerError = true,
                        error = "Сервер не отвечает."
                    )
                }
            } catch (e: java.net.ConnectException) {
                val cachedApps = loadAppointmentsFromCache()
                if (cachedApps.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        myAppointments = cachedApps,
                        showMyAppointments = true,
                        isLoading = false,
                        error = "⚠️ Нет связи с сервером. Оффлайн режим.",
                        isServerError = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isServerError = true,
                        error = "Нет связи с сервером. Проверьте интернет."
                    )
                }
            } catch (e: Exception) {
                val cachedApps = loadAppointmentsFromCache()
                if (cachedApps.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        myAppointments = cachedApps,
                        showMyAppointments = true,
                        isLoading = false,
                        error = "⚠️ Ошибка соединения. Показаны сохранённые записи.",
                        isServerError = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadSalon() {
        val id = _state.value.salonId.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val r = api.getSalonInfo(id)
                if (r.isSuccessful) {
                    _state.value = _state.value.copy(
                        salonInfo = r.body(),
                        isLoading = false,
                        isPremium = r.body()?.isPremium ?: false
                    )
                } else {
                    _state.value = _state.value.copy(error = "Салон не найден", isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка: ${e.message}", isLoading = false)
            }
        }
    }

    fun selectService(s: ServiceDto) {
        _state.value =
            _state.value.copy(selectedService = s, selectedDate = null, selectedTime = null)
    }

    fun selectMaster(m: MasterDto?) {
        _state.value = _state.value.copy(
            selectedMaster = m, selectedDate = null, selectedTime = null,
            workStart = "09:00", workEnd = "18:00"
        )
    }

    fun selectDate(date: String) {
        _state.value = _state.value.copy(selectedDate = date, selectedTime = null)
        loadSlots(date)
        loadWorkHours(date)
    }

    private fun loadWorkHours(date: String) {
        val master = _state.value.selectedMaster ?: return
        _state.value = _state.value.copy(loadingWorkHours = true)
        viewModelScope.launch {
            try {
                val r = api.getWorkHours(master.id, date)
                if (r.isSuccessful) {
                    val body = r.body()!!
                    if (body.containsKey("error")) {
                        _state.value = _state.value.copy(
                            workStart = "00:00", workEnd = "00:00",
                            timeStep = 30, stickTime = false, loadingWorkHours = false
                        )
                    } else {
                        val timeStep = (body["timeStep"] ?: "30").toIntOrNull() ?: 30
                        _state.value = _state.value.copy(
                            workStart = body["workStart"] ?: "09:00",
                            workEnd = body["workEnd"] ?: "18:00",
                            timeStep = timeStep,
                            stickTime = body["stickTime"]?.toBooleanStrictOrNull() ?: false,
                            bookingLimit = body["bookingLimit"] ?: "none",
                            loadingWorkHours = false
                        )
                    }
                } else {
                    _state.value = _state.value.copy(loadingWorkHours = false)
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(loadingWorkHours = false)
            }
        }
    }

    fun selectTime(time: String) {
        _state.value = _state.value.copy(selectedTime = time)
    }

    private fun loadSlots(date: String) {
        val masterId = _state.value.selectedMaster?.id ?: return
        viewModelScope.launch {
            try {
                val r = api.getBookedSlots(masterId, date)
                if (r.isSuccessful) _state.value =
                    _state.value.copy(bookedSlots = r.body() ?: emptyList())
            } catch (_: Exception) {
            }
        }
    }

    fun getNextDays(): List<String> {
        val days = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
        val months = listOf(
            "янв",
            "фев",
            "мар",
            "апр",
            "мая",
            "июн",
            "июл",
            "авг",
            "сен",
            "окт",
            "ноя",
            "дек"
        )
        val now = LocalDate.now()
        val start = if (LocalTime.now().hour >= 20) 1 else 0
        return (start..start + 6).map {
            val d = now.plusDays(it.toLong())
            "${days[d.dayOfWeek.value % 7]} ${d.dayOfMonth} ${months[d.monthValue - 1]}|$d"
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getTimeSlots(): List<String> {
        val state = _state.value
        state.selectedMaster ?: return emptyList()
        val date = state.selectedDate ?: return emptyList()

        val workStart = state.workStart
        val workEnd = state.workEnd
        val timeStep = state.timeStep
        val stickTime = state.stickTime
        val serviceDuration = state.selectedService?.durationMinutes ?: 30
        val startH = workStart.split(":")[0].toInt()
        val endH = workEnd.split(":")[0].toInt()

        val bookedMinutes = state.bookedSlots
            .mapNotNull { bs ->
                val parts = bs.startTime.split(" ")
                if (parts.size < 2 || parts[0] != date) return@mapNotNull null
                val t = parts[1].split(":").map { it.toInt() }
                val start = t[0] * 60 + t[1]
                start to (start + bs.durationMinutes)
            }
            .sortedBy { it.first }

        val slots = mutableListOf<String>()
        var totalMin = startH * 60
        val endMin = endH * 60
        while (totalMin + serviceDuration <= endMin) {
            val h = totalMin / 60
            val m = totalMin % 60
            val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
            val full = "$date $time"
            val slotMin = totalMin
            val slotEnd = slotMin + serviceDuration

            val isBooked = state.bookedSlots.any { bs ->
                val bsStart = bs.startTime
                val bsParts = bsStart.split(" ")[1].split(":").map { it.toInt() }
                val bsMin = bsParts[0] * 60 + bsParts[1]
                val bsEnd = bsMin + bs.durationMinutes
                slotMin < bsEnd && slotEnd > bsMin
            }
            if (isBooked) {
                totalMin += timeStep; continue
            }

            if (stickTime && bookedMinutes.isNotEmpty()) {
                val mergedBlocks = mutableListOf<Pair<Int, Int>>()
                for ((start, end) in bookedMinutes) {
                    if (mergedBlocks.isEmpty() || start > mergedBlocks.last().second) {
                        mergedBlocks.add(start to end)
                    } else {
                        val last = mergedBlocks.removeLast()
                        mergedBlocks.add(last.first to maxOf(last.second, end))
                    }
                }

                var stickOk = false
                for ((bmStart, bmEnd) in mergedBlocks) {
                    if (slotEnd <= bmStart && (bmStart - slotEnd) < timeStep) stickOk = true
                    if (slotMin >= bmEnd && (slotMin - bmEnd) < timeStep) stickOk = true
                }
                if (!stickOk) {
                    totalMin += timeStep; continue
                }
            }

            val bookingLimit = state.bookingLimit
            if (bookingLimit != "none") {
                val now = LocalDateTime.now()
                val maxTime: LocalDateTime? = when (bookingLimit) {
                    "today_tomorrow" -> now.plusDays(2).withHour(0).withMinute(0).withSecond(0)
                        .withNano(0)

                    "today" -> now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                    "12h" -> now.plusHours(12)
                    "4h" -> now.plusHours(4)
                    "3h" -> now.plusHours(3)
                    "2h" -> now.plusHours(2)
                    "1h" -> now.plusHours(1)
                    else -> null
                }
                if (maxTime != null) {
                    val slotDateTime = LocalDateTime.parse(
                        "${date}T${time}:00",
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    )
                    if (!slotDateTime.isAfter(maxTime)) {
                        totalMin += timeStep; continue
                    }
                }
            }

            slots.add(time)
            totalMin += timeStep
        }

        if (date == LocalDate.now().toString()) {
            val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
            return slots.filter {
                val parts = it.split(":")
                parts[0].toInt() * 60 + parts[1].toInt() > nowMin
            }
        }
        return slots
    }

    fun book() {
        val s = _state.value
        if (s.isLoading) return
        if (s.clientName.isBlank() || s.clientPhone.isBlank()) {
            _state.value = s.copy(error = "Заполните имя и телефон")
            return
        }
        val time = if (s.startTime.isNotBlank()) {
            s.startTime.take(minOf(19, s.startTime.length))
        } else {
            "${s.selectedDate}T${s.selectedTime}:00"
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                val r = api.createAppointment(
                    AppointmentRequest(
                        s.clientName, s.clientPhone,
                        s.selectedMaster!!.id, s.selectedService!!.id, time,
                        clientFcmToken = fcmToken
                    )
                )
                if (r.isSuccessful) {
                    val prefs = app.getSharedPreferences("client_info", Context.MODE_PRIVATE)
                    prefs.edit { putString("name", s.clientName).putString("phone", s.clientPhone) }
                    _state.value = _state.value.copy(isLoading = false, showConfirm = false)
                    loadMyAppointments()
                } else {
                    _state.value = _state.value.copy(error = "Ошибка записи", isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка: ${e.message}", isLoading = false)
            }
        }
    }

    fun cancelAppointment(id: Long) {
        viewModelScope.launch {
            try {
                val phone = _state.value.clientPhone.replace(Regex("[^0-9+]"), "")
                api.cancelAppointment(id, phone)
                loadMyAppointments()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка отмены: ${e.message}")
            }
        }
    }

    fun getNextSlot() {
        val s = _state.value
        if (s.salonInfo == null || s.selectedService == null) return
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
            try {
                val r = api.getNextSlot(
                    s.salonInfo.companyId,
                    s.selectedService.id,
                    s.selectedMaster?.id
                )
                if (r.isSuccessful) {
                    val body = r.body()!!
                    if (body.containsKey("error")) {
                        _state.value = s.copy(error = body["error"].toString(), isLoading = false)
                    } else {
                        _state.value = s.copy(
                            selectedMaster = s.salonInfo.masters.find { it.id == (body["masterId"] as? Number)?.toLong() },
                            selectedDate = body["date"].toString(),
                            selectedTime = body["time"].toString(),
                            startTime = body["startTime"].toString(),
                            isLoading = false,
                            showConfirm = true
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = s.copy(error = "Ошибка: ${e.message}", isLoading = false)
            }
        }
    }

    fun hideConfirm() {
        _state.value = _state.value.copy(
            showConfirm = false,
            selectedService = null,
            selectedMaster = null,
            selectedDate = null,
            selectedTime = null
        )
    }

    fun backToSalon() {
        val info = _state.value.salonInfo
        _state.value = ClientUiState(
            clientName = _state.value.clientName,
            clientPhone = _state.value.clientPhone,
            salonId = _state.value.salonId,
            salonInfo = info
        )
    }

    fun showConfirmation() {
        val s = _state.value
        if (s.clientName.isBlank() || s.clientPhone.isBlank()) {
            _state.value = s.copy(error = "Заполните имя и телефон")
            return
        }
        if (s.selectedService == null || s.selectedMaster == null || s.selectedDate == null || s.selectedTime == null) {
            _state.value = s.copy(error = "Выберите услугу, мастера, дату и время")
            return
        }
        _state.value = s.copy(showConfirm = true, error = null)
    }

    fun reset() {
        _state.value = ClientUiState()
    }



    fun getCalendarDays(): List<CalendarDay> {
        val year = _state.value.calendarYear
        val month = _state.value.calendarMonth
        val today = LocalDate.now()
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
        val startDow = if (firstDay.dayOfWeek.value == 7) 6 else firstDay.dayOfWeek.value - 1

        val master = _state.value.selectedMaster
        // 1. Получаем шаблон недели (JSON-строка)
        val weekTemplate = _state.value.salonInfo?.masters?.find { it.id == master?.id }?.weekScheduleTemplate
            ?: ""

        // 2. Парсим JSON-строку в Map<Int, Boolean> (день недели -> рабочий/выходной)
        val weekDayStatus = parseWeekTemplate(weekTemplate)

        val days = mutableListOf<CalendarDay>()
        repeat(startDow) { days.add(CalendarDay("", "", false, true)) }

        var d = firstDay
        while (!d.isAfter(lastDay)) {
            val dateStr = d.toString()
            val dayOfWeek = d.dayOfWeek.value // 1=Пн, 2=Вт ... 7=Вс

            val isPast = d.isBefore(today) || (d == today && LocalTime.now().hour >= 20)

            // 3. Проверяем по шаблону: если в шаблоне нет записи или isWorking == true — день рабочий
            val isWorking = weekDayStatus[dayOfWeek] ?: true

            days.add(CalendarDay(dateStr, d.dayOfMonth.toString(), !isPast && isWorking, false))
            d = d.plusDays(1)
        }

        while (days.size % 7 != 0) {
            days.add(CalendarDay("", "", false, true))
        }

        return days
    }

    // 4. Вспомогательная функция для парсинга JSON-шаблона
    private fun parseWeekTemplate(template: String): Map<Int, Boolean> {
        if (template.isBlank()) return emptyMap()

        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val weekDays: List<Map<String, Any>> = Gson().fromJson(template, type)
            weekDays.associate {
                val dayOfWeek = (it["dayOfWeek"] as Number).toInt()
                val isWorking = it["isWorking"] as Boolean
                dayOfWeek to isWorking
            }
        } catch (e: Exception) {
            emptyMap() // если шаблон битый — считаем все дни рабочими
        }
    }



    fun nextMonth() {
        val m = _state.value.calendarMonth
        val y = _state.value.calendarYear
        if (m == 12) _state.value = _state.value.copy(calendarMonth = 1, calendarYear = y + 1)
        else _state.value = _state.value.copy(calendarMonth = m + 1)
    }

    fun prevMonth() {
        val m = _state.value.calendarMonth
        val y = _state.value.calendarYear
        val today = LocalDate.now()
        if (y < today.year || (y == today.year && m <= today.monthValue)) return // нельзя в прошлое
        if (m == 1) _state.value = _state.value.copy(calendarMonth = 12, calendarYear = y - 1)
        else _state.value = _state.value.copy(calendarMonth = m - 1)
    }

    data class CalendarDay(
        val date: String,
        val label: String,
        val enabled: Boolean,
        val empty: Boolean
    )

    fun showProfile() {
        _state.value = _state.value.copy(showMyAppointments = false, showProfile = true)
    }

    fun hideProfile() {
        _state.value = _state.value.copy(showProfile = false)
    }

    fun showNamePrompt() {
        _state.value = _state.value.copy(showNamePrompt = true)
    }

    fun saveName() {
        val name = _state.value.clientName
        if (name.isBlank()) return

        app.getSharedPreferences("client_info", Context.MODE_PRIVATE).edit {
            putString("name", name)
        }

        viewModelScope.launch {
            try {
                val phone = _state.value.clientPhone.replace(Regex("[^0-9+]"), "")
                api.updateClientName(mapOf("phone" to phone, "name" to name))
            } catch (_: Exception) {
            }
        }

        _state.value = _state.value.copy(showNamePrompt = false)
    }

    @SuppressLint("UseKtx")
    fun checkBeforeDelete() {
        viewModelScope.launch {
            try {
                val phone = _state.value.clientPhone.replace(Regex("[^0-9+]"), "")
                val response = api.deleteClient(phone)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.get("status") == "error") {
                        _state.value = _state.value.copy(deleteError = body["message"]?.toString())
                    } else {
                        val app = getApplication<Application>()
                        app.getSharedPreferences("verify_prefs", Context.MODE_PRIVATE).edit()
                            .clear().apply()
                        app.getSharedPreferences("client_info", Context.MODE_PRIVATE).edit {
                            clear()
                        }
                        _state.value = _state.value.copy(accountDeleted = true)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun clearDeleteError() {
        _state.value = _state.value.copy(deleteError = null)
    }

    fun saveProfile() {
        val s = _state.value
        viewModelScope.launch {
            try {
                val phone = s.clientPhone.replace(Regex("[^0-9+]"), "")
                api.updateClientName(mapOf("phone" to phone, "name" to s.clientName))
            } catch (_: Exception) {
            }
        }
        _state.value = _state.value.copy(showProfile = false)
    }
}