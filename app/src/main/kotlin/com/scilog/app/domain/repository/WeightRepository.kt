package com.scilog.app.domain.repository

import com.scilog.app.domain.model.Weight
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun getAllWeights(): Flow<List<Weight>>
    fun getRecentWeights(limit: Int): Flow<List<Weight>>
    fun getWeightsFrom(fromMs: Long): Flow<List<Weight>>
    suspend fun logWeight(weight: Weight): Long
    suspend fun updateWeight(weight: Weight)
    suspend fun deleteWeight(weight: Weight)
}
