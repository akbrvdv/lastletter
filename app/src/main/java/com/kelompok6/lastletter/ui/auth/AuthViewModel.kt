package com.kelompok6.lastletter.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object RegisterSuccess : AuthState() // 1. TAMBAHKAN STATE BARU INI
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Tetap mati sesuai algoritma awal kita kemarin
        /* if (auth.currentUser != null) {
            _authState.value = AuthState.Success
        } */
    }

    // 2. TAMBAHKAN FUNGSI BARU INI: Untuk mereset status setelah berhasil register
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email dan password tidak boleh kosong")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    val errorMsg = task.exception?.message?.lowercase() ?: ""
                    val customMsg = when {
                        errorMsg.contains("password") || errorMsg.contains("credential") -> "Password anda salah"
                        errorMsg.contains("user not found") || errorMsg.contains("email") || errorMsg.contains("record") -> "Email anda salah atau belum terdaftar"
                        else -> "Gagal: ${task.exception?.localizedMessage}"
                    }
                    _authState.value = AuthState.Error(customMsg)
                }
            }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email dan password tidak boleh kosong")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 3. UBAH DI SINI: Sign out otomatis dan ubah state ke RegisterSuccess
                    auth.signOut()
                    _authState.value = AuthState.RegisterSuccess
                } else {
                    val errorMsg = task.exception?.message?.lowercase() ?: ""
                    val customMsg = if (errorMsg.contains("email address is already in use")) {
                        "Email ini sudah terdaftar, silakan login"
                    } else {
                        "Gagal mendaftar: ${task.exception?.localizedMessage}"
                    }
                    _authState.value = AuthState.Error(customMsg)
                }
            }
    }
}