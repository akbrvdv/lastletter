package com.kelompok6.lastletter.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity
import com.kelompok6.lastletter.data.local.entity.PlayedWordItem
import com.kelompok6.lastletter.data.repository.MatchHistoryRepository
import com.kelompok6.lastletter.data.repository.WordRepository
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
    private val wordRepository: WordRepository,
    private val historyRepository: MatchHistoryRepository,
    private val wordValidator: WordValidator
) : ViewModel() {

    private val _currentWord = MutableStateFlow("")
    val currentWord: StateFlow<String> = _currentWord.asStateFlow()

    private val _turn = MutableStateFlow("PLAYER") // PLAYER or BOT
    val turn: StateFlow<String> = _turn.asStateFlow()

    private val _playerLives = MutableStateFlow(3)
    val playerLives: StateFlow<Int> = _playerLives.asStateFlow()

    private val _botLives = MutableStateFlow(3)
    val botLives: StateFlow<Int> = _botLives.asStateFlow()

    private val _status = MutableStateFlow("Playing") // Playing, Finished
    val status: StateFlow<String> = _status.asStateFlow()

    private val _winner = MutableStateFlow("") // PLAYER or BOT
    val winner: StateFlow<String> = _winner.asStateFlow()

    private val _usedWords = MutableStateFlow<List<String>>(emptyList())
    val usedWords: StateFlow<List<String>> = _usedWords.asStateFlow()

    private val _timeLeft = MutableStateFlow(15)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private var timerJob: Job? = null
    private var score = 0
    private var correctWordsCount = 0
    private var wrongWordsCount = 0
    private val playedWordsList = mutableListOf<PlayedWordItem>()

    init {
        startPlayerTurn()
    }

    private fun startPlayerTurn() {
        _turn.value = "PLAYER"
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _timeLeft.value = 15
            while (_timeLeft.value > 0) {
                delay(1000)
                _timeLeft.value -= 1
            }
            handlePlayerMistake(timeout = true)
        }
    }

    fun submitWord(word: String) {
        timerJob?.cancel()
        viewModelScope.launch {
            val cleanedWord = word.trim().lowercase()
            val cw = _currentWord.value
            
            // Menggunakan WordValidator super canggih yang kita buat di Tahap 2
            val validationResult = wordValidator.validate(
                inputWord = cleanedWord,
                lastWord = cw.ifEmpty { null },
                usedWords = _usedWords.value.toSet()
            )

            val isValid = validationResult is WordValidationResult.Success
            
            playedWordsList.add(
                PlayedWordItem(
                    word = cleanedWord.ifEmpty { "-" },
                    isCorrect = isValid,
                    isTimeout = cleanedWord.isEmpty()
                )
            )

            if (isValid) {
                _usedWords.value = _usedWords.value + cleanedWord
                _currentWord.value = cleanedWord
                score += 10
                correctWordsCount++
                startBotTurn()
            } else {
                handlePlayerMistake(timeout = false)
            }
        }
    }

    private fun handlePlayerMistake(timeout: Boolean = false) {
        if (timeout) {
            playedWordsList.add(PlayedWordItem(word = "-", isCorrect = false, isTimeout = true))
        }
        score -= 5
        wrongWordsCount++
        _playerLives.value -= 1
        
        if (_playerLives.value <= 0) {
            endGame("BOT")
        } else {
            startPlayerTurn()
        }
    }

    private fun startBotTurn() {
        _turn.value = "BOT"
        viewModelScope.launch {
            try {
                // Jeda buatan agar bot terlihat sedang "berpikir" (1.5 detik)
                delay(1500)

                val currentWordStr = _currentWord.value
                val prefix = if (currentWordStr.isNotEmpty()) currentWordStr.last().toString() else listOf("a","b","c","k","m","p","r","s","t").random()
                var botWord: String? = null

                // Mencoba mencari kata yang belum digunakan maksimal 5 kali percobaan
                for (i in 1..5) {
                    val candidate = wordRepository.getRandomWord(prefix)
                    if (candidate != null && !_usedWords.value.contains(candidate)) {
                        botWord = candidate
                        break
                    }
                }

                if (botWord != null) {
                    _usedWords.value = _usedWords.value + botWord
                    _currentWord.value = botWord
                    startPlayerTurn()
                } else {
                    // Bot kehabisan kata / melakukan kesalahan
                    _botLives.value -= 1
                    if (_botLives.value <= 0) {
                        endGame("PLAYER")
                    } else {
                        startPlayerTurn()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Jika terjadi error pada bot, berikan giliran kembali ke pemain agar game tidak freeze
                startPlayerTurn()
            }
        }
    }

    private fun endGame(winningSide: String) {
        _status.value = "Finished"
        _winner.value = winningSide

        viewModelScope.launch {
            val isWin = winningSide == "PLAYER"
            val finalScore = if (isWin) score + 50 else score // Bonus 50 poin jika menang
            
            val history = MatchHistoryEntity(
                date = System.currentTimeMillis(),
                mode = "OFFLINE",
                opponent = "Bot",
                result = if (isWin) "WIN" else "LOSE",
                score = finalScore,
                correctWords = correctWordsCount,
                wrongWords = wrongWordsCount,
                wordsPlayedJson = Json.encodeToString(playedWordsList)
            )
            historyRepository.insertHistory(history)
        }
    }
}
