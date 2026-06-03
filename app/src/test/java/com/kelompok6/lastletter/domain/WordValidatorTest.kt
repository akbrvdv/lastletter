package com.kelompok6.lastletter.domain

import com.kelompok6.lastletter.data.local.WordDao
import com.kelompok6.lastletter.domain.model.WordValidationResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WordValidatorTest {

    private lateinit var wordDao: WordDao
    private lateinit var wordValidator: WordValidator

    @Before
    fun setUp() {
        // Inisialisasi MockK untuk DAO dan SUT (System Under Test)
        wordDao = mockk()
        wordValidator = WordValidator(wordDao)
    }

    @Test
    fun validateWord_whenWordAlreadyUsed_returnsAlreadyUsed() = runTest {
        // Given
        val inputWord = "apel"
        val lastWord = "semangka" // Berakhiran 'a'
        val usedWords = setOf("apel", "lain")

        // When
        val result = wordValidator.validate(inputWord, lastWord, usedWords)

        // Then
        assertEquals(WordValidationResult.AlreadyUsed, result)
    }

    @Test
    fun validateWord_whenFirstLetterMismatchesLastWord_returnsInvalidLetter() = runTest {
        // Given
        val inputWord = "cacing"
        val lastWord = "bunga" // Berakhiran 'a', sedangkan inputWord berawalan 'c'
        val usedWords = emptySet<String>()

        // When
        val result = wordValidator.validate(inputWord, lastWord, usedWords)

        // Then
        assertEquals(WordValidationResult.InvalidFirstLetter('a'), result)
    }

    @Test
    fun validateWord_whenWordNotInDictionary_returnsNotInDictionary() = runTest {
        // Given
        val inputWord = "ayam"
        val lastWord = "bunga" // Berakhiran 'a', cocok dengan 'a'yam
        val usedWords = emptySet<String>()

        // Mock response false dari DAO
        coEvery { wordDao.isValidWord("ayam") } returns false

        // When
        val result = wordValidator.validate(inputWord, lastWord, usedWords)

        // Then
        assertEquals(WordValidationResult.NotInDictionary, result)
    }

    @Test
    fun validateWord_whenFirstTurnAndValid_returnsSuccess() = runTest {
        // Given
        val inputWord = "bunga"
        val lastWord = null // Null menandakan giliran pertama
        val usedWords = emptySet<String>()

        // Mock response true dari DAO
        coEvery { wordDao.isValidWord("bunga") } returns true

        // When
        val result = wordValidator.validate(inputWord, lastWord, usedWords)

        // Then
        assertEquals(WordValidationResult.Success, result)
    }

    @Test
    fun validateWord_whenAllRulesValid_returnsSuccess() = runTest {
        // Given
        val inputWord = "ayam"
        val lastWord = "bunga"
        val usedWords = emptySet<String>()

        // Mock response true dari DAO
        coEvery { wordDao.isValidWord("ayam") } returns true

        // When
        val result = wordValidator.validate(inputWord, lastWord, usedWords)

        // Then
        assertEquals(WordValidationResult.Success, result)
    }

    @Test
    fun validateWord_whenInputHasSpacesOrUppercase_remainsValid() = runTest {
        // Given
        val inputWord = "  AyaM  "
        val lastWord = "bungA" // Test ketahanan terhadap huruf besar pada lastWord
        val usedWords = emptySet<String>()

        // Mock response true dari DAO dengan kata yang sudah dibersihkan (cleaned)
        coEvery { wordDao.isValidWord("ayam") } returns true

        // When
        val result = wordValidator.validate(inputWord, lastWord, usedWords)

        // Then
        assertEquals(WordValidationResult.Success, result)
    }
}
