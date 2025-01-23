package com.falldetect.falldetection.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.falldetect.falldetection.AuthState
import com.falldetect.falldetection.AuthViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


// Function for tabs navigation

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    // Track the Tab
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Fall Data", "User Linking")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDD0))
    ) {
        // Tab UI
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color(0xFFDC3545),
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

        //Display content of selected tab
        when (selectedTabIndex) {
            0 -> FallDataPage()                 // tab for Fall Data
            1 -> UserLinkPage(navController = navController, authViewModel = authViewModel)   // Tab for User Linkage
        }
            Spacer(modifier = Modifier.weight(1f))
        // Sign Out button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDC3545))
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
    }
}

// Function for fall data tab
@Composable
fun FallDataPage(){
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "Fall Data",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF45231D)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder for fall events                  // Subject to change when its time for integration with arduino data
        val fallEvents = listOf(
            "Fall detected on 2025-01-20 at 10:30 AM",
            "Fall detected on 2025-01-21 at 6:15 PM"
        )
        fallEvents.forEach{ event ->
            Text(
                text = event,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// Function for User Linking tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLinkPage(navController: NavController, authViewModel: AuthViewModel){
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid      //Gets the user Uid
    val context = LocalContext.current
    var otherUid by remember { mutableStateOf("") }
    var linkResultMessage by remember { mutableStateOf("") }//Variable needed for the other user Uid

    Column (                                    // User Linking UI
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "User Linking",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF45231D)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Display User UID
        if (currentUid != null){
            Text(
                text = "User ID:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUid,
                fontSize = 16.sp,
                color = Color(0xFF45231D),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFFF5F5F5))
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Button for User ID copy
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("User ID", currentUid)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545),
                    contentColor = Color.White
                )
            ) {
                Text("Copy User ID", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Input Other User ID
            Text(
                text = "Enter User ID:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = otherUid,
                onValueChange = { otherUid = it },
                label = { Text("User ID") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedLabelColor = Color(0xFF6C757D)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Link User Button
            Button(
                onClick = {
                    if (currentUid != null && otherUid.isNotEmpty()) {
                        authViewModel.linkUsers(currentUid, otherUid) { success, message ->
                            linkResultMessage = message
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Please enter a valid User ID.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF28A745),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Link User ID", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Result Message
            if (linkResultMessage.isNotEmpty()) {
                Text(
                    text = linkResultMessage,
                    color = if (linkResultMessage.contains("success", true)) Color.Green else Color.Red,
                    fontSize = 16.sp
                )
            }
        } else {
            Text(
                text = "Error: Could not retrieve your User ID. Please log in again.",
                color = Color.Red,
                fontSize = 16.sp
            )
        }
    }
}