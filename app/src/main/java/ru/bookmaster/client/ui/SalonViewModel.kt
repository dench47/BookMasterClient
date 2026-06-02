package ru.bookmaster.client.ui

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
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
    val bookedSlots: List<String> = emptyList(),
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
    val breakAfterMinutes: Int = 0
)

class SalonViewModel(application: Application) : AndroidViewModel(application) {

    private val api = RetrofitClient.instance
    private val app = application
    private val _state = MutableStateFlow(ClientUiState())
    val state = _state.asStateFlow()

    init {
        loadSavedClientInfo()
    }

    fun loadSavedClientInfo() {
        val prefs = app.getSharedPreferences("client_info", android.content.Context.MODE_PRIVATE)
        val savedName = prefs.getString("name", "")
        val savedPhone = prefs.getString("phone", "")
        if (!savedName.isNullOrBlank()) _state.value = _state.value.copy(clientName = savedName)
        if (!savedPhone.isNullOrBlank()) _state.value = _state.value.copy(clientPhone = savedPhone)
    }

    fun onSalonIdChange(id: String) { _state.value = _state.value.copy(salonId = id) }
    fun onNameChange(n: String) { _state.value = _state.value.copy(clientName = n) }
    fun onPhoneChange(p: String) { _state.value = _state.value.copy(clientPhone = p) }
    fun hideMyAppointments() { _state.value = _state.value.copy(showMyAppointments = false) }

    fun loadMyAppointments() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await() ?: ""
                val r = api.getMyAppointments(fcmToken)
                if (r.isSuccessful) {
                    val apps = r.body()?.filter {
                        val dt = LocalDateTime.parse(it.startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        dt.isAfter(LocalDateTime.now()) && it.cancelled != true
                    } ?: emptyList()
                    _state.value = _state.value.copy(myAppointments = apps, showMyAppointments = true, isLoading = false)
                } else {
                    _state.value = _state.value.copy(error = "Записи не найдены", isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка: ${e.message}", isLoading = false)
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

    fun selectService(s: ServiceDto) { _state.value = _state.value.copy(selectedService = s, selectedDate = null, selectedTime = null) }
    fun selectMaster(m: MasterDto?) {
        _state.value = _state.value.copy(
            selectedMaster = m,
            selectedDate = null,
            selectedTime = null,
            workStart = "09:00",
            workEnd = "18:00"
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
                            workStart = "00:00",
                            workEnd = "00:00",
                            timeStep = 30,
                            stickTime = false,
                            breakAfterMinutes = 0,
                            loadingWorkHours = false
                        )
                    } else {
                        val timeStep = (body["timeStep"]?.toString() ?: "30").toIntOrNull() ?: 30
                        _state.value = _state.value.copy(
                            workStart = body["workStart"]?.toString() ?: "09:00",
                            workEnd = body["workEnd"]?.toString() ?: "18:00",
                            timeStep = timeStep,
                            stickTime = body["stickTime"]?.toString()?.toBooleanStrictOrNull() ?: false,
                            breakAfterMinutes = (body["breakAfter"]?.toString()?.toIntOrNull() ?: 0),
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

    fun selectTime(time: String) { _state.value = _state.value.copy(selectedTime = time) }

    private fun loadSlots(date: String) {
        val masterId = _state.value.selectedMaster?.id ?: return
        viewModelScope.launch {
            try {
                val r = api.getBookedSlots(masterId, date)
                if (r.isSuccessful) _state.value = _state.value.copy(bookedSlots = r.body() ?: emptyList())
            } catch (_: Exception) { }
        }
    }

    fun getNextDays(): List<String> {
        val days = listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб")
        val months = listOf("янв","фев","мар","апр","мая","июн","июл","авг","сен","окт","ноя","дек")
        val now = LocalDate.now()
        val start = if (LocalTime.now().hour >= 20) 1 else 0
        return (start..start+6).map {
            val d = now.plusDays(it.toLong())
            "${days[d.dayOfWeek.value % 7]} ${d.dayOfMonth} ${months[d.monthValue-1]}|$d"
        }
    }

    fun getTimeSlots(): List<String> {
        val state = _state.value
        state.selectedMaster ?: return emptyList()
        val date = state.selectedDate ?: return emptyList()

        val workStart = state.workStart
        val workEnd = state.workEnd
        val timeStep = state.timeStep
        val stickTime = state.stickTime
        val breakAfter = state.breakAfterMinutes
        val serviceDuration = state.selectedService?.durationMinutes ?: 30
        val startH = workStart.split(":")[0].toInt()
        val endH = workEnd.split(":")[0].toInt()

        // Прилипание: вычисляем занятые минуты
        val bookedMinutes = state.bookedSlots
            .mapNotNull { bs ->
                val parts = bs.split(" ")
                if (parts.size < 2 || parts[0] != date) return@mapNotNull null
                val t = parts[1].split(":").map { it.toInt() }
                t[0] * 60 + t[1]
            }
            .sorted()

        val slots = mutableListOf<String>()
        for (h in startH until endH) {
            var mnt = 0
            while (mnt < 60) {
                val time = "${h.toString().padStart(2,'0')}:${mnt.toString().padStart(2,'0')}"
                val full = "$date $time"
                val slotMin = h * 60 + mnt
                val slotEnd = slotMin + serviceDuration

                // Занятость
                val isBooked = state.bookedSlots.any { it.startsWith(full) }
                if (isBooked) { mnt += timeStep; continue }

                // Прилипание
                if (stickTime && bookedMinutes.isNotEmpty()) {
                    var stickOk = false
                    val first = bookedMinutes.first()
                    val last = bookedMinutes.last()

                    // Первый слот после последней записи
                    if (slotMin == last + 30 + breakAfter) stickOk = true
                    // Слот до первой записи с зазором < timeStep
                    if (slotEnd <= first && (first - slotEnd) < timeStep) stickOk = true
                    // Между двумя
                    for (i in 0 until bookedMinutes.size - 1) {
                        val a = bookedMinutes[i]
                        val b = bookedMinutes[i + 1]
                        if (slotMin >= a + 30 + breakAfter && slotEnd <= b) {
                            if ((slotMin - a - 30) < timeStep && (b - slotEnd) < timeStep) {
                                stickOk = true
                                break
                            }
                        }
                    }
                    if (!stickOk) { mnt += timeStep; continue }
                }

                slots.add(time)
                mnt += timeStep
            }
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
                val r = api.createAppointment(AppointmentRequest(
                    s.clientName, s.clientPhone,
                    s.selectedMaster!!.id, s.selectedService!!.id, time,
                    clientFcmToken = fcmToken
                ))
                if (r.isSuccessful) {
                    val prefs = app.getSharedPreferences("client_info", android.content.Context.MODE_PRIVATE)
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
                val fcmToken = FirebaseMessaging.getInstance().token.await() ?: ""
                api.cancelAppointment(id, fcmToken)
                loadMyAppointments()
            } catch (_: Exception) { }
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
                        // Сохраняем найденный слот и переходим к подтверждению
                        _state.value = s.copy(
                            selectedMaster = s.salonInfo.masters.find { it.id == (body["masterId"] as? Number)?.toLong() },                            selectedDate = body["date"].toString(),
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
        _state.value = _state.value.copy(showConfirm = false,
            selectedService = null,
            selectedMaster = null,
            selectedDate = null,
            selectedTime = null)
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

    fun reset() { _state.value = ClientUiState() }

    fun getCalendarDays(): List<CalendarDay> {
        val year = _state.value.calendarYear
        val month = _state.value.calendarMonth
        val today = LocalDate.now()
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
        val startDow = if (firstDay.dayOfWeek.value == 7) 6 else firstDay.dayOfWeek.value - 1
        val master = _state.value.selectedMaster
        val workingDays = _state.value.salonInfo?.masters
            ?.find { it.id == master?.id }?.workingDays ?: emptyList()

        val days = mutableListOf<CalendarDay>()
        repeat(startDow) { days.add(CalendarDay("", "", false, true)) }

        var d = firstDay
        while (!d.isAfter(lastDay)) {
            val dateStr = d.toString()
            val dow = listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб")[(d.dayOfWeek.value % 7)]
            val isPast = d.isBefore(today) || (d == today && LocalTime.now().hour >= 20)
            val isWorking = workingDays.contains(dateStr)
            days.add(CalendarDay(dateStr, d.dayOfMonth.toString(), !isPast && isWorking, false))
            d = d.plusDays(1)
        }
        while (days.size % 7 != 0) {
            days.add(CalendarDay("", "", false, true))
        }
        return days
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
        if (m == 1) _state.value = _state.value.copy(calendarMonth = 12, calendarYear = y - 1)
        else _state.value = _state.value.copy(calendarMonth = m - 1)
    }

    data class CalendarDay(
        val date: String,
        val label: String,
        val enabled: Boolean,
        val empty: Boolean
    )
}