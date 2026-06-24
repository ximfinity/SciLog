package com.scilog.app.domain.model

import com.scilog.app.data.local.entity.DailyEssentialsEntity

data class DailyEssentials(
    val id: Long = 0,
    val dateMs: Long,
    val waterOunces: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val waterTargetOz: Double = 64.0,
    val proteinTargetG: Double = 100.0
) {
    val waterProgress: Float get() = (waterOunces / waterTargetOz).toFloat().coerceIn(0f, 1f)
    val proteinProgress: Float get() = (proteinGrams / proteinTargetG).toFloat().coerceIn(0f, 1f)
}

fun DailyEssentialsEntity.toDomain() = DailyEssentials(
    id, dateMs, waterOunces, proteinGrams, waterTargetOz, proteinTargetG
)

fun DailyEssentials.toEntity() = DailyEssentialsEntity(
    id, dateMs, waterOunces, proteinGrams, waterTargetOz, proteinTargetG
)
