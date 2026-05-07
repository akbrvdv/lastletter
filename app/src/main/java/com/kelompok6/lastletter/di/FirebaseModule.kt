package com.kelompok6.lastletter.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FirebaseModule — menyediakan singleton instance Firebase ke seluruh graph DI.
 *
 * Mengapa @Singleton?
 * - FirebaseAuth & FirebaseDatabase adalah thread-safe singletons di level SDK.
 *   Membuat multiple instance tidak diperlukan dan boros resource.
 * - Persistence diaktifkan SEKALI di sini, tidak di setiap Repository.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Firebase Realtime Database.
     *
     * keepSynced(true) pada node kritis akan di-setup di Repository masing-masing,
     * bukan di sini, agar tetap granular.
     *
     * setPersistenceEnabled(true): mengaktifkan disk cache Firebase RTDB
     * sehingga data terakhir tetap tersedia saat offline.
     */
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().also {
            it.setPersistenceEnabled(true)
        }
    }

    /**
     * Firebase Authentication.
     * Digunakan oleh AuthRepository untuk Anonymous Sign-In di Phase 0.3.
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
