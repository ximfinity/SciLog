package com.scilog.app.data.local.dao

import androidx.room.*
import com.scilog.app.data.local.entity.InjectionSiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InjectionSiteDao {
    @Query("SELECT * FROM injection_sites ORDER BY timestampMs DESC")
    fun getAllSites(): Flow<List<InjectionSiteEntity>>

    @Query("SELECT * FROM injection_sites ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLastSite(): InjectionSiteEntity?

    @Query("SELECT * FROM injection_sites ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentSites(limit: Int): Flow<List<InjectionSiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: InjectionSiteEntity): Long

    @Delete
    suspend fun deleteSite(site: InjectionSiteEntity)
}
