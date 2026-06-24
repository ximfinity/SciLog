package com.scilog.app.domain.model

import com.scilog.app.data.local.entity.WeightEntity

data class Weight(
    val id: Long = 0,
    val timestampMs: Long,
    val weightLbs: Double,
    val notes: String = ""
) {
    val weightKg: Double get() = weightLbs * 0.453592
}

fun WeightEntity.toDomain() = Weight(id, timestampMs, weightLbs, notes)
fun Weight.toEntity() = WeightEntity(id, timestampMs, weightLbs, notes)
