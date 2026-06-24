package com.scilog.app.domain.model

import com.scilog.app.data.local.entity.ShotEntity

data class Shot(
    val id: Long = 0,
    val timestampMs: Long,
    val doseMg: Double,
    val medicationType: MedicationType,
    val halfLifeHoursOverride: Double? = null,
    val isMicrodose: Boolean = false,
    val injectionSite: InjectionSite? = null,
    val vialId: Long? = null,
    val notes: String = ""
) {
    val effectiveHalfLifeHours: Double
        get() = halfLifeHoursOverride ?: medicationType.halfLifeHours
}

fun ShotEntity.toDomain() = Shot(
    id = id,
    timestampMs = timestampMs,
    doseMg = doseMg,
    medicationType = MedicationType.fromString(medicationType),
    halfLifeHoursOverride = halfLifeHoursOverride,
    isMicrodose = isMicrodose,
    injectionSite = InjectionSite.fromString(injectionSite),
    vialId = vialId,
    notes = notes
)

fun Shot.toEntity() = ShotEntity(
    id = id,
    timestampMs = timestampMs,
    doseMg = doseMg,
    medicationType = medicationType.name,
    halfLifeHoursOverride = halfLifeHoursOverride,
    isMicrodose = isMicrodose,
    injectionSite = injectionSite?.name ?: "",
    vialId = vialId,
    notes = notes
)
