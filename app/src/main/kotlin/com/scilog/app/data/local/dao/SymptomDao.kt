package com.scilog.app.data.local.dao

import androidx.room.*
import com.scilog.app.data.local.entity.SymptomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms ORDER BY timestampMs DESC")
    fun getAllSymptoms(): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE timestampMs >= :fromMs ORDER BY timestampMs ASC")
    fun getSymptomsFrom(fromMs: Long): Flow<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE symptomType = :type ORDER BY timestampMs DESC")
    fun getSymptomsByType(type: String): Flow<List<SymptomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomEntity): Long

    @Delete
    suspend fun deleteSymptom(symptom: SymptomEntity)
}
