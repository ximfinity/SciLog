package com.scilog.app.presentation.siterotator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.domain.model.InjectionSite
import com.scilog.app.domain.repository.InjectionSiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiteRotatorUiState(
    val recentSites: List<InjectionSite> = emptyList(),
    val lastSite: InjectionSite? = null,
    val nextRecommended: InjectionSite = InjectionSite.LEFT_ABDOMEN,
    val isLoading: Boolean = true
)

@HiltViewModel
class SiteRotatorViewModel @Inject constructor(
    private val repository: InjectionSiteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SiteRotatorUiState())
    val state: StateFlow<SiteRotatorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecentSites(16).collect { sites ->
                val last = sites.firstOrNull()
                _state.value = SiteRotatorUiState(
                    recentSites = sites,
                    lastSite = last,
                    nextRecommended = InjectionSite.nextAfter(last),
                    isLoading = false
                )
            }
        }
    }

    fun recordManualSite(site: InjectionSite) {
        viewModelScope.launch { repository.recordSite(site) }
    }
}
