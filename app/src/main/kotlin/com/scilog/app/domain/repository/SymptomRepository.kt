package com.scilog.app.domain.repository

import com.scilog.app.domain.model.Symptom
import kotlinx.coroutines.flow.Flow

interface SymptomRepository {
    fun getAllSymptoms(): Flow<List<Symptom>>
    fun getSymptomsFrom(fromMs: Long): Flow<List<Symptom>>
    suspend fun logSymptom(symptom: Symptom): Long
    suspend fun deleteSymptom(symptom: Symptom)
}
