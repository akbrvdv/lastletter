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

    private val _turn = MutableStateFlow("HOST") // HOST atau GUEST
    val turn: StateFlow<String> = _turn.asStateFlow()

    private val _playerLives = MutableStateFlow(3)
    val playerLives: StateFlow<Int> = _playerLives.asStateFlow()

    private val _opponentLives = MutableStateFlow(3)
    val opponentLives: StateFlow<Int> = _opponentLives.asStateFlow()

    private val _timeLeft = MutableStateFlow(15)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage.asStateFlow()

    private var roomListener: ValueEventListener? = null
    private var timerJob: Job? = null

    // Timer lokal untuk memastikan host mengendalikan waktu agar tidak terjadi tabrakan Firebase
    private var localTimerJob: Job? = null

    private val usedWords = mutableListOf<String>()
    private val playedWordsList = mutableListOf<PlayedWordItem>()
    private var score = 0
    private var correctWords = 0
    private var wrongWords = 0

    // 1. HOST MEMBUAT ROOM
    fun createRoom() {
        val code = Random.nextInt(100000, 999999).toString()
        _roomCode.value = code
        _isHost.value = true
        _roomStatus.value = "WAITING"
        _turn.value = "HOST"

        val playerName = currentUser?.displayName ?: "Player 1"

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
        db.child(code).setValue(roomData)
        listenToRoom(code)
    }

    // 2. GUEST MASUK KE ROOM
    fun joinRoom(code: String) {
        val roomRef = db.child(code)
        roomRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && snapshot.child("status").value == "WAITING") {
                _roomCode.value = code
                _isHost.value = false
                _turn.value = "HOST" // Giliran pertama selalu Host

                val hostName = snapshot.child("hostName").value.toString()
                _opponentName.value = hostName

                val guestName = currentUser?.displayName ?: "Player 2"

                // Memicu trigger PLAYING di Firebase
                val updates = mapOf(
                    "guestName" to guestName,
                    "status" to "PLAYING"
                )

                roomRef.updateChildren(updates).addOnSuccessListener {
                    _roomStatus.value = "PLAYING"
                    listenToRoom(code)
                }
            } else {
                _infoMessage.value = "Kode Room tidak valid atau Room penuh!"
                // Auto hapus pesan error setelah 3 detik
                viewModelScope.launch { delay(3000); _infoMessage.value = "" }
            }
        }.addOnFailureListener {
            _infoMessage.value = "Gagal menghubungi server!"
        }
    }

    // 3. LISTENER UTAMA MULTIPLAYER
    private fun listenToRoom(code: String) {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Jika data terhapus mendadak (Host keluar)
                    if (_roomStatus.value != "FINISHED") {
                        _infoMessage.value = "Room ditutup oleh Host."
                        leaveRoom()
                    }
                    return
                }

                val status = snapshot.child("status").value.toString()

                if (status == "PLAYING" && _roomStatus.value == "WAITING") {
                    // Host menyadari Guest masuk!
                    _roomStatus.value = "PLAYING"
                    _opponentName.value = snapshot.child("guestName").value.toString()
                }

                if (status == "PLAYING") {
                    val fireWord = snapshot.child("currentWord").value.toString()
                    val fireTurn = snapshot.child("turn").value.toString()

                    // Hindari loop tak terbatas saat mengetik
                    if (_currentWord.value != fireWord) _currentWord.value = fireWord
                    _turn.value = fireTurn

                    val hLives = snapshot.child("hostLives").value.toString().toIntOrNull() ?: 3
                    val gLives = snapshot.child("guestLives").value.toString().toIntOrNull() ?: 3

                    _playerLives.value = if (_isHost.value) hLives else gLives
                    _opponentLives.value = if (_isHost.value) gLives else hLives

                    // Sinkronisasi Timer dari Firebase
                    val fireTime = snapshot.child("timeLeft").value.toString().toIntOrNull() ?: 15
                    _timeLeft.value = fireTime

                    // Host bertanggung jawab menurunkan waktu agar tidak terjadi dobel hitungan
                    if (_isHost.value) manageTimer()
                } else if (status == "FINISHED" && _roomStatus.value != "FINISHED") {
                    _roomStatus.value = "FINISHED"
                    saveHistory()
                    timerJob?.cancel()
                    localTimerJob?.cancel()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.child(code).addValueEventListener(roomListener!!)
    }

    // 4. TIMER CONTROL (Hanya dijalankan oleh HOST untuk update Firebase)
    private fun manageTimer() {
        if (!_isHost.value) return // Guest hanya membaca waktu, tidak menguranginya di database

        localTimerJob?.cancel()
        if (_roomStatus.value == "PLAYING") {
            localTimerJob = viewModelScope.launch {
                delay(1000)
                if (_timeLeft.value > 0) {
                    val newTime = _timeLeft.value - 1
                    db.child(_roomCode.value).child("timeLeft").setValue(newTime)
                } else {
                    // Waktu habis, siapa yang kena denda?
                    handleMistake(timeout = true)
                }
            }
        }
    }

    // 5. INPUT JAWABAN (Baik Host maupun Guest)
    fun submitWord(word: String) {
        val cleanedWord = word.trim().lowercase()
        val isMyTurn = (_isHost.value && _turn.value == "HOST") || (!_isHost.value && _turn.value == "GUEST")

        if (!isMyTurn || _roomStatus.value != "PLAYING") return

        localTimerJob?.cancel() // Pause timer lokal saat mengecek kata

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
                handleMistake(timeout = false)
            }
        }
    }

    // 6. PENANGANAN SALAH JAWAB / WAKTU HABIS
    private fun handleMistake(timeout: Boolean) {
        // Tentukan nyawa siapa yang dikurangi berdasarkan siapa yang sedang jalan (Turn)
        val isHostTurn = _turn.value == "HOST"
        val livesRef = if (isHostTurn) "hostLives" else "guestLives"

        // Ambil sisa nyawa saat ini
        val currentLives = if (isHostTurn) {
            if (_isHost.value) _playerLives.value else _opponentLives.value
        } else {
            if (!_isHost.value) _playerLives.value else _opponentLives.value
        }

        val newLives = currentLives - 1

        if (timeout) playedWordsList.add(PlayedWordItem("-", false, true))

        // Catat statistik jika kita yang salah
        if ((isHostTurn && _isHost.value) || (!isHostTurn && !_isHost.value)) {
            score -= 5
            wrongWords++
        }

        if (newLives <= 0) {
            db.child(_roomCode.value).child("status").setValue("FINISHED")
            db.child(_roomCode.value).child(livesRef).setValue(0)
        } else {
            val nextTurn = if (isHostTurn) "GUEST" else "HOST"
            val updates = mapOf(
                livesRef to newLives,
                "turn" to nextTurn,
                "timeLeft" to 15,
                // Beri kata pancingan gampang kalau timeout/salah agar game tidak macet
                "currentWord" to listOf("RUMAH", "BUMI", "LAMPU", "MEJA").random()
            )
            db.child(_roomCode.value).updateChildren(updates)
        }
    }

    private fun saveHistory() {
        if (playedWordsList.isEmpty()) return
        viewModelScope.launch {
            val isWin = _playerLives.value > 0
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
                // Host keluar -> Hancurkan room
                db.child(_roomCode.value).removeValue()
            } else if (_roomStatus.value == "PLAYING") {
                // Guest keluar di tengah game -> Beri kemenangan ke Host
                db.child(_roomCode.value).child("status").setValue("FINISHED")
                db.child(_roomCode.value).child("guestLives").setValue(0)
            }
        }
        _roomStatus.value = "NONE"
        _roomCode.value = ""
        _opponentName.value = "Menunggu Lawan..."
        usedWords.clear()
        playedWordsList.clear()
    }
}