package com.example.appfinanzas

import com.example.appfinanzas.data.constants.SheetConstants
import com.example.appfinanzas.data.repository.PardoFinanzasRepositoryImpl
import com.example.appfinanzas.domain.model.FixedExpenseStatus
import com.example.appfinanzas.domain.model.MovementType
import com.example.appfinanzas.domain.model.PardoFixedExpense
import com.example.appfinanzas.domain.model.PardoMovement
import com.example.appfinanzas.domain.usecase.AddMovementUseCase
import com.example.appfinanzas.domain.usecase.GenerateUniqueIdUseCase
import com.example.appfinanzas.domain.usecase.GetDashboardStatusUseCase
import com.example.appfinanzas.domain.usecase.PayFixedExpenseUseCase
import com.example.appfinanzas.domain.usecase.RollbackLastBatchUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PardoUseCasesTest {

    private lateinit var repository: PardoFinanzasRepositoryImpl
    private lateinit var generateUniqueIdUseCase: GenerateUniqueIdUseCase
    private lateinit var addMovementUseCase: AddMovementUseCase
    private lateinit var payFixedExpenseUseCase: PayFixedExpenseUseCase
    private lateinit var rollbackLastBatchUseCase: RollbackLastBatchUseCase
    private lateinit var getDashboardStatusUseCase: GetDashboardStatusUseCase

    @Before
    fun setUp() {
        repository = PardoFinanzasRepositoryImpl()
        generateUniqueIdUseCase = GenerateUniqueIdUseCase()
        addMovementUseCase = AddMovementUseCase(repository, generateUniqueIdUseCase)
        payFixedExpenseUseCase = PayFixedExpenseUseCase(repository, generateUniqueIdUseCase)
        rollbackLastBatchUseCase = RollbackLastBatchUseCase(repository)
        getDashboardStatusUseCase = GetDashboardStatusUseCase(repository)
    }

    @Test
    fun testGenerateUniqueIdUseCase() {
        val idMovement = generateUniqueIdUseCase(SheetConstants.PREFIX_MOVEMENT)
        assertTrue(idMovement.startsWith(SheetConstants.PREFIX_MOVEMENT))

        val idFixed = generateUniqueIdUseCase(SheetConstants.PREFIX_FIXED)
        assertTrue(idFixed.startsWith(SheetConstants.PREFIX_FIXED))

        val idSavings = generateUniqueIdUseCase(SheetConstants.PREFIX_SAVINGS)
        assertTrue(idSavings.startsWith(SheetConstants.PREFIX_SAVINGS))
    }

    @Test
    fun testAddMovementUseCaseInternalTransfer() = runTest {
        val movement = PardoMovement(
            tipo = MovementType.TRANSFERENCIA_INTERNA,
            descripcion = "Transferencia Nequi a ahorro",
            valor = -50000.0
        )

        val result = addMovementUseCase(movement, batchBlockId = "BATCH-01")
        assertTrue(result.isSuccess)
        assertEquals("BATCH-01", repository.getLastBlockId())
    }

    @Test
    fun testPayFixedExpenseUseCase() = runTest {
        val sampleFixed = PardoFixedExpense(
            id = "F-0001",
            concepto = "Fenix Parking",
            categoria = "Transporte",
            diaCobro = 5,
            cuotaBase = 150000.0,
            abonado = 0.0,
            estado = FixedExpenseStatus.PENDIENTE,
            prioridad = 1,
            medioPago = "Nequi"
        )
        repository.updateFixedExpense(sampleFixed)

        val result = payFixedExpenseUseCase(
            fixedExpenseId = "F-0001",
            amountPaid = 150000.0,
            paymentMethod = "Nequi",
            periodTag = "Septiembre 2026"
        )

        assertTrue(result.isSuccess)

        val updatedFixedExpense = repository.getFixedExpenseById("F-0001")
        assertNotNull(updatedFixedExpense)
        assertEquals(150000.0, updatedFixedExpense?.abonado ?: 0.0, 0.01)
        assertEquals(FixedExpenseStatus.PAGADO, updatedFixedExpense?.estado)
    }

    @Test
    fun testRollbackLastBatchUseCase() = runTest {
        val movement = PardoMovement(
            descripcion = "Supermercado",
            valor = 80000.0
        )

        addMovementUseCase(movement, batchBlockId = "BLOCK-TO-ROLLBACK")
        assertEquals("BLOCK-TO-ROLLBACK", repository.getLastBlockId())

        val rollbackResult = rollbackLastBatchUseCase.borrarUltimoConjunto()
        assertTrue(rollbackResult.isSuccess)

        assertEquals(null, repository.getLastBlockId())
    }

    @Test
    fun testGetDashboardStatusUseCase() = runTest {
        val sampleFixed = PardoFixedExpense(
            id = "F-0003",
            concepto = "Internet Fibra",
            categoria = "Servicios",
            diaCobro = 10,
            cuotaBase = 90000.0,
            abonado = 0.0,
            estado = FixedExpenseStatus.VENCIDO,
            prioridad = 2,
            medioPago = "Tarjeta"
        )
        repository.updateFixedExpense(sampleFixed)

        val statusResult = getDashboardStatusUseCase()
        assertTrue(statusResult.isSuccess)

        val status = statusResult.getOrNull()
        assertNotNull(status)
        assertTrue(status!!.fixedExpenses.any { it.estado == FixedExpenseStatus.VENCIDO })
    }
}
