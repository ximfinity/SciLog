package com.scilog.app.domain.usecase.weight

import com.scilog.app.domain.model.Weight
import javax.inject.Inject
import java.util.concurrent.TimeUnit

class GetWeightGuidanceUseCase @Inject constructor() {

    enum class Guidance {
        ON_TRACK,
        CONSIDER_DOSE_INCREASE,
        WEIGHT_LOSS_TOO_RAPID,
        INSUFFICIENT_DATA
    }

    data class GuidanceResult(
        val guidance: Guidance,
        val weeklyLossLbs: Double?,
        val message: String
    )

    /**
     * Evaluate weight loss progress using clinically informed criteria.
     *
     * Rapid loss guard: >1.5% of body weight per week is flagged as potentially too fast.
     * Escalation trigger: <2% total body weight lost since dose change AND ≥4 weeks at dose.
     * On track: ≥2% total loss over the measurement window (SURPASS/SCALE trial benchmark).
     *
     * @param weights        Recent weight entries (sorted any order).
     * @param startingWeightLbs  Baseline weight for % calculation (uses first entry if null).
     * @param doseChangedAtMs    Timestamp when current dose first started (null = use all data).
     */
    operator fun invoke(
        weights: List<Weight>,
        startingWeightLbs: Double? = null,
        doseChangedAtMs: Long? = null
    ): GuidanceResult {
        if (weights.size < 2) {
            return GuidanceResult(Guidance.INSUFFICIENT_DATA, null, "Log more weigh-ins for guidance.")
        }

        val sorted = weights.sortedBy { it.timestampMs }
        val oldest = sorted.first()
        val newest = sorted.last()
        val weeksDiff = (newest.timestampMs - oldest.timestampMs) / TimeUnit.DAYS.toMillis(7).toDouble()

        if (weeksDiff < 1.0) {
            return GuidanceResult(Guidance.INSUFFICIENT_DATA, null, "Keep logging — need at least 1 week of data.")
        }

        val baselineWeight = startingWeightLbs ?: oldest.weightLbs
        val totalLoss = oldest.weightLbs - newest.weightLbs
        val weeklyLoss = totalLoss / weeksDiff
        val weeklyLossPct = (weeklyLoss / baselineWeight) * 100.0

        // Safety guard: >1.5% of body weight per week is too rapid
        if (weeklyLossPct > 1.5) {
            return GuidanceResult(
                Guidance.WEIGHT_LOSS_TOO_RAPID,
                weeklyLoss,
                "Losing %.1f lbs/week (%.1f%% of body weight) may be too rapid. Monitor closely and consult your provider.".format(weeklyLoss, weeklyLossPct)
            )
        }

        // Compute % loss since current dose started
        val doseSinceLoss: Double? = if (doseChangedAtMs != null) {
            val doseChangeWeight = sorted.firstOrNull { it.timestampMs >= doseChangedAtMs }
            val doseStartWeight = doseChangeWeight?.weightLbs ?: oldest.weightLbs
            ((doseStartWeight - newest.weightLbs) / baselineWeight) * 100.0
        } else {
            ((oldest.weightLbs - newest.weightLbs) / baselineWeight) * 100.0
        }

        val weeksAtCurrentDose = if (doseChangedAtMs != null) {
            ((System.currentTimeMillis() - doseChangedAtMs) / TimeUnit.DAYS.toMillis(7).toDouble())
        } else weeksDiff

        return when {
            doseSinceLoss != null && doseSinceLoss < 2.0 && weeksAtCurrentDose >= 4.0 -> GuidanceResult(
                Guidance.CONSIDER_DOSE_INCREASE,
                weeklyLoss,
                "<2%% body weight lost over %.0f weeks at current dose. Consider discussing a dose increase with your provider.".format(weeksAtCurrentDose)
            )
            else -> GuidanceResult(
                Guidance.ON_TRACK,
                weeklyLoss,
                "On track — averaging %.1f lbs/week (%.1f%% of body weight).".format(weeklyLoss, weeklyLossPct)
            )
        }
    }
}
