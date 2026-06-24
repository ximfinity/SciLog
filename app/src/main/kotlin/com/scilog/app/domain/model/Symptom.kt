package com.scilog.app.domain.model

import com.scilog.app.data.local.entity.SymptomEntity

data class Symptom(
    val id: Long = 0,
    val timestampMs: Long,
    val symptomType: SymptomType,
    val severity: Int,
    val notes: String = ""
)

fun SymptomEntity.toDomain() = Symptom(
    id, timestampMs, SymptomType.fromString(symptomType), severity, notes
)

fun Symptom.toEntity() = SymptomEntity(
    id, timestampMs, symptomType.name, severity, notes
)
