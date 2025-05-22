package com.falldetect.falldetection.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.falldetect.falldetection.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

// Function for User Linking tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLinkScreen(navController: NavController, authViewModel: AuthViewModel){
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid      //Gets the user Uid
    val context = LocalContext.current
    var otherUid by remember { mutableStateOf("") }
    var linkResultMessage by remember { mutableStateOf("") }//Variable needed for the other user Uid

    Column (                                // User Linking UI
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