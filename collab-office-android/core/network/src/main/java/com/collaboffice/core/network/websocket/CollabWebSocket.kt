package com.collaboffice.core.network.websocket

import com.collaboffice.core.model.ws.MessageType
import com.collaboffice.core.model.ws.WebSocketMessage
import com.collaboffice.core.network.auth.TokenManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
}

@Singleton
class CollabWebSocket @Inject constructor(
    private val tokenManager: TokenManager,
    private val moshi: Moshi
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val wsAdapter = moshi.adapter(WebSocketMessage::class.java)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<WebSocketMessage>(replay = 0, extraBufferCapacity = 100)
    val messages: SharedFlow<WebSocketMessage> = _messages.asSharedFlow()

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val reconnectPolicy = ReconnectPolicy()

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    fun connect(host: String, port: Int) {
        val token = tokenManager.accessToken ?: return
        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url("ws://$host:$port/ws?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
                reconnectPolicy.reset()
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val message = wsAdapter.fromJson(text) ?: return
                    scope.launch { _messages.emit(message) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                handleDisconnect(host, port)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                handleDisconnect(host, port)
            }
        })
    }

    fun send(message: WebSocketMessage): Boolean {
        val json = wsAdapter.toJson(message) ?: return false
        return webSocket?.send(json) ?: false
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000)
                send(WebSocketMessage(
                    type = MessageType.HEARTBEAT.code,
                    clientTimestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    private fun handleDisconnect(host: String, port: Int) {
        heartbeatJob?.cancel()
        _connectionState.value = ConnectionState.RECONNECTING

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = reconnectPolicy.nextDelay()
            delay(delay)
            if (tokenManager.isLoggedIn) {
                connect(host, port)
            }
        }
    }
}
