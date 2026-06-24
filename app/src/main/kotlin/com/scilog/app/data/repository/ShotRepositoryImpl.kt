package com.scilog.app.data.repository

import com.scilog.app.data.local.dao.ShotDao
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.model.toDomain
import com.scilog.app.domain.model.toEntity
import com.scilog.app.domain.repository.ShotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShotRepositoryImpl @Inject constructor(
    private val dao: ShotDao
) : ShotRepository {
    override fun getAllShots(): Flow<List<Shot>> =
        dao.getAllShots().map { list -> list.map { it.toDomain() } }

    override fun getShotsFrom(fromMs: Long): Flow<List<Shot>> =
        dao.getShotsFrom(fromMs).map { list -> list.map { it.toDomain() } }

    override suspend fun getShotById(id: Long): Shot? =
        dao.getShotById(id)?.toDomain()

    override suspend fun logShot(shot: Shot): Long = dao.insertShot(shot.toEntity())

    override suspend fun updateShot(shot: Shot) = dao.updateShot(shot.toEntity())

    override suspend fun deleteShot(shot: Shot) = dao.deleteShot(shot.toEntity())

    override suspend fun getLastDoseChangedAt(): Long? {
        val shots = dao.getAllShotsOnce()
        if (shots.size < 2) return null
        val currentDose = shots.first().doseMg
        // Walk newest→oldest; the oldest shot still at currentDose is when this dose started.
        val firstAtCurrentDose = shots.takeWhile { it.doseMg == currentDose }.lastOrNull()
        return firstAtCurrentDose?.timestampMs
    }
}
