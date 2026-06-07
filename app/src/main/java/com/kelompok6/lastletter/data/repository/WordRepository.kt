package com.kelompok6.lastletter.data.repository

import com.kelompok6.lastletter.data.local.WordDao
import javax.inject.Inject

class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {
    suspend fun checkWordExists(word: String): Boolean {
        return wordDao.isValidWord(word)
    }

    // Perbaikan: Tambahkan parameter usedWords agar sinkron dengan WordDao
    suspend fun getRandomWord(prefix: String, usedWords: List<String>): String? {
        return wordDao.getRandomWordStartingWith(prefix, usedWords)
    }
}