package ru.bookmaster.client.data.model

data class AppointmentRequest(
    val clientName: String,
    val clientPhone: String,
    val masterId: Long,
    val serviceId: Long,
    val startTime: String
)