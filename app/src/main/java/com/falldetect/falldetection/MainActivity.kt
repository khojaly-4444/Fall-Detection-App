package com.falldetect.falldetection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.falldetect.falldetection.navigation.AppNavigation
import com.falldetect.falldetection.repositories.ArduinoManager
import com.falldetect.falldetection.repositories.FirebaseRepository
import com.falldetect.falldetection.ui.theme.FallDetectionTheme
import com.falldetect.falldetection.viewmodels.AuthViewModel
import com.falldetect.falldetection.viewmodels.AuthViewModelFactory
import com.falldetect.falldetection.viewmodels.FallDataViewModel
import com.falldetect.falldetection.models.FallEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Step 1: Define required permissions (Bluetooth, Location, Notifications)
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        emptyArray()
    }

    // Step 2: Register permission launcher for Bluetooth & Location
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { entry ->
                Log.d("Permissions", "${entry.key} = ${entry.value}")
            }
            checkNotificationPermission() // If Bluetooth granted, request Notification permission
        }

    // Step 3: Register permission launcher for Notifications (Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("Permissions", "Notification permission granted")
            } else {
                Log.e("Permissions", "Notification permission denied")
            }
        }

    // Step 4: Function to check Bluetooth & Location permissions first
    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkNotificationPermission() // If Bluetooth is granted, check notifications
        }
    }

    // Step 5: Check and request Notification permission (Android 13+)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 6: Start permission check (Bluetooth first, then Notifications)
        checkPermissions()

        // Step 7: Fetch & Save FCM Token for notifications
        fetchAndSaveFCMToken()

        // Enable edge-to-edge UI
        enableEdgeToEdge()

        // Initialize Firebase Repository with context
        val firebaseRepository = FirebaseRepository(context = this)

        // Initialize ViewModels
        val authViewModel: AuthViewModel by viewModels {
            AuthViewModelFactory(applicationContext)
        }
        val fallDataViewModel: FallDataViewModel by viewModels {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return FallDataViewModel(firebaseRepository) as T
                }
            }
        }

        // Step 8: Start BLE scanning and observe fall events
        val arduinoManager = ArduinoManager(this)
        arduinoManager.startScanning()

        lifecycleScope.launch {
            ArduinoManager.onFallDetected = { fallEvent ->
                Log.d("MainActivity", "⬇️ Received fall event: $fallEvent")
                fallDataViewModel.addFallEvent(fallEvent)
            }
        }

        // Step 9: Set Compose UI
        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        authViewModel = authViewModel,
                        firebaseRepository = firebaseRepository
                    )
                }
            }
        }
    }

    // Function to fetch a fresh token and save it
    private fun fetchAndSaveFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM Token Failed", task.exception)
                    return@addOnCompleteListener
                }
                // Get the new FCM token
                val token = task.result
                Log.d("FCM", "FCM Token: $token")

                // Save token to Firebase
                saveTokenToFirebase(token)
            }
    }

    // Save token to Firebase under user node
    private fun saveTokenToFirebase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        userRef.child("fcmToken").setValue(token)
            .addOnSuccessListener {
                Log.d("FCM", "Token saved to Firebase successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to save token", e)
            }
    }

}
