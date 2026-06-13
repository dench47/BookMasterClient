package ru.bookmaster.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.bookmaster.client.data.api.RetrofitClient

data class VerifyUiState(
    val phone: String = "+7",
    val callPhone: String = "",
    val isLoading: Boolean = false,
    val isCalling: Boolean = false,
    val isVerified: Boolean = false,
    val error: String? = null
)

class VerifyViewModel : ViewModel() {
    private val api = RetrofitClient.instance
    private val _state = MutableStateFlow(VerifyUiState())
    val state = _state.asStateFlow()

    fun onPhoneChange(phone: String) {
        _state.value = _state.value.copy(phone = phone, error = null)
    }

    fun requestVerification() {
        val phone = _state.value.phone.replace(Regex("[^0-9+]"), "")
        if (phone.length < 10) {
            _state.value = _state.value.copy(error = "Введите корректный номер")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = api.requestCallCheck(mapOf("phone" to phone, "type" to "client"))
                if (response.isSuccessful) {
                    val body = response.body() ?: emptyMap()
                    if (body["status"] == "ok") {
                        if (body["already_verified"] == true) {
                            _state.value = _state.value.copy(isLoading = false, isVerified = true)
                        } else {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                callPhone = body["call_phone"]?.toString() ?: "",
                                isCalling = true
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = body["message"]?.toString() ?: "Ошибка запроса"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Ошибка: ${e.message}")
            }
        }
    }
    fun startChecking() {
        viewModelScope.launch {
            var attempts = 0
            while (attempts < 30 && !_state.value.isVerified) {
                delay(3000)
                attempts++
                try {
                    val response = api.checkCallStatus(mapOf(
                        "phone" to _state.value.phone.replace(Regex("[^0-9+]"), ""),
                        "type" to "client"  // ← добавить
                    ))
                    if (response.isSuccessful) {
                        val body = response.body() ?: emptyMap()
                        if (body["verified"] == true) {
                            _state.value = _state.value.copy(isVerified = true, isCalling = false)
                            return@launch
                        }
                    }
                } catch (_: Exception) { }
            }
            if (!_state.value.isVerified) {
                _state.value = _state.value.copy(
                    isCalling = false,
                    error = "Время верификации истекло. Попробуйте снова."
                )
            }
        }
    }

    fun reset() {
        _state.value = VerifyUiState()
    }
}