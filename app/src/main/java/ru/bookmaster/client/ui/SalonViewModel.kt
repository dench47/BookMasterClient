package ru.bookmaster.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.bookmaster.client.data.api.RetrofitClient
import ru.bookmaster.client.data.model.*
import java.time.LocalDate
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
    val isSuccess: Boolean = false
)

class SalonViewModel : ViewModel() {
    private val api = RetrofitClient.instance
    private val _state = MutableStateFlow(ClientUiState())
    val state = _state.asStateFlow()

    fun onSalonIdChange(id: String) { _state.value = _state.value.copy(salonId = id) }
    fun onNameChange(n: String) { _state.value = _state.value.copy(clientName = n) }
    fun onPhoneChange(p: String) { _state.value = _state.value.copy(clientPhone = p) }

    fun loadSalon() {
        val id = _state.value.salonId.toLongOrNull() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val r = api.getSalonInfo(id)
                if (r.isSuccessful) _state.value = _state.value.copy(salonInfo = r.body(), isLoading = false)
                else _state.value = _state.value.copy(error = "Салон не найден", isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка: ${e.message}", isLoading = false)
            }
        }
    }

    fun selectService(s: ServiceDto) { _state.value = _state.value.copy(selectedService = s, selectedDate = null, selectedTime = null) }
    fun selectMaster(m: MasterDto) { _state.value = _state.value.copy(selectedMaster = m, selectedDate = null, selectedTime = null) }
    fun selectDate(date: String) {
        _state.value = _state.value.copy(selectedDate = date, selectedTime = null)
        loadSlots(date)
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
            "${days[d.dayOfWeek.value % 7]} ${d.dayOfMonth} ${months[d.monthValue-1]}|${d.toString()}"
        }
    }

    fun getTimeSlots(): List<String> {
        val info = _state.value.salonInfo ?: return emptyList()
        val master = _state.value.selectedMaster ?: return emptyList()
        val m = info.masters.find { it.id == master.id } ?: return emptyList()
        val workStart = m.workStart?.take(5) ?: "09:00"
        val workEnd = m.workEnd?.take(5) ?: "18:00"
        val startH = workStart.split(":")[0].toInt()
        val endH = workEnd.split(":")[0].toInt()
        val slots = mutableListOf<String>()
        for (h in startH until endH) {
            for (mnt in 0..30 step 30) {
                val time = "${h.toString().padStart(2,'0')}:${mnt.toString().padStart(2,'0')}"
                val full = "${_state.value.selectedDate} $time"
                if (!_state.value.bookedSlots.any { it.startsWith(full) }) slots.add(time)
            }
        }
        // Убираем прошедшее время если сегодня
        if (_state.value.selectedDate == LocalDate.now().toString()) {
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
        if (s.clientName.isBlank() || s.clientPhone.isBlank()) {
            _state.value = s.copy(error = "Заполните имя и телефон")
            return
        }
        val startTime = "${s.selectedDate}T${s.selectedTime}:00"
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            try {
                val r = api.createAppointment(AppointmentRequest(
                    s.clientName, s.clientPhone,
                    s.selectedMaster!!.id, s.selectedService!!.id, startTime
                ))
                if (r.isSuccessful) _state.value = _state.value.copy(isLoading = false, isSuccess = true, selectedDate = null, selectedTime = null, clientName = "", clientPhone = "")
                else _state.value = _state.value.copy(error = "Ошибка записи", isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка: ${e.message}", isLoading = false)
            }
        }
    }
}