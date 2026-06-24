package com.scilog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vials")
data class VialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationType: String,
    val startingVolumeMl: Double,
    val remainingVolumeMl: Double,
    val concentrationMgPerMl: Double,
    val expirationMs: Long,
    val isActive: Boolean = true,
    val notes: String = "",
    // v2 fields
    val isOpen: Boolean = false,
    val openedDateMs: Long = 0L,
    val additives: String = "",
    val provider: String = "",
    val pharmacy: String = "",
    val costUsd: Double = -1.0
)
