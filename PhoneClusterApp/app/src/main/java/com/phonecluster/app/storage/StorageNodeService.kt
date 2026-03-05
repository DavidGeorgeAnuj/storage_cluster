package com.phonecluster.app.storage

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.phonecluster.app.R
import com.phonecluster.app.utils.heartbeat.HeartbeatManager
import com.phonecluster.app.utils.websocket.WebSocketManager

class StorageNodeService : Service() {

    private val CHANNEL_ID = "StorageNodeChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val deviceId = intent?.getIntExtra("deviceId", -1) ?: -1

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketCluster Node Running")
            .setContentText("Listening for chunk transfers...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        if (deviceId != -1) {
            HeartbeatManager.start(
                serverBaseUrl = "http://10.0.2.2:8000",
                deviceId = deviceId
            )

            WebSocketManager.connect(
                context = this,
                serverIp = "10.0.2.2"
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        HeartbeatManager.stop()
        WebSocketManager.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startNode() {
        // TODO: Start your socket server / websocket / heartbeat here
    }

    private fun stopNode() {
        // TODO: Stop your networking logic safely here
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Storage Node Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}