package com.falldetect.falldetection.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun FallDataScreen(viewModel: FallDataViewModel) {
    // Observe the list of fall events from the ViewModel
    val fallEvents: List<FallEvent> by viewModel.fallData.collectAsState()

    // Fetch fall data from Firebase on first load
    LaunchedEffect(Unit) {
        viewModel.fetchFallData()
    }

    // Display list of fall events in scrollable column
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        items(fallEvents) { event ->
            FallDataCard(
                fallType = event.fallType,
                date = event.date,
                time = event.time,
                impactIntensity = event.impactIntensity
            )
        }
    }
}

@Composable
fun FallDataCard(fallType: String, date: String, time: String, impactIntensity: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD2B48C))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Type: $fallType", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Date: $date", fontSize = 16.sp)
            Text("Time: $time", fontSize = 16.sp)
            Text("Impact Intensity: $impactIntensity", fontSize = 16.sp, color = Color.Red)
        }
    }
}
