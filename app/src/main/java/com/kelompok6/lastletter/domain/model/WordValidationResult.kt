package com.kelompok6.lastletter.domain.model

sealed class WordValidationResult {
    object Success : WordValidationResult()
    object EmptyInput : WordValidationResult()
    data class InvalidFirstLetter(val expectedLetter: Char) : WordValidationResult()
    object NotInDictionary : WordValidationResult()
    object AlreadyUsed : WordValidationResult()
}
