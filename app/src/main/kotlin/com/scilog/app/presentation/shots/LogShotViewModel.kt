package com.scilog.app.presentation.shots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.model.*
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.VialRepository
import com.scilog.app.domain.usecase.shot.LogShotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogShotUiState(
    val medicationType: MedicationType = MedicationType.SEMAGLUTIDE,
    val doseMg: String = "",
    val isMicrodose: Boolean = false,
    val selectedSite: InjectionSite? = null,
    val selectedVialId: Long? = null,
    val notes: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val activeVials: List<Vial> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LogShotViewModel @Inject constructor(
    private val logShotUseCase: LogShotUseCase,
    private val shotRepository: ShotRepository,
    private val vialRepository: VialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val shotId: Long = savedStateHandle["shotId"] ?: -1L
    private val isEditing = shotId > 0L

    private val _state = MutableStateFlow(LogShotUiState(isEditMode = isEditing))
    val state: StateFlow<LogShotUiState> = _state.asStateFlow()

    private var originalShot: Shot? = null

    init {
        viewModelScope.launch {
            vialRepository.getActiveVials().collect { vials ->
                _state.update { it.copy(activeVials = vials) }
            }
        }
        if (isEditing) {
            viewModelScope.launch {
                val shot = shotRepository.getShotById(shotId) ?: return@launch
                originalShot = shot
                _state.update {
                    it.copy(
                        medicationType = shot.medicationType,
                        doseMg = shot.doseMg.toString(),
                        isMicrodose = shot.isMicrodose,
                        selectedSite = shot.injectionSite,
                        selectedVialId = shot.vialId,
                        notes = shot.notes,
                        timestampMs = shot.timestampMs
                    )
                }
            }
        }
    }

    fun setMedicationType(type: MedicationType) = _state.update { it.copy(medicationType = type) }
    fun setDose(dose: String) = _state.update { it.copy(doseMg = dose) }
    fun setMicrodose(micro: Boolean) = _state.update { it.copy(isMicrodose = micro) }
    fun setSite(site: InjectionSite) = _state.update { it.copy(selectedSite = site) }
    fun setVial(id: Long?) = _state.update { it.copy(selectedVialId = id) }
    fun setNotes(notes: String) = _state.update { it.copy(notes = notes) }
    fun setTimestamp(ms: Long) = _state.update { it.copy(timestampMs = ms) }

    fun logShot() {
        val s = _state.value
        val dose = s.doseMg.toDoubleOrNull()
        if (dose == null || dose <= 0.0) {
            _state.update { it.copy(error = "Enter a valid dose.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            if (isEditing) {
                val existing = originalShot ?: run {
                    _state.update { it.copy(error = "Shot not found.", isLoading = false) }
                    return@launch
                }
                val updated = existing.copy(
                    timestampMs = s.timestampMs,
                    doseMg = dose,
                    medicationType = s.medicationType,
                    isMicrodose = s.isMicrodose,
                    injectionSite = s.selectedSite,
                    vialId = s.selectedVialId,
                    notes = s.notes
                )
                runCatching { shotRepository.updateShot(updated) }
                    .onSuccess { _state.update { it.copy(isSaved = true, isLoading = false) } }
                    .onFailure { e -> _state.update { it.copy(error = e.message, isLoading = false) } }
            } else {
                val shot = Shot(
                    timestampMs = s.timestampMs,
                    doseMg = dose,
                    medicationType = s.medicationType,
                    isMicrodose = s.isMicrodose,
                    injectionSite = s.selectedSite,
                    vialId = s.selectedVialId,
                    notes = s.notes
                )
                logShotUseCase(shot)
                    .onSuccess { _state.update { it.copy(isSaved = true, isLoading = false) } }
                    .onFailure { e -> _state.update { it.copy(error = e.message, isLoading = false) } }
            }
        }
    }
}
