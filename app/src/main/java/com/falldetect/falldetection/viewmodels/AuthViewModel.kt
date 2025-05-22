package com.falldetect.falldetection.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.falldetect.falldetection.repositories.FirebaseRepository
import com.google.firebase.messaging.FirebaseMessaging

class AuthViewModel(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    // Function for checking whether user is authenticated or not
    fun checkAuthStatus() {
        if (firebaseRepository.isUserAuthenticated()) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // Function for login process
    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email and Password can't be empty")
            Log.d("AuthViewModel", "Login failed: Empty fields")
            return
        }

        _authState.value = AuthState.Loading
        Log.d("AuthViewModel", "Login started for email: $email")

        firebaseRepository.login(email, password) { success, error ->
            if (success) {
                _authState.value = AuthState.Authenticated
            } else {
                Log.e("AuthViewModel", "Login failed: ${error ?: "Unknown error"}")
                _authState.value = AuthState.Error(error ?: "Something went wrong")
            }
        }
    }

    // Function for sign-up process
    fun signup(email: String, password: String, name: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            Log.d("AuthViewModel", "Signup failed: Empty fields")
            return
        }

        _authState.value = AuthState.Loading
        firebaseRepository.signup(email, password, name) { success, error ->
            if (success) {
                _authState.value = AuthState.Authenticated
            } else {
                Log.e("AuthViewModel", "Signup failed: ${error ?: "Unknown error"}")
                _authState.value = AuthState.Error(error ?: "Something went wrong")
            }
        }
    }

    // Function for sign-out process
    fun signout() {
        firebaseRepository.signout()
        _authState.value = AuthState.Unauthenticated
    }

    // Function for linking users
    fun linkUsers(currentUid: String, otherUid: String, callback: (Boolean, String) -> Unit) {
        if (currentUid.isEmpty() || otherUid.isEmpty()) {
            callback(false, "Invalid User ID(s).")
            return
        }

        firebaseRepository.linkUsers(currentUid, otherUid) { success, message ->
            callback(success, message)
        }
    }
}

// Objects for representing different states of authentication
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState() // State when Firebase is loading
    data class Error(val message: String) : AuthState()
}
