package com.kelompok6.lastletter.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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

    // --- TEMA GAME BOT ---
    val DarkPurple = Color(0xFF2D0A59)
    val MidPurple = Color(0xFF5D12D2)
    val AccentPurple = Color(0xFF900BFC)
    val GameWhite = Color(0xFFF4F4F9)
    val TextPurple = Color(0xFF38087B)

    val currentUser = FirebaseAuth.getInstance().currentUser
    val playerName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Player"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(DarkPurple, MidPurple)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(if (roomStatus == "NONE") "Multiplayer PVP" else "Room: $roomCode", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        if (roomStatus != "NONE") {
                            TextButton(onClick = { viewModel.leaveRoom(); navController.popBackStack() }) {
                                Text("KELUAR", color = Color(0xFFFF5252), fontWeight = FontWeight.Black)
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                // STATE 1: BUAT / JOIN ROOM
                if (roomStatus == "NONE") {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AccentPurple)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Buat Room Baru", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Buat kode unik dan bagikan ke temanmu!", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.createRoom() },
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = GameWhite),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    if (isLoading) CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(24.dp))
                                    else Text("CREATE ROOM", color = AccentPurple, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("ATAU", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(32.dp))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GameWhite)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Gabung ke Room", color = TextPurple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = inputCode, onValueChange = { inputCode = it.uppercase() },
                                    label = { Text("Kode Room 6 Digit") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.joinRoom(inputCode) },
                                    enabled = inputCode.isNotBlank() && !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("JOIN ROOM", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STATE 2: MENUNGGU LAWAN
                else if (roomStatus == "WAITING") {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = GameWhite)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Menunggu Lawan Bergabung...", color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = AccentPurple), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Kode Room Kamu:", color = Color.White.copy(alpha = 0.8f))
                                Text(roomCode, color = GameWhite, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp)
                            }
                        }
                    }
                }

                // STATE 3 & 4: PLAYING & FINISHED (MIRIP BOT SCREEN)
                else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        // 1. PAPAN SKOR
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = GameWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ANDA", fontWeight = FontWeight.Black, color = TextPurple, fontSize = 16.sp)
                                    Row {
                                        repeat(3) { index -> Icon(Icons.Default.Favorite, contentDescription = "Nyawa", tint = if (index < playerLives) Color(0xFFFF1744) else Color(0xFFE0E0E0), modifier = Modifier.size(28.dp)) }
                                    }
                                }
                                Box(modifier = Modifier.clip(CircleShape).background(AccentPurple).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                    Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(opponentName, fontWeight = FontWeight.Black, color = TextPurple, fontSize = 16.sp)
                                    Row {
                                        repeat(3) { index -> Icon(Icons.Default.Favorite, contentDescription = "Nyawa", tint = if (index < opponentLives) Color(0xFFFF1744) else Color(0xFFE0E0E0), modifier = Modifier.size(28.dp)) }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // 2. TIMER BESAR
                        val timerColor by animateColorAsState(targetValue = if (timeLeft <= 5) Color(0xFFFF5252) else Color.White, label = "timerColor")
                        Text(
                            text = "00:${timeLeft.toString().padStart(2, '0')}",
                            fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = timerColor,
                            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f))
                        )

                        Text(
                            text = if (isMyTurn) "GILIRAN ANDA!" else "GILIRAN $opponentName...",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = if (isMyTurn) Color(0xFF00E676) else Color(0xFFFFD600)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // 3. AREA KATA (KARTU PUTIH BESAR)
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = GameWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "KATA SEBELUMNYA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPurple.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val wordToShow = currentWord.ifEmpty { "MULAI" }.uppercase()
                                    val annotatedString = buildAnnotatedString {
                                        if (wordToShow.length > 1 && wordToShow != "MULAI") {
                                            append(wordToShow.dropLast(1))
                                            withStyle(style = SpanStyle(color = AccentPurple, fontWeight = FontWeight.Black)) {
                                                append(wordToShow.takeLast(1))
                                            }
                                        } else { append(wordToShow) }
                                    }
                                    Text(text = annotatedString, fontSize = 48.sp, fontWeight = FontWeight.Black, color = TextPurple, letterSpacing = 4.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. PESAN ERROR
                        if (infoMessage.isNotEmpty()) {
                            Surface(color = Color(0xFFFF5252), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                                Text(text = infoMessage, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                            }
                        }

                        // 5. INPUT DAN TOMBOL KIRIM
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = inputWord,
                                onValueChange = { inputWord = it.uppercase() },
                                placeholder = { Text(text = if (isMyTurn) "Ketik kata..." else "Tunggu...", color = TextPurple.copy(alpha = 0.5f)) },
                                enabled = isMyTurn && roomStatus == "PLAYING",
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = GameWhite, unfocusedContainerColor = GameWhite,
                                    disabledContainerColor = GameWhite.copy(alpha = 0.7f), focusedTextColor = TextPurple,
                                    unfocusedTextColor = TextPurple, focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            FloatingActionButton(
                                onClick = {
                                    if (inputWord.isNotBlank()) { viewModel.submitWord(inputWord); inputWord = "" }
                                },
                                containerColor = if (isMyTurn && inputWord.isNotBlank() && roomStatus == "PLAYING") AccentPurple else Color(0xFFBDBDBD),
                                shape = CircleShape
                            ) { Icon(Icons.Default.Send, contentDescription = "Kirim Kata", tint = Color.White, modifier = Modifier.size(24.dp)) }
                        }
                    }

                    // MODAL DIALOG KETIKA SELESAI
                    if (roomStatus == "FINISHED") {
                        val isWin = playerLives > opponentLives
                        AlertDialog(
                            onDismissRequest = { },
                            containerColor = GameWhite,
                            title = { Text(if (isWin) "🏆 KAMU MENANG!" else "💀 GAME OVER", color = TextPurple, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            text = { Text("Pertandingan melawan $opponentName selesai.", color = TextPurple.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            confirmButton = {
                                Button(onClick = { viewModel.leaveRoom(); navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) {
                                    Text("KEMBALI KE HOME", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}