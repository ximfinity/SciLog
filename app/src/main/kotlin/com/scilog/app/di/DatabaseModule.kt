package com.scilog.app.di

import android.content.Context
import androidx.room.Room
import com.scilog.app.data.local.SciLogDatabase
import com.scilog.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SciLogDatabase =
        Room.databaseBuilder(context, SciLogDatabase::class.java, "scilog.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideShotDao(db: SciLogDatabase): ShotDao = db.shotDao()
    @Provides fun provideWeightDao(db: SciLogDatabase): WeightDao = db.weightDao()
    @Provides fun provideVialDao(db: SciLogDatabase): VialDao = db.vialDao()
    @Provides fun provideSymptomDao(db: SciLogDatabase): SymptomDao = db.symptomDao()
    @Provides fun provideInjectionSiteDao(db: SciLogDatabase): InjectionSiteDao = db.injectionSiteDao()
    @Provides fun provideDailyEssentialsDao(db: SciLogDatabase): DailyEssentialsDao = db.dailyEssentialsDao()
}
