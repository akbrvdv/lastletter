package com.kelompok6.lastletter.domain

import com.kelompok6.lastletter.data.local.WordDao
import com.kelompok6.lastletter.domain.model.WordValidationResult
import javax.inject.Inject

class WordValidator @Inject constructor(
    private val wordDao: WordDao
) {
    suspend fun validate(
        inputWord: String,
        lastWord: String?,
        usedWords: Set<String>
    ): WordValidationResult {
        // Tahap 1: Pembersihan input
        val cleanedWord = inputWord.trim().lowercase()

        // Tahap 2: Cek Kosong
        if (cleanedWord.isEmpty()) {
            return WordValidationResult.EmptyInput
        }

        // Tahap 3: Cek apakah kata SUDAH PERNAH DIPAKAI sebelumnya
        if (usedWords.contains(cleanedWord)) {
            return WordValidationResult.AlreadyUsed
        }

        // Tahap 4: Cek Huruf Awal
        if (!lastWord.isNullOrEmpty()) {
            val expectedLetter = lastWord.trim().lowercase().last()
            if (cleanedWord.first() != expectedLetter) {
                return WordValidationResult.InvalidFirstLetter(expectedLetter)
            }
        }

        // Tahap 5: Cek Kamus Lokal (Menggunakan fungsi aslimu: isValidWord)
        val isLocalValid = wordDao.isValidWord(cleanedWord)
        if (!isLocalValid) {
            return WordValidationResult.NotInDictionary
        }

        // Tahap 6: Sukses
        return WordValidationResult.Success
    }
}