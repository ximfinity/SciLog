package com.scilog.app.data.prefs

import android.content.Context
import com.scilog.app.domain.model.AppConfig
import com.scilog.app.domain.repository.AppConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppConfigRepository {

    private val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(read())
    override val configFlow: StateFlow<AppConfig> = _configFlow.asStateFlow()

    private fun read() = AppConfig(
        cycleHours = prefs.getInt("cycle_hours", 168),
        initialWeightLbs = if (prefs.contains("initial_weight")) prefs.getFloat("initial_weight", 0f).toDouble() else null,
        startDateMs = if (prefs.contains("start_date_ms")) prefs.getLong("start_date_ms", 0L) else null,
        targetWeightLbs = if (prefs.contains("target_weight")) prefs.getFloat("target_weight", 0f).toDouble() else null,
        targetDoseMg = if (prefs.contains("target_dose_mg")) prefs.getFloat("target_dose_mg", 0f).toDouble() else null
    )

    override fun getConfig() = _configFlow.value

    override suspend fun updateCycleHours(hours: Int) {
        prefs.edit().putInt("cycle_hours", hours).apply()
        _configFlow.value = read()
    }

    override suspend fun updateInitialWeight(lbs: Double?) {
        if (lbs != null) prefs.edit().putFloat("initial_weight", lbs.toFloat()).apply()
        else prefs.edit().remove("initial_weight").apply()
        _configFlow.value = read()
    }

    override suspend fun updateStartDate(ms: Long?) {
        if (ms != null) prefs.edit().putLong("start_date_ms", ms).apply()
        else prefs.edit().remove("start_date_ms").apply()
        _configFlow.value = read()
    }

    override suspend fun updateTargetWeight(lbs: Double?) {
        if (lbs != null) prefs.edit().putFloat("target_weight", lbs.toFloat()).apply()
        else prefs.edit().remove("target_weight").apply()
        _configFlow.value = read()
    }

    override suspend fun updateTargetDose(mg: Double?) {
        if (mg != null) prefs.edit().putFloat("target_dose_mg", mg.toFloat()).apply()
        else prefs.edit().remove("target_dose_mg").apply()
        _configFlow.value = read()
    }
}
