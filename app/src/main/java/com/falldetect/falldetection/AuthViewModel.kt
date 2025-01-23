package com.falldetect.falldetection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {
    // Firebase Authentication

    private val auth : FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    // Function for checking whether user is authenticated or not

    fun checkAuthStatus(){
        if(auth.currentUser==null){
            _authState.value = AuthState.Unauthenticated
        } else{
            _authState.value = AuthState.Authenticated
        }
    }

    // Functions for communication with firebase for authentication

    //Function for login process

    fun login(email : String, password : String){

        if(email.isEmpty() && password.isEmpty()){
            _authState.value = AuthState.Error("Email and Password can't be empty" )
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                } else{
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    // Function for sign up process

    fun signup(email : String,password : String){

        if(email.isEmpty() && password.isEmpty()){
            _authState.value = AuthState.Error("Email and Password can't be empty" )
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                } else{
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }


}

// Objects for representing different states of authentication
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()        // state when firebase is loading
    data class Error(val message : String) : AuthState()
}