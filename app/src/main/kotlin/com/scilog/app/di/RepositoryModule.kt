package com.scilog.app.di

import com.scilog.app.data.repository.*
import com.scilog.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindShotRepository(impl: ShotRepositoryImpl): ShotRepository

    @Binds @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds @Singleton
    abstract fun bindVialRepository(impl: VialRepositoryImpl): VialRepository

    @Binds @Singleton
    abstract fun bindSymptomRepository(impl: SymptomRepositoryImpl): SymptomRepository

    @Binds @Singleton
    abstract fun bindInjectionSiteRepository(impl: InjectionSiteRepositoryImpl): InjectionSiteRepository

    @Binds @Singleton
    abstract fun bindDailyEssentialsRepository(impl: DailyEssentialsRepositoryImpl): DailyEssentialsRepository
}
