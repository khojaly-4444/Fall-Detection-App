package com.falldetect.falldetection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.falldetect.falldetection.navigation.AppNavigation
import com.falldetect.falldetection.repositories.FirebaseRepository
import com.falldetect.falldetection.ui.theme.FallDetectionTheme
import com.falldetect.falldetection.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AuthViewModel
        val authViewModel: AuthViewModel by viewModels()

        // Initialize FirebaseRepository
        val firebaseRepository = FirebaseRepository()

        setContent {
            FallDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Call Navigation Function
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
