package com.scilog.app.data.repository

import com.scilog.app.data.local.dao.WeightDao
import com.scilog.app.domain.model.Weight
import com.scilog.app.domain.model.toDomain
import com.scilog.app.domain.model.toEntity
import com.scilog.app.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightDao
) : WeightRepository {
    override fun getAllWeights(): Flow<List<Weight>> =
        dao.getAllWeights().map { it.map { e -> e.toDomain() } }

    override fun getRecentWeights(limit: Int): Flow<List<Weight>> =
        dao.getRecentWeights(limit).map { it.map { e -> e.toDomain() } }

    override fun getWeightsFrom(fromMs: Long): Flow<List<Weight>> =
        dao.getWeightsFrom(fromMs).map { it.map { e -> e.toDomain() } }

    override suspend fun logWeight(weight: Weight): Long = dao.insertWeight(weight.toEntity())

    override suspend fun updateWeight(weight: Weight) = dao.updateWeight(weight.toEntity())

    override suspend fun deleteWeight(weight: Weight) = dao.deleteWeight(weight.toEntity())
}
