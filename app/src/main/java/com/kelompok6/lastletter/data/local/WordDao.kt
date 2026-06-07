package com.kelompok6.lastletter.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface WordDao {
    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE word = :inputWord LIMIT 1)")
    suspend fun isValidWord(inputWord: String): Boolean

    // TAMBAHAN: instr(word, ' ') = 0 artinya pastikan tidak ada spasi di dalam kata
    @Query("""
        SELECT word FROM words 
        WHERE word LIKE :prefix || '%' 
        AND word NOT IN (:usedWords) 
        AND instr(word, ' ') = 0 
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomWordStartingWith(prefix: String, usedWords: List<String>): String?
}