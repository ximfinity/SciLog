package com.scilog.app.domain.repository

import com.scilog.app.domain.model.Shot
import kotlinx.coroutines.flow.Flow

interface ShotRepository {
    fun getAllShots(): Flow<List<Shot>>
    fun getShotsFrom(fromMs: Long): Flow<List<Shot>>
    suspend fun getShotById(id: Long): Shot?
    suspend fun logShot(shot: Shot): Long
    suspend fun updateShot(shot: Shot)
    suspend fun deleteShot(shot: Shot)
    /** Timestamp of the first shot at the current dose level (null if only one dose exists). */
    suspend fun getLastDoseChangedAt(): Long?
}
