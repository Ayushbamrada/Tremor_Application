package com.example.yourcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.yourcare.ui.screens.*
//import com.example.yourcare.ui.theme.TremorAppTheme
import com.example.yourcare.ui.theme.YourCareAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YourCareAppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    // Add default animations for all screens
                    enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
                ) {

                    // 1. Splash Screen
                    composable("splash") {
                        SplashScreen(onNavigate = {
                            // Use popBackStack to remove Splash from history
                            navController.popBackStack()
                            navController.navigate("details")
                        })
                    }

                    // 2. User Details Form
                    composable("details") {
                        UserDetailsScreen(onNavigateNext = { userName ->
                            // You could pass the name if you wanted, e.g. "guide/$userName"
                            navController.navigate("guide")
                        })
                    }

                    // 3. Guide/Video Screen
                    composable("guide") {
                        GuideScreen(onStartTest = {
                            navController.navigate("test")
                        })
                    }

                    // 4. Test Screen (Sensor)
                    composable("test") {
                        TestScreen(
                            onTestComplete = { avg, max, min ->
                                navController.navigate("report/$avg/$max/$min")
                            }
                        )
                    }

                    // 5. Report Screen (with Arguments)
                    composable(
                        route = "report/{avg}/{max}/{min}",
                        arguments = listOf(
                            navArgument("avg") { type = NavType.FloatType },
                            navArgument("max") { type = NavType.FloatType },
                            navArgument("min") { type = NavType.FloatType }
                        )
                    ) { backStackEntry ->
                        val avg = backStackEntry.arguments?.getFloat("avg") ?: 0f
                        val max = backStackEntry.arguments?.getFloat("max") ?: 0f
                        val min = backStackEntry.arguments?.getFloat("min") ?: 0f

                        ReportScreen(
                            avg = avg,
                            max = max,
                            min = min,
                            onHome = {
                                // Go back to start, clearing stack up to details
                                navController.popBackStack("details", inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}