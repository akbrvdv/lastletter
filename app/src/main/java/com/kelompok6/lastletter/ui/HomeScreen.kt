package com.kelompok6.lastletter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    // Definisi warna sesuai desain Figma kita
    val bgColorTop = Color(0xFF3D1F85)
    val bgColorBottom = Color(0xFF2C165F)
    val primaryYellow = Color(0xFFFBBC05)
    val secondaryPurple = Color(0xFF6A2FF9)

    Scaffold(
        containerColor = bgColorBottom
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Header (Profil & Koin)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder Profil
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Player123", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Lv. 12", color = Color.LightGray, fontSize = 12.sp)
                    }
                }

                // Placeholder Koin
                Card(
                    colors = CardDefaults.cardColors(containerColor = secondaryPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "🪙 1.250",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 2. Judul Game
            Text(
                text = "Last\nLetter",
                color = primaryYellow,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 48.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Tebak kata terakhir, jadi awal untuk lawan!",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 48.dp)
            )

            // 3. Tombol MAIN
            Button(
                onClick = {
                    // Nanti navigasi ke halaman Matchmaking/Room
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryYellow)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MAIN", color = bgColorBottom, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Cari lawan sekarang!", color = bgColorBottom, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = bgColorBottom, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Tombol MAIN VS BOT
            Button(
                onClick = {
                    // Nanti navigasi ke halaman In-Game vs Bot
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondaryPurple)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MAIN VS BOT", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Latih kemampuanmu", color = Color.White, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}