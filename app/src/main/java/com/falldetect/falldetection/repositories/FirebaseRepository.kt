package com.falldetect.falldetection.repositories

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.falldetect.falldetection.models.FallEvent
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.InputStream

// Firebase Operations
class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference,
    private val context: Context
) {

    // Check if the user is currently authenticated
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    // Login Function
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null) // Success
                } else {
                    callback(false, task.exception?.message ?: "Login failed") // Error
                }
            }
    }

    // Signup Function
    fun signup(email: String, password: String, name: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userMap = mapOf(
                            "name" to name,
                            "email" to email
                        )
                        database.child("users").child(uid).setValue(userMap)
                            .addOnSuccessListener {
                                callback(true, null) // Success
                            }
                            .addOnFailureListener { exception ->
                                callback(false, exception.message ?: "Failed to save user data")
                            }
                    } else {
                        callback(false, "User ID is null")
                    }
                } else {
                    callback(false, task.exception?.message ?: "Signup failed")
                }
            }
    }

    // Link Users Function
    fun linkUsers(currentUid: String, otherUid: String, callback: (Boolean, String) -> Unit) {
        database.child("users").child(otherUid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
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

    // Signout Function
    fun signout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Function to add fall event and notify linked user
    suspend fun addFallEvent(fallEvent: FallEvent) {
        val currentUid = auth.currentUser?.uid ?: return

        // Store event for the current user
        val eventRef = database.child("fall-data").child(currentUid).push()
        eventRef.setValue(fallEvent).await()

        // Get the linked user ID
        val linkedUidSnapshot = database.child("linked-users").child(currentUid).get().await()
        val linkedUid = linkedUidSnapshot.getValue(String::class.java)

        // Store event for linked user if one exists and notify them
        if (!linkedUid.isNullOrEmpty()) {
            val linkedEventRef = database.child("fall-data").child(linkedUid).push()
            linkedEventRef.setValue(fallEvent).await()

            // Get the linked user's FCM token
            val linkedUserTokenSnapshot = database.child("users").child(linkedUid).child("fcmToken").get().await()
            val linkedUserToken = linkedUserTokenSnapshot.getValue(String::class.java)

            if (!linkedUserToken.isNullOrEmpty()) {
                sendFCMNotification(
                    linkedUserToken,
                    "Fall Alert!",
                    "A fall has been detected for your linked user."
                )
            }
        }
    }

    // Function to fetch fall data
    suspend fun getFallData(): List<FallEvent> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()

        // Get Linked User ID
        val linkedUidSnapshot = database.child("linked-users").child(currentUid).get().await()
        val linkedUid = linkedUidSnapshot.getValue(String::class.java)

        // Get fall events for the current user
        val currentUserSnapshot = database.child("fall-data").child(currentUid).get().await()
        val currentUserFallData =
            currentUserSnapshot.children.mapNotNull { it.getValue(FallEvent::class.java) }

        // Get fall events for the linked user if exists
        val linkedUserFallData = if (!linkedUid.isNullOrEmpty()) {
            val linkedUserSnapshot = database.child("fall-data").child(linkedUid).get().await()
            linkedUserSnapshot.children.mapNotNull { it.getValue(FallEvent::class.java) }
        } else {
            emptyList()
        }

        // Combine both datasets and return unique fall events
        return (currentUserFallData + linkedUserFallData).distinctBy { it.date + it.time + it.impactSeverity }
    }

    // Function to listen for real-time fall events and notify linked user
    fun listenForFallEvents(onFallEventDetected: (FallEvent) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return

        val fallDataRef = database.child("fall-data").child(currentUid)

        fallDataRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val fallEvent = snapshot.getValue(FallEvent::class.java) ?: return

                // Get linked user ID
                database.child("linked-users").child(currentUid).get()
                    .addOnSuccessListener { linkedUserSnapshot ->
                        val linkedUid = linkedUserSnapshot.getValue(String::class.java)

                        if (!linkedUid.isNullOrEmpty()) {
                            // Retrieve linked user's FCM token
                            database.child("users").child(linkedUid).child("fcmToken").get()
                                .addOnSuccessListener { tokenSnapshot ->
                                    val linkedUserToken = tokenSnapshot.getValue(String::class.java)

                                    if (!linkedUserToken.isNullOrEmpty()) {
                                        sendFCMNotification(
                                            linkedUserToken,
                                            "Fall Alert!",
                                            "A fall has been detected for your linked user."
                                        )
                                    } else {
                                        Log.e("FCM", "Linked user FCM token is missing!")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Error retrieving linked user token: ${e.message}")
                                }
                        } else {
                            Log.e("FCM", "No linked user found for $currentUid")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Error retrieving linked user ID: ${e.message}")
                    }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "Failed to listen for fall events: ${error.message}")
            }
        })
    }

    private fun getAccessToken(): String {
        return try {
            val credentials = GoogleCredentials
                .fromStream(context.assets.open("service-account.json"))
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("FCM", "Error getting access token", e)
            ""
        }
    }



    // Function to send push notifications using Firebase Cloud Messaging
    private fun sendFCMNotification(token: String, title: String, message: String) {
        val jsonObject = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", token)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", message)
                })
            })
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "https://fcm.googleapis.com/v1/projects/fall-detection-app-eae40/messages:send",
            jsonObject,
            { response -> Log.d("FCM", "Notification sent successfully: $response") },
            { error -> Log.e("FCM", "Failed to send notification: ${error.message}") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "Bearer ${getAccessToken()}",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }


}
