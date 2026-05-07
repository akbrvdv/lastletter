package com.kelompok6.lastletter.di

import android.content.Context
import androidx.room.Room
import com.kelompok6.lastletter.data.local.WordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DatabaseModule — menyediakan WordDatabase dan WordDao ke dependency graph.
 *
 * Strategi inisialisasi:
 * - Menggunakan createFromAsset("dictionary.db") agar database yang sudah
 *   ter-populate dengan kamus Indonesia langsung tersedia tanpa setup runtime.
 * - File dictionary.db akan ditambahkan di Phase 1.1.
 * - fallbackToDestructiveMigration() hanya untuk development; akan diganti
 *   dengan migration strategy proper saat menambah schema baru.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWordDatabase(
        @ApplicationContext context: Context
    ): WordDatabase {
        return Room.databaseBuilder(
            context,
            WordDatabase::class.java,
            "lastletter.db"
        )
            // Phase 1: uncomment setelah dictionary.db tersedia di assets/
            // .createFromAsset("dictionary.db")
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(database: WordDatabase) = database.wordDao()
}
