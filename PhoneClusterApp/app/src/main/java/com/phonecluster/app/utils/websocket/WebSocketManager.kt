package com.phonecluster.app.utils.websocket

import android.content.Context
import android.util.Log
import com.phonecluster.app.storage.PreferencesManager
import com.phonecluster.app.utils.DeviceInfoProvider
import org.json.JSONObject

object WebSocketManager {

    private var client: DeviceWebSocketClient? = null
    private var isConnected = false

    fun connect(context: Context, serverIp: String) {

        if (isConnected) {
            Log.d("WS_DEBUG", "Already connected, skipping")
            return
        }

        val deviceId = PreferencesManager.getDeviceId(context) ?: return

        val registerPayload = JSONObject().apply {
            put("type", "register")
            put("device_id", deviceId)
            put("fingerprint", DeviceInfoProvider.getDeviceFingerprint(context))
            put("device_name", DeviceInfoProvider.getDeviceName())
            put("storage_capacity", DeviceInfoProvider.getTotalStorageBytes())
            put("available_storage", DeviceInfoProvider.getAvailableStorageBytes())
        }

        val wsUrl = "ws://$serverIp:8000/ws/device"

        client = DeviceWebSocketClient(
            serverWsUrl = wsUrl,

            onOpenCallback = {
                Log.d("WS_DEBUG", "Connection established")
                isConnected = true
            },

            onMessageReceived = { msg ->
                handleServerMessage(msg)
            },

            onDisconnected = {
                Log.d("WS_DEBUG", "Disconnected from server")
                isConnected = false
            }
        )

        client?.connect(registerPayload)
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        isConnected = false
    }

    private fun handleServerMessage(msg: JSONObject) {
        Log.d("WS_DEBUG", "Handling message: $msg")

        when (msg.optString("type")) {
            "ready" -> {
                Log.d("WS_DEBUG", "Server acknowledged connection")
            }

            "STORE_CHUNK" -> {
                Log.d("WS_DEBUG", "Received STORE_CHUNK command")
            }

            else -> {
                Log.d("WS_DEBUG", "Unknown message type")
            }
        }
    }

    fun sendAck(taskId: String) {
        val ack = JSONObject().apply {
            put("type", "cmd_ack")
            put("task_id", taskId)
        }
        client?.send(ack)
    }
}