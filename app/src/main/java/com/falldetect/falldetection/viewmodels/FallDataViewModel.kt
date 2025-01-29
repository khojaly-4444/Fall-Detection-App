package com.falldetect.falldetection.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falldetect.falldetection.models.FallEvent
import com.falldetect.falldetection.repositories.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FallDataViewModel(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    // Stateflow to manage and observe fall data state
    private val _fallData = MutableStateFlow<List<FallEvent>>(emptyList())
    val fallData: StateFlow<List<FallEvent>> = _fallData

    // Function to fetch fall data
    fun fetchFallData() {
        viewModelScope.launch {
            val result = firebaseRepository.getFallData()
            _fallData.value = result
        }
    }
    // Function to add fall data
    fun addFallEvent(fallEvent: FallEvent) {
        viewModelScope.launch {
            val currentUserId = firebaseRepository.getCurrentUserId() // ✅ Get user ID
            if (currentUserId != null) {
                firebaseRepository.addFallEvent(currentUserId, fallEvent) // ✅ Now it passes userId
                fetchFallData()
            } else {
                Log.e("FallDataViewModel", "Error: User ID is null")
            }
        }
    }
    // Function to add mock fall event
    fun addMockFallEvent(userId: String) {
        viewModelScope.launch {
            val mockFallEvent = FallEvent(
                fallType = "Hard Fall",
                date = getCurrentDate(),
                time = getCurrentTime(),
                heartRate = (60..120).random().toString() // Random heart rate for variety
            )

            firebaseRepository.addFallEvent(userId, mockFallEvent)
            fetchFallData()  // Refresh data to reflect the new event
        }
    }

    // Function to get the current date
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Function to get the current time
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }


}