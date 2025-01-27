package com.falldetect.falldetection.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Function for fall data tab
@Composable
fun FallDataScreen() {
    val fallEvents = listOf(
        "Fall detected on 2025-01-20 at 10:30 AM",
        "Fall detected on 2025-01-21 at 6:15 PM",
        "Fall detected on 2025-01-22 at 8:45 PM",
        "Fall detected on 2025-01-23 at 7:00 AM",
        "Fall detected on 2025-01-24 at 9:30 PM",
    )

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
        }
        items(fallEvents.size) { index ->
            Text(
                text = fallEvents[index],
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }

}