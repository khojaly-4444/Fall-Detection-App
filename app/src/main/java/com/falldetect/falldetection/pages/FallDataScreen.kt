package com.falldetect.falldetection.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falldetect.falldetection.models.FallEvent
import com.falldetect.falldetection.viewmodels.FallDataViewModel
import com.google.firebase.auth.FirebaseAuth

// Function for fall data tab
@Composable
fun FallDataScreen(viewModel: FallDataViewModel) {
    val fallEvents: List<FallEvent> by viewModel.fallData.collectAsState()

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: "" // Get current user id

    // Mock data for now, changing to real data later
    LaunchedEffect(Unit) {
        viewModel.fetchFallData()
    }

    LazyColumn (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ){
        // Header                       // Subject to change with arduino
        item {
            Text(
                text = "Fall Data",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF45231D),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Button to add mock data
            Button(
                onClick = { viewModel.addMockFallEvent(userId) }, // Ensure `userId` is passed
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Add Mock Fall Event")
            }
        }
        // Calls Class from FallEvent.kt
        items(fallEvents) { event ->
            FallDataCard(
                fallType = event.fallType,
                date = event.date,
                time = event.time,
                heartRate = event.heartRate
            )
        }
    }
}

@Composable
fun FallDataCard(fallType: String, date: String, time: String, heartRate: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD7CCC8)) // Light brown color
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Type: $fallType", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Date: $date", fontSize = 16.sp)
            Text("Time: $time", fontSize = 16.sp)
            Text("Heart Rate: $heartRate BPM", fontSize = 16.sp, color = Color.Red)
        }
    }
}