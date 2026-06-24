package com.scilog.app.domain.usecase.shot

import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.repository.ShotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetShotsUseCase @Inject constructor(
    private val repository: ShotRepository
) {
    operator fun invoke(): Flow<List<Shot>> = repository.getAllShots()
    fun fromDays(days: Int): Flow<List<Shot>> {
        val fromMs = System.currentTimeMillis() - days * 86_400_000L
        return repository.getShotsFrom(fromMs)
    }
}
