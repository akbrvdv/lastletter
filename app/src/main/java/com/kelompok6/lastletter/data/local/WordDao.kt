package com.kelompok6.lastletter.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT * FROM words LIMIT 1")
    suspend fun getDummyWord(): WordEntity?
}