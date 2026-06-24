package com.scilog.app.domain.model

enum class InjectionSite(val displayName: String, val abbreviation: String) {
    LEFT_ABDOMEN("Left Abdomen", "L. Abd"),
    RIGHT_ABDOMEN("Right Abdomen", "R. Abd"),
    LEFT_THIGH("Left Thigh", "L. Thigh"),
    RIGHT_THIGH("Right Thigh", "R. Thigh"),
    LEFT_UPPER_ARM("Left Upper Arm", "L. Arm"),
    RIGHT_UPPER_ARM("Right Upper Arm", "R. Arm"),
    LEFT_BUTTOCK("Left Buttock", "L. Butt"),
    RIGHT_BUTTOCK("Right Buttock", "R. Butt");

    companion object {
        fun fromString(value: String): InjectionSite? =
            entries.firstOrNull { it.name == value }

        /** Returns the next recommended site in round-robin order. */
        fun nextAfter(current: InjectionSite?): InjectionSite {
            if (current == null) return LEFT_ABDOMEN
            val idx = entries.indexOf(current)
            return entries[(idx + 1) % entries.size]
        }
    }
}
