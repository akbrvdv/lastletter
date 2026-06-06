package com.kelompok6.lastletter.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Fungsi Login Valid dengan Pengecekan Password Ke Firebase
    fun login(email: String, passwordUiStateStr: String) {
        if (email.isBlank() || passwordUiStateStr.isBlank()) {
            _authState.value = AuthState.Error("Email dan password tidak boleh kosong!")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email.trim(), passwordUiStateStr)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error(
                        task.exception?.message ?: "Login gagal. Silakan periksa kembali email dan password Anda."
                    )
                }
            }
    }

    // Fungsi Register Akun Baru + Menyimpan Nama Tampilan Awal
    fun register(email: String, passwordUiStateStr: String, nameDisplayRegister: String) {
        if (email.isBlank() || passwordUiStateStr.isBlank() || nameDisplayRegister.isBlank()) {
            _authState.value = AuthState.Error("Semua kolom pengisian wajib diisi!")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email.trim(), passwordUiStateStr)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nameDisplayRegister.trim())
                        .build()

                    auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        _authState.value = AuthState.Success
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Registrasi akun baru gagal.")
                }
            }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}