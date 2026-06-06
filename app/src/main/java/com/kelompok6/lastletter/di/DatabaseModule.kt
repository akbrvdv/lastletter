package com.kelompok6.lastletter.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideWordDatabase(@ApplicationContext context: Context): WordDatabase {
        return Room.databaseBuilder(
            context,
            WordDatabase::class.java,
            "kamus.db"
        )
            .createFromAsset("database/kamus.db")
            // Ini akan menghapus database lama yang error dan membuat tabel baru yang fresh
            .fallbackToDestructiveMigration()
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