package com.scilog.app.domain.model

data class AppConfig(
    val cycleHours: Int = 168,
    val initialWeightLbs: Double? = null,
    val startDateMs: Long? = null,
    val targetWeightLbs: Double? = null,
    val targetDoseMg: Double? = null
)
