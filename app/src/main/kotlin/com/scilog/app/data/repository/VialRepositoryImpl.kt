package com.scilog.app.data.repository

import com.scilog.app.data.local.dao.VialDao
import com.scilog.app.domain.model.Vial
import com.scilog.app.domain.model.toDomain
import com.scilog.app.domain.model.toEntity
import com.scilog.app.domain.repository.VialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VialRepositoryImpl @Inject constructor(
    private val dao: VialDao
) : VialRepository {
    override fun getAllVials(): Flow<List<Vial>> =
        dao.getAllVials().map { it.map { e -> e.toDomain() } }

    override fun getActiveVials(): Flow<List<Vial>> =
        dao.getActiveVials().map { it.map { e -> e.toDomain() } }

    override suspend fun addVial(vial: Vial): Long = dao.insertVial(vial.toEntity())

    override suspend fun updateVial(vial: Vial) = dao.updateVial(vial.toEntity())

    override suspend fun decrementVolume(vialId: Long, doseMg: Double) {
        val vial = dao.getVialById(vialId) ?: return
        val volumeUsed = if (vial.concentrationMgPerMl > 0.0) doseMg / vial.concentrationMgPerMl else 0.0
        val newRemaining = (vial.remainingVolumeMl - volumeUsed).coerceAtLeast(0.0)
        dao.updateRemainingVolume(vialId, newRemaining)
    }

    override suspend fun deleteVial(vial: Vial) = dao.deleteVial(vial.toEntity())
}
