package com.meya.doumenculture.data.network

import com.google.gson.GsonBuilder
import com.meya.doumenculture.data.local.TokenManager
import com.meya.doumenculture.data.model.Venue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

//    private const val BASE_URL = "https://meyastudio.cn/api/"
private const val BASE_URL = "http://10.0.2.2:8000/api/"
    private var tokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Venue::class.java, VenueDeserializer())
            .create()
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    addHeader("Accept", "application/json")
                    addHeader("Content-Type", "application/json")
                    val token = runBlocking { tokenManager?.token?.first() }
                    if (!token.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }.build()
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
