package com.falldetect.falldetection.viewmodels

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

}