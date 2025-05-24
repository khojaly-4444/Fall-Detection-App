package com.falldetect.falldetection.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.falldetect.falldetection.MainActivity
import com.falldetect.falldetection.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        saveTokenToFirebase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Message received from: ${remoteMessage.from}")


        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Data Payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Fall Alert!"
            val message = remoteMessage.data["message"] ?: "A fall has been detected."
            Log.d("FCM", "Notification received on: ${FirebaseAuth.getInstance().currentUser?.uid}")
            sendNotification(title, message)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "fall_detection_alerts"

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_fall_notification) // Replace with actual icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a unique notification ID (prevents overwriting notifications)
        val notificationId = Random().nextInt(100000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Fall Detection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun saveTokenToFirebase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        userRef.child("fcmToken").setValue(token)
            .addOnSuccessListener {
                Log.d("FCM", "Token saved to Firebase successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to save token", e)
            }
    }
}
