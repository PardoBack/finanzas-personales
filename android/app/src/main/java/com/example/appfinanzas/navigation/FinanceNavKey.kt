package com.example.appfinanzas.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class FinanceNavKey : NavKey {
    @Serializable
    data object Onboarding : FinanceNavKey()

    @Serializable
    data object Dashboard : FinanceNavKey()
    
    @Serializable
    data object Movements : FinanceNavKey()
    
    @Serializable
    data object FixedExpenses : FinanceNavKey()

    @Serializable
    data object Analytics : FinanceNavKey()

    @Serializable
    data object Goals : FinanceNavKey()

    @Serializable
    data object Budgets : FinanceNavKey()

    @Serializable
    data object AiAssistant : FinanceNavKey()
}
