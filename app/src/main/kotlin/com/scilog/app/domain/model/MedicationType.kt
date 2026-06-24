package com.scilog.app.domain.model

import com.scilog.app.core.math.HalfLifeEngine

enum class MedicationType(val displayName: String, val halfLifeHours: Double) {
    SEMAGLUTIDE("Semaglutide", HalfLifeEngine.SEMAGLUTIDE_HALF_LIFE_HOURS),
    TIRZEPATIDE("Tirzepatide", HalfLifeEngine.TIRZEPATIDE_HALF_LIFE_HOURS),
    CUSTOM("Custom", 168.0);

    companion object {
        fun fromString(value: String): MedicationType =
            entries.firstOrNull { it.name == value } ?: SEMAGLUTIDE
    }
}
