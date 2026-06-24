package com.scilog.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scilog.app.data.local.dao.*
import com.scilog.app.data.local.entity.*

@Database(
    entities = [
        ShotEntity::class,
        WeightEntity::class,
        VialEntity::class,
        SymptomEntity::class,
        InjectionSiteEntity::class,
        DailyEssentialsEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class SciLogDatabase : RoomDatabase() {
    abstract fun shotDao(): ShotDao
    abstract fun weightDao(): WeightDao
    abstract fun vialDao(): VialDao
    abstract fun symptomDao(): SymptomDao
    abstract fun injectionSiteDao(): InjectionSiteDao
    abstract fun dailyEssentialsDao(): DailyEssentialsDao
}
