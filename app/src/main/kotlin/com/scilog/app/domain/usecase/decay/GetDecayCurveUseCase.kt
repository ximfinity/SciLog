package com.scilog.app.domain.usecase.decay

import com.scilog.app.core.math.HalfLifeEngine
import com.scilog.app.domain.model.MedicationType
import com.scilog.app.domain.model.Shot
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetDecayCurveUseCase @Inject constructor() {

    data class DecayCurveResult(
        val points: List<HalfLifeEngine.DecayPoint>,
        val currentLevel: Double,
        val peakLevel: Double,
        val troughLevel: Double
    )

    operator fun invoke(
        shots: List<Shot>,
        medicationType: MedicationType,
        lookbackDays: Int = 30,
        lookaheadDays: Int = 14
    ): DecayCurveResult {
        val now = System.currentTimeMillis()
        val startMs = now - TimeUnit.DAYS.toMillis(lookbackDays.toLong())
        val endMs = now + TimeUnit.DAYS.toMillis(lookaheadDays.toLong())

        val shotPairs = shots.map { it.timestampMs to it.doseMg }
        val halfLife = medicationType.halfLifeHours
        val lag = HalfLifeEngine.ABSORPTION_LAG_HOURS

        val points = HalfLifeEngine.generateDecayCurve(
            shots = shotPairs,
            halfLifeHours = halfLife,
            startMs = startMs,
            endMs = endMs,
            intervalHours = 4,
            absorptionLagHours = lag
        )

        val currentLevel = HalfLifeEngine.cumulativeLevel(shotPairs, now, halfLife, lag)
        val peakLevel = points.maxOfOrNull { it.levelMg } ?: 0.0
        val troughLevel = points.filter { it.timestampMs >= now }.minOfOrNull { it.levelMg } ?: 0.0

        return DecayCurveResult(points, currentLevel, peakLevel, troughLevel)
    }
}
