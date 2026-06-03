package com.kelompok6.lastletter.data.repository

import com.kelompok6.lastletter.data.local.dao.MatchHistoryDao
import com.kelompok6.lastletter.data.local.entity.MatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchHistoryRepository @Inject constructor(
    private val matchHistoryDao: MatchHistoryDao
) {
    fun getAllHistory(): Flow<List<MatchHistoryEntity>> = matchHistoryDao.getAllHistory()

    suspend fun insertHistory(match: MatchHistoryEntity) {
        matchHistoryDao.insertMatchHistory(match)
    }
}
