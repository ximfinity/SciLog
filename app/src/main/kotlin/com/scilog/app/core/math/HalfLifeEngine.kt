package com.scilog.app.core.math

import kotlin.math.exp
import kotlin.math.ln

object HalfLifeEngine {

    const val SEMAGLUTIDE_HALF_LIFE_HOURS = 168.0  // ~7 days
    const val TIRZEPATIDE_HALF_LIFE_HOURS = 120.0  // ~5 days

    /** Typical onset lag from subcutaneous injection to peak effect. */
    const val ABSORPTION_LAG_HOURS = 8.0

    data class DecayPoint(
        val timestampMs: Long,
        val levelMg: Double
    )

    /**
     * Remaining effect level of a single dose at [hoursElapsed] after injection.
     *
     * When [absorptionLagHours] > 0 the function models a linear absorption ramp-up
     * (0 → doseMg over [absorptionLagHours]) followed by first-order exponential decay.
     * This reflects the ~8-hour subcutaneous absorption delay before peak GLP-1 effect.
     */
    fun remainingLevel(
        doseMg: Double,
        halfLifeHours: Double,
        hoursElapsed: Double,
        absorptionLagHours: Double = 0.0
    ): Double {
        if (hoursElapsed < 0.0) return 0.0
        if (absorptionLagHours > 0.0 && hoursElapsed < absorptionLagHours) {
            // Linear absorption phase: ramps from 0 to doseMg
            return doseMg * (hoursElapsed / absorptionLagHours)
        }
        val lambda = ln(2.0) / halfLifeHours
        val effectiveElapsed = hoursElapsed - absorptionLagHours
        return doseMg * exp(-lambda * effectiveElapsed)
    }

    /**
     * Cumulative active effect level at [atTimestampMs] by summing contributions
     * of every prior dose, each with the given absorption lag.
     */
    fun cumulativeLevel(
        shots: List<Pair<Long, Double>>,
        atTimestampMs: Long,
        halfLifeHours: Double,
        absorptionLagHours: Double = 0.0
    ): Double = shots.sumOf { (shotMs, doseMg) ->
        val hoursElapsed = (atTimestampMs - shotMs) / 3_600_000.0
        remainingLevel(doseMg, halfLifeHours, hoursElapsed, absorptionLagHours)
    }

    /**
     * Generates a time-series effect curve at [intervalHours] resolution
     * between [startMs] and [endMs].
     */
    fun generateDecayCurve(
        shots: List<Pair<Long, Double>>,
        halfLifeHours: Double,
        startMs: Long,
        endMs: Long,
        intervalHours: Int = 4,
        absorptionLagHours: Double = 0.0
    ): List<DecayPoint> {
        val intervalMs = intervalHours * 3_600_000L
        val points = mutableListOf<DecayPoint>()
        var t = startMs
        while (t <= endMs) {
            points.add(DecayPoint(t, cumulativeLevel(shots, t, halfLifeHours, absorptionLagHours)))
            t += intervalMs
        }
        return points
    }

    /**
     * Returns the estimated trough (minimum) level between two weekly injections.
     */
    fun troughLevel(doseMg: Double, halfLifeHours: Double, intervalHours: Double = 168.0): Double =
        remainingLevel(doseMg, halfLifeHours, intervalHours)

    /**
     * Estimates hours until the cumulative level drops below [threshold].
     */
    fun hoursUntilBelow(
        shots: List<Pair<Long, Double>>,
        halfLifeHours: Double,
        threshold: Double,
        fromMs: Long,
        searchHours: Int = 720,
        absorptionLagHours: Double = 0.0
    ): Double? {
        for (h in 0..searchHours) {
            val tMs = fromMs + h * 3_600_000L
            if (cumulativeLevel(shots, tMs, halfLifeHours, absorptionLagHours) < threshold) return h.toDouble()
        }
        return null
    }
}
