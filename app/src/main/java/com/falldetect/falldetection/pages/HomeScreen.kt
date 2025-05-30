package com.falldetect.falldetection.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.falldetect.falldetection.repositories.FirebaseRepository
import com.falldetect.falldetection.viewmodels.AuthState
import com.falldetect.falldetection.viewmodels.AuthViewModel
import com.falldetect.falldetection.viewmodels.FallDataViewModel
import com.falldetect.falldetection.viewmodels.FallDataViewModelFactory


// Function for tabs navigation

@Composable
fun HomeScreen(modifier: Modifier = Modifier,
               navController: NavController,
               authViewModel: AuthViewModel,
               firebaseRepository: FirebaseRepository
) {
    // ViewModel for fetching fall event data
    val fallDataViewModel: FallDataViewModel = viewModel(
        factory = FallDataViewModelFactory(firebaseRepository)
    )
    val authState = authViewModel.authState.observeAsState()

    // Redirect to login screen if user is unauthenticated
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    // Track which tab is selected (0 = fall data, 1 = User linking)
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Fall Data", "User Linking")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5DC))
    ) {
        // Sign Out button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFB22222))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = { authViewModel.signout() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Sign Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Tab UI
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFFB22222),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }

                )
            }
        }

        // Display content of selected tab
        when (selectedTabIndex) {
            0 -> FallDataScreen(viewModel = fallDataViewModel)                 // tab for Fall Data
            1 -> UserLinkScreen(navController = navController, authViewModel = authViewModel)   // Tab for User Linkage
        }

    }
}


