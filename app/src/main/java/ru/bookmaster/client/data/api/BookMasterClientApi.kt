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
        @Header("X-Client-Phone") phone: String
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

    @PUT("api/clients/update-name")
    suspend fun updateClientName(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("api/clients/by-phone")
    suspend fun getClientByPhone(@Query("phone") phone: String): Response<Map<String, Any>>

    @POST("api/waiting-list")
    suspend fun addToWaitingList(@Body body: Map<String, String>): Response<Map<String, String>>

    @DELETE("api/device/unregister-client")
    suspend fun unregisterClient(@Query("phone") phone: String): Response<Unit>

    // Waiting List
    @POST("api/waiting-list/client")
    suspend fun addToWaitingListClient(@Body body: Map<String, String>): Response<Map<String, String>>

    @GET("api/waiting-list/client/{phone}")
    suspend fun getWaitingListByPhone(@Path("phone") phone: String): Response<List<Map<String, String>>>

    @DELETE("api/waiting-list/client/{phone}")
    suspend fun deleteAllWaitingEntries(@Path("phone") phone: String): Response<Map<String, String>>

    @POST("api/waiting-list/{id}/accept")
    suspend fun acceptWaitingOffer(@Path("id") id: Long): Response<Map<String, Any>>

    @POST("api/waiting-list/{id}/decline")
    suspend fun declineWaitingOffer(@Path("id") id: Long): Response<Map<String, Any>>

}
