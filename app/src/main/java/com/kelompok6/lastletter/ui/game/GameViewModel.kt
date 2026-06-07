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

    private val _roomCode = MutableStateFlow("")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    private val _roomStatus = MutableStateFlow("NONE")
    val roomStatus: StateFlow<String> = _roomStatus.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _opponentName = MutableStateFlow("Menunggu Lawan...")
    val opponentName: StateFlow<String> = _opponentName.asStateFlow()

    private val _currentWord = MutableStateFlow("")
    val currentWord: StateFlow<String> = _currentWord.asStateFlow()

    private val _turn = MutableStateFlow("HOST")
    val turn: StateFlow<String> = _turn.asStateFlow()

    private val _playerLives = MutableStateFlow(3)
    val playerLives: StateFlow<Int> = _playerLives.asStateFlow()

    private val _opponentLives = MutableStateFlow(3)
    val opponentLives: StateFlow<Int> = _opponentLives.asStateFlow()

    private val _timeLeft = MutableStateFlow(15)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var roomListener: ValueEventListener? = null
    private var timerJob: Job? = null
    private var localTimerJob: Job? = null

    private val usedWords = mutableListOf<String>()
    private val playedWordsList = mutableListOf<PlayedWordItem>()
    private var score = 0
    private var correctWords = 0
    private var wrongWords = 0

    // PERBAIKAN: Ambil nama langsung dari auth terbaru agar tidak "Pemain 98"
    private fun getMyName(): String {
        val user = auth.currentUser
        user?.reload() // Paksa reload agar data terbaru terbaca

        val name = user?.displayName
        if (!name.isNullOrBlank()) return name

        val email = user?.email
        if (!email.isNullOrBlank()) {
            return email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        return "Pemain " + Random.nextInt(10, 99)
    }

    fun createRoom() {
        _isLoading.value = true
        val code = Random.nextInt(100000, 999999).toString()
        val playerName = getMyName()

        val roomData = mapOf(
            "status" to "WAITING",
            "hostName" to playerName,
            "guestName" to "",
            "hostLives" to 3,
            "guestLives" to 3,
            "currentWord" to "",
            "turn" to "HOST",
            "timeLeft" to 15
        )

        var isResolved = false

        db.child(code).setValue(roomData)
            .addOnSuccessListener {
                isResolved = true
                _isLoading.value = false
                _roomCode.value = code
                _isHost.value = true
                _roomStatus.value = "WAITING"
                _turn.value = "HOST"
                listenToRoom(code)
            }
            .addOnFailureListener { error ->
                isResolved = true
                _isLoading.value = false
                _infoMessage.value = "Gagal membuat room: ${error.message}"
                viewModelScope.launch { delay(3000); _infoMessage.value = "" }
            }

        viewModelScope.launch {
            delay(10000)
            if (!isResolved) {
                _isLoading.value = false
                _infoMessage.value = "Waktu habis. Cek koneksi internetmu!"
                viewModelScope.launch { delay(3000); _infoMessage.value = "" }
            }
        }
    }

    fun joinRoom(code: String) {
        _isLoading.value = true
        var isResolved = false

        val roomRef = db.child(code)
        roomRef.get().addOnSuccessListener { snapshot ->
            isResolved = true
            _isLoading.value = false
            if (snapshot.exists() && snapshot.child("status").value == "WAITING") {
                _roomCode.value = code
                _isHost.value = false
                _turn.value = "HOST"

                val guestName = getMyName()

                val updates = mapOf(
                    "guestName" to guestName,
                    "status" to "PLAYING"
                )

                roomRef.updateChildren(updates).addOnSuccessListener {
                    _roomStatus.value = "PLAYING"
                    listenToRoom(code)
                }
            } else {
                _infoMessage.value = "Kode Room tidak valid atau penuh!"
                viewModelScope.launch { delay(3000); _infoMessage.value = "" }
            }
        }.addOnFailureListener { error ->
            isResolved = true
            _isLoading.value = false
            _infoMessage.value = "Gagal terhubung ke server: ${error.message}"
            viewModelScope.launch { delay(3000); _infoMessage.value = "" }
        }

        viewModelScope.launch {
            delay(10000)
            if (!isResolved) {
                _isLoading.value = false
                _infoMessage.value = "Waktu habis. Cek koneksi internetmu!"
                viewModelScope.launch { delay(3000); _infoMessage.value = "" }
            }
        }
    }

    private fun listenToRoom(code: String) {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    if (_roomStatus.value != "FINISHED") {
                        _infoMessage.value = "Room ditutup oleh Host."
                        viewModelScope.launch { delay(3000); _infoMessage.value = "" }
                        leaveRoom()
                    }
                    return
                }

                val status = snapshot.child("status").value.toString()

                // PERBAIKAN: Realtime Name Sync
                val fireHostName = snapshot.child("hostName").value?.toString() ?: "Lawan"
                val fireGuestName = snapshot.child("guestName").value?.toString() ?: "Lawan"

                if (_isHost.value) {
                    _opponentName.value = if (fireGuestName.isNotBlank()) fireGuestName else "Menunggu Lawan..."
                } else {
                    _opponentName.value = if (fireHostName.isNotBlank()) fireHostName else "Menunggu Lawan..."
                }

                if (status == "PLAYING" && _roomStatus.value == "WAITING") {
                    _roomStatus.value = "PLAYING"
                }

                val hLives = snapshot.child("hostLives").value.toString().toIntOrNull() ?: 3
                val gLives = snapshot.child("guestLives").value.toString().toIntOrNull() ?: 3

                _playerLives.value = if (_isHost.value) hLives else gLives
                _opponentLives.value = if (_isHost.value) gLives else hLives

                if (status == "PLAYING") {
                    val fireWord = snapshot.child("currentWord").value.toString()
                    val fireTurn = snapshot.child("turn").value.toString()

                    if (_currentWord.value != fireWord) _currentWord.value = fireWord
                    _turn.value = fireTurn

                    val fireTime = snapshot.child("timeLeft").value.toString().toIntOrNull() ?: 15
                    _timeLeft.value = fireTime

                    if (_isHost.value) manageTimer()
                } else if (status == "FINISHED" && _roomStatus.value != "FINISHED") {
                    _roomStatus.value = "FINISHED"
                    saveHistory()
                    timerJob?.cancel()
                    localTimerJob?.cancel()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _infoMessage.value = "Koneksi database terputus: ${error.message}"
                viewModelScope.launch { delay(3000); _infoMessage.value = "" }
                leaveRoom()
            }
        }
        db.child(code).addValueEventListener(roomListener!!)
    }

    private fun manageTimer() {
        if (!_isHost.value) return

        localTimerJob?.cancel()
        if (_roomStatus.value == "PLAYING") {
            localTimerJob = viewModelScope.launch {
                delay(1000)
                if (_timeLeft.value > 0) {
                    val newTime = _timeLeft.value - 1
                    db.child(_roomCode.value).child("timeLeft").setValue(newTime)
                } else {
                    handleMistake(timeout = true)
                }
            }
        }
    }

    fun submitWord(word: String) {
        val cleanedWord = word.trim().lowercase()
        val isMyTurn = (_isHost.value && _turn.value == "HOST") || (!_isHost.value && _turn.value == "GUEST")

        if (!isMyTurn || _roomStatus.value != "PLAYING") return

        localTimerJob?.cancel()

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
                    "timeLeft" to 15
                )
                db.child(_roomCode.value).updateChildren(updates)
            } else {
                val errorMsg = when (validationResult) {
                    is WordValidationResult.AlreadyUsed -> "Kata '$cleanedWord' sudah dipakai!"
                    is WordValidationResult.InvalidFirstLetter -> "Huruf awal harus '${validationResult.expectedLetter.uppercaseChar()}'!"
                    is WordValidationResult.NotInDictionary -> "Kata '$cleanedWord' tidak ada di KBBI!"
                    else -> "Jawaban Salah!"
                }
                handleMistake(timeout = false, errorMessage = errorMsg)
            }
        }
    }

    private fun handleMistake(timeout: Boolean, errorMessage: String? = null) {
        val isHostTurn = _turn.value == "HOST"
        val livesRef = if (isHostTurn) "hostLives" else "guestLives"

        val currentLives = if (isHostTurn) {
            if (_isHost.value) _playerLives.value else _opponentLives.value
        } else {
            if (!_isHost.value) _playerLives.value else _opponentLives.value
        }

        val newLives = currentLives - 1

        if (timeout) playedWordsList.add(PlayedWordItem("-", false, true))

        if ((isHostTurn && _isHost.value) || (!isHostTurn && !_isHost.value)) {
            score -= 5
            wrongWords++

            _infoMessage.value = if (timeout) "Waktu Habis!" else (errorMessage ?: "Jawaban Salah!")
            viewModelScope.launch { delay(2000); _infoMessage.value = "" }
        }

        if (newLives <= 0) {
            val finalUpdates = mapOf(
                "status" to "FINISHED",
                livesRef to 0
            )
            db.child(_roomCode.value).updateChildren(finalUpdates)
        } else {
            val nextTurn = if (isHostTurn) "GUEST" else "HOST"
            val updates = mapOf(
                livesRef to newLives,
                "turn" to nextTurn,
                "timeLeft" to 15,
                "currentWord" to listOf("RUMAH", "BUMI", "LAMPU", "MEJA").random()
            )
            db.child(_roomCode.value).updateChildren(updates)
        }
    }

    private fun saveHistory() {
        if (playedWordsList.isEmpty()) return
        viewModelScope.launch {
            val isWin = _playerLives.value > _opponentLives.value
            val finalScore = if (isWin) score + 50 else score

            val history = MatchHistoryEntity(
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
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
            playedWordsList.clear()
        }
    }

    fun leaveRoom() {
        localTimerJob?.cancel()
        timerJob?.cancel()
        if (_roomCode.value.isNotEmpty()) {
            roomListener?.let { db.child(_roomCode.value).removeEventListener(it) }

            if (_isHost.value) {
                db.child(_roomCode.value).removeValue()
            } else if (_roomStatus.value == "PLAYING") {
                val leaveUpdates = mapOf(
                    "status" to "FINISHED",
                    "guestLives" to 0
                )
                db.child(_roomCode.value).updateChildren(leaveUpdates)
            }
        }
        _roomStatus.value = "NONE"
        _roomCode.value = ""
        _opponentName.value = "Menunggu Lawan..."
        usedWords.clear()
        playedWordsList.clear()
    }
}