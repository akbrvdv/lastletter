package com.kelompok6.lastletter.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok6.lastletter.data.local.WordDao
import com.kelompok6.lastletter.domain.WordValidator
import com.kelompok6.lastletter.domain.model.WordValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineMatchViewModel @Inject constructor(
    private val wordValidator: WordValidator,
    private val wordDao: WordDao // Inject WordDao untuk Bot mencari kata di KBBI
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

    private val _timeLeft = MutableStateFlow(10)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage.asStateFlow()

    private var timerJob: Job? = null
    private val usedWords = mutableListOf<String>()

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

        timerJob?.cancel()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timeLeft.value = 10

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
                    _currentWord.value = cleanedWord
                    _infoMessage.value = ""
                    botTurn()
                }
                is WordValidationResult.AlreadyUsed -> {
                    handleMistake(isPlayer = true, timeout = false, errorMessage = "Kata '$cleanedWord' sudah dipakai!")
                }
                is WordValidationResult.InvalidFirstLetter -> {
                    val expected = validationResult.expectedLetter.uppercaseChar()
                    handleMistake(isPlayer = true, timeout = false, errorMessage = "Harus diawali huruf '$expected'!")
                }
                is WordValidationResult.NotInDictionary -> {
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
            _infoMessage.value = if (timeout) "Waktu Habis!" else (errorMessage ?: "Kata salah!")
        } else {
            _botLives.value -= 1
            _infoMessage.value = if (timeout) "Waktu Bot Habis!" else (errorMessage ?: "Bot kehabisan kata!")
        }

        viewModelScope.launch {
            delay(2000)

            if (_playerLives.value <= 0) {
                _infoMessage.value = "GAME OVER: ANDA KALAH!"
                delay(2500)
                _gameStatus.value = "FINISHED"
            } else if (_botLives.value <= 0) {
                _infoMessage.value = "SELAMAT: ANDA MENANG!"
                delay(2500)
                _gameStatus.value = "FINISHED"
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
}