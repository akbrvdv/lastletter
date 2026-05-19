package com.kelompok6.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kelompok6.lastletter.ui.HomeScreen // Import HomeScreen buatanmu
import com.kelompok6.lastletter.ui.auth.AuthScreen // Import AuthScreen dari temanmu
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "auth_screen") {

                        // Layar Login
                        composable("auth_screen") {
                            AuthScreen(
                                onLoginSuccess = {
                                    // Pindah ke Homepage setelah login sukses
                                    navController.navigate("home_screen") {
                                        // Hapus layar login dari history agar tidak bisa di-"Back"
                                        popUpTo("auth_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Layar Home Utama
                        composable("home_screen") {
                            HomeScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}