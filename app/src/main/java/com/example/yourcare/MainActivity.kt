package com.example.yourcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.yourcare.ui.screens.*
import com.example.yourcare.ui.theme.YourCareAppTheme
import com.example.yourcare.ui.viewmodel.TestViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YourCareAppTheme {
                val navController = rememberNavController()

                // --- CRITICAL CHANGE ---
                // Create the ViewModel here so it survives navigation between screens.
                // This ensures 'TestScreen' and 'ReportScreen' share the same data.
                val sharedViewModel: TestViewModel = viewModel()

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
                            // Pass the shared ViewModel so it records data into the same instance
                            viewModel = sharedViewModel,
                            onTestComplete = { avg, max, min, freq ->
                                navController.navigate("report/$avg/$max/$min/$freq")
                            }
                        )
                    }

                    // 5. Report Screen (Updated Arguments)
                    composable(
                        route = "report/{avg}/{max}/{min}/{freq}",
                        arguments = listOf(
                            navArgument("avg") { type = NavType.FloatType },
                            navArgument("max") { type = NavType.FloatType },
                            navArgument("min") { type = NavType.FloatType },
                            navArgument("freq") { type = NavType.FloatType }
                        )
                    ) { backStackEntry ->
                        val avg = backStackEntry.arguments?.getFloat("avg") ?: 0f
                        val max = backStackEntry.arguments?.getFloat("max") ?: 0f
                        val min = backStackEntry.arguments?.getFloat("min") ?: 0f
                        val freq = backStackEntry.arguments?.getFloat("freq") ?: 0f

                        ReportScreen(
                            avg = avg,
                            max = max,
                            min = min,
                            freq = freq,
                            // Pass the same shared ViewModel so we can read the raw graph data for the PDF
                            viewModel = sharedViewModel,
                            onHome = {
                                navController.popBackStack("details", inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}