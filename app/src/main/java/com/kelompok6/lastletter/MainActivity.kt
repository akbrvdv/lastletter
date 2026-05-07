package com.kelompok6.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kelompok6.lastletter.ui.theme.LastLetterTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — Single Activity, entry point UI.
 *
 * @AndroidEntryPoint: memungkinkan Hilt menginjeksi dependency ke Activity ini
 * dan ke semua Composable yang di-host di sini via hiltViewModel().
 *
 * Navigation graph (NavHost) akan ditambahkan di Phase 3 saat semua screen siap.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LastLetterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Phase 3: Ganti dengan NavHost + AppNavigation()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Lexilink ⚡",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }
        }
    }
}