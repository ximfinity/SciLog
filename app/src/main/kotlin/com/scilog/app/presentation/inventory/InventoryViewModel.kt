package com.scilog.app.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.model.MedicationType
import com.scilog.app.domain.model.Vial
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.VialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class InventoryUiState(
    val vials: List<Vial> = emptyList(),
    val currentDoseMg: Double = 0.0,
    val showAddDialog: Boolean = false,
    val newMedType: MedicationType = MedicationType.SEMAGLUTIDE,
    val newVolumeMl: String = "",
    val newConcentration: String = "",
    val newExpirationMs: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(90),
    val newIsOpen: Boolean = false,
    val newOpenedDateMs: Long = System.currentTimeMillis(),
    val newAdditives: String = "",
    val newProvider: String = "",
    val newPharmacy: String = "",
    val newCostUsd: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: VialRepository,
    private val shotRepository: ShotRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllVials(),
                shotRepository.getAllShots()
            ) { vials, shots ->
                val latestDose = shots.firstOrNull()?.doseMg ?: 0.0
                _state.update { it.copy(vials = vials, currentDoseMg = latestDose, isLoading = false) }
            }.collect()
        }
    }

    fun showAddDialog() = _state.update { it.copy(showAddDialog = true, error = null) }
    fun dismissDialog() = _state.update { it.copy(showAddDialog = false) }
    fun setMedType(t: MedicationType) = _state.update { it.copy(newMedType = t) }
    fun setVolume(v: String) = _state.update { it.copy(newVolumeMl = v) }
    fun setConcentration(c: String) = _state.update { it.copy(newConcentration = c) }
    fun setIsOpen(v: Boolean) = _state.update { it.copy(newIsOpen = v) }
    fun setAdditives(v: String) = _state.update { it.copy(newAdditives = v) }
    fun setProvider(v: String) = _state.update { it.copy(newProvider = v) }
    fun setPharmacy(v: String) = _state.update { it.copy(newPharmacy = v) }
    fun setCostUsd(v: String) = _state.update { it.copy(newCostUsd = v) }

    fun addVial() {
        val s = _state.value
        val vol = s.newVolumeMl.toDoubleOrNull()
        val conc = s.newConcentration.toDoubleOrNull()
        if (vol == null || vol <= 0 || conc == null || conc <= 0) {
            _state.update { it.copy(error = "Enter valid volume and concentration.") }
            return
        }
        viewModelScope.launch {
            repository.addVial(
                Vial(
                    medicationType = s.newMedType,
                    startingVolumeMl = vol,
                    remainingVolumeMl = vol,
                    concentrationMgPerMl = conc,
                    expirationMs = s.newExpirationMs,
                    isOpen = s.newIsOpen,
                    openedDateMs = if (s.newIsOpen) s.newOpenedDateMs else 0L,
                    additives = s.newAdditives.trim(),
                    provider = s.newProvider.trim(),
                    pharmacy = s.newPharmacy.trim(),
                    costUsd = s.newCostUsd.toDoubleOrNull() ?: -1.0
                )
            )
            _state.update {
                it.copy(
                    showAddDialog = false,
                    newVolumeMl = "", newConcentration = "",
                    newIsOpen = false, newAdditives = "",
                    newProvider = "", newPharmacy = "", newCostUsd = ""
                )
            }
        }
    }

    fun markOpen(vial: Vial) {
        viewModelScope.launch {
            repository.updateVial(
                vial.copy(isOpen = true, openedDateMs = System.currentTimeMillis())
            )
        }
    }

    fun deleteVial(vial: Vial) {
        viewModelScope.launch { repository.deleteVial(vial) }
    }
}
