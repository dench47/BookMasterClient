package ru.bookmaster.client.data.model

data class SalonInfo(
    val companyId: Long,
    val companyName: String,
    val masters: List<MasterDto>,
    val services: List<ServiceDto>,
    val isPremium: Boolean = false
)

data class MasterDto(
    val id: Long,
    val name: String,
    val specialization: String?,
    val workStart: String?,
    val workEnd: String?,
    val serviceIds: String? = null,
    val workingDays: List<String>? = null
)

data class ServiceDto(val id: Long, val name: String, val price: Double, val durationMinutes: Int)