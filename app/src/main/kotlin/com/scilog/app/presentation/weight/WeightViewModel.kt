package com.scilog.app.presentation.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.model.Weight
import com.scilog.app.domain.repository.AppConfigRepository
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.WeightRepository
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase
import com.scilog.app.domain.usecase.weight.LogWeightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WeightDateRange(val label: String, val days: Int?) {
    D30("30d", 30),
    D60("60d", 60),
    D90("90d", 90),
    ALL("All", null)
}

data class WeightUiState(
    val weights: List<Weight> = emptyList(),
    val allWeights: List<Weight> = emptyList(),
    val shots: List<Shot> = emptyList(),
    val targetWeightLbs: Double? = null,
    val selectedRange: WeightDateRange = WeightDateRange.D90,
    val showTrendLine: Boolean = true,
    val showProjection: Boolean = true,
    val showMinMax: Boolean = true,
    val inputLbs: String = "",
    val notes: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val guidance: GetWeightGuidanceUseCase.GuidanceResult? = null,
    val editingWeight: Weight? = null,
    val editInputLbs: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
    private val shotRepository: ShotRepository,
    private val configRepository: AppConfigRepository,
    private val logWeight: LogWeightUseCase,
    private val getGuidance: GetWeightGuidanceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WeightUiState())
    val state: StateFlow<WeightUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.configFlow.flatMapLatest { config ->
                combine(
                    repository.getRecentWeights(365),
                    shotRepository.getAllShots()
                ) { weights, shots ->
                    val recent = weights.take(30)
                    WeightUiState(
                        weights         = recent,
                        allWeights      = weights,
                        shots           = shots,
                        targetWeightLbs = config.targetWeightLbs,
                        selectedRange   = _state.value.selectedRange,
                        showTrendLine   = _state.value.showTrendLine,
                        showProjection  = _state.value.showProjection,
                        showMinMax      = _state.value.showMinMax,
                        inputLbs        = _state.value.inputLbs,
                        notes           = _state.value.notes,
                        timestampMs     = _state.value.timestampMs,
                        guidance        = if (weights.size >= 2) getGuidance(weights) else null,
                        editingWeight   = _state.value.editingWeight,
                        editInputLbs    = _state.value.editInputLbs,
                        isSaving        = _state.value.isSaving,
                        error           = _state.value.error
                    )
                }
            }.collect { _state.value = it }
        }
    }

    fun setInput(lbs: String) = _state.update { it.copy(inputLbs = lbs) }
    fun setNotes(n: String) = _state.update { it.copy(notes = n) }
    fun setTimestamp(ms: Long) = _state.update { it.copy(timestampMs = ms) }

    fun setSelectedRange(range: WeightDateRange) = _state.update { it.copy(selectedRange = range) }
    fun toggleTrendLine() = _state.update { it.copy(showTrendLine = !it.showTrendLine) }
    fun toggleProjection() = _state.update { it.copy(showProjection = !it.showProjection) }
    fun toggleMinMax() = _state.update { it.copy(showMinMax = !it.showMinMax) }

    fun save() {
        val s = _state.value
        val lbs = s.inputLbs.toDoubleOrNull()
        if (lbs == null || lbs <= 0.0) { _state.update { it.copy(error = "Enter a valid weight.") }; return }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            logWeight(Weight(timestampMs = s.timestampMs, weightLbs = lbs, notes = s.notes))
            _state.update { it.copy(isSaving = false, inputLbs = "", notes = "", timestampMs = System.currentTimeMillis()) }
        }
    }

    fun startEdit(weight: Weight) =
        _state.update { it.copy(editingWeight = weight, editInputLbs = weight.weightLbs.toString()) }

    fun setEditInput(lbs: String) = _state.update { it.copy(editInputLbs = lbs) }

    fun cancelEdit() = _state.update { it.copy(editingWeight = null, editInputLbs = "") }

    fun confirmEdit() {
        val s = _state.value
        val newLbs = s.editInputLbs.toDoubleOrNull()
        val target = s.editingWeight
        if (newLbs == null || newLbs <= 0.0 || target == null) return
        viewModelScope.launch {
            repository.updateWeight(target.copy(weightLbs = newLbs))
            _state.update { it.copy(editingWeight = null, editInputLbs = "") }
        }
    }

    fun delete(weight: Weight) {
        viewModelScope.launch { repository.deleteWeight(weight) }
    }
}
