package com.scilog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val doseMg: Double,
    val medicationType: String,
    val halfLifeHoursOverride: Double? = null,
    val isMicrodose: Boolean = false,
    val injectionSite: String = "",
    val vialId: Long? = null,
    val notes: String = ""
)
