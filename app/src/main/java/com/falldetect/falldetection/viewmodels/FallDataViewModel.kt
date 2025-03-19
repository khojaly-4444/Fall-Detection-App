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

    // StateFlow to observe fall data updates
    private val _fallData = MutableStateFlow<List<FallEvent>>(emptyList())
    val fallData: StateFlow<List<FallEvent>> = _fallData

    // Function to fetch fall data from Firebase
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
            fetchFallData() // Refresh the UI with the new event
        }
    }

    // Function to add a mock fall event (used for testing)
    fun addMockFallEvent(userId: String) {
        viewModelScope.launch {
            val mockFallEvent = FallEvent(
                fallType = "Hard Fall",
                date = getCurrentDate(),
                time = getCurrentTime(),
                heartRate = (60..120).random().toString() // Random heart rate for testing
            )

            firebaseRepository.addFallEvent(mockFallEvent)
            fetchFallData() // Refresh UI with new event
        }
    }

    // Function to start listening for real-time fall events
    fun startListeningForFallEvents(context: Context) {
        firebaseRepository.listenForFallEvents { fallEvent ->
            sendNotification(context, fallEvent)  // Notify the user on fall detection
            fetchFallData() //  Refresh UI with new fall data
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use actual notification icon
            .setContentTitle("Fall Detected!")
            .setContentText("Type: ${fallEvent.fallType}, Heart Rate: ${fallEvent.heartRate} BPM")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // Function to get the current date as a string
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Function to get the current time as a string
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}
