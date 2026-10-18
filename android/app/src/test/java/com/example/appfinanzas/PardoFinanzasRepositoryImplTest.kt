package com.example.appfinanzas

import com.example.appfinanzas.data.repository.PardoFinanzasRepositoryImpl
import com.example.appfinanzas.domain.model.FixedExpenseStatus
import com.example.appfinanzas.domain.model.MovementType
import com.example.appfinanzas.domain.model.PardoFixedExpense
import com.example.appfinanzas.domain.model.PardoMovement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PardoFinanzasRepositoryImplTest {

    private lateinit var repository: PardoFinanzasRepositoryImpl

    @Before
    fun setUp() {
        repository = PardoFinanzasRepositoryImpl()
    }

    @Test
    fun testInsertAndGetMovement() = runTest {
        val movement = PardoMovement(
            id = "M-100",
            descripcion = "Almuerzo",
            valor = 25000.0,
            tipo = MovementType.EGRESO,
            blockId = "B-001"
        )

        val result = repository.insertMovement(movement)
        assertTrue(result.isSuccess)
        assertEquals("B-001", repository.getLastBlockId())
    }

    @Test
    fun testGetAndUpdateFixedExpense() = runTest {
        val initialFixed = PardoFixedExpense(
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
        repository.updateFixedExpense(initialFixed)

        val expense = repository.getFixedExpenseById("F-0001")
        assertNotNull(expense)
        assertEquals("Fenix Parking", expense?.concepto)

        val updated = expense!!.copy(abonado = 150000.0, estado = FixedExpenseStatus.PAGADO)
        repository.updateFixedExpense(updated)

        val fetchedUpdated = repository.getFixedExpenseById("F-0001")
        assertEquals(150000.0, fetchedUpdated?.abonado ?: 0.0, 0.01)
        assertEquals(FixedExpenseStatus.PAGADO, fetchedUpdated?.estado)
    }

    @Test
    fun testDeleteBlockById() = runTest {
        val movement1 = PardoMovement(id = "M-1", blockId = "B-100", valor = 1000.0)
        val movement2 = PardoMovement(id = "M-2", blockId = "B-100", valor = 2000.0)
        repository.insertMovement(movement1)
        repository.insertMovement(movement2)

        assertEquals("B-100", repository.getLastBlockId())

        val deleteResult = repository.deleteBlockById("B-100")
        assertTrue(deleteResult.isSuccess)

        val lastBlock = repository.getLastBlockId()
        assertNull(lastBlock)
    }

    @Test
    fun testDashboardStatusCalculation() = runTest {
        val ingreso = PardoMovement(id = "M-IN", valor = 500000.0, tipo = MovementType.INGRESO)
        val egreso = PardoMovement(id = "M-EG", valor = 200000.0, tipo = MovementType.EGRESO)
        val neutral = PardoMovement(id = "M-TR", valor = 100000.0, tipo = MovementType.TRANSFERENCIA_INTERNA)

        repository.insertMovement(ingreso)
        repository.insertMovement(egreso)
        repository.insertMovement(neutral)

        val statusResult = repository.getDashboardStatus()
        assertTrue(statusResult.isSuccess)

        val status = statusResult.getOrNull()
        assertNotNull(status)
        // Base 0 + 500,000 - 200,000 = 300,000
        assertEquals(300000.0, status!!.disponible, 0.01)
    }
}
