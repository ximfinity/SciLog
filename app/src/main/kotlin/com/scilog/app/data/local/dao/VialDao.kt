package com.scilog.app.data.local.dao

import androidx.room.*
import com.scilog.app.data.local.entity.VialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VialDao {
    @Query("SELECT * FROM vials ORDER BY expirationMs ASC")
    fun getAllVials(): Flow<List<VialEntity>>

    @Query("SELECT * FROM vials WHERE isActive = 1 ORDER BY expirationMs ASC")
    fun getActiveVials(): Flow<List<VialEntity>>

    @Query("SELECT * FROM vials WHERE id = :id")
    suspend fun getVialById(id: Long): VialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVial(vial: VialEntity): Long

    @Update
    suspend fun updateVial(vial: VialEntity)

    @Query("UPDATE vials SET remainingVolumeMl = :remaining WHERE id = :id")
    suspend fun updateRemainingVolume(id: Long, remaining: Double)

    @Delete
    suspend fun deleteVial(vial: VialEntity)
}
