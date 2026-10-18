package com.example.appfinanzas.domain.usecase

import com.example.appfinanzas.domain.repository.PardoFinanzasRepository
import javax.inject.Inject

class RollbackLastBatchUseCase @Inject constructor(
    private val repository: PardoFinanzasRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return borrarUltimoConjunto()
    }

    suspend fun borrarUltimoConjunto(): Result<Unit> {
        val lastBlockId = repository.getLastBlockId()
            ?: return Result.failure(IllegalStateException("No hay un conjunto de registros previo para borrar"))

        return repository.deleteBlockById(lastBlockId)
    }
}
