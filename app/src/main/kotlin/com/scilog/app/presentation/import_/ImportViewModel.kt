package com.scilog.app.presentation.import_

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.data.import_.GlappImportParser
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Processing : ImportUiState()
    data class Preview(val shots: Int, val weights: Int, val errors: List<String>) : ImportUiState()
    data class Success(val shotsImported: Int, val weightsImported: Int) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val weightRepository: WeightRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var pendingResult: GlappImportParser.ImportResult? = null

    fun parseContent(content: String, @Suppress("UNUSED_PARAMETER") isJson: Boolean = false) {
        viewModelScope.launch {
            _state.value = ImportUiState.Processing
            val result = GlappImportParser.parseCsv(content)
            pendingResult = result
            _state.value = ImportUiState.Preview(result.shots.size, result.weights.size, result.errors)
        }
    }

    fun confirmImport() {
        val result = pendingResult ?: return
        viewModelScope.launch {
            _state.value = ImportUiState.Processing
            result.shots.forEach { shotRepository.logShot(it) }
            result.weights.forEach { weightRepository.logWeight(it) }
            _state.value = ImportUiState.Success(result.shots.size, result.weights.size)
        }
    }

    fun reset() { _state.value = ImportUiState.Idle; pendingResult = null }
}
