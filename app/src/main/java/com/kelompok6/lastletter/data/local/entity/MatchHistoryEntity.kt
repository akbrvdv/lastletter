package com.kelompok6.lastletter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class PlayedWordItem(
    val word: String,
    val isCorrect: Boolean,
    val isTimeout: Boolean
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val mode: String, // "OFFLINE", "ONLINE"
    val opponent: String,
    val result: String, // "WIN", "LOSE"
    val score: Int,
    val correctWords: Int,
    val wrongWords: Int,
    val wordsPlayedJson: String = "[]"
)
