package com.kelompok6.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.kelompok6.lastletter.ui.BotScreen
import com.kelompok6.lastletter.ui.DuelScreen
import com.kelompok6.lastletter.ui.HomeScreen
import com.kelompok6.lastletter.ui.auth.AuthScreen
import com.kelompok6.lastletter.ui.game.GameArenaScreen
import com.kelompok6.lastletter.ui.game.GameViewModel
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

                    // 1. Cek sesi Firebase untuk Fitur Auto-Login
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val startDest = if (currentUser != null) "home_screen" else "auth_screen"

                    // 2. Inisialisasi GameViewModel di level NavHost (Shared ViewModel)
                    // Ini memastikan data Room tidak hilang saat pindah dari DuelScreen ke GameArenaScreen
                    val gameViewModel: GameViewModel = hiltViewModel()

                    NavHost(navController = navController, startDestination = startDest) {

                        // Layar Login / Register
                        composable("auth_screen") {
                            AuthScreen(
                                onLoginSuccess = {
                                    navController.navigate("home_screen") {
                                        popUpTo("auth_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Layar Home Utama (Sudah dibersihkan dari duplikasi)
                        composable("home_screen") {
                            HomeScreen(
                                onNavigateToDuel = {
                                    navController.navigate("duel_screen")
                                },
                                onNavigateToBot = {
                                    navController.navigate("bot_screen")
                                },
                                onLogout = {
                                    // Hancurkan sesi login Firebase
                                    FirebaseAuth.getInstance().signOut()

                                    // Kembali ke segmen Login dan bersihkan tumpukan halaman backstack
                                    navController.navigate("auth_screen") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Layar Menu Duel PvP (Input Kode / Buat Room)
                        composable("duel_screen") {
                            DuelScreen(navController = navController, viewModel = gameViewModel)
                        }

                        // Layar Arena Pertandingan PvP Online
                        composable("game_arena_screen/{roomId}") { backStackEntry ->
                            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                            GameArenaScreen(
                                roomId = roomId,
                                navController = navController,
                                viewModel = gameViewModel
                            )
                        }

                        // Layar Mode vs Bot
                        composable("bot_screen") {
                            BotScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}