package com.scilog.app.presentation.essentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scilog.app.core.util.DateTimeUtils
import com.scilog.app.domain.model.DailyEssentials
import com.scilog.app.domain.repository.DailyEssentialsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EssentialsViewModel @Inject constructor(
    private val repository: DailyEssentialsRepository
) : ViewModel() {

    private val todayMs = DateTimeUtils.startOfDayMs(System.currentTimeMillis())

    val today: StateFlow<DailyEssentials?> = repository.getForDate(todayMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addWater(oz: Double) {
        viewModelScope.launch {
            val current = today.value?.waterOunces ?: 0.0
            repository.addWater(todayMs, current + oz)
        }
    }

    fun setWater(oz: Double) {
        viewModelScope.launch { repository.addWater(todayMs, oz) }
    }

    fun addProtein(grams: Double) {
        viewModelScope.launch {
            val current = today.value?.proteinGrams ?: 0.0
            repository.addProtein(todayMs, current + grams)
        }
    }

    fun setProtein(grams: Double) {
        viewModelScope.launch { repository.addProtein(todayMs, grams) }
    }
}
