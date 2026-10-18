package com.example.appfinanzas.domain.usecase

import com.example.appfinanzas.data.constants.SheetConstants
import com.example.appfinanzas.domain.model.FixedExpenseStatus
import com.example.appfinanzas.domain.model.MovementType
import com.example.appfinanzas.domain.model.PardoMovement
import com.example.appfinanzas.domain.repository.PardoFinanzasRepository
import javax.inject.Inject

class PayFixedExpenseUseCase @Inject constructor(
    private val repository: PardoFinanzasRepository,
    private val generateUniqueIdUseCase: GenerateUniqueIdUseCase
) {
    suspend operator fun invoke(
        fixedExpenseId: String,
        amountPaid: Double,
        paymentMethod: String = "",
        account: String = "",
        periodTag: String = "Septiembre 2026",
        fecha: String = "",
        hora: String = ""
    ): Result<Unit> {
        return execute(
            fixedExpenseId = fixedExpenseId,
            amountPaid = amountPaid,
            paymentMethod = paymentMethod,
            account = account,
            periodTag = periodTag,
            fecha = fecha,
            hora = hora
        )
    }

    suspend fun execute(
        fixedExpenseId: String,
        amountPaid: Double,
        paymentMethod: String = "",
        account: String = "",
        periodTag: String = "Septiembre 2026",
        fecha: String = "",
        hora: String = ""
    ): Result<Unit> {
        val fixedExpense = repository.getFixedExpenseById(fixedExpenseId)
            ?: return Result.failure(IllegalArgumentException("Gasto fijo no encontrado con id: $fixedExpenseId"))

        val newAbonado = fixedExpense.abonado + amountPaid
        val newStatus = when {
            newAbonado >= fixedExpense.cuotaBase -> FixedExpenseStatus.PAGADO
            newAbonado > 0.0 -> FixedExpenseStatus.PARCIAL
            fixedExpense.estado == FixedExpenseStatus.VENCIDO -> FixedExpenseStatus.VENCIDO
            else -> FixedExpenseStatus.PENDIENTE
        }

        val updatedExpense = fixedExpense.copy(
            abonado = newAbonado,
            estado = newStatus,
            medioPago = paymentMethod.ifBlank { fixedExpense.medioPago }
        )

        val updateResult = repository.updateFixedExpense(updatedExpense)
        if (updateResult.isFailure) {
            return updateResult
        }

        val formattedTag = String.format(SheetConstants.PAGO_FIJO_TAG_FORMAT, fixedExpenseId, periodTag)
        val movement = PardoMovement(
            id = generateUniqueIdUseCase(SheetConstants.PREFIX_MOVEMENT),
            fecha = fecha,
            hora = hora,
            tipo = MovementType.EGRESO,
            categoria = fixedExpense.categoria,
            subcategoria = "Pago Fijo",
            descripcion = fixedExpense.concepto,
            valor = amountPaid,
            metodoPago = paymentMethod.ifBlank { fixedExpense.medioPago },
            cuenta = account,
            observaciones = formattedTag,
            blockId = generateUniqueIdUseCase("B-")
        )

        return repository.insertMovement(movement)
    }
}
