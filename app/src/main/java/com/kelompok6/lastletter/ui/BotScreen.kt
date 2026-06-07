package com.kelompok6.lastletter.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kelompok6.lastletter.ui.game.OfflineMatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotScreen(
    navController: NavController,
    viewModel: OfflineMatchViewModel = hiltViewModel()
) {
    // --- STATE DARI VIEWMODEL ---
    val gameStatus by viewModel.gameStatus.collectAsState()
    val currentWord by viewModel.currentWord.collectAsState()
    val isPlayerTurn by viewModel.isPlayerTurn.collectAsState()
    val playerLives by viewModel.playerLives.collectAsState()
    val botLives by viewModel.botLives.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    var inputWord by remember { mutableStateOf("") }

    // --- WARNA TEMA GAME UNGU-PUTIH ---
    val DarkPurple = Color(0xFF2D0A59)   // Ungu sangat tua untuk atas
    val MidPurple = Color(0xFF5D12D2)    // Ungu terang untuk bawah
    val AccentPurple = Color(0xFF900BFC) // Ungu neon untuk tombol/highlight
    val GameWhite = Color(0xFFF4F4F9)    // Putih agak abu agar tidak silau
    val TextPurple = Color(0xFF38087B)   // Ungu pekat untuk teks di atas putih

    // Jika permainan selesai, kembalikan user ke home
    LaunchedEffect(gameStatus) {
        if (gameStatus == "FINISHED" || gameStatus == "NONE") {
            navController.navigate("home_screen") {
                popUpTo("home_screen") { inclusive = true }
            }
        }
    }

    // Membungkus seluruh layar dengan Gradient Background
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
            // Set transparan agar gradient dari Box di atasnya terlihat
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Mode vs AI Bot", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("MENYERAH", color = Color(0xFFFF5252), fontWeight = FontWeight.Black)
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
                // 1. PAPAN SKOR (VS BAR) - PUTIH
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

                        // Lencana VS - Ungu Neon
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AccentPurple)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("VS", color = Color.White, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }

                        // Sisi Bot
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AI BOT", fontWeight = FontWeight.Black, color = TextPurple, fontSize = 16.sp)
                            Row {
                                repeat(3) { index ->
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Nyawa",
                                        tint = if (index < botLives) Color(0xFFFF1744) else Color(0xFFE0E0E0),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2. TIMER (Putih Besar)
                val timerColor by animateColorAsState(
                    targetValue = if (timeLeft <= 5) Color(0xFFFF5252) else Color.White,
                    label = "timerColor"
                )

                Text(
                    text = "00:${timeLeft.toString().padStart(2, '0')}",
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
                    text = if (isPlayerTurn) "GILIRAN ANDA!" else "BOT SEDANG BERPIKIR...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlayerTurn) Color(0xFF00E676) else Color(0xFFFFD600) // Hijau jika jalan, Kuning jika nunggu
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 3. AREA KATA TERAKHIR (PUTIH)
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
                                text = "KATA SEBELUMNYA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPurple.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val wordToShow = currentWord.ifEmpty { "MULAI" }.uppercase()

                            // DITAMBAHKAN: Logika Dynamic Font Sizing agar kata panjang tidak terpotong (overflow)
                            val dynamicFontSize = when {
                                wordToShow.length >= 14 -> 22.sp
                                wordToShow.length in 10..13 -> 30.sp
                                wordToShow.length in 7..9 -> 38.sp
                                else -> 48.sp
                            }

                            val dynamicLetterSpacing = if (wordToShow.length >= 10) 1.sp else 4.sp

                            // Highlight Huruf Terakhir dengan warna Ungu Neon
                            val annotatedString = buildAnnotatedString {
                                if (wordToShow.length > 1 && wordToShow != "MULAI") {
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
                                fontSize = dynamicFontSize, // Menggunakan ukuran dinamis
                                fontWeight = FontWeight.Black,
                                color = TextPurple,
                                letterSpacing = dynamicLetterSpacing, // Spasi merapat jika kata panjang
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. PESAN ERROR
                if (infoMessage.isNotEmpty()) {
                    Surface(
                        color = Color(0xFFFF5252), // Merah Error
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
                    // TextField Putih
                    TextField(
                        value = inputWord,
                        onValueChange = { inputWord = it.uppercase() },
                        placeholder = {
                            Text(
                                text = if (isPlayerTurn) "Ketik kata..." else "Tunggu...",
                                color = TextPurple.copy(alpha = 0.5f)
                            )
                        },
                        enabled = isPlayerTurn && gameStatus == "PLAYING",
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GameWhite,
                            unfocusedContainerColor = GameWhite,
                            disabledContainerColor = GameWhite.copy(alpha = 0.7f),
                            focusedTextColor = TextPurple,
                            unfocusedTextColor = TextPurple,
                            focusedIndicatorColor = Color.Transparent, // Hilangkan garis bawah default
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Tombol Kirim Bulat
                    FloatingActionButton(
                        onClick = {
                            viewModel.submitWord(inputWord)
                            inputWord = ""
                        },
                        containerColor = if (isPlayerTurn && inputWord.isNotBlank() && gameStatus == "PLAYING")
                            AccentPurple // Nyala jika bisa diklik
                        else Color(0xFFBDBDBD), // Abu-abu jika disable
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