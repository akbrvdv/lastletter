package com.kelompok6.lastletter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchHistoryDao {
    @Query("SELECT * FROM match_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(match: MatchHistoryEntity)
}
