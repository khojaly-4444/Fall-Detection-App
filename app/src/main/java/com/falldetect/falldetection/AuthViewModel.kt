package com.falldetect.falldetection

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class AuthViewModel : ViewModel() {
    // Firebase Authentication
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()

    //  Firebase Realtime Database
    private val database: DatabaseReference = Firebase.database.reference


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

    fun signup(email: String, password: String, name: String, callback: (Boolean) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email and Password can't be empty")
            callback(false)
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser
                    val uid = currentUser?.uid

                    if (uid != null) {
                        // Save user data to Firebase Realtime Database
                        val userMap = mapOf(
                            "name" to name,
                            "email" to email
                        )
                        database.child("users").child(uid).setValue(userMap)
                            .addOnSuccessListener {
                                Log.d("AuthViewModel", "User data saved successfully!")
                                _authState.value = AuthState.Authenticated
                                callback(true) // Notify success
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AuthViewModel", "Failed to save user data: ${exception.message}")
                                _authState.value = AuthState.Error("Failed to save user data")
                                callback(false) // Notify failure
                            }
                    } else {
                        Log.e("AuthViewModel", "User ID is null after signup")
                        _authState.value = AuthState.Error("User ID is null")
                        callback(false)
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                    callback(false)
                }
            }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun linkUsers(currentUid: String, otherUid: String, callback: (Boolean, String) -> Unit) {
        // Ensure both UIDs are valid
        if (currentUid.isEmpty() || otherUid.isEmpty()) {
            callback(false, "Invalid User ID(s).")
            return
        }

        // Check if the other UID exists in Firebase
        database.child("users").child(otherUid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Link the users
                    val updates = mapOf(
                        "linked-users/$currentUid" to otherUid,
                        "linked-users/$otherUid" to currentUid
                    )
                    database.updateChildren(updates)
                        .addOnSuccessListener {
                            callback(true, "Successfully linked to User ID: $otherUid")
                        }
                        .addOnFailureListener { exception ->
                            callback(false, "Failed to link users: ${exception.message}")
                        }
                } else {
                    callback(false, "User ID not found in Database.")
                }
            }
            .addOnFailureListener { exception ->
                callback(false, "Error: ${exception.message}")
            }
    }

}



// Objects for representing different states of authentication
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()        // state when firebase is loading
    data class Error(val message : String) : AuthState()
}