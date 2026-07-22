package com.checkin.partner.network.ws

import android.util.Log
import com.checkin.partner.BuildConfig
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * Socket.IO 风格 WebSocket（简化版，用 OkHttp 原生 WebSocket）
 * 事件协议：emit("event:name", jsonData)
 */
class WebSocketManager {

    private var ws: WebSocket? = null
    private var userId: String? = null
    private val gson = Gson()
    private val listeners = mutableMapOf<String, MutableList<(String) -> Unit>>()
    private var reconnectAttempts = 0

    fun connect(userId: String) {
        this.userId = userId
        reconnectAttempts = 0
        doConnect()
    }

    private fun doConnect() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // 不超时
            .build()

        val wsUrl = BuildConfig.WS_URL.trimEnd('/')

        // Socket.IO 握手
        val request = Request.Builder()
            .url("${wsUrl}/socket.io/?EIO=4&transport=websocket")
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS", "已连接")
                reconnectAttempts = 0
                // Socket.IO 协议：40 表示认证
                userId?.let {
                    sendRaw("40")
                    emit("auth:login", """{"userId":"$it"}""")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseSocketIOMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "连接失败: ${t.message}")
                reconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS", "已关闭")
                reconnect()
            }
        })
    }

    private fun parseSocketIOMessage(text: String) {
        // Socket.IO 格式: "42[\"event:name\", {...}]"
        try {
            if (text.startsWith("42")) {
                val json = text.substring(2)
                val arr = gson.fromJson(json, Array::class.java)
                if (arr.size >= 2) {
                    val event = arr[0] as? String ?: return
                    val data = arr[1]?.toString() ?: "{}"
                    dispatch(event, data)
                }
            }
        } catch (e: Exception) {
            Log.w("WS", "解析失败: ${e.message}")
        }
    }

    private fun dispatch(event: String, data: String) {
        listeners[event]?.forEach { it(data) }
    }

    fun on(event: String, handler: (String) -> Unit) {
        listeners.getOrPut(event) { mutableListOf() }.add(handler)
    }

    fun emit(event: String, data: String) {
        val msg = "42${gson.toJson(arrayOf(event, gson.fromJson(data, Any::class.java)))}"
        sendRaw(msg)
    }

    private fun sendRaw(text: String) {
        ws?.send(text)
    }

    private fun reconnect() {
        if (reconnectAttempts >= 10) return
        reconnectAttempts++
        Thread.sleep(3000L * reconnectAttempts.coerceAtMost(5))
        doConnect()
    }

    fun disconnect() {
        ws?.close(1000, "用户断开")
        ws = null
        listeners.clear()
    }

    fun isConnected(): Boolean = ws != null
}
