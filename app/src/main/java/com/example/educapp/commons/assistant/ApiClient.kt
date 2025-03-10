package com.example.educapp.commons.assistant

import com.example.educapp.commons.assistant.network.AssistantRepositoryImpl.GenerationRequest
import com.example.educapp.commons.assistant.network.ChatCompletionRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

object ApiClient {
    // Change to your Cloudflare worker URL
    private const val BASE_URL = "https://fragrant-bar-6c1d.edmedina1990.workers.dev/"
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    // Update Retrofit instance
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    // Add new Cloudflare service
    val cloudflareService: CloudflareService by lazy {
        retrofit.create(CloudflareService::class.java)
    }
}

// New interface for Cloudflare
interface CloudflareService {
    @POST("/")
    suspend fun generateContent(
        @Header("Authorization") authToken: String,
        @Body request: GenerationRequest
    ): DeepSeekResponse

    @POST("/")
    @Streaming
    suspend fun streamChatCompletion(
        @Header("Authorization") authToken: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
}