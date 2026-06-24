package com.scilog.app.presentation.shots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.repository.ShotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShotHistoryUiState(
    val shots: List<Shot> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ShotHistoryViewModel @Inject constructor(
    private val shotRepository: ShotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShotHistoryUiState())
    val uiState: StateFlow<ShotHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shotRepository.getAllShots()
                .collect { shots ->
                    _uiState.value = ShotHistoryUiState(shots = shots, isLoading = false)
                }
        }
    }

    fun deleteShot(shot: Shot) {
        viewModelScope.launch { shotRepository.deleteShot(shot) }
    }
}
