package com.checkin.partner.network.api

import android.content.Context
import com.checkin.partner.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var baseUrl: String = BuildConfig.BASE_URL
    var token: String? = null

    private lateinit var api: ApiService
    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE)
        token = prefs.getString("jwt_token", null)
        val savedUrl = prefs.getString("base_url", null)
        if (savedUrl != null) baseUrl = savedUrl

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
            token?.let { request.addHeader("Authorization", "Bearer $it") }
            chain.proceed(request.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun getApi(): ApiService = api

    fun saveToken(newToken: String, userId: String? = null, username: String? = null) {
        token = newToken
        prefs.edit().putString("jwt_token", newToken).apply()
        userId?.let { prefs.edit().putString("user_id", it).apply() }
        username?.let { prefs.edit().putString("username", it).apply() }
    }

    fun clearToken() {
        token = null
        prefs.edit().remove("jwt_token").remove("user_id").remove("username").apply()
    }

    fun getSavedUserId(): String? = prefs.getString("user_id", null)
    fun getSavedUsername(): String? = prefs.getString("username", null)

    fun isLoggedIn(): Boolean = !token.isNullOrEmpty()

    fun getBaseUrl(): String = baseUrl
}
