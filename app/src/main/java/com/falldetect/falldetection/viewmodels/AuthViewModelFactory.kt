package com.falldetect.falldetection.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.falldetect.falldetection.repositories.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Properly initialize FirebaseRepository with required parameters
            val firebaseRepository = FirebaseRepository(
                auth = FirebaseAuth.getInstance(),
                database = FirebaseDatabase.getInstance().reference,
                context = context
            )
            return AuthViewModel(firebaseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
