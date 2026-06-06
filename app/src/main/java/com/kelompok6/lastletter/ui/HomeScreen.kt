package com.kelompok6.lastletter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity
import com.kelompok6.lastletter.data.local.entity.PlayedWordItem
import com.kelompok6.lastletter.ui.history.HistoryViewModel
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToDuel: () -> Unit = {},
    onNavigateToBot: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    val auth = FirebaseAuth.getInstance()

    // MITIGASI ERROR: Menggunakan rememberSaveable agar status tidak hancur saat berpindah halaman tab
    var userName by rememberSaveable { mutableStateOf(auth.currentUser?.displayName ?: "Player") }
    var userEmail by rememberSaveable { mutableStateOf(auth.currentUser?.email ?: "Email tidak ditemukan") }
    var userUid by rememberSaveable { mutableStateOf(auth.currentUser?.uid ?: "0000") }

    // Proteksi pengaman data: Hanya perbarui state jika emisi data Firebase valid (Bukan null/kosong akibat transisi thread)
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val detectedName = user.displayName
                if (!detectedName.isNullOrBlank()) {
                    userName = detectedName
                }
                val detectedEmail = user.email
                if (!detectedEmail.isNullOrBlank()) {
                    userEmail = detectedEmail
                }
                userUid = user.uid
            }
        }
        auth.addAuthStateListener(authStateListener)
        onDispose { auth.removeAuthStateListener(authStateListener) }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedItem) {
                0 -> HomeContent(userName, onNavigateToDuel, onNavigateToBot)
                1 -> HistoryContent()
                2 -> ProfileContent(
                    userName = userName,
                    userEmail = userEmail,
                    userUid = userUid,
                    onNameUpdated = { newName -> userName = newName },
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun HomeContent(userName: String, onNavigateToDuel: () -> Unit, onNavigateToBot: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Halo, $userName!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Siap berduel hari ini?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Last Letter", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Buktikan kemampuan kosa kata kamu dan jadilah juara!", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNavigateToDuel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Duel PvP", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToBot,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Mode vs bot", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileContent(
    userName: String,
    userEmail: String,
    userUid: String,
    onNameUpdated: (String) -> Unit,
    onLogout: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf(userName) }
    var isSaving by remember { mutableStateOf(false) }

    val userHashTag = userUid.takeLast(4).padStart(4, '0').uppercase()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Profil Pemain", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                label = { Text("Nama Tampilan Baru") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            isSaving = true
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(inputName.trim())
                                .build()

                            FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { task ->
                                    isSaving = false
                                    if (task.isSuccessful) {
                                        isEditing = false
                                        onNameUpdated(inputName.trim())
                                    } else {
                                        isEditing = false
                                        inputName = userName
                                    }
                                }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) { Text(if (isSaving) "Loading..." else "Simpan") }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { isEditing = false; inputName = userName },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) { Text("Batal") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = userName, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "#$userHashTag", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                IconButton(onClick = { inputName = userName; isEditing = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Nama", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Email Akun Terhubung", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = userEmail, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // FUNGSI LOGOUT MUTLAK: Keluar dari sesi Firebase & Triger pembersihan stack navigasi utama
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Keluar (Logout)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HistoryContent(viewModel: HistoryViewModel = hiltViewModel()) {
    val historyList by viewModel.history.collectAsState(initial = emptyList<MatchHistoryEntity>())

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Riwayat Pertandingan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Belum ada riwayat pertandingan", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(historyList) { historyItem ->
                    HistoryCard(historyItem)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(history: MatchHistoryEntity) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(history.date))

    val parsedWords = remember(history.wordsPlayedJson) {
        try {
            Json.decodeFromString<List<PlayedWordItem>>(history.wordsPlayedJson)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Lawan: ${history.opponent}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "${history.mode} MODE • $dateString", fontSize = 12.sp, color = Color.Gray)
                }
                Text(
                    text = if (history.result == "WIN") "WIN" else "LOSE",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (history.result == "WIN") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Skor Akhir: ${history.score} Poin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Detail Kata Dimainkan:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    if (parsedWords != null) {
                        parsedWords.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    imageVector = if (item.isCorrect) Icons.Filled.CheckCircle else if (item.isTimeout) Icons.Filled.Warning else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (item.isCorrect) Color(0xFF4CAF50) else Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (item.isTimeout) "Habis Waktu" else item.word.uppercase(),
                                    fontSize = 14.sp,
                                    color = if (item.isCorrect) MaterialTheme.colorScheme.onSurface else Color.Red
                                )
                            }
                        }
                    } else {
                        Text("Gagal memuat detail kata.", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(selectedItem: Int, onItemSelected: (Int) -> Unit) {
    val items = listOf("Home", "History", "Profile")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.History, Icons.Filled.Person)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}