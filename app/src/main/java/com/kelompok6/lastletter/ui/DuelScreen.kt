package com.kelompok6.lastletter.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kelompok6.lastletter.ui.game.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelScreen(
    navController: NavController,
    viewModel: GameViewModel = hiltViewModel()
) {
    val roomStatus by viewModel.roomStatus.collectAsState()
    val roomCode by viewModel.roomCode.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val currentWord by viewModel.currentWord.collectAsState()
    val turn by viewModel.turn.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val playerLives by viewModel.playerLives.collectAsState()
    val opponentLives by viewModel.opponentLives.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputCode by remember { mutableStateOf("") }
    var inputWord by remember { mutableStateOf("") }

    val bgColorBottom = Color(0xFF2C165F)
    val primaryYellow = Color(0xFFFBBC05)
    val secondaryPurple = Color(0xFF6A2FF9)

    val currentUser = FirebaseAuth.getInstance().currentUser
    val playerName = currentUser?.displayName ?: "Player"
    val context = LocalContext.current

    val isMyTurn = (isHost && turn == "HOST") || (!isHost && turn == "GUEST")

    BackHandler {
        viewModel.leaveRoom()
        navController.popBackStack()
    }

    LaunchedEffect(infoMessage) {
        if (infoMessage.isNotEmpty()) {
            Toast.makeText(context, infoMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (roomStatus == "NONE") "Multiplayer" else "Room: $roomCode", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.leaveRoom()
                        navController.popBackStack()
                    }) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColorBottom)
            )
        },
        containerColor = bgColorBottom
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            if (roomStatus == "NONE") {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = secondaryPurple)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Buat Room Baru", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Buat kode unik dan bagikan ke temanmu!", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.createRoom() },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryYellow),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = bgColorBottom, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("CREATE ROOM", color = bgColorBottom, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("ATAU", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gabung ke Room", color = bgColorBottom, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(value = inputCode, onValueChange = { inputCode = it.uppercase() }, label = { Text("Kode Room 6 Digit") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.joinRoom(inputCode) },
                                enabled = inputCode.isNotBlank() && !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = secondaryPurple),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("JOIN ROOM", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } // INI ADALAH KURUNG KURAWAL YANG TADI HILANG

            else if (roomStatus == "WAITING") {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = primaryYellow)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Menunggu Lawan Bergabung...", color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = secondaryPurple), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kode Room Kamu:", color = Color.White.copy(alpha = 0.8f))
                            Text(roomCode, color = primaryYellow, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp)
                        }
                    }
                }
            }

            else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().background(secondaryPurple).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(playerName, color = Color.White, fontWeight = FontWeight.Bold)
                            Row { repeat(playerLives) { Icon(Icons.Filled.Favorite, null, tint = Color.Red, modifier = Modifier.size(20.dp)) } }
                        }
                        Card(shape = RoundedCornerShape(50), colors = CardDefaults.cardColors(containerColor = if (timeLeft <= 3) Color.Red else primaryYellow)) {
                            Text("$timeLeft", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = bgColorBottom, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(opponentName, color = Color.White, fontWeight = FontWeight.Bold)
                            Row { repeat(opponentLives) { Icon(Icons.Filled.Favorite, null, tint = Color.Red, modifier = Modifier.size(20.dp)) } }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(if (currentWord.isEmpty()) "Game Dimulai!" else "Kata Sebelumnya:", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (currentWord.isEmpty()) "MULAI" else currentWord.uppercase(), color = primaryYellow, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = if (isMyTurn && roomStatus == "PLAYING") primaryYellow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp)) {
                            val msg = if (roomStatus == "FINISHED") "Permainan Selesai!"
                            else if (isMyTurn) (if (currentWord.isEmpty()) "Giliranmu! Ketik kata pertama." else "GILIRAN KAMU!\nAwalan: '${currentWord.last().uppercaseChar()}'")
                            else "Giliran $opponentName..."
                            Text(msg, color = if (isMyTurn) primaryYellow else Color.White, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(color = secondaryPurple, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = inputWord,
                                onValueChange = { inputWord = it.split("\\s+".toRegex()).firstOrNull() ?: "" },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Ketik 1 kata...", color = Color.LightGray) },
                                singleLine = true,
                                enabled = isMyTurn && roomStatus == "PLAYING",
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = bgColorBottom, unfocusedContainerColor = bgColorBottom, focusedBorderColor = primaryYellow, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            FloatingActionButton(
                                onClick = {
                                    if (inputWord.isNotBlank()) { viewModel.submitWord(inputWord); inputWord = "" }
                                },
                                containerColor = primaryYellow, contentColor = bgColorBottom, modifier = Modifier.size(56.dp)
                            ) { Icon(Icons.Filled.Send, contentDescription = "Kirim") }
                        }
                    }
                }

                if (roomStatus == "FINISHED") {
                    val isWin = playerLives > 0
                    AlertDialog(
                        onDismissRequest = { },
                        containerColor = Color(0xFF3D1F85),
                        title = { Text(if (isWin) "🏆 KAMU MENANG!" else "💀 GAME OVER", color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        text = { Text("Pertandingan PVP selesai. Riwayat sudah disimpan.", color = Color.LightGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        confirmButton = {
                            Button(onClick = { viewModel.leaveRoom(); navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = primaryYellow)) {
                                Text("KEMBALI KE HOME", color = bgColorBottom, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}