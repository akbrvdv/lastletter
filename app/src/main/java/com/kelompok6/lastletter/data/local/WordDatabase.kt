package com.kelompok6.lastletter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kelompok6.lastletter.data.local.dao.MatchHistoryDao
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity

@Database(
    entities = [WordEntity::class, MatchHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WordDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun matchHistoryDao(): MatchHistoryDao
}