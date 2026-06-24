package com.scilog.app.domain.usecase.shot

import com.scilog.app.core.math.TwoCompartmentPKEngine.PKParameters
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.model.Weight
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class EscalationStatus {
    TOO_EARLY,            // <4 weeks at current dose
    ON_TRACK,             // adequate weight loss
    CONSIDER_ESCALATION,  // ≥4 weeks, <2% body weight lost at current dose
    AT_MAX_DOSE           // already at labeled maximum
}

data class DosageInsight(
    val weeksAtCurrentDose: Int,
    val weightChangePctSinceDoseChange: Double?,  // negative = loss
    val currentDoseNumber: Int,
    val dosesUntilSS: Int,
    val escalationStatus: EscalationStatus,
    val message: String
)

class GetDosageInsightUseCase @Inject constructor() {

    operator fun invoke(
        shots: List<Shot>,
        weights: List<Weight>,
        pkParams: PKParameters,
        startingWeightLbs: Double?,
        doseChangedAtMs: Long?,
        dosesUntilSS: Int,
        targetWeightLbs: Double? = null
    ): DosageInsight? {
        if (shots.isEmpty()) return null

        val now = System.currentTimeMillis()
        val currentDose = shots.first().doseMg

        val doseStartMs = doseChangedAtMs ?: shots.first().timestampMs
        val weeksAtDose = ((now - doseStartMs) / TimeUnit.DAYS.toMillis(7).toDouble()).toInt()

        // Count how many shots have been taken at the current dose
        val shotsAtCurrentDose = shots.count { it.doseMg == currentDose }

        // % body weight change since the dose was started
        val baselineWeight = startingWeightLbs
        val weightChangePct: Double? = if (weights.size >= 2 && baselineWeight != null && baselineWeight > 0) {
            val sortedW = weights.sortedBy { it.timestampMs }
            val doseStartWeight = sortedW.firstOrNull { it.timestampMs >= doseStartMs }?.weightLbs
                ?: sortedW.first().weightLbs
            val latestWeight = sortedW.last().weightLbs
            ((latestWeight - doseStartWeight) / baselineWeight) * 100.0  // negative = loss
        } else null

        val atMaxDose = currentDose >= pkParams.maxDoseMg

        val status = when {
            atMaxDose -> EscalationStatus.AT_MAX_DOSE
            weeksAtDose < 4 -> EscalationStatus.TOO_EARLY
            weightChangePct != null && weightChangePct < -2.0 -> EscalationStatus.ON_TRACK
            weightChangePct != null && weightChangePct >= -2.0 -> EscalationStatus.CONSIDER_ESCALATION
            else -> EscalationStatus.TOO_EARLY  // no weight data
        }

        // Goal weight progress
        val currentWeightLbs = weights.maxByOrNull { it.timestampMs }?.weightLbs
        val goalProgressStr: String? = if (targetWeightLbs != null && startingWeightLbs != null &&
            currentWeightLbs != null && startingWeightLbs > targetWeightLbs) {
            val totalToLose = startingWeightLbs - targetWeightLbs
            val lost = startingWeightLbs - currentWeightLbs
            val pct = (lost / totalToLose * 100).toInt().coerceIn(0, 100)
            " · $pct% to goal"
        } else null

        val message = when (status) {
            EscalationStatus.AT_MAX_DOSE -> "At maximum labeled dose (${pkParams.maxDoseMg} mg)${goalProgressStr ?: ""}."
            EscalationStatus.TOO_EARLY -> "Only $weeksAtDose week${if (weeksAtDose == 1) "" else "s"} at current dose. Assess after 4 weeks${goalProgressStr ?: ""}."
            EscalationStatus.ON_TRACK -> {
                val lossStr = weightChangePct?.let { "%.1f%%".format(-it) } ?: "—"
                "Losing $lossStr body weight at current dose — on track${goalProgressStr ?: ""}."
            }
            EscalationStatus.CONSIDER_ESCALATION -> {
                val lossStr = weightChangePct?.let { "%.1f%%".format(-it) } ?: "<2%"
                "$lossStr body weight lost over $weeksAtDose weeks. May be time to discuss a dose increase${goalProgressStr ?: ""}."
            }
        }

        return DosageInsight(
            weeksAtCurrentDose = weeksAtDose,
            weightChangePctSinceDoseChange = weightChangePct,
            currentDoseNumber = shotsAtCurrentDose,
            dosesUntilSS = dosesUntilSS,
            escalationStatus = status,
            message = message
        )
    }
}
