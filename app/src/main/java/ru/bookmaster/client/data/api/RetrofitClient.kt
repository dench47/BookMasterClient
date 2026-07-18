package ru.bookmaster.client.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
//    private const val BASE_URL = "http://192.168.0.152:8080/"
    const val BASE_URL = "http://10.144.206.231:8080/"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Основной клиент для обычных запросов
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Клиент для "мои записи" с меньшим таймаутом (5 секунд)
    private val quickOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val instance: BookMasterClientApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BookMasterClientApi::class.java)
    }

    // Отдельный экземпляр для быстрых запросов
    val quickInstance: BookMasterClientApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(quickOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BookMasterClientApi::class.java)
    }
}