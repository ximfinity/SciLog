package com.scilog.app.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.repository.AppConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigUiState(
    val cycleDaysInput: String = "7",
    val initialWeightInput: String = "",
    val targetWeightLbsInput: String = "",
    val targetDoseMgInput: String = "",
    val startDateMs: Long? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val repository: AppConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigUiState())
    val state: StateFlow<ConfigUiState> = _state.asStateFlow()

    init {
        val config = repository.getConfig()
        _state.update {
            it.copy(
                cycleDaysInput       = (config.cycleHours / 24).toString(),
                initialWeightInput   = config.initialWeightLbs?.let { lbs -> "%.1f".format(lbs) } ?: "",
                targetWeightLbsInput = config.targetWeightLbs?.let { lbs -> "%.1f".format(lbs) } ?: "",
                targetDoseMgInput    = config.targetDoseMg?.let { mg -> "%.1f".format(mg) } ?: "",
                startDateMs          = config.startDateMs
            )
        }
    }

    fun setCycleDays(days: String)         = _state.update { it.copy(cycleDaysInput = days) }
    fun setInitialWeight(lbs: String)      = _state.update { it.copy(initialWeightInput = lbs) }
    fun setTargetWeightLbs(lbs: String)    = _state.update { it.copy(targetWeightLbsInput = lbs) }
    fun setTargetDoseMg(mg: String)        = _state.update { it.copy(targetDoseMgInput = mg) }
    fun setStartDate(ms: Long?)            = _state.update { it.copy(startDateMs = ms) }

    fun save() {
        val s = _state.value
        val days         = s.cycleDaysInput.toIntOrNull()?.coerceIn(1, 30) ?: 7
        val weight       = s.initialWeightInput.toDoubleOrNull()?.takeIf { it > 0 }
        val targetWeight = s.targetWeightLbsInput.toDoubleOrNull()?.takeIf { it > 0 }
        val targetDose   = s.targetDoseMgInput.toDoubleOrNull()?.takeIf { it > 0 }
        viewModelScope.launch {
            repository.updateCycleHours(days * 24)
            repository.updateInitialWeight(weight)
            repository.updateStartDate(s.startDateMs)
            repository.updateTargetWeight(targetWeight)
            repository.updateTargetDose(targetDose)
            _state.update { it.copy(saved = true) }
        }
    }
}
