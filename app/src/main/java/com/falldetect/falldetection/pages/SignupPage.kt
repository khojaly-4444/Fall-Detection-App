package com.falldetect.falldetection.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.falldetect.falldetection.AuthState
import com.falldetect.falldetection.AuthViewModel
import com.falldetect.falldetection.R
import com.falldetect.falldetection.ui.theme.SubtitleStyle
import com.falldetect.falldetection.ui.theme.TitleStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    // Input state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    // Authentication state handling
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Authenticated -> navController.navigate("home")
            is AuthState.Error -> Toast.makeText(
                context, (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
    }

    // Start of UI Design
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDD0)) // Background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // Add horizontal padding
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo, Title, and Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp) // Move higher on the screen
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fu_logo),
                    contentDescription = "Uni Logo",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Fall Guard",
                    style = TitleStyle,
                    color = Color(0xFF45231D)
                )
                Text(
                    text = "Your safety companion",
                    style = SubtitleStyle,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp)) // Space between title and form

            // Signup Form Section
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sign up", // Section heading
                    style = TitleStyle.copy(fontSize = 24.sp),
                    color = Color(0xFF45231D)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it},
                    label = { Text("Name") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedLabelColor = Color(0xFF6C757D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Email Input Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF6C757D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Input Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF6C757D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Up Button
            Button(
                onClick = {
                    authViewModel.signup(email, password, name) { isSuccess ->
                        if (isSuccess) {
                            Toast.makeText(context, "Signup successful!", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") // Navigate to the home page
                        } else {
                            Toast.makeText(
                                context,
                                "Signup failed. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                },
                enabled = authState.value != AuthState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545), // Button color
                    contentColor = Color.White
                )
            ) {
                Text("Sign up", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigate to Login Page
            TextButton(
                onClick = { navController.navigate("login") },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF6C757D)
                )
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}
