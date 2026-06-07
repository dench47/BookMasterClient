package ru.bookmaster.client.data.api

import retrofit2.Response
import retrofit2.http.*
import ru.bookmaster.client.data.model.AppointmentRequest
import ru.bookmaster.client.data.model.AppointmentResponse
import ru.bookmaster.client.data.model.SalonInfo
import ru.bookmaster.client.data.model.SlotInfo

interface BookMasterClientApi {
    @GET("salon/api/{salonId}")
    suspend fun getSalonInfo(@Path("salonId") salonId: Long): Response<SalonInfo>

    @POST("api/appointments")
    suspend fun createAppointment(@Body request: AppointmentRequest): Response<Unit>

    @GET("api/appointments/slots/{masterId}")
    suspend fun getBookedSlots(
        @Path("masterId") masterId: Long,
        @Query("date") date: String
    ): Response<List<SlotInfo>>
    @POST("api/device/register-client")
    suspend fun registerClientToken(@Body body: Map<String, String>): Response<Unit>

    @GET("api/appointments/my")
    suspend fun getMyAppointments(@Query("phone") phone: String): Response<List<AppointmentResponse>>

    @POST("api/appointments/{id}/cancel-by-client")
    suspend fun cancelAppointment(
        @Path("id") id: Long,
        @Header("X-Client-Token") token: String
    ): Response<Unit>

    @GET("api/appointments/next-slot")
    suspend fun getNextSlot(
        @Query("salonId") salonId: Long,
        @Query("serviceId") serviceId: Long,
        @Query("masterId") masterId: Long?
    ): Response<Map<String, Any>>

    @GET("api/master/{id}/work-hours")
    suspend fun getWorkHours(
        @Path("id") masterId: Long,
        @Query("date") date: String
    ): Response<Map<String, String>>

    @POST("api/verify/send")
    suspend fun requestCallCheck(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("api/verify/check")
    suspend fun checkCallStatus(@Body body: Map<String, String>): Response<Map<String, Any>>

    @DELETE("api/appointments/client")
    suspend fun deleteClient(@Query("phone") phone: String): Response<Map<String, Any>>


}