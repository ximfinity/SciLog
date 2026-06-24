package com.scilog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "injection_sites")
data class InjectionSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val site: String,
    val shotId: Long? = null
)
