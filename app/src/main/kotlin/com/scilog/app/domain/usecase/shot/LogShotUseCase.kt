package com.scilog.app.domain.usecase.shot

import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.repository.InjectionSiteRepository
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.VialRepository
import javax.inject.Inject

class LogShotUseCase @Inject constructor(
    private val shotRepository: ShotRepository,
    private val vialRepository: VialRepository,
    private val siteRepository: InjectionSiteRepository
) {
    suspend operator fun invoke(shot: Shot): Result<Long> = runCatching {
        val shotId = shotRepository.logShot(shot)

        // Decrement vial volume if a vial is assigned
        shot.vialId?.let { vialId ->
            // dose mg / concentration = volume used
            // We don't have concentration here; handled by VialRepository from vial data
            vialRepository.decrementVolume(vialId, shot.doseMg)
        }

        // Record injection site history
        shot.injectionSite?.let { site ->
            siteRepository.recordSite(site, shotId)
        }

        shotId
    }
}
