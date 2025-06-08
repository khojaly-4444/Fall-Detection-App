package com.falldetect.falldetection.viewmodels

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falldetect.falldetection.R
import com.falldetect.falldetection.models.FallEvent
import com.falldetect.falldetection.repositories.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FallDataViewModel(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    // Observes fall data updates
    private val _fallData = MutableStateFlow<List<FallEvent>>(emptyList())
    val fallData: StateFlow<List<FallEvent>> = _fallData

    // Fetch all fall data from Firebase (user + linked user)
    fun fetchFallData() {
        viewModelScope.launch {
            val result = firebaseRepository.getFallData()
            _fallData.value = result
        }
    }

    // Function to add a fall event and update the UI
    fun addFallEvent(fallEvent: FallEvent) {
        viewModelScope.launch {
            firebaseRepository.addFallEvent(fallEvent)
            fetchFallData()
        }
    }

    // No longer used
    fun startListeningForFallEvents(context: Context) {
        firebaseRepository.listenForFallEvents { fallEvent ->
            sendNotification(context, fallEvent)
            fetchFallData()
        }
    }

    // Function to send a local notification when a fall is detected
    private fun sendNotification(context: Context, fallEvent: FallEvent) {
        val channelId = "fall-alerts"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (Required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fall Detection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build and display the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Fall Detected!")
            .setContentText("Type: ${fallEvent.fallType}, Impact Intensity: ${fallEvent.impactIntensity} g")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Unique notification ID using current time
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

}
