package com.scilog.app.data.repository

import com.scilog.app.data.local.dao.DailyEssentialsDao
import com.scilog.app.data.local.entity.DailyEssentialsEntity
import com.scilog.app.domain.model.DailyEssentials
import com.scilog.app.domain.model.toDomain
import com.scilog.app.domain.model.toEntity
import com.scilog.app.domain.repository.DailyEssentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DailyEssentialsRepositoryImpl @Inject constructor(
    private val dao: DailyEssentialsDao
) : DailyEssentialsRepository {
    override fun getForDate(dateMs: Long): Flow<DailyEssentials?> =
        dao.getForDate(dateMs).map { it?.toDomain() }

    override fun getRecent(limit: Int): Flow<List<DailyEssentials>> =
        dao.getRecent(limit).map { it.map { e -> e.toDomain() } }

    override suspend fun upsert(essentials: DailyEssentials) = dao.upsert(essentials.toEntity()).let {}

    override suspend fun addWater(dateMs: Long, oz: Double) {
        ensureTodayExists(dateMs)
        dao.updateWater(dateMs, oz)
    }

    override suspend fun addProtein(dateMs: Long, grams: Double) {
        ensureTodayExists(dateMs)
        dao.updateProtein(dateMs, grams)
    }

    private suspend fun ensureTodayExists(dateMs: Long) {
        dao.upsert(
            DailyEssentialsEntity(dateMs = dateMs)
        )
    }
}
