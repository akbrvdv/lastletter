package com.kelompok6.lastletter.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.kelompok6.lastletter.data.model.GameRoom
import com.kelompok6.lastletter.domain.WordValidator
import com.kelompok6.lastletter.domain.model.WordValidationResult
import com.kelompok6.lastletter.domain.repository.GameRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GameRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val wordValidator: WordValidator
) : GameRepository {

    private val roomsRef = firebaseDatabase.getReference("rooms")

    override fun createOrJoinRoom(playerId: String): Flow<GameRoom?> = callbackFlow {
        // Cari room yang sedang WAITING
        val query = roomsRef.orderByChild("status").equalTo("WAITING").limitToFirst(1)

        query.get().addOnSuccessListener { snapshot ->
            try {
                if (snapshot.exists() && snapshot.children.count() > 0) {
                    val roomSnapshot = snapshot.children.first()
                    val room = roomSnapshot.getValue(GameRoom::class.java)

                    if (room != null && room.player1Id != playerId) {
                        // Ada room menunggu, kita bergabung
                        val currentTurn = listOf(room.player1Id, playerId).random()
                        val updates = mapOf(
                            "player2Id" to playerId,
                            "status" to "PLAYING",
                            "currentTurnId" to currentTurn
                        )

                        roomSnapshot.ref.updateChildren(updates).addOnSuccessListener {
                            val updatedRoom = room.copy(
                                player2Id = playerId,
                                status = "PLAYING",
                                currentTurnId = currentTurn
                            )
                            trySend(updatedRoom)
                            close()
                        }.addOnFailureListener {
                            close(it)
                        }
                    } else {
                        // Jika room yang menunggu ternyata milik kita sendiri
                        createNewRoom(playerId) { resultRoom, exception ->
                            if (exception != null) close(exception)
                            else {
                                trySend(resultRoom)
                                close()
                            }
                        }
                    }
                } else {
                    // Tidak ada room menunggu, buat baru
                    createNewRoom(playerId) { resultRoom, exception ->
                        if (exception != null) close(exception)
                        else {
                            trySend(resultRoom)
                            close()
                        }
                    }
                }
            } catch (e: Exception) {
                close(e)
            }
        }.addOnFailureListener {
            close(it)
        }

        awaitClose { }
    }

    private fun createNewRoom(playerId: String, onComplete: (GameRoom?, Exception?) -> Unit) {
        val newRoomRef = roomsRef.push()
        val roomId = newRoomRef.key ?: ""
        val newRoom = GameRoom(
            roomId = roomId,
            status = "WAITING",
            player1Id = playerId
        )
        newRoomRef.setValue(newRoom)
            .addOnSuccessListener { onComplete(newRoom, null) }
            .addOnFailureListener { onComplete(null, it) }
    }

    override fun observeRoom(roomId: String): Flow<GameRoom?> = callbackFlow {
        val roomRef = roomsRef.child(roomId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val room = snapshot.getValue(GameRoom::class.java)
                    trySend(room)
                } catch (e: Exception) {
                    close(e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        roomRef.addValueEventListener(listener)
        awaitClose {
            roomRef.removeEventListener(listener)
        }
    }

    override suspend fun submitWord(roomId: String, playerId: String, wordInput: String): Result<Unit> {
        val roomRef = roomsRef.child(roomId)

        return suspendCancellableCoroutine { continuation ->
            roomRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val room = mutableData.getValue(GameRoom::class.java)
                        ?: return Transaction.success(mutableData)

                    // Pastikan game sedang berjalan dan giliran pemain ini
                    if (room.status != "PLAYING" || room.currentTurnId != playerId) {
                        return Transaction.abort()
                    }

                    // Karena kita butuh memanggil suspend function (WordValidator), 
                    // dan doTransaction dari Firebase bersifat sinkron (non-suspend), kita gunakan runBlocking
                    val validationResult = runBlocking {
                        wordValidator.validate(wordInput, room.lastWord, room.usedWords.keys)
                    }

                    if (validationResult is WordValidationResult.Success) {
                        val cleanedWord = wordInput.trim().lowercase()
                        val nextPlayerId = if (playerId == room.player1Id) room.player2Id else room.player1Id

                        val updatedUsedWords = room.usedWords.toMutableMap()
                        updatedUsedWords[cleanedWord] = true

                        // Terapkan perubahan pada Room
                        room.lastWord = cleanedWord
                        room.usedWords = updatedUsedWords
                        room.currentTurnId = nextPlayerId

                        mutableData.value = room
                        return Transaction.success(mutableData)
                    } else {
                        // Jika validasi gagal (salah huruf, sudah terpakai, atau tidak ada di kamus)
                        return Transaction.abort()
                    }
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        if (continuation.isActive) continuation.resume(Result.failure(Exception("Firebase error: ${error.message}")))
                    } else if (!committed) {
                        if (continuation.isActive) continuation.resume(Result.failure(Exception("Transaksi gagal. Kemungkinan kata tidak valid atau bukan giliran Anda.")))
                    } else {
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
                }
            })
        }
    }

    override suspend fun updateLastSeen(roomId: String, playerId: String) {
        return suspendCancellableCoroutine { continuation ->
            val lastSeenRef = roomsRef.child(roomId).child("players").child(playerId)
            lastSeenRef.setValue(ServerValue.TIMESTAMP)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        }
    }

    override suspend fun claimVictory(roomId: String, winnerId: String) {
        return suspendCancellableCoroutine { continuation ->
            val updates = mapOf(
                "status" to "FINISHED",
                "winnerId" to winnerId
            )
            roomsRef.child(roomId).updateChildren(updates)
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        }
    }
}
