package com.scilog.app.domain.model

enum class SymptomType(val displayName: String) {
    NAUSEA("Nausea"),
    FATIGUE("Fatigue"),
    APPETITE_SUPPRESSION("Appetite Suppression"),
    HEADACHE("Headache"),
    CONSTIPATION("Constipation"),
    DIARRHEA("Diarrhea"),
    INJECTION_SITE_PAIN("Injection Site Pain");

    companion object {
        fun fromString(value: String): SymptomType =
            entries.firstOrNull { it.name == value } ?: NAUSEA
    }
}
