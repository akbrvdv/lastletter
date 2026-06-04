package com.kelompok6.lastletter.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelompok6.lastletter.data.repository.MatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: MatchHistoryRepository
) : ViewModel() {
    // Mengambil data secara real-time dari database SQLite lokal
    val history = historyRepository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
