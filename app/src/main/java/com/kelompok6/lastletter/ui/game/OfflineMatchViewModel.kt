package com.kelompok6.lastletter.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kelompok6.lastletter.data.local.WordDao
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity
import com.kelompok6.lastletter.data.local.entity.PlayedWordItem
import com.kelompok6.lastletter.data.repository.MatchHistoryRepository
import com.kelompok6.lastletter.domain.WordValidator
import com.kelompok6.lastletter.domain.model.WordValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class OfflineMatchViewModel @Inject constructor(
    private val wordValidator: WordValidator,
    private val wordDao: WordDao, // Inject WordDao untuk Bot mencari kata di KBBI
    private val historyRepository: MatchHistoryRepository // DITAMBAHKAN: Untuk simpan riwayat
) : ViewModel() {

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

    private val _timeLeft = MutableStateFlow(15) // DITAMBAHKAN: Ubah jadi 15 Detik
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage.asStateFlow()

    private var timerJob: Job? = null
    private val usedWords = mutableListOf<String>()

    // DITAMBAHKAN: Variabel pelacakan untuk History
    private val playedWordsList = mutableListOf<PlayedWordItem>()
    private var score = 0
    private var correctWords = 0
    private var wrongWords = 0

    init {
        startGame()
    }

    private fun startGame() {
        _gameStatus.value = "PLAYING"
        _playerLives.value = 3
        _botLives.value = 3
        _currentWord.value = ""
        usedWords.clear()
        playedWordsList.clear()
        score = 0
        correctWords = 0
        wrongWords = 0
        _isPlayerTurn.value = true
        _timeLeft.value = 15 // DITAMBAHKAN: Ubah jadi 15 Detik

        timerJob?.cancel()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timeLeft.value = 15 // DITAMBAHKAN: Ubah jadi 15 Detik

        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && _gameStatus.value == "PLAYING") {
                delay(1000)
                _timeLeft.value -= 1
            }

            if (_timeLeft.value <= 0 && _gameStatus.value == "PLAYING") {
                handleMistake(isPlayer = _isPlayerTurn.value, timeout = true)
            }
        }
    }

    fun submitWord(word: String) {
        val cleanedWord = word.trim().uppercase()

        if (cleanedWord.isBlank() || !_isPlayerTurn.value || _gameStatus.value != "PLAYING") return

        viewModelScope.launch {
            val current = _currentWord.value

            val validationResult = wordValidator.validate(
                inputWord = cleanedWord,
                lastWord = current.ifEmpty { null },
                usedWords = usedWords.map { it.lowercase() }.toSet()
            )

            when (validationResult) {
                is WordValidationResult.Success -> {
                    usedWords.add(cleanedWord)
                    playedWordsList.add(PlayedWordItem(cleanedWord, isCorrect = true, isTimeout = false)) // Record history
                    score += 10
                    correctWords++
                    _currentWord.value = cleanedWord
                    _infoMessage.value = ""
                    botTurn()
                }
                is WordValidationResult.AlreadyUsed -> {
                    playedWordsList.add(PlayedWordItem(cleanedWord, isCorrect = false, isTimeout = false))
                    handleMistake(isPlayer = true, timeout = false, errorMessage = "Kata '$cleanedWord' sudah dipakai!")
                }
                is WordValidationResult.InvalidFirstLetter -> {
                    playedWordsList.add(PlayedWordItem(cleanedWord, isCorrect = false, isTimeout = false))
                    val expected = validationResult.expectedLetter.uppercaseChar()
                    handleMistake(isPlayer = true, timeout = false, errorMessage = "Harus diawali huruf '$expected'!")
                }
                is WordValidationResult.NotInDictionary -> {
                    playedWordsList.add(PlayedWordItem(cleanedWord, isCorrect = false, isTimeout = false))
                    handleMistake(isPlayer = true, timeout = false, errorMessage = "Kata '$cleanedWord' tidak ada di KBBI!")
                }
                is WordValidationResult.EmptyInput -> {
                    // Abaikan input kosong
                }
            }
        }
    }

    private fun botTurn() {
        _isPlayerTurn.value = false
        startTimer()

        viewModelScope.launch {
            // Waktu mikir bot acak antara 1 sampai 3 detik biar terlihat natural
            val thinkingTime = (1000L..3000L).random()
            delay(thinkingTime)

            if (_gameStatus.value != "PLAYING") return@launch

            val currentLastChar = _currentWord.value.last().lowercaseChar().toString()

            // Konversi riwayat kata ke huruf kecil untuk query ke database
            val usedListLower = usedWords.map { it.lowercase() }

            // Minta WordDao mencarikan 1 kata dari KBBI yang belum dipakai
            val botWord = wordDao.getRandomWordStartingWith(currentLastChar, usedListLower)

            if (botWord != null) {
                // Bot menemukan kata
                val finalBotWord = botWord.uppercase()
                usedWords.add(finalBotWord)
                playedWordsList.add(PlayedWordItem(finalBotWord, isCorrect = true, isTimeout = false)) // Record history bot

                _currentWord.value = finalBotWord

                // Kembalikan giliran ke player
                _isPlayerTurn.value = true
                startTimer()
            } else {
                // Sangat mustahil bot kehabisan kata jika menggunakan DB KBBI, tapi ini jaga-jaga
                handleMistake(isPlayer = false, timeout = false, errorMessage = "Bot kehabisan kata-kata!")
            }
        }
    }

    private fun handleMistake(isPlayer: Boolean, timeout: Boolean, errorMessage: String? = null) {
        timerJob?.cancel()

        if (isPlayer) {
            _playerLives.value -= 1
            score -= 5
            wrongWords++
            if (timeout) playedWordsList.add(PlayedWordItem("-", isCorrect = false, isTimeout = true)) // Record timeout

            _infoMessage.value = if (timeout) "Waktu Habis!" else (errorMessage ?: "Kata salah!")
        } else {
            _botLives.value -= 1
            if (timeout) playedWordsList.add(PlayedWordItem("-", isCorrect = false, isTimeout = true))
            _infoMessage.value = if (timeout) "Waktu Bot Habis!" else (errorMessage ?: "Bot kehabisan kata!")
        }

        viewModelScope.launch {
            delay(2000)

            if (_playerLives.value <= 0) {
                _infoMessage.value = "GAME OVER: ANDA KALAH!"
                delay(2500)
                _gameStatus.value = "FINISHED"
                saveHistory() // DITAMBAHKAN: Simpan sejarah sebelum selesai
            } else if (_botLives.value <= 0) {
                _infoMessage.value = "SELAMAT: ANDA MENANG!"
                delay(2500)
                _gameStatus.value = "FINISHED"
                saveHistory() // DITAMBAHKAN: Simpan sejarah sebelum selesai
            } else {
                _infoMessage.value = ""
                _currentWord.value = listOf("RUMAH", "BUMI", "LAMPU", "MEJA", "KURSI").random()

                _isPlayerTurn.value = !isPlayer

                if (_isPlayerTurn.value) {
                    startTimer()
                } else {
                    botTurn()
                }
            }
        }
    }

    // Fungsi untuk Push History ke Database Room Lokal
    private fun saveHistory() {
        if (playedWordsList.isEmpty()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "GUEST_USER"

        viewModelScope.launch {
            val isWin = _playerLives.value > 0
            val finalScore = if (isWin) score + 50 else score

            val history = MatchHistoryEntity(
                userId = uid,
                date = System.currentTimeMillis(),
                mode = "BOT OFFLINE",
                opponent = "AI BOT",
                result = if (isWin) "WIN" else "LOSE",
                score = finalScore,
                correctWords = correctWords,
                wrongWords = wrongWords,
                wordsPlayedJson = Json.encodeToString(playedWordsList)
            )
            historyRepository.insertHistory(history)
        }
    }
}