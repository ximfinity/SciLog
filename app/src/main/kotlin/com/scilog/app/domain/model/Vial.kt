package com.scilog.app.domain.model

import com.scilog.app.data.local.entity.VialEntity

data class Vial(
    val id: Long = 0,
    val medicationType: MedicationType,
    val startingVolumeMl: Double,
    val remainingVolumeMl: Double,
    val concentrationMgPerMl: Double,
    val expirationMs: Long,
    val isActive: Boolean = true,
    val notes: String = "",
    val isOpen: Boolean = false,
    val openedDateMs: Long = 0L,
    val additives: String = "",
    val provider: String = "",
    val pharmacy: String = "",
    val costUsd: Double = -1.0
) {
    val remainingMg: Double get() = remainingVolumeMl * concentrationMgPerMl
    val percentRemaining: Float get() = (remainingVolumeMl / startingVolumeMl).toFloat().coerceIn(0f, 1f)
    val isLow: Boolean get() = percentRemaining <= 0.2f
    val isExpired: Boolean get() = System.currentTimeMillis() > expirationMs
    val bestUseDateMs: Long? get() = if (openedDateMs > 0L) openedDateMs + 28L * 86_400_000L else null
    fun shotsRemaining(doseMg: Double): Int? =
        if (doseMg > 0 && concentrationMgPerMl > 0) (remainingMg / doseMg).toInt() else null
}

fun VialEntity.toDomain() = Vial(
    id, MedicationType.fromString(medicationType),
    startingVolumeMl, remainingVolumeMl, concentrationMgPerMl,
    expirationMs, isActive, notes, isOpen, openedDateMs, additives, provider, pharmacy, costUsd
)

fun Vial.toEntity() = VialEntity(
    id, medicationType.name,
    startingVolumeMl, remainingVolumeMl, concentrationMgPerMl,
    expirationMs, isActive, notes, isOpen, openedDateMs, additives, provider, pharmacy, costUsd
)
