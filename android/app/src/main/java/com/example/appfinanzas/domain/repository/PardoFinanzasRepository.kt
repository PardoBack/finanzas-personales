package com.example.appfinanzas.domain.repository

import com.example.appfinanzas.domain.model.DashboardStatus
import com.example.appfinanzas.domain.model.PardoFixedExpense
import com.example.appfinanzas.domain.model.PardoMovement

interface PardoFinanzasRepository {
    suspend fun insertMovement(movement: PardoMovement): Result<Unit>
    suspend fun updateFixedExpense(fixedExpense: PardoFixedExpense): Result<Unit>
    suspend fun getFixedExpenseById(id: String): PardoFixedExpense?
    suspend fun deleteBlockById(blockId: String): Result<Unit>
    suspend fun getDashboardStatus(): Result<DashboardStatus>
    suspend fun getLastBlockId(): String?
}
