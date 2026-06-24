package com.scilog.app.core.math

/**
 * Computes a catch-up dose that, when taken at [nowMs], results in a serum trough
 * concentration ≥ [targetCminMgL] at the end of the next full dosing interval.
 *
 * Uses binary search over [0, maxDoseMg] with 30 iterations (~1e-9 mg precision).
 */
object CatchUpDoseEngine {

    fun calculateCatchUpDose(
        historicalDoses: List<Pair<Long, Double>>,
        nowMs: Long,
        targetCminMgL: Double,
        intervalMs: Long,
        params: TwoCompartmentPKEngine.PKParameters,
        maxDoseMg: Double
    ): Double {
        val nextDoseMs = nowMs + intervalMs
        // Evaluate trough just before the scheduled next dose
        val evalMs = nextDoseMs - 60_000L  // 1 minute before

        fun troughForCatchUp(catchUpDose: Double): Double {
            val dosesWithCatchUp = historicalDoses + listOf(nowMs to catchUpDose)
            val sim = TwoCompartmentPKEngine.simulate(
                historicalDoses = dosesWithCatchUp,
                projectedDoses  = emptyList(),
                nowMs           = evalMs,
                params          = params
            )
            return sim.currentConcMgL
        }

        // If even maxDoseMg can't reach target, return max
        if (troughForCatchUp(maxDoseMg) < targetCminMgL) return maxDoseMg

        // Binary search for the minimum dose that achieves the target trough
        var lo = 0.0
        var hi = maxDoseMg
        repeat(30) {
            val mid = (lo + hi) / 2.0
            if (troughForCatchUp(mid) >= targetCminMgL) hi = mid else lo = mid
        }
        return hi
    }
}
