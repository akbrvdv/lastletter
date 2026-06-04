package com.kelompok6.lastletter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kelompok6.lastletter.ui.game.OfflineMatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotScreen(
    navController: NavController,
    viewModel: OfflineMatchViewModel = hiltViewModel()
) {
    var inputText by remember { mutableStateOf("") }

    val timeLeft by viewModel.timeLeft.collectAsState()
    val playerLives by viewModel.playerLives.collectAsState()
    val botLives by viewModel.botLives.collectAsState()
    val currentWord by viewModel.currentWord.collectAsState()
    val turn by viewModel.turn.collectAsState()
    val status by viewModel.status.collectAsState()
    val winner by viewModel.winner.collectAsState()

    val isPlayerTurn = turn == "PLAYER"

    // Mengambil NAMA PEMAIN secara langsung dari Firebase
    val currentUser = FirebaseAuth.getInstance().currentUser
    val playerName = currentUser?.displayName.takeIf { !it.isNullOrBlank() } ?: "Player"

    val bgColorBottom = Color(0xFF2C165F)
    val primaryYellow = Color(0xFFFBBC05)
    val secondaryPurple = Color(0xFF6A2FF9)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lawan Bot", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColorBottom)
            )
        },
        containerColor = bgColorBottom
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // --- STATUS BAR ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(secondaryPurple)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // NAMA PEMAIN DARI PROFIL MUNCUL DI SINI
                        Text(playerName, color = Color.White, fontWeight = FontWeight.Bold)
                        Row {
                            repeat(playerLives) { Icon(Icons.Filled.Favorite, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = if (timeLeft <= 3) Color.Red else primaryYellow)
                    ) {
                        Text(
                            text = "$timeLeft",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = bgColorBottom,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bot AI", color = Color.White, fontWeight = FontWeight.Bold)
                        Row {
                            repeat(botLives) { Icon(Icons.Filled.Favorite, null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }

                // --- ARENA ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (currentWord.isEmpty()) "Game Dimulai!" else "Kata Sebelumnya:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (currentWord.isEmpty()) "MULAI" else currentWord.uppercase(),
                        color = primaryYellow,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlayerTurn && status == "Playing") primaryYellow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val infoMsg = if (status == "Finished") {
                            "Permainan Selesai"
                        } else if (isPlayerTurn) {
                            if (currentWord.isEmpty()) "Giliranmu! Ketik kata pertama bebas."
                            else "GILIRAN KAMU!\nAwalan: '${currentWord.last().uppercaseChar()}'"
                        } else {
                            "Bot sedang berpikir..."
                        }

                        Text(
                            text = infoMsg,
                            color = if (isPlayerTurn) primaryYellow else Color.White,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- INPUT KATA ---
                Surface(
                    color = secondaryPurple,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { newText ->
                                val singleWord = newText.split("\\s+".toRegex()).firstOrNull() ?: ""
                                inputText = singleWord.uppercase()
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ketik 1 kata...", color = Color.LightGray) },
                            singleLine = true,
                            enabled = isPlayerTurn && status == "Playing",
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = bgColorBottom,
                                unfocusedContainerColor = bgColorBottom,
                                focusedBorderColor = primaryYellow,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.submitWord(inputText)
                                    inputText = ""
                                }
                            },
                            containerColor = primaryYellow,
                            contentColor = bgColorBottom,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Kirim")
                        }
                    }
                }
            }

            // --- POP UP GAME SELESAI ---
            if (status == "Finished") {
                AlertDialog(
                    onDismissRequest = { },
                    containerColor = Color(0xFF3D1F85),
                    titleContentColor = Color.White,
                    textContentColor = Color.White,
                    title = {
                        Text(
                            text = if (winner == "PLAYER") "🏆 KAMU MENANG!" else "💀 GAME OVER",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Pertandingan Selesai!", color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Skor dan data kata-kata kamu sudah otomatis disimpan.", textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cek menu History di Beranda untuk melihat detailnya!", textAlign = TextAlign.Center, color = Color(0xFFFBBC05))
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBC05))
                        ) {
                            Text("MULAI LAGI", color = Color(0xFF2C165F), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("KEMBALI KE HOME")
                        }
                    }
                )
            }
        }
    }
}