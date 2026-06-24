package com.scilog.app.domain.repository

import com.scilog.app.domain.model.InjectionSite
import kotlinx.coroutines.flow.Flow

interface InjectionSiteRepository {
    fun getRecentSites(limit: Int): Flow<List<InjectionSite>>
    suspend fun getLastSite(): InjectionSite?
    suspend fun recordSite(site: InjectionSite, shotId: Long? = null)
}
