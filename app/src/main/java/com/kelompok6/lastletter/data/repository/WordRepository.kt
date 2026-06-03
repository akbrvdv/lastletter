package com.kelompok6.lastletter.data.repository

import com.kelompok6.lastletter.data.local.WordDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {
    suspend fun getRandomWord(prefix: String): String? {
        return wordDao.getRandomWordStartingWith(prefix)
    }
}
