package com.example.appfinanzas.data

import java.time.LocalDate

data class Transaction(
    val id: String,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    val category: String,
    val isExpense: Boolean
)

data class FixedExpense(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDate: Int, // Day of the month
    val isPaid: Boolean
)

data class BudgetCategory(
    val id: String,
    val name: String,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val icon: String
)

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: LocalDate
)

data class AntExpense(
    val id: String,
    val description: String,
    val amount: Double,
    val date: LocalDate
)

data class ReceivableDebt(
    val id: String,
    val debtorName: String,
    val amount: Double,
    val dueDate: LocalDate,
    val isPaid: Boolean
)

data class PayStub(
    val id: String,
    val period: String,
    val netAmount: Double,
    val date: LocalDate
)
