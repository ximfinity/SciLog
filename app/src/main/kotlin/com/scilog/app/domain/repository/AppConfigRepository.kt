package com.scilog.app.domain.repository

import com.scilog.app.domain.model.AppConfig
import kotlinx.coroutines.flow.StateFlow

interface AppConfigRepository {
    val configFlow: StateFlow<AppConfig>
    fun getConfig(): AppConfig
    suspend fun updateCycleHours(hours: Int)
    suspend fun updateInitialWeight(lbs: Double?)
    suspend fun updateStartDate(ms: Long?)
    suspend fun updateTargetWeight(lbs: Double?)
    suspend fun updateTargetDose(mg: Double?)
}
