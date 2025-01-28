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
import com.falldetect.falldetection.navigation.AppNavigation
import com.falldetect.falldetection.repositories.ArduinoManager
import com.falldetect.falldetection.repositories.FirebaseRepository
import com.falldetect.falldetection.ui.theme.FallDetectionTheme
import com.falldetect.falldetection.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    // Bluetooth Permissions
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        emptyArray() // location permission for older devices not needed
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { entry ->
                Log.d("Permissions", "${entry.key} = ${entry.value}")
            }
        }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check Bluetooth and location permissions
        checkPermissions()

        // Enable edge-to-edge UI
        enableEdgeToEdge()

        // Initialize ArduinoManager
        val arduinoManager = ArduinoManager(this)

        // Initialize AuthViewModel
        val authViewModel: AuthViewModel by viewModels()

        // Initialize FirebaseRepository
        val firebaseRepository = FirebaseRepository()

        // Start scanning when the app launches
        arduinoManager.startScanning()

        // Set Compose content
        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Call App Navigation Function
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        authViewModel = authViewModel,
                        firebaseRepository = firebaseRepository
                    )
                }
            }
        }
    }
}