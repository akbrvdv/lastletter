package com.kelompok6.lastletter.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kelompok6.lastletter.data.local.WordDatabase
import com.kelompok6.lastletter.data.local.WordDao
import com.kelompok6.lastletter.data.local.dao.MatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // Hapus variabel MIGRATION_1_2 dari sini

    @Provides
    @Singleton
    fun provideWordDatabase(@ApplicationContext context: Context): WordDatabase {

        // Pindahkan MIGRATION_1_2 ke dalam scope fungsi ini
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `match_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `mode` TEXT NOT NULL, 
                        `opponent` TEXT NOT NULL, 
                        `result` TEXT NOT NULL, 
                        `score` INTEGER NOT NULL, 
                        `correctWords` INTEGER NOT NULL, 
                        `wrongWords` INTEGER NOT NULL, 
                        `wordsPlayedJson` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        return Room.databaseBuilder(
            context,
            WordDatabase::class.java,
            "kamus.db"
        )
            .createFromAsset("database/kamus.db")
            .addMigrations(MIGRATION_1_2) // Panggil di sini
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(database: WordDatabase): WordDao {
        return database.wordDao()
    }

    @Provides
    @Singleton
    fun provideMatchHistoryDao(database: WordDatabase): MatchHistoryDao {
        return database.matchHistoryDao()
    }
}