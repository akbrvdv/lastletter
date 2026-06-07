package com.kelompok6.lastletter.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineMatchViewModel @Inject constructor() : ViewModel() {

    private val _gameStatus = MutableStateFlow("PLAYING")
    val gameStatus: StateFlow<String> = _gameStatus.asStateFlow()

    private val _currentWord = MutableStateFlow("")
    val currentWord: StateFlow<String> = _currentWord.asStateFlow()

    private val _isPlayerTurn = MutableStateFlow(true)
    val isPlayerTurn: StateFlow<Boolean> = _isPlayerTurn.asStateFlow()

    private val _playerLives = MutableStateFlow(3)
    val playerLives: StateFlow<Int> = _playerLives.asStateFlow()

    private val _botLives = MutableStateFlow(3)
    val botLives: StateFlow<Int> = _botLives.asStateFlow()

    private val _timeLeft = MutableStateFlow(10)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage.asStateFlow()

    private var timerJob: Job? = null
    private val usedWords = mutableListOf<String>()

    // Kamus kata untuk Bot
    private val botDictionary = listOf(
        "APEL", "LEMARI", "IKAN", "NANAS", "SAPI", "ITIK", "KUCING", "GAJAH",
        "HARIMAU", "ULAR", "RUSA", "ANGSA", "AYAM", "MONYET", "TIKUS", "SEMUT",
        "TOPI", "INDONESIA", "ANGGUR", "RUMAH", "HUTAN", "NAGA", "API",
        "ILMU", "UDANG", "GELANG", "GURITA", "ANGIN", "NILAI", "INTAN",
        "RAMBUT", "TANGAN", "NANGKA", "ANGKASA", "AWAN", "NYAMUK", "KASUR"
    )

    init {
        startGame()
    }

    private fun startGame() {
        _gameStatus.value = "PLAYING"
        _playerLives.value = 3
        _botLives.value = 3
        _currentWord.value = ""
        usedWords.clear()
        _isPlayerTurn.value = true
        _timeLeft.value = 10

        // Timer TIDAK dipanggil di awal agar user punya waktu mikir kata pertama
        timerJob?.cancel()
    }

    // Fungsi Timer Universal (Dipakai untuk Player dan Bot)
    private fun startTimer() {
        timerJob?.cancel()
        _timeLeft.value = 10

        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && _gameStatus.value == "PLAYING") {
                delay(1000)
                _timeLeft.value -= 1 // Detik berkurang terus
            }

            // Jika waktu habis dan game masih berjalan
            if (_timeLeft.value <= 0 && _gameStatus.value == "PLAYING") {
                handleMistake(isPlayer = _isPlayerTurn.value, timeout = true)
            }
        }
    }

    // Fungsi Submit dari UI
    fun submitWord(word: String) {
        val cleanedWord = word.trim().uppercase()

        if (cleanedWord.isBlank() || !_isPlayerTurn.value || _gameStatus.value != "PLAYING") return

        val current = _currentWord.value.uppercase()

        // 1. Cek apakah kata sudah dipakai (Mengurangi nyawa)
        if (usedWords.contains(cleanedWord)) {
            handleMistake(isPlayer = true, timeout = false, errorMessage = "Kata '$cleanedWord' sudah dipakai!")
            return
        }

        // 2. Cek apakah huruf pertama sesuai dengan huruf terakhir kata sebelumnya
        val isValid = if (current.isEmpty()) {
            true
        } else {
            cleanedWord.first() == current.last()
        }

        if (isValid) {
            // Jika Benar
            usedWords.add(cleanedWord)
            _currentWord.value = cleanedWord
            _infoMessage.value = ""

            // Oper ke Bot
            botTurn()
        } else {
            // Jika Salah Huruf
            handleMistake(isPlayer = true, timeout = false, errorMessage = "Harus diawali huruf '${current.last()}'!")
        }
    }

    // Fungsi Logika AI Bot
    private fun botTurn() {
        _isPlayerTurn.value = false

        // JALANKAN TIMER BOT! Agar saat bot berpikir, detiknya tetap berkurang
        startTimer()

        viewModelScope.launch {
            // Bot pura-pura mikir 2 sampai 4 detik
            val thinkingTime = (2000L..4000L).random()
            delay(thinkingTime)

            // Jika pas mikir tiba-tiba game udah kelar, batalkan eksekusi
            if (_gameStatus.value != "PLAYING") return@launch

            val currentLastChar = _currentWord.value.last()

            // Cari kata yang cocok di kamus bot
            val possibleWords = botDictionary.filter {
                it.first() == currentLastChar && !usedWords.contains(it)
            }

            if (possibleWords.isNotEmpty()) {
                // Jawaban Bot Ketemu
                val botWord = possibleWords.random()
                usedWords.add(botWord)
                _currentWord.value = botWord

                // Oper balik ke Player
                _isPlayerTurn.value = true
                startTimer() // Reset timer dan jalankan untuk player
            } else {
                // Bot nyerah / kehabisan kata
                handleMistake(isPlayer = false, timeout = false, errorMessage = "Bot kehabisan kata-kata!")
            }
        }
    }

    // Fungsi Penanganan Salah & Game Over
    private fun handleMistake(isPlayer: Boolean, timeout: Boolean, errorMessage: String? = null) {
        timerJob?.cancel() // Langsung hentikan timer saat salah

        // Kurangi nyawa dan set pesan error sementara
        if (isPlayer) {
            _playerLives.value -= 1
            _infoMessage.value = if (timeout) "Waktu Habis!" else (errorMessage ?: "Kata salah!")
        } else {
            _botLives.value -= 1
            _infoMessage.value = if (timeout) "Waktu Bot Habis!" else (errorMessage ?: "Bot kehabisan kata!")
        }

        viewModelScope.launch {
            delay(2000) // Tampilkan pesan kesalahan selama 2 detik biar kebaca

            // CEK APAKAH ADA YANG MATI
            if (_playerLives.value <= 0) {
                // PESAN ANDA KALAH SEBELUM KELUAR
                _infoMessage.value = "GAME OVER: ANDA KALAH!"
                delay(2500)
                _gameStatus.value = "FINISHED"
            } else if (_botLives.value <= 0) {
                // PESAN ANDA MENANG SEBELUM KELUAR
                _infoMessage.value = "SELAMAT: ANDA MENANG!"
                delay(2500)
                _gameStatus.value = "FINISHED"
            } else {
                // KALAU MASIH ADA NYAWA, LANJUT RONDE BARU
                _infoMessage.value = ""
                _currentWord.value = listOf("RUMAH", "BUMI", "LAMPU", "MEJA", "KURSI").random()

                // Giliran dilempar ke pihak yang TIDAK salah
                _isPlayerTurn.value = !isPlayer

                if (_isPlayerTurn.value) {
                    startTimer()
                } else {
                    botTurn() // Fungsi ini sudah mencakup startTimer() buat bot
                }
            }
        }
    }
}