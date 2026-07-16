package com.empire.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.empire.myapplication.ui.auth.AuthScreen
import com.empire.myapplication.ui.auth.OnboardingScreen
import com.empire.myapplication.ui.chat.ChatScreen

@Composable
fun AppNavigation(startDestination: String = "auth") {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                onLoginSuccess = { isNewUser ->
                    if (isNewUser) {
                        navController.navigate("onboarding") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else {
                        navController.navigate("chat") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                },
                onGuestLogin = {
                    navController.navigate("chat") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("chat") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        composable("chat") {
            ChatScreen(
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
