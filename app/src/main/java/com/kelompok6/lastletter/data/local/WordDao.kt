package com.kelompok6.lastletter.data.local

import androidx.room.Dao

/**
 * WordDao — stub placeholder untuk Task 0.2.
 * Implementasi lengkap dengan @Query MATCH akan dibuat di Task 1.2.
 */
@Dao
interface WordDao {
    // Phase 1.2: implementasi FTS5 MATCH query
    // @Query("SELECT COUNT(*) FROM words WHERE words MATCH :word")
    // suspend fun isValidWord(word: String): Int
}
