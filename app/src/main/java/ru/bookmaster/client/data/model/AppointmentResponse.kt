package ru.bookmaster.client.data.model

data class AppointmentResponse(
    val id: Long,
    val clientName: String,
    val clientPhone: String,
    val masterName: String,
    val serviceName: String,
    val startTime: String,
    val confirmed: Boolean?,
    val cancelled: Boolean?,
    val salonName: String? = null
)