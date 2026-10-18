package com.example.appfinanzas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanzas.data.AntExpense
import com.example.appfinanzas.data.BudgetCategory
import com.example.appfinanzas.data.FixedExpense
import com.example.appfinanzas.data.MockDataProvider
import com.example.appfinanzas.data.PayStub
import com.example.appfinanzas.data.ReceivableDebt
import com.example.appfinanzas.data.SavingsGoal
import com.example.appfinanzas.data.Transaction
import com.example.appfinanzas.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinancialUiState(
    val transactions: List<Transaction> = emptyList(),
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val antExpenses: List<AntExpense> = emptyList(),
    val receivableDebts: List<ReceivableDebt> = emptyList(),
    val payStubs: List<PayStub> = emptyList(),
    val isLoading: Boolean = false,
    val totalBalance: Double = 0.0,
    val monthlyAvailable: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyIncome: Double = 0.0,
)

@HiltViewModel
class FinancialViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState.asStateFlow()

    val isFirstLaunch = userPreferencesRepository.isFirstLaunch

    init {
        loadMockData()
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _uiState.value = FinancialUiState(
                transactions = MockDataProvider.transactions,
                fixedExpenses = MockDataProvider.fixedExpenses,
                budgetCategories = MockDataProvider.budgetCategories,
                savingsGoals = MockDataProvider.savingsGoals,
                antExpenses = MockDataProvider.antExpenses,
                receivableDebts = MockDataProvider.receivableDebts,
                payStubs = MockDataProvider.payStubs,
                totalBalance = 0.0,
                monthlyAvailable = 0.0,
                monthlyExpenses = 0.0,
                monthlyIncome = 0.0
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setFirstLaunchCompleted()
        }
    }
}
