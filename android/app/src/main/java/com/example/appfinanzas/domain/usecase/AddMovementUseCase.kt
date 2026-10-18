package com.example.appfinanzas.domain.usecase

import com.example.appfinanzas.data.constants.SheetConstants
import com.example.appfinanzas.domain.model.MovementType
import com.example.appfinanzas.domain.model.PardoMovement
import com.example.appfinanzas.domain.repository.PardoFinanzasRepository
import javax.inject.Inject

class AddMovementUseCase @Inject constructor(
    private val repository: PardoFinanzasRepository,
    private val generateUniqueIdUseCase: GenerateUniqueIdUseCase
) {
    suspend operator fun invoke(
        movement: PardoMovement,
        batchBlockId: String? = null
    ): Result<Unit> {
        return execute(movement, batchBlockId)
    }

    suspend fun execute(
        movement: PardoMovement,
        batchBlockId: String? = null
    ): Result<Unit> {
        val finalId = if (movement.id.isBlank()) {
            generateUniqueIdUseCase(SheetConstants.PREFIX_MOVEMENT)
        } else {
            movement.id
        }

        val finalBlockId = when {
            !batchBlockId.isNullOrBlank() -> batchBlockId
            movement.blockId.isNotBlank() -> movement.blockId
            else -> generateUniqueIdUseCase("B-")
        }

        val processedValue = if (movement.tipo == MovementType.TRANSFERENCIA_INTERNA) {
            kotlin.math.abs(movement.valor)
        } else {
            movement.valor
        }

        val processedMovement = movement.copy(
            id = finalId,
            blockId = finalBlockId,
            valor = processedValue
        )

        return repository.insertMovement(processedMovement)
    }
}
