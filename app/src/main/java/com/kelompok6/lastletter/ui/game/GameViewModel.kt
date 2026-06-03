package com.kelompok6.lastletter.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok6.lastletter.data.model.GameRoom
import com.kelompok6.lastletter.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow<GameRoom?>(null)
    val gameState: StateFlow<GameRoom?> = _gameState.asStateFlow()

    private var heartbeatJob: Job? = null
    private var isPlaying = false
    private var currentPlayerId = ""

    fun joinOrCreateMatch(playerId: String) {
        currentPlayerId = playerId
        viewModelScope.launch {
            // Kita gabung atau buat ruangan terlebih dahulu
            repository.createOrJoinRoom(playerId).collectLatest { room ->
                if (room != null) {
                    // Setelah sukses masuk antrean/ruangan, mulai pantau data real-timenya
                    observeGameRoom(room.roomId)
                }
            }
        }
    }

    private fun observeGameRoom(roomId: String) {
        viewModelScope.launch {
            repository.observeRoom(roomId).collectLatest { room ->
                _gameState.value = room

                if (room != null && room.status == "PLAYING") {
                    // Mulai proses pengiriman Heartbeat jika belum berjalan
                    if (!isPlaying) {
                        isPlaying = true
                        startHeartbeat(roomId)
                    }

                    // Logika DC Monitor: Cek aktivitas lawan
                    val opponentId = if (currentPlayerId == room.player1Id) room.player2Id else room.player1Id
                    if (opponentId.isNotEmpty()) {
                        val opponentLastSeen = room.players[opponentId] ?: 0L
                        val currentTime = System.currentTimeMillis()

                        // Jika lawan sudah pernah mengirim heartbeat dan tidak aktif lebih dari 15 detik (15.000 ms)
                        if (opponentLastSeen > 0 && (currentTime - opponentLastSeen > 15000)) {
                            claimVictory(roomId, currentPlayerId)
                        }
                    }
                } else if (room?.status == "FINISHED") {
                    // Permainan selesai, hentikan ping jaringan untuk menghemat resources
                    stopHeartbeat()
                }
            }
        }
    }

    private fun startHeartbeat(roomId: String) {
        // Hentikan coroutine sebelumnya (jika ada) untuk mencegah duplikasi
        heartbeatJob?.cancel()
        
        heartbeatJob = viewModelScope.launch {
            // Perulangan ini aman dan otomatis berhenti jika viewModelScope mati (onCleared)
            while (isActive) {
                try {
                    repository.updateLastSeen(roomId, currentPlayerId)
                } catch (e: Exception) {
                    // Error ringan saat mengirim heartbeat dapat diabaikan atau di-log
                }
                delay(3000) // Kirim ping/heartbeat setiap 3 detik
            }
        }
    }

    private fun stopHeartbeat() {
        isPlaying = false
        heartbeatJob?.cancel()
    }

    private fun claimVictory(roomId: String, winnerId: String) {
        viewModelScope.launch {
            try {
                repository.claimVictory(roomId, winnerId)
            } catch (e: Exception) {
                // Tangani error koneksi (jika ada)
            }
        }
    }

    // Dipanggil otomatis oleh Jetpack Lifecycle saat ViewModel dihancurkan
    override fun onCleared() {
        super.onCleared()
        stopHeartbeat()
    }
}
