package com.scilog.app.presentation.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.core.math.TwoCompartmentPKEngine
import com.scilog.app.domain.model.Symptom
import com.scilog.app.domain.repository.AppConfigRepository
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.SymptomRepository
import com.scilog.app.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartUiState(
    val actualPoints: List<TwoCompartmentPKEngine.PKPoint> = emptyList(),
    val projectedPoints: List<TwoCompartmentPKEngine.PKPoint> = emptyList(),
    val symptoms: List<Symptom> = emptyList(),
    val currentConcMgL: Double = 0.0,
    val cMaxSS: Double = 0.0,
    val cMinSS: Double = 0.0,
    val targetCmaxSS: Double = 0.0,
    val targetCminSS: Double = 0.0,
    val targetDoseMg: Double? = null,
    val targetDoseInput: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DecayChartViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val symptomRepository: SymptomRepository,
    private val weightRepository: WeightRepository,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    private val _targetDoseInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val fromMs = System.currentTimeMillis() - 56L * 86_400_000L  // 8 weeks history
            configRepository.configFlow.flatMapLatest { config ->
                _targetDoseInput.value = config.targetDoseMg?.toString() ?: ""
                combine(
                    shotRepository.getShotsFrom(fromMs),
                    symptomRepository.getSymptomsFrom(fromMs),
                    weightRepository.getRecentWeights(1),
                    _targetDoseInput
                ) { shots, symptoms, weights, targetInput ->
                    val nowMs      = System.currentTimeMillis()
                    val cycleHours = config.cycleHours.toDouble()
                    val cycleDays  = cycleHours / 24.0
                    val historical = shots.map { it.timestampMs to it.doseMg }
                    val lastDoseMg = shots.firstOrNull()?.doseMg ?: 5.0
                    val baseParams = TwoCompartmentPKEngine.Params.forMedication(
                        shots.firstOrNull()?.medicationType ?: com.scilog.app.domain.model.MedicationType.SEMAGLUTIDE
                    )
                    val pkParams = weights.firstOrNull()?.weightKg
                        ?.let { baseParams.scaledForWeight(it) }
                        ?: baseParams

                    val projected: List<Pair<Long, Double>> = if (shots.isNotEmpty()) {
                        val intervalMs = (cycleHours * 3_600_000.0).toLong()
                        var nextMs = shots.first().timestampMs + intervalMs
                        while (nextMs < nowMs) nextMs += intervalMs
                        (0 until 3).map { i -> (nextMs + i * intervalMs) to lastDoseMg }
                    } else emptyList()

                    val result = TwoCompartmentPKEngine.simulate(historical, projected, nowMs, pkParams)
                    val (cMax, cMin) = TwoCompartmentPKEngine.steadyState(lastDoseMg, cycleDays, pkParams)

                    val previewDoseMg = targetInput.toDoubleOrNull() ?: config.targetDoseMg
                    val (targetCmax, targetCmin) = previewDoseMg?.let { tdMg ->
                        TwoCompartmentPKEngine.steadyState(tdMg, cycleDays, pkParams)
                    } ?: (0.0 to 0.0)

                    ChartUiState(
                        actualPoints    = result.actualPoints,
                        projectedPoints = result.projectedPoints,
                        symptoms        = symptoms,
                        currentConcMgL  = result.currentConcMgL,
                        cMaxSS          = cMax,
                        cMinSS          = cMin,
                        targetCmaxSS    = targetCmax,
                        targetCminSS    = targetCmin,
                        targetDoseMg    = previewDoseMg,
                        targetDoseInput = targetInput,
                        isLoading       = false
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun setTargetDoseInput(s: String) { _targetDoseInput.value = s }

    fun saveTargetDose() {
        viewModelScope.launch {
            val mg = _targetDoseInput.value.toDoubleOrNull()
            configRepository.updateTargetDose(mg)
        }
    }
}
