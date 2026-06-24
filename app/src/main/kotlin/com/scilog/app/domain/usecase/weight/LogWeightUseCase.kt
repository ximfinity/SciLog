package com.scilog.app.domain.usecase.weight

import com.scilog.app.domain.model.Weight
import com.scilog.app.domain.repository.WeightRepository
import javax.inject.Inject

class LogWeightUseCase @Inject constructor(
    private val repository: WeightRepository
) {
    suspend operator fun invoke(weight: Weight): Result<Long> = runCatching {
        repository.logWeight(weight)
    }
}
