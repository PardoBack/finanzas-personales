package com.example.appfinanzas.data.repository

import com.example.appfinanzas.domain.model.BudgetAlert
import com.example.appfinanzas.domain.model.DashboardStatus
import com.example.appfinanzas.domain.model.MovementType
import com.example.appfinanzas.domain.model.PardoFixedExpense
import com.example.appfinanzas.domain.model.PardoMovement
import com.example.appfinanzas.domain.repository.PardoFinanzasRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PardoFinanzasRepositoryImpl @Inject constructor() : PardoFinanzasRepository {

    private val movements = mutableListOf<PardoMovement>()
    private val fixedExpenses = mutableMapOf<String, PardoFixedExpense>()
    private val budgetAlerts = mutableListOf<BudgetAlert>()
    private var lastBlockId: String? = null
    private var baseDisponible: Double = 0.0

    init {
        // Starts 100% clean with $0 initial balances and empty data lists
    }

    override suspend fun insertMovement(movement: PardoMovement): Result<Unit> = runCatching {
        movements.add(movement)
        if (movement.blockId.isNotBlank()) {
            lastBlockId = movement.blockId
        }
    }

    override suspend fun updateFixedExpense(fixedExpense: PardoFixedExpense): Result<Unit> = runCatching {
        fixedExpenses[fixedExpense.id] = fixedExpense
    }

    override suspend fun getFixedExpenseById(id: String): PardoFixedExpense? {
        return fixedExpenses[id]
    }

    override suspend fun deleteBlockById(blockId: String): Result<Unit> = runCatching {
        movements.removeAll { it.blockId == blockId }
        if (lastBlockId == blockId) {
            lastBlockId = movements.lastOrNull { it.blockId.isNotBlank() }?.blockId
        }
    }

    override suspend fun getDashboardStatus(): Result<DashboardStatus> = runCatching {
        val totalIngresos = movements
            .filter { it.tipo == MovementType.INGRESO }
            .sumOf { it.valor }
        val totalEgresos = movements
            .filter { it.tipo == MovementType.EGRESO }
            .sumOf { it.valor }

        val disponible = baseDisponible + totalIngresos - totalEgresos

        DashboardStatus(
            disponible = disponible,
            budgetAlerts = budgetAlerts.toList(),
            fixedExpenses = fixedExpenses.values.toList()
        )
    }

    override suspend fun getLastBlockId(): String? {
        return lastBlockId
    }
}
