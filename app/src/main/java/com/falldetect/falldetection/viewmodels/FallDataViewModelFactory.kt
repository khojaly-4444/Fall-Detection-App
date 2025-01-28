package com.falldetect.falldetection.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.falldetect.falldetection.repositories.FirebaseRepository

class FallDataViewModelFactory(
    private val firebaseRepository: FirebaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FallDataViewModel::class.java)) {
            return FallDataViewModel(firebaseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}