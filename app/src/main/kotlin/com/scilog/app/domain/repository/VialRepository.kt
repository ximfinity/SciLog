package com.scilog.app.domain.repository

import com.scilog.app.domain.model.Vial
import kotlinx.coroutines.flow.Flow

interface VialRepository {
    fun getAllVials(): Flow<List<Vial>>
    fun getActiveVials(): Flow<List<Vial>>
    suspend fun addVial(vial: Vial): Long
    suspend fun updateVial(vial: Vial)
    suspend fun decrementVolume(vialId: Long, doseMg: Double)
    suspend fun deleteVial(vial: Vial)
}
