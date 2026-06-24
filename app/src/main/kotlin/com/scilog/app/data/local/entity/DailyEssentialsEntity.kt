package com.scilog.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_essentials",
    indices = [Index(value = ["dateMs"], unique = true)]
)
data class DailyEssentialsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMs: Long,
    val waterOunces: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val waterTargetOz: Double = 64.0,
    val proteinTargetG: Double = 100.0
)
