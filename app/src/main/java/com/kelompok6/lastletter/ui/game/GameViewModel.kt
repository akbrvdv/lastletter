package com.kelompok6.lastletter.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
import kotlin.random.Random

@HiltViewModel
class GameViewModel @Inject constructor(
    private val historyRepository: MatchHistoryRepository,
    private val wordValidator: WordValidator
) : ViewModel() {

    private val db = FirebaseDatabase.getInstance().getReference("rooms")
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    private val _roomStatus = MutableStateFlow("NONE") // NONE, WAITING, PLAYING, FINISHED
    val roomStatus: StateFlow<String> = _roomStatus.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _opponentName = MutableStateFlow("Menunggu Lawan...")
    val opponentName: StateFlow<String> = _opponentName.asStateFlow()

    private val _currentWord = MutableStateFlow("")
    val currentWord: StateFlow<String> = _currentWord.asStateFlow()

    private val _turn = MutableStateFlow("") // HOST atau GUEST
    val turn: StateFlow<String> = _turn.asStateFlow()

    private val _playerLives = MutableStateFlow(3)
    val playerLives: StateFlow<Int> = _playerLives.asStateFlow()

    private val _opponentLives = MutableStateFlow(3)
    val opponentLives: StateFlow<Int> = _opponentLives.asStateFlow()

    // WAKTU DIUBAH MENJADI 15 DETIK
    private val _timeLeft = MutableStateFlow(15)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage.asStateFlow()

    private var roomListener: ValueEventListener? = null
    private var timerJob: Job? = null

    // History Tracking
    private val usedWords = mutableListOf<String>()
    private val playedWordsList = mutableListOf<PlayedWordItem>()
    private var score = 0
    private var correctWords = 0
    private var wrongWords = 0

    // 1. CREATE ROOM
    fun createRoom() {
        val code = Random.nextInt(100000, 999999).toString() // Generate 6 Digit Code
        _roomCode.value = code
        _isHost.value = true
        _roomStatus.value = "WAITING"

        val playerName = currentUser?.displayName ?: "Player 1"

        val roomData = mapOf(
            "status" to "WAITING",
            "hostName" to playerName,
            "guestName" to "",
            "hostLives" to 3,
            "guestLives" to 3,
            "currentWord" to "",
            "turn" to "HOST",
            "timeLeft" to 15 // Set awal 15 detik
        )
        db.child(code).setValue(roomData)
        listenToRoom(code)
    }

    // 2. JOIN ROOM
    fun joinRoom(code: String) {
        val roomRef = db.child(code)
        roomRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && snapshot.child("status").value == "WAITING") {
                _roomCode.value = code
                _isHost.value = false
                _roomStatus.value = "PLAYING"

                val hostName = snapshot.child("hostName").value.toString()
                _opponentName.value = hostName

                val guestName = currentUser?.displayName ?: "Player 2"
                roomRef.child("guestName").setValue(guestName)
                roomRef.child("status").setValue("PLAYING")

                listenToRoom(code)
            } else {
                _infoMessage.value = "Room tidak ditemukan atau sedang bermain!"
            }
        }
    }

    // 3. MENDENGARKAN PERUBAHAN DARI FIREBASE
    private fun listenToRoom(code: String) {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val status = snapshot.child("status").value.toString()
                _roomStatus.value = status

                if (status == "PLAYING") {
                    _opponentName.value = if (_isHost.value) snapshot.child("guestName").value.toString() else snapshot.child("hostName").value.toString()
                    _currentWord.value = snapshot.child("currentWord").value.toString()
                    _turn.value = snapshot.child("turn").value.toString()
                    _timeLeft.value = snapshot.child("timeLeft").value.toString().toIntOrNull() ?: 15

                    val hLives = snapshot.child("hostLives").value.toString().toIntOrNull() ?: 3
                    val gLives = snapshot.child("guestLives").value.toString().toIntOrNull() ?: 3

                    _playerLives.value = if (_isHost.value) hLives else gLives
                    _opponentLives.value = if (_isHost.value) gLives else hLives

                    manageTimer()
                } else if (status == "FINISHED") {
                    saveHistory()
                    timerJob?.cancel()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child(code).addValueEventListener(roomListener!!)
    }

    // 4. MENGELOLA TIMER 15 DETIK
    private fun manageTimer() {
        val isMyTurn = (_isHost.value && _turn.value == "HOST") || (!_isHost.value && _turn.value == "GUEST")

        timerJob?.cancel()
        if (isMyTurn && _roomStatus.value == "PLAYING") {
            timerJob = viewModelScope.launch {
                while (_timeLeft.value > 0) {
                    delay(1000)
                    val newTime = _timeLeft.value - 1
                    db.child(_roomCode.value).child("timeLeft").setValue(newTime)
                }
                // Kalau Waktu Habis
                handleMistake(timeout = true)
            }
        }
    }

    // 5. INPUT KATA PEMAIN
    fun submitWord(word: String) {
        val cleanedWord = word.trim().lowercase()
        timerJob?.cancel()

        viewModelScope.launch {
            val cw = _currentWord.value
            val validationResult = wordValidator.validate(cleanedWord, cw.ifEmpty { null }, usedWords.toSet())
            val isValid = validationResult is WordValidationResult.Success

            playedWordsList.add(PlayedWordItem(cleanedWord, isValid, cleanedWord.isEmpty()))

            if (isValid) {
                usedWords.add(cleanedWord)
                score += 10
                correctWords++

                val nextTurn = if (_isHost.value) "GUEST" else "HOST"
                val updates = mapOf(
                    "currentWord" to cleanedWord,
                    "turn" to nextTurn,
                    "timeLeft" to 15 // Reset ke 15 detik
                )
                db.child(_roomCode.value).updateChildren(updates)
            } else {
                handleMistake(timeout = false)
            }
        }
    }

    private fun handleMistake(timeout: Boolean) {
        if (timeout) playedWordsList.add(PlayedWordItem("-", false, true))
        score -= 5
        wrongWords++

        val livesRef = if (_isHost.value) "hostLives" else "guestLives"
        val newLives = _playerLives.value - 1

        if (newLives <= 0) {
            db.child(_roomCode.value).child("status").setValue("FINISHED")
        } else {
            val nextTurn = if (_isHost.value) "GUEST" else "HOST"
            val updates = mapOf(livesRef to newLives, "turn" to nextTurn, "timeLeft" to 15) // Reset ke 15 detik
            db.child(_roomCode.value).updateChildren(updates)
        }
    }

    private fun saveHistory() {
        if (playedWordsList.isEmpty()) return // Jangan simpan kalau belum main
        viewModelScope.launch {
            val isWin = _playerLives.value > 0
            val finalScore = if (isWin) score + 50 else score

            val history = MatchHistoryEntity(
                date = System.currentTimeMillis(),
                mode = "PVP ONLINE",
                opponent = _opponentName.value,
                result = if (isWin) "WIN" else "LOSE",
                score = finalScore,
                correctWords = correctWords,
                wrongWords = wrongWords,
                wordsPlayedJson = Json.encodeToString(playedWordsList)
            )
            historyRepository.insertHistory(history)
            playedWordsList.clear() // Bersihkan setelah simpan
        }
    }

    fun leaveRoom() {
        timerJob?.cancel()
        if (_roomCode.value.isNotEmpty()) {
            roomListener?.let { db.child(_roomCode.value).removeEventListener(it) }
            // Jika host keluar saat nunggu, hapus room
            if (_isHost.value && _roomStatus.value == "WAITING") {
                db.child(_roomCode.value).removeValue()
            }
        }
        _roomStatus.value = "NONE"
        _roomCode.value = ""
    }
}