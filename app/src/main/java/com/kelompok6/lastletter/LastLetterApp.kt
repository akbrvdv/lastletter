package com.kelompok6.lastletter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — titik entry Hilt.
 * @HiltAndroidApp men-trigger code generation untuk seluruh dependency graph.
 * Wajib dideklarasikan di AndroidManifest.xml via android:name=".LastLetterApp"
 */
@HiltAndroidApp
class LastLetterApp : Application()
