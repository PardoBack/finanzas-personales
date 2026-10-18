package com.example.appfinanzas.domain.usecase

import com.example.appfinanzas.domain.model.DashboardStatus
import com.example.appfinanzas.domain.model.FixedExpenseStatus
import com.example.appfinanzas.domain.repository.PardoFinanzasRepository
import javax.inject.Inject

class GetDashboardStatusUseCase @Inject constructor(
    private val repository: PardoFinanzasRepository
) {
    suspend operator fun invoke(): Result<DashboardStatus> {
        return execute()
    }

    suspend fun execute(): Result<DashboardStatus> {
        val result = repository.getDashboardStatus()
        return result.map { status ->
            val evaluatedAlerts = status.budgetAlerts.map { alert ->
                alert.copy(isOverdraft = alert.isOverdraft || alert.gastado > alert.limite)
            }

            val evaluatedExpenses = status.fixedExpenses.map { expense ->
                if (expense.estado == FixedExpenseStatus.VENCIDO) {
                    expense.copy(estado = FixedExpenseStatus.VENCIDO)
                } else {
                    expense
                }
            }

            status.copy(
                budgetAlerts = evaluatedAlerts,
                fixedExpenses = evaluatedExpenses
            )
        }
    }
}
