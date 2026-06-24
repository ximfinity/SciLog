package com.scilog.app.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.core.math.TwoCompartmentPKEngine
import com.scilog.app.domain.model.*
import com.scilog.app.domain.repository.*
import com.scilog.app.domain.usecase.export.ExportDataUseCase
import com.scilog.app.domain.usecase.shot.DosageInsight
import com.scilog.app.domain.usecase.shot.GetDosageInsightUseCase
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StoplightLevel { GREEN, YELLOW, RED }

data class DashboardUiState(
    val currentLevelMg: Double = 0.0,
    val peakLevelMg: Double = 0.0,
    val fractionOfPeak: Double = 0.0,
    val stoplight: StoplightLevel = StoplightLevel.RED,
    val stoplightLabel: String = "No Data",
    val stoplightDescription: String = "Log your first injection to begin tracking",
    val hoursUntilNextDose: Double? = null,
    val decayPoints: List<TwoCompartmentPKEngine.PKPoint> = emptyList(),
    val projectedDecayPoints: List<TwoCompartmentPKEngine.PKPoint> = emptyList(),
    val projectedDosesMs: List<Long> = emptyList(),
    val pkParams: TwoCompartmentPKEngine.PKParameters = TwoCompartmentPKEngine.Params.SEMAGLUTIDE,
    val cMaxSS: Double = 0.0,
    val cMinSS: Double = 0.0,
    val targetCmaxSS: Double = 0.0,
    val targetCminSS: Double = 0.0,
    val targetDoseMg: Double? = null,
    val dosesUntilSS: Int = 0,
    val recentShots: List<Shot> = emptyList(),
    val allShotsForChart: List<Shot> = emptyList(),
    val latestWeight: Weight? = null,
    val weightHistory: List<Weight> = emptyList(),
    val activeVials: List<Vial> = emptyList(),
    val weightGuidance: GetWeightGuidanceUseCase.GuidanceResult? = null,
    val dosageInsight: DosageInsight? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val weightRepository: WeightRepository,
    private val vialRepository: VialRepository,
    private val getWeightGuidance: GetWeightGuidanceUseCase,
    private val getDosageInsight: GetDosageInsightUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _exportUri = MutableSharedFlow<Uri>()
    val exportUri: SharedFlow<Uri> = _exportUri.asSharedFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val thirtyDaysAgoMs = System.currentTimeMillis() - 30L * 86_400_000L
        viewModelScope.launch {
            configRepository.configFlow.flatMapLatest { config ->
                combine(
                    shotRepository.getShotsFrom(thirtyDaysAgoMs),
                    weightRepository.getRecentWeights(60),
                    vialRepository.getActiveVials()
                ) { shots, weights, vials ->
                    val now        = System.currentTimeMillis()
                    val cycleHours = config.cycleHours.toDouble()
                    val baseParams = TwoCompartmentPKEngine.Params.forMedication(
                        shots.firstOrNull()?.medicationType ?: MedicationType.SEMAGLUTIDE
                    )
                    val pkParams = weights.firstOrNull()?.weightKg
                        ?.let { baseParams.scaledForWeight(it) }
                        ?: baseParams

                    val lastDoseMg  = shots.firstOrNull()?.doseMg ?: 5.0
                    val projectedDoses = projectFutureDoses(shots, cycleHours)

                    val historicalFrom70d = shots
                        .filter { it.timestampMs >= now - 70L * 86_400_000L }
                        .map { it.timestampMs to it.doseMg }
                    val projectedPairs = projectedDoses.map { it to lastDoseMg }
                    val simResult = if (shots.isNotEmpty()) {
                        TwoCompartmentPKEngine.simulate(historicalFrom70d, projectedPairs, now, pkParams)
                    } else null

                    val currentAmountMg = simResult?.currentAmountMg ?: 0.0
                    val currentConcMgL  = simResult?.currentConcMgL  ?: 0.0
                    val (cMaxSS, cMinSS) = if (shots.isNotEmpty())
                        TwoCompartmentPKEngine.steadyState(lastDoseMg, cycleHours / 24.0, pkParams)
                    else 0.0 to 0.0

                    val (targetCmaxSS, targetCminSS) = config.targetDoseMg?.let { tdMg ->
                        TwoCompartmentPKEngine.steadyState(tdMg, cycleHours / 24.0, pkParams)
                    } ?: (0.0 to 0.0)

                    val dosesUntilSS = if (shots.isNotEmpty())
                        TwoCompartmentPKEngine.dosesUntilSteadyState(lastDoseMg, pkParams, cycleHours / 24.0)
                    else 0

                    val fraction = if (cMaxSS > 0) currentConcMgL / cMaxSS else 0.0
                    val peakAmountMg = (simResult?.actualPoints?.maxOfOrNull { it.amountMg } ?: 0.0)
                        .coerceAtLeast(simResult?.projectedPoints?.maxOfOrNull { it.amountMg } ?: 0.0)

                    val hoursSinceLast = shots.firstOrNull()
                        ?.let { (now - it.timestampMs) / 3_600_000.0 } ?: 0.0
                    val hoursUntilNext = (cycleHours - hoursSinceLast).takeIf { it > 0 }
                    val (stoplight, label, desc) = computeStoplight(shots.isEmpty(), fraction, hoursUntilNext)

                    // Fetch dose-change timestamp for guidance accuracy
                    val doseChangedAtMs = runCatching { shotRepository.getLastDoseChangedAt() }.getOrNull()

                    val guidance = if (weights.size >= 2) {
                        getWeightGuidance(
                            weights = weights,
                            startingWeightLbs = config.initialWeightLbs,
                            doseChangedAtMs = doseChangedAtMs
                        )
                    } else null

                    val insight = getDosageInsight(
                        shots = shots,
                        weights = weights,
                        pkParams = pkParams,
                        startingWeightLbs = config.initialWeightLbs,
                        doseChangedAtMs = doseChangedAtMs,
                        dosesUntilSS = dosesUntilSS,
                        targetWeightLbs = config.targetWeightLbs
                    )

                    DashboardUiState(
                        currentLevelMg       = currentAmountMg,
                        peakLevelMg          = peakAmountMg,
                        fractionOfPeak       = fraction,
                        stoplight            = stoplight,
                        stoplightLabel       = label,
                        stoplightDescription = desc,
                        hoursUntilNextDose   = hoursUntilNext,
                        decayPoints          = simResult?.actualPoints    ?: emptyList(),
                        projectedDecayPoints = simResult?.projectedPoints ?: emptyList(),
                        projectedDosesMs     = projectedDoses,
                        pkParams             = pkParams,
                        cMaxSS               = cMaxSS,
                        cMinSS               = cMinSS,
                        targetCmaxSS         = targetCmaxSS,
                        targetCminSS         = targetCminSS,
                        targetDoseMg         = config.targetDoseMg,
                        dosesUntilSS         = dosesUntilSS,
                        recentShots          = shots.take(2),
                        allShotsForChart     = shots,
                        latestWeight         = weights.firstOrNull(),
                        weightHistory        = weights,
                        activeVials          = vials,
                        weightGuidance       = guidance,
                        dosageInsight        = insight,
                        isLoading            = false
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun deleteShot(shot: Shot) {
        viewModelScope.launch { shotRepository.deleteShot(shot) }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            runCatching { exportDataUseCase.exportCsv() }
                .onSuccess { _exportUri.emit(it) }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    private fun projectFutureDoses(shots: List<Shot>, cycleHours: Double): List<Long> {
        val lastShot = shots.firstOrNull() ?: return emptyList()
        val now = System.currentTimeMillis()
        val intervalMs = (cycleHours * 3_600_000.0).toLong()
        var nextMs = lastShot.timestampMs + intervalMs
        while (nextMs < now) nextMs += intervalMs
        return (0 until 3).map { nextMs + it * intervalMs }
    }

    private fun computeStoplight(
        noData: Boolean,
        fraction: Double,
        hoursUntilNext: Double?
    ): Triple<StoplightLevel, String, String> {
        if (noData) return Triple(
            StoplightLevel.RED, "No Data", "Log your first injection to begin tracking"
        )
        return when {
            fraction >= 0.60 -> Triple(
                StoplightLevel.GREEN,
                "Peak Effect",
                "${"%.0f".format(fraction * 100)}% of peak · Strong appetite suppression active"
            )
            fraction >= 0.25 -> Triple(
                StoplightLevel.YELLOW,
                "Moderate Effect",
                "${"%.0f".format(fraction * 100)}% of peak · Effects active but declining"
            )
            else -> {
                val timeStr = hoursUntilNext?.let {
                    val d = (it / 24).toInt(); val h = (it % 24).toInt()
                    if (d > 0) "${d}d ${h}h" else "${h}h"
                } ?: "now"
                Triple(
                    StoplightLevel.RED,
                    "Low — Check Schedule",
                    "Medication level low · Next dose window in $timeStr"
                )
            }
        }
    }
}
