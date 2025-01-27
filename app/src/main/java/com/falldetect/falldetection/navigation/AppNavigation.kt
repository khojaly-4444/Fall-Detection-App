package com.falldetect.falldetection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.falldetect.falldetection.pages.HomeScreen
import com.falldetect.falldetection.pages.LoginScreen
import com.falldetect.falldetection.pages.SignupScreen
import com.falldetect.falldetection.viewmodels.AuthViewModel

@Composable
fun AppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login", builder = {
         composable("login"){
             LoginScreen(modifier,navController,authViewModel)
         }
        composable("signup"){
             SignupScreen(modifier,navController,authViewModel)
         }
        composable("home"){
            HomeScreen(modifier,navController,authViewModel)
        }
    })
}