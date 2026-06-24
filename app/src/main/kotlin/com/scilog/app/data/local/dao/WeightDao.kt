package com.scilog.app.data.local.dao

import androidx.room.*
import com.scilog.app.data.local.entity.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weights ORDER BY timestampMs DESC")
    fun getAllWeights(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentWeights(limit: Int): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights WHERE timestampMs >= :fromMs ORDER BY timestampMs ASC")
    fun getWeightsFrom(fromMs: Long): Flow<List<WeightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntity): Long

    @Update
    suspend fun updateWeight(weight: WeightEntity)

    @Delete
    suspend fun deleteWeight(weight: WeightEntity)

    @Query("DELETE FROM weights WHERE id = :id")
    suspend fun deleteWeightById(id: Long)
}
