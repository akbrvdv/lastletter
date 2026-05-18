package com.kelompok6.lastletter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kelompok6.lastletter.ui.auth.AuthScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { // Menggunakan tema bawaan sementara
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Panggil halaman Auth
                    AuthScreen(
                        onLoginSuccess = {
                            // TODO: Nanti Dava akan menyambungkan ini ke Homepage buatannya
                            Toast.makeText(this, "Login Sukses! Siap masuk Homepage", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}