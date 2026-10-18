package com.example.appfinanzas.data

object MockDataProvider {
    val transactions = emptyList<Transaction>()

    val fixedExpenses = emptyList<FixedExpense>()

    val budgetCategories = listOf(
        BudgetCategory("1", "Alimentación", 0.0, 0.0, "shopping_cart"),
        BudgetCategory("2", "Transporte", 0.0, 0.0, "directions_car"),
        BudgetCategory("3", "Servicios", 0.0, 0.0, "flash_on"),
        BudgetCategory("4", "Ocio", 0.0, 0.0, "movie"),
        BudgetCategory("5", "Hogar", 0.0, 0.0, "home"),
        BudgetCategory("6", "Salud", 0.0, 0.0, "local_hospital")
    )

    val savingsGoals = emptyList<SavingsGoal>()

    val antExpenses = emptyList<AntExpense>()

    val receivableDebts = emptyList<ReceivableDebt>()

    val payStubs = emptyList<PayStub>()
}
