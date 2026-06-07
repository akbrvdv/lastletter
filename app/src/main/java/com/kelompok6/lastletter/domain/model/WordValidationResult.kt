package com.kelompok6.lastletter.domain.model

sealed class WordValidationResult {
    object Success : WordValidationResult()
    object EmptyInput : WordValidationResult()
    class InvalidFirstLetter(val expectedLetter: Char) : WordValidationResult()
    object NotInDictionary : WordValidationResult()
    object AlreadyUsed : WordValidationResult() // Ini yang memicu pengurangan nyawa jika kata berulang
}