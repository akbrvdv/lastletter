package com.kelompok6.lastletter.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kelompok6.lastletter.data.repository.MatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: MatchHistoryRepository
) : ViewModel() {

    // Ambil UID user yang sedang aktif
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Gunakan fungsi baru yang memfilter by ID
    val history = historyRepository.getHistoryByUserId(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}