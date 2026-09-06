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

    // 拦截器在 OkHttp 线程读写，主线程在登录/登出时写，需要 volatile 保证可见性
    @Volatile
    var token: String? = null

    // token 失效（401）回调，由上层清登录态并跳转登录页。
    // 登录/注册接口的 401 是"用户名或密码错误"，不算会话过期。
    var onSessionExpired: (() -> Unit)? = null

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

        val sessionInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                val path = response.request.url.encodedPath
                val isAuthRequest = path.endsWith("/login") || path.endsWith("/register")
                if (!isAuthRequest && token != null) {
                    clearToken()
                    onSessionExpired?.invoke()
                }
            }
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
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
