package ru.bookmaster.client.data.api

import retrofit2.Response
import retrofit2.http.*
import ru.bookmaster.client.data.model.AppointmentRequest
import ru.bookmaster.client.data.model.SalonInfo

interface BookMasterClientApi {
    @GET("salon/api/{salonId}")
    suspend fun getSalonInfo(@Path("salonId") salonId: Long): Response<SalonInfo>

    @POST("api/appointments")
    suspend fun createAppointment(@Body request: AppointmentRequest): Response<Unit>

    @GET("api/appointments/slots/{masterId}")
    suspend fun getBookedSlots(@Path("masterId") masterId: Long, @Query("date") date: String): Response<List<String>>
}