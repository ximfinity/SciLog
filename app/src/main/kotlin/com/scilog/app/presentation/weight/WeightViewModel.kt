package com.scilog.app.presentation.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.model.Weight
import com.scilog.app.domain.repository.WeightRepository
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase
import com.scilog.app.domain.usecase.weight.LogWeightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeightUiState(
    val weights: List<Weight> = emptyList(),
    val inputLbs: String = "",
    val notes: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val guidance: GetWeightGuidanceUseCase.GuidanceResult? = null,
    val editingWeight: Weight? = null,
    val editInputLbs: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
    private val logWeight: LogWeightUseCase,
    private val getGuidance: GetWeightGuidanceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WeightUiState())
    val state: StateFlow<WeightUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecentWeights(30).collect { weights ->
                _state.update {
                    it.copy(
                        weights = weights,
                        guidance = if (weights.size >= 2) getGuidance(weights, 0.0) else null
                    )
                }
            }
        }
    }

    fun setInput(lbs: String) = _state.update { it.copy(inputLbs = lbs) }
    fun setNotes(n: String) = _state.update { it.copy(notes = n) }
    fun setTimestamp(ms: Long) = _state.update { it.copy(timestampMs = ms) }

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
