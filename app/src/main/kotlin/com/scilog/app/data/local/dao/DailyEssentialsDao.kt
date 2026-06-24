package com.scilog.app.data.local.dao

import androidx.room.*
import com.scilog.app.data.local.entity.DailyEssentialsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyEssentialsDao {
    @Query("SELECT * FROM daily_essentials WHERE dateMs = :dateMs LIMIT 1")
    fun getForDate(dateMs: Long): Flow<DailyEssentialsEntity?>

    @Query("SELECT * FROM daily_essentials ORDER BY dateMs DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<DailyEssentialsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyEssentialsEntity): Long

    @Query("UPDATE daily_essentials SET waterOunces = :oz WHERE dateMs = :dateMs")
    suspend fun updateWater(dateMs: Long, oz: Double)

    @Query("UPDATE daily_essentials SET proteinGrams = :grams WHERE dateMs = :dateMs")
    suspend fun updateProtein(dateMs: Long, grams: Double)
}
