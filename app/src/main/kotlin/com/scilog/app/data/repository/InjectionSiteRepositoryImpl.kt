package com.scilog.app.data.repository

import com.scilog.app.data.local.dao.InjectionSiteDao
import com.scilog.app.data.local.entity.InjectionSiteEntity
import com.scilog.app.domain.model.InjectionSite
import com.scilog.app.domain.repository.InjectionSiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InjectionSiteRepositoryImpl @Inject constructor(
    private val dao: InjectionSiteDao
) : InjectionSiteRepository {
    override fun getRecentSites(limit: Int): Flow<List<InjectionSite>> =
        dao.getRecentSites(limit).map { list ->
            list.mapNotNull { InjectionSite.fromString(it.site) }
        }

    override suspend fun getLastSite(): InjectionSite? =
        dao.getLastSite()?.let { InjectionSite.fromString(it.site) }

    override suspend fun recordSite(site: InjectionSite, shotId: Long?) {
        dao.insertSite(
            InjectionSiteEntity(
                timestampMs = System.currentTimeMillis(),
                site = site.name,
                shotId = shotId
            )
        )
    }
}
