package com.kelompok6.lastletter.domain.repository

import com.kelompok6.lastletter.data.model.GameRoom
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun createOrJoinRoom(playerId: String): Flow<GameRoom?>
    fun observeRoom(roomId: String): Flow<GameRoom?>
    suspend fun submitWord(roomId: String, playerId: String, wordInput: String): Result<Unit>
    suspend fun updateLastSeen(roomId: String, playerId: String)
    suspend fun claimVictory(roomId: String, winnerId: String)
}
