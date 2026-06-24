package com.scilog.app.data.repository

import com.scilog.app.data.local.dao.SymptomDao
import com.scilog.app.domain.model.Symptom
import com.scilog.app.domain.model.toDomain
import com.scilog.app.domain.model.toEntity
import com.scilog.app.domain.repository.SymptomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SymptomRepositoryImpl @Inject constructor(
    private val dao: SymptomDao
) : SymptomRepository {
    override fun getAllSymptoms(): Flow<List<Symptom>> =
        dao.getAllSymptoms().map { it.map { e -> e.toDomain() } }

    override fun getSymptomsFrom(fromMs: Long): Flow<List<Symptom>> =
        dao.getSymptomsFrom(fromMs).map { it.map { e -> e.toDomain() } }

    override suspend fun logSymptom(symptom: Symptom): Long = dao.insertSymptom(symptom.toEntity())

    override suspend fun deleteSymptom(symptom: Symptom) = dao.deleteSymptom(symptom.toEntity())
}
