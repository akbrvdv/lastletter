package com.kelompok6.lastletter.data.repository

import com.kelompok6.lastletter.data.local.dao.MatchHistoryDao
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchHistoryRepository @Inject constructor(private val dao: MatchHistoryDao) {
    fun getHistoryByUserId(userId: String) = dao.getHistoryByUserId(userId)
    suspend fun insertHistory(history: MatchHistoryEntity) = dao.insertHistory(history)
}