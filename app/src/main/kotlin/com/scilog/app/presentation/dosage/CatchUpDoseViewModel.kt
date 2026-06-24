package com.scilog.app.presentation.dosage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.core.math.CatchUpDoseEngine
import com.scilog.app.core.math.TwoCompartmentPKEngine
import com.scilog.app.domain.model.MedicationType
import com.scilog.app.domain.repository.AppConfigRepository
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatchUpUiState(
    val cMaxSS: Double = 0.0,
    val cMinSS: Double = 0.0,
    val targetCminMgL: Double = 0.0,
    val recommendedDoseMg: Double = 0.0,
    val currentDoseMg: Double = 0.0,
    val pkParams: TwoCompartmentPKEngine.PKParameters = TwoCompartmentPKEngine.Params.TIRZEPATIDE,
    val actualPoints: List<TwoCompartmentPKEngine.PKPoint> = emptyList(),
    val projectedWithCatchUp: List<TwoCompartmentPKEngine.PKPoint> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CatchUpDoseViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val weightRepository: WeightRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatchUpUiState())
    val uiState: StateFlow<CatchUpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.configFlow.flatMapLatest { config ->
                combine(
                    shotRepository.getAllShots(),
                    weightRepository.getRecentWeights(5)
                ) { shots, weights -> Triple(config, shots, weights) }
            }.collect { (config, shots, weights) ->
                val now = System.currentTimeMillis()
                val baseParams = TwoCompartmentPKEngine.Params.forMedication(
                    shots.firstOrNull()?.medicationType ?: MedicationType.TIRZEPATIDE
                )
                val pkParams = weights.firstOrNull()?.weightKg
                    ?.let { baseParams.scaledForWeight(it) }
                    ?: baseParams

                val lastDoseMg = shots.firstOrNull()?.doseMg ?: 5.0
                val cycleMs = config.cycleHours * 3_600_000L
                val (cMaxSS, cMinSS) = TwoCompartmentPKEngine.steadyState(
                    doseMg = lastDoseMg,
                    cycleDays = config.cycleHours / 24.0,
                    params = pkParams
                )

                val historicalDoses = shots
                    .filter { it.timestampMs >= now - 70L * 86_400_000L }
                    .map { it.timestampMs to it.doseMg }

                val simResult = if (shots.isNotEmpty()) {
                    TwoCompartmentPKEngine.simulate(historicalDoses, emptyList(), now, pkParams)
                } else null

                val defaultTarget = cMinSS.coerceAtLeast(0.0)
                val recommended = if (shots.isNotEmpty()) {
                    CatchUpDoseEngine.calculateCatchUpDose(
                        historicalDoses = historicalDoses,
                        nowMs = now,
                        targetCminMgL = defaultTarget,
                        intervalMs = cycleMs,
                        params = pkParams,
                        maxDoseMg = pkParams.maxDoseMg
                    )
                } else lastDoseMg

                val catchUpProjDoses = listOf(now + cycleMs, now + 2 * cycleMs).map { it to lastDoseMg }
                val catchUpSim = if (shots.isNotEmpty()) {
                    TwoCompartmentPKEngine.simulate(historicalDoses, catchUpProjDoses, now, pkParams)
                } else null

                _uiState.value = CatchUpUiState(
                    cMaxSS = cMaxSS,
                    cMinSS = cMinSS,
                    targetCminMgL = defaultTarget,
                    recommendedDoseMg = recommended,
                    currentDoseMg = lastDoseMg,
                    pkParams = pkParams,
                    actualPoints = simResult?.actualPoints ?: emptyList(),
                    projectedWithCatchUp = catchUpSim?.projectedPoints ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun setTargetCmin(targetMgL: Double) {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            val shots = shotRepository.getAllShots().first()
            val config = configRepository.getConfig()
            val now = System.currentTimeMillis()
            val cycleMs = config.cycleHours * 3_600_000L
            val historicalDoses = shots
                .filter { it.timestampMs >= now - 70L * 86_400_000L }
                .map { it.timestampMs to it.doseMg }

            val recommended = if (shots.isNotEmpty()) {
                CatchUpDoseEngine.calculateCatchUpDose(
                    historicalDoses = historicalDoses,
                    nowMs = now,
                    targetCminMgL = targetMgL,
                    intervalMs = cycleMs,
                    params = state.pkParams,
                    maxDoseMg = state.pkParams.maxDoseMg
                )
            } else state.currentDoseMg

            val catchUpProjDoses = listOf(now + cycleMs, now + 2 * cycleMs).map { it to state.currentDoseMg }
            val catchUpSim = if (shots.isNotEmpty()) {
                TwoCompartmentPKEngine.simulate(historicalDoses, catchUpProjDoses, now, state.pkParams)
            } else null

            _uiState.update {
                it.copy(
                    targetCminMgL = targetMgL,
                    recommendedDoseMg = recommended,
                    projectedWithCatchUp = catchUpSim?.projectedPoints ?: emptyList()
                )
            }
        }
    }
}
