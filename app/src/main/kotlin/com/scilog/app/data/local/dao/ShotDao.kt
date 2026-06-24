package com.scilog.app.data.local.dao

import androidx.room.*
import com.scilog.app.data.local.entity.ShotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots ORDER BY timestampMs DESC")
    fun getAllShots(): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots ORDER BY timestampMs DESC")
    suspend fun getAllShotsOnce(): List<ShotEntity>

    @Query("SELECT * FROM shots WHERE timestampMs >= :fromMs ORDER BY timestampMs DESC")
    fun getShotsFrom(fromMs: Long): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots WHERE id = :id")
    suspend fun getShotById(id: Long): ShotEntity?

    @Query("SELECT * FROM shots WHERE vialId = :vialId")
    suspend fun getShotsByVial(vialId: Long): List<ShotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: ShotEntity): Long

    @Update
    suspend fun updateShot(shot: ShotEntity)

    @Delete
    suspend fun deleteShot(shot: ShotEntity)

    @Query("DELETE FROM shots WHERE id = :id")
    suspend fun deleteShotById(id: Long)
}
