package com.example.appfinanzas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: String? = null,
    val fecha: String,
    val concepto: String,
    val monto: Int,
    val tipo: String, // "ingreso" or "gasto"
    val categoria: String,
    val cuenta: String,
    val observaciones: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey val id: String,
    val concepto: String,
    val monto: Double,
    val diaPago: Int
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val category: String = "General"
)
