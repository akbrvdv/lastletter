package com.kelompok6.lastletter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * WordDatabase — Room database stub untuk Task 0.2.
 *
 * Akan di-populate dengan entities dan DAO konkret di Phase 1.1:
 * - WordEntity (FTS5 virtual table)
 * - WordDao dengan MATCH query
 *
 * version = 1: Ini adalah schema awal. Setiap perubahan schema
 * HARUS menaikkan version dan menyediakan Migration.
 */
@Database(
    entities = [], // Phase 1.1: tambah WordEntity::class
    version = 1,
    exportSchema = true
)
abstract class WordDatabase : RoomDatabase() {
    // Phase 1.2: tambah abstract fun wordDao(): WordDao
    abstract fun wordDao(): WordDao
}
