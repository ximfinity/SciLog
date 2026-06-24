package com.scilog.app.domain.repository

import com.scilog.app.domain.model.DailyEssentials
import kotlinx.coroutines.flow.Flow

interface DailyEssentialsRepository {
    fun getForDate(dateMs: Long): Flow<DailyEssentials?>
    fun getRecent(limit: Int): Flow<List<DailyEssentials>>
    suspend fun upsert(essentials: DailyEssentials)
    suspend fun addWater(dateMs: Long, oz: Double)
    suspend fun addProtein(dateMs: Long, grams: Double)
}
