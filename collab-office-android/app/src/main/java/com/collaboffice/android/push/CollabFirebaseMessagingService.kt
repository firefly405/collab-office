package com.collaboffice.android.push

import android.util.Log
import com.collaboffice.core.common.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CollabFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_PUSH", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_PUSH", "Message received from: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "新消息"
            val content = remoteMessage.data["content"] ?: ""
            val sessionId = remoteMessage.data["sessionId"] ?: "general"
            
            NotificationHelper.showMessageNotification(
                context = applicationContext,
                title = title,
                message = content,
                sessionId = sessionId
            )
        }
    }
}
