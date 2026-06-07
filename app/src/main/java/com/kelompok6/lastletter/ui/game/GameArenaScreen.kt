package com.kelompok6.lastletter.ui.game

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameArenaScreen(
    roomId: String,
    navController: NavController,
    viewModel: GameViewModel
) {
    // --- STATE DARI VIEWMODEL PVP ---
    val roomStatus by viewModel.roomStatus.collectAsState()
    val currentWord by viewModel.currentWord.collectAsState()
    val turn by viewModel.turn.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    val playerLives by viewModel.playerLives.collectAsState()
    val opponentLives by viewModel.opponentLives.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    var inputWord by remember { mutableStateOf("") }

    // Menentukan apakah saat ini giliran Anda
    val isMyTurn = (isHost && turn == "HOST") || (!isHost && turn == "GUEST")

    // --- WARNA TEMA GAME UNGU-PUTIH ---
    val DarkPurple = Color(0xFF2D0A59)
    val MidPurple = Color(0xFF5D12D2)
    val AccentPurple = Color(0xFF900BFC)
    val GameWhite = Color(0xFFF4F4F9)
    val TextPurple = Color(0xFF38087B)

    // Jika permainan selesai atau lawan keluar, kembalikan user ke home
    LaunchedEffect(roomStatus) {
        if (roomStatus == "NONE" || roomStatus == "FINISHED") {
            navController.navigate("home_screen") {
                popUpTo("home_screen") { inclusive = true }
            }
        }
    }

    // Background Gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkPurple, MidPurple)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Mode Duel", fontWeight = FontWeight.Bold)
                            Text("Room: $roomId", fontSize = 12.sp, color = Color.LightGray)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        TextButton(onClick = { viewModel.leaveRoom() }) {
                            Text("KELUAR", color = Color(0xFFFF5252), fontWeight = FontWeight.Black)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. PAPAN SKOR (VS BAR)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = GameWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sisi Player
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ANDA", fontWeight = FontWeight.Black, color = TextPurple, fontSize = 16.sp)
                            Row {
                                repeat(3) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Nyawa",
                                        tint = if (index < playerLives) Color(0xFFFF1744) else Color(0xFFE0E0E0),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Lencana VS
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AccentPurple)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                        }

                        // Sisi Lawan (PvP)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (roomStatus == "WAITING") "MENUNGGU..." else opponentName.ifEmpty { "LAWAN" }.uppercase(),
                                fontWeight = FontWeight.Black,
                                color = TextPurple,
                                fontSize = 16.sp
                            )
                            Row {
                                repeat(3) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Nyawa",
                                        tint = if (index < opponentLives) Color(0xFFFF1744) else Color(0xFFE0E0E0),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2. TIMER (Tergantung Status Room)
                val timerColor by animateColorAsState(
                    targetValue = if (timeLeft <= 5 && roomStatus == "PLAYING") Color(0xFFFF5252) else Color.White,
                    label = "timerColor"
                )

                Text(
                    text = if (roomStatus == "WAITING") "..." else "00:${timeLeft.toString().padStart(2, '0')}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = timerColor,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            blurRadius = 8f
                        )
                    )
                )

                Text(
                    text = if (roomStatus == "WAITING") "Berikan kode room ke lawan" else if (isMyTurn) "GILIRAN ANDA!" else "MENUNGGU LAWAN...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (roomStatus == "WAITING") Color.White else if (isMyTurn) Color(0xFF00E676) else Color(0xFFFFD600)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3. AREA KATA TERAKHIR
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = GameWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (roomStatus == "WAITING") "KODE ROOM" else "KATA SEBELUMNYA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPurple.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val wordToShow = if (roomStatus == "WAITING") roomId else currentWord.ifEmpty { "MULAI" }.uppercase()

                            // Highlight Huruf Terakhir dengan warna Ungu Neon (hanya jika sedang main)
                            val annotatedString = buildAnnotatedString {
                                if (roomStatus == "PLAYING" && wordToShow.length > 1 && wordToShow != "MULAI") {
                                    append(wordToShow.dropLast(1))
                                    withStyle(style = SpanStyle(color = AccentPurple, fontWeight = FontWeight.Black)) {
                                        append(wordToShow.takeLast(1))
                                    }
                                } else {
                                    append(wordToShow)
                                }
                            }

                            Text(
                                text = annotatedString,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPurple,
                                letterSpacing = 4.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. PESAN ERROR
                if (infoMessage.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFF5252),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = infoMessage,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 5. INPUT DAN TOMBOL KIRIM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputWord,
                        onValueChange = { inputWord = it.uppercase() },
                        placeholder = {
                            Text(
                                text = if (roomStatus == "WAITING") "Menunggu pemain 2..." else if (isMyTurn) "Ketik kata..." else "Giliran lawan...",
                                color = TextPurple.copy(alpha = 0.5f)
                            )
                        },
                        enabled = isMyTurn && roomStatus == "PLAYING",
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GameWhite,
                            unfocusedContainerColor = GameWhite,
                            disabledContainerColor = GameWhite.copy(alpha = 0.7f),
                            focusedTextColor = TextPurple,
                            unfocusedTextColor = TextPurple,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    FloatingActionButton(
                        onClick = {
                            viewModel.submitWord(inputWord)
                            inputWord = ""
                        },
                        containerColor = if (isMyTurn && inputWord.isNotBlank() && roomStatus == "PLAYING")
                            AccentPurple
                        else Color(0xFFBDBDBD),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Kirim Kata",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}