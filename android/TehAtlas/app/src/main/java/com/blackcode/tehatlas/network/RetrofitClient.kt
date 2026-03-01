package com.blackcode.tehatlas.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client with JWT auth interceptor.
 *
 * Change BASE_URL to your Go server address:
 *   - Emulator → "http://10.0.2.2:8080/api/"   (localhost from emulator)
 *   - Real device same network → "http://<PC_IP>:8080/api/"
 *   - Via Nginx → "http://<PC_IP>/api/"
 */
object RetrofitClient {

    // Default to emulator localhost. Change for real device.
    var BASE_URL = "http://10.0.2.2:8080/api/"

    private var sessionManager: SessionManager? = null
    private var apiService: ApiService? = null

    fun init(sessionManager: SessionManager, baseUrl: String? = null) {
        this.sessionManager = sessionManager
        if (baseUrl != null) BASE_URL = baseUrl
        apiService = null // force rebuild
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            apiService = buildApiService()
        }
        return apiService!!
    }

    private fun buildApiService(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = sessionManager?.getToken()
            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .build()
            } else {
                original.newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
            }
            
            val response = chain.proceed(request)
            
            if (response.code == 401) {
                // Token expired or invalid - clear session and redirect to login
                sessionManager?.let { sm ->
                    sm.clearSession()
                    refresh() // Clear cached API service
                    
                    // Redirect to MainActivity (Login) from the main thread
                    val context = sm.context
                    val intent = android.content.Intent(context, com.blackcode.tehatlas.MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
            }
            
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /** Force rebuild after token change (login/logout) */
    fun refresh() {
        apiService = null
    }
}
