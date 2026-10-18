package com.example.appfinanzas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanzas.data.BudgetCategory
import com.example.appfinanzas.data.UserPreferencesRepository
import com.example.appfinanzas.data.constants.SheetConstants
import com.example.appfinanzas.data.local.SavingsGoalEntity
import com.example.appfinanzas.data.remote.GastoFijo
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.data.remote.ResumenResponse
import com.example.appfinanzas.data.repository.FinanceRepository
import com.example.appfinanzas.data.repository.GoogleDriveSyncRepository
import com.example.appfinanzas.data.repository.SecurityRepository
import com.example.appfinanzas.domain.model.FinancialNotification
import com.example.appfinanzas.domain.model.MovementType
import com.example.appfinanzas.domain.model.NotificationPriority
import com.example.appfinanzas.domain.model.NotificationType
import com.example.appfinanzas.domain.model.PardoMovement
import com.example.appfinanzas.domain.usecase.AddMovementUseCase
import com.example.appfinanzas.domain.usecase.GetDashboardStatusUseCase
import com.example.appfinanzas.domain.usecase.PayFixedExpenseUseCase
import com.example.appfinanzas.util.CurrencyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class UserProfileState(
    val userName: String = "Pro",
    val userCurrency: String = "COP",
    val averageSalary: Double = 2500000.0,
    val googleWebAppUrl: String = "",
    val geminiApiKey: String = ""
)

data class SyncUiState(
    val isSyncing: Boolean = false,
    val isSuccess: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

data class NotificationsUiState(
    val notifications: List<FinancialNotification> = emptyList(),
    val unreadCount: Int = 0
)

data class DashboardUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val resumen: ResumenResponse? = null,
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val comprometidoFijos: Double = 0.0,
    val disponibleReal: Double = 0.0,
    val movimientos: List<Movimiento> = emptyList(),
    val gastosFijos: List<GastoFijo> = emptyList(),
    val pagadosIds: Set<String> = emptySet(),
    val budgetCategories: List<BudgetCategory> = emptyList(),
    val overspentCategories: List<BudgetCategory> = emptyList(),
    val dailyAntTotal: Double = 0.0,
    val dailyAntLimit: Double = 30000.0,
    val error: String? = null,
)

data class MovementsUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val movimientos: List<Movimiento> = emptyList(),
    val filterType: String = "Todos",
    val searchQuery: String = "",
    val error: String? = null,
)

data class FixedExpensesUiState(
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val gastosFijos: List<GastoFijo> = emptyList(),
    val pagadosIds: Set<String> = emptySet(),
    val error: String? = null,
)

data class CategoryAnalyticItem(
    val category: String = "",
    val amount: Double = 0.0,
    val percentage: Double = 0.0,
    val count: Int = 0
)

data class AnalyticsUiState(
    val selectedPeriod: String = "Este Mes",
    val healthScore: Int = 0,
    val healthScoreLabel: String = "Excelente",
    val healthScoreDescription: String = "",
    val fixedExpenseRatio: Double = 0.0,
    val lifestyleRatio: Double = 0.0,
    val savingsRatio: Double = 0.0,
    val dailyAverageExpense: Double = 0.0,
    val monthEndProjectedSpent: Double = 0.0,
    val monthEndProjectedBalance: Double = 0.0,
    val remainingFixedExpenses: Double = 0.0,
    val daysPassed: Int = 1,
    val remainingDaysInMonth: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val antExpensesTotal: Double = 0.0,
    val antExpensesPercentageOfIncome: Double = 0.0,
    val categoryBreakdown: List<CategoryAnalyticItem> = emptyList(),
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val monthlyComparison: Map<String, Pair<Double, Double>> = emptyMap(),
)

data class SavingsGoalsUiState(
    val goals: List<SavingsGoalEntity> = emptyList(),
    val isLoading: Boolean = false,
)

data class AddTransactionUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val prefill: PrefillData? = null,
)

@Serializable
data class PrefillData(
    val concepto: String,
    val monto: Int,
    val tipo: String,
    val categoria: String,
    val esPagoFijo: Boolean = false,
    val fijoId: String? = null,
    val periodo: String? = null,
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val addMovementUseCase: AddMovementUseCase,
    private val payFixedExpenseUseCase: PayFixedExpenseUseCase,
    private val getDashboardStatusUseCase: GetDashboardStatusUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    val securityRepository: SecurityRepository,
    val googleDriveSyncRepository: GoogleDriveSyncRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val userProfileState: StateFlow<UserProfileState> = combine(
        userPreferencesRepository.userName,
        userPreferencesRepository.userCurrency,
        userPreferencesRepository.averageSalary,
        userPreferencesRepository.googleWebAppUrl,
        userPreferencesRepository.geminiApiKey
    ) { name, currency, salary, webUrl, geminiKey ->
        UserProfileState(
            userName = name,
            userCurrency = currency,
            averageSalary = salary,
            googleWebAppUrl = webUrl,
            geminiApiKey = geminiKey
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileState())

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    val notificationsState: StateFlow<NotificationsUiState> = combine(
        repository.getMovimientosFlow(),
        repository.getFijosFlow(),
        userPreferencesRepository.budgetLimits,
        userPreferencesRepository.dismissedNotifications
    ) { movimientos, fijos, limitsMap, dismissedSet ->
        val sdfPeriod = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val periodoActual = sdfPeriod.format(Date())

        val pagadosIds = movimientos
            .filter { it.observaciones?.startsWith("PAGO_FIJO|") == true }
            .filter { it.observaciones?.endsWith("|") == true }
            .mapNotNull { it.observaciones?.split("|")?.getOrNull(1) }
            .flatMap { listOf(it, it.removePrefix("F-"), "F-") }
            .toSet()

        val totalIngresos = movimientos.filter { it.tipo == "ingreso" }.sumOf { it.monto }.toDouble()
        val totalGastos = movimientos.filter { it.tipo == "gasto" }.sumOf { it.monto }.toDouble()
        val totalBalance = totalIngresos - totalGastos
        val fijosPendientes = fijos.filter { !pagadosIds.contains(it.id) }
        val comprometidoFijos = fijosPendientes.sumOf { it.monto }
        val disponibleReal = (totalBalance - comprometidoFijos).coerceAtLeast(0.0)

        val list = mutableListOf<FinancialNotification>()

        // 1. Fixed expense alerts
        fijosPendientes.forEach { fijo ->
            val notifId = "fixed_${fijo.id}_$periodoActual"
            if (!dismissedSet.contains(notifId)) {
                list.add(
                    FinancialNotification(
                        id = notifId,
                        title = "🚨 Cuota Pendiente: ${fijo.concepto}",
                        message = "Tienes pendiente la cuota de ${CurrencyUtils.formatCOP(fijo.monto)} con día de cobro ${fijo.diaPago} del mes.",
                        type = NotificationType.FIXED_EXPENSE_DUE,
                        priority = NotificationPriority.HIGH,
                        dateText = "Día ${fijo.diaPago}",
                        actionType = "PAY_FIXED",
                        targetId = fijo.id
                    )
                )
            }
        }

        // 2. Budget overdraft alerts
        limitsMap.forEach { (catName, limit) ->
            if (limit > 0) {
                val spent = movimientos.filter { it.categoria == catName && it.tipo == "gasto" }.sumOf { it.monto }.toDouble()
                if (spent >= limit) {
                    val notifId = "budget_${catName}_$periodoActual"
                    if (!dismissedSet.contains(notifId)) {
                        list.add(
                            FinancialNotification(
                                id = notifId,
                                title = "⚡ Presupuesto Superado: $catName",
                                message = "Has gastado ${CurrencyUtils.formatCOP(spent)}, sobrepasando el límite de ${CurrencyUtils.formatCOP(limit)}.",
                                type = NotificationType.BUDGET_OVERBUDGET,
                                priority = NotificationPriority.HIGH,
                                dateText = "Este Mes",
                                actionType = "NAVIGATE_BUDGETS",
                                targetCategory = catName
                            )
                        )
                    }
                }
            }
        }

        // 3. Ant expense warning
        val sdfToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdfToday.format(Date())
        val dailyAntTotal = movimientos.filter {
            (it.fecha == todayStr) && (
                it.categoria == "Ocio" ||
                it.observaciones == "GASTO_HORMIGA" ||
                it.concepto.startsWith("Hormiga:")
            )
        }.sumOf { it.monto }.toDouble()

        if (dailyAntTotal >= 30000.0) {
            val notifId = "ant_$todayStr"
            if (!dismissedSet.contains(notifId)) {
                list.add(
                    FinancialNotification(
                        id = notifId,
                        title = "⚡ Alerta de Gastos Hormiga",
                        message = "Has acumulado ${CurrencyUtils.formatCOP(dailyAntTotal)} hoy en microgastos. ¡Modera tus antojos!",
                        type = NotificationType.ANT_EXPENSE_ALERT,
                        priority = NotificationPriority.MEDIUM,
                        dateText = "Hoy",
                        actionType = "NAVIGATE_MOVEMENTS"
                    )
                )
            }
        }

        // 4. AI Recommendation
        if (disponibleReal > 200000.0) {
            val notifId = "ai_rec_disponible_$periodoActual"
            if (!dismissedSet.contains(notifId)) {
                list.add(
                    FinancialNotification(
                        id = notifId,
                        title = "💡 Recomendación IA Pardo",
                        message = "Cuentas con ${CurrencyUtils.formatCOP(disponibleReal)} de disponible real libre. Considera abonar a tus Metas de Ahorro.",
                        type = NotificationType.AI_RECOMMENDATION,
                        priority = NotificationPriority.LOW,
                        dateText = "Recomendación",
                        actionType = "NAVIGATE_AI"
                    )
                )
            }
        }

        NotificationsUiState(
            notifications = list,
            unreadCount = list.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUiState())

    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.getMovimientosFlow(),
        repository.getFijosFlow(),
        userPreferencesRepository.budgetLimits,
        _isRefreshing
    ) { movimientos, fijos, limitsMap, refreshing ->
        val totalIngresos = movimientos.asSequence().filter { it.tipo == "ingreso" }.sumOf { it.monto }.toDouble()
        val totalGastos = movimientos.asSequence().filter { it.tipo == "gasto" }.sumOf { it.monto }.toDouble()
        val totalBalance = totalIngresos - totalGastos

        val sdfPeriod = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val periodoActual = sdfPeriod.format(Date())

        val pagadosIds = movimientos
            .filter { it.observaciones?.startsWith("PAGO_FIJO|") == true }
            .filter { it.observaciones?.endsWith("|") == true }
            .mapNotNull { it.observaciones?.split("|")?.getOrNull(1) }
            .flatMap { listOf(it, it.removePrefix("F-"), "F-") }
            .toSet()

        val comprometidoFijos = fijos
            .filter { !pagadosIds.contains(it.id) }
            .sumOf { it.monto }

        val disponibleReal = (totalBalance - comprometidoFijos).coerceAtLeast(0.0)

        val sdfToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdfToday.format(Date())
        val dailyAntTotal = movimientos
            .filter {
                (it.fecha == todayStr) && (
                    it.categoria == "Ocio" ||
                        it.observaciones == "GASTO_HORMIGA" ||
                        it.concepto.startsWith("Hormiga:")
                    )
            }
            .sumOf { it.monto }
            .toDouble()

        val budgetCategories = limitsMap.map { (catName, limit) ->
            val spent = movimientos.filter { it.categoria == catName && it.tipo == "gasto" }
                .sumOf { it.monto }.toDouble()
            BudgetCategory(
                id = catName,
                name = catName,
                allocatedAmount = limit,
                spentAmount = spent,
                icon = "category"
            )
        }
        val overspentCategories = budgetCategories.filter { it.spentAmount >= it.allocatedAmount && it.allocatedAmount > 0 }

        val resumen = ResumenResponse(
            balance = totalBalance,
            disponible = disponibleReal,
            ingresos = totalIngresos,
            gastos = totalGastos
        )

        DashboardUiState(
            isRefreshing = refreshing,
            isLoading = false,
            resumen = resumen,
            totalBalance = totalBalance,
            monthlyIncome = totalIngresos,
            monthlyExpenses = totalGastos,
            comprometidoFijos = comprometidoFijos,
            disponibleReal = disponibleReal,
            movimientos = movimientos.take(5),
            gastosFijos = fijos,
            pagadosIds = pagadosIds,
            budgetCategories = budgetCategories,
            overspentCategories = overspentCategories,
            dailyAntTotal = dailyAntTotal,
            dailyAntLimit = 30000.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState(isLoading = true))

    private val _filterType = MutableStateFlow("Todos")
    private val _searchQuery = MutableStateFlow("")

    val movementsState: StateFlow<MovementsUiState> = combine(
        repository.getMovimientosFlow(),
        _filterType,
        _searchQuery,
        _isRefreshing
    ) { movimientos, filter, queryText, refreshing ->
        val filteredByChip = when (filter) {
            "Gastos" -> movimientos.filter { it.tipo == "gasto" }
            "Ingresos" -> movimientos.filter { it.tipo == "ingreso" }
            else -> movimientos
        }

        val q = queryText.trim().lowercase()
        val finalFiltered = if (q.isEmpty()) {
            filteredByChip
        } else {
            filteredByChip.filter { m ->
                m.concepto.lowercase().contains(q) ||
                m.categoria.lowercase().contains(q) ||
                m.cuenta.lowercase().contains(q) ||
                (m.observaciones?.lowercase()?.contains(q) == true)
            }
        }

        MovementsUiState(
            movimientos = finalFiltered,
            filterType = filter,
            searchQuery = queryText,
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MovementsUiState(isLoading = true))

    val fixedExpensesState: StateFlow<FixedExpensesUiState> = combine(
        repository.getFijosFlow(),
        repository.getMovimientosFlow(),
        _isRefreshing
    ) { fijos, movimientos, refreshing ->
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val periodoActual = sdf.format(Date())
        val pagadosIds = movimientos
            .filter { it.observaciones?.startsWith("PAGO_FIJO|") == true }
            .filter { it.observaciones?.endsWith("|") == true }
            .mapNotNull { it.observaciones?.split("|")?.getOrNull(1) }
            .flatMap { listOf(it, it.removePrefix("F-"), "F-") }
            .toSet()
        FixedExpensesUiState(
            gastosFijos = fijos,
            pagadosIds = pagadosIds,
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FixedExpensesUiState(isLoading = true))

    private val _selectedAnalyticsPeriod = MutableStateFlow("Este Mes")

    fun setAnalyticsPeriod(period: String) {
        _selectedAnalyticsPeriod.value = period
    }

    val analyticsState: StateFlow<AnalyticsUiState> = combine(
        repository.getMovimientosFlow(),
        repository.getFijosFlow(),
        _selectedAnalyticsPeriod
    ) { movimientos, fijos, selectedPeriod ->
        val calendar = Calendar.getInstance()
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val currentYearMonth = sdfMonth.format(Date())
        val currentYear = calendar.get(Calendar.YEAR)

        val filteredMovimientos = when (selectedPeriod) {
            "Últimos 3 Meses" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -2)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = cal.time
                movimientos.filter {
                    runCatching { sdfInput.parse(it.fecha) }.getOrNull()?.let { d -> d >= startDate } ?: true
                }
            }
            "Año Actual" -> {
                movimientos.filter {
                    it.fecha.startsWith("$currentYear")
                }
            }
            else -> { // "Este Mes"
                movimientos.filter {
                    it.fecha.startsWith(currentYearMonth)
                }
            }
        }

        val totalIngresos = filteredMovimientos.filter { it.tipo == "ingreso" }.sumOf { it.monto }.toDouble()
        val totalGastos = filteredMovimientos.filter { it.tipo == "gasto" }.sumOf { it.monto }.toDouble()

        // 1. Calculate 50/30/20 Financial Health Score
        val fijosTotal = fijos.sumOf { it.monto }
        val fijosRatio = if (totalIngresos > 0) (fijosTotal / totalIngresos) else 0.50

        val estiloVidaMonto = filteredMovimientos
            .filter { it.tipo == "gasto" && it.categoria != "Servicios" && it.categoria != "Vivienda" && it.categoria != "Ahorro" }
            .sumOf { it.monto }.toDouble()
        val estiloVidaRatio = if (totalIngresos > 0) (estiloVidaMonto / totalIngresos) else 0.30

        val ahorroMonto = (totalIngresos - totalGastos).coerceAtLeast(0.0)
        val ahorroRatio = if (totalIngresos > 0) (ahorroMonto / totalIngresos) else 0.20

        val fijosScore = when {
            fijosRatio <= 0.50 -> 40.0
            fijosRatio <= 0.80 -> 40.0 * (1.0 - (fijosRatio - 0.50) / 0.30)
            else -> 0.0
        }
        val estiloVidaScore = when {
            estiloVidaRatio <= 0.30 -> 30.0
            estiloVidaRatio <= 0.60 -> 30.0 * (1.0 - (estiloVidaRatio - 0.30) / 0.30)
            else -> 0.0
        }
        val ahorroScore = when {
            ahorroRatio >= 0.20 -> 30.0
            ahorroRatio > 0.0 -> 30.0 * (ahorroRatio / 0.20)
            else -> 0.0
        }

        val healthScore = (fijosScore + estiloVidaScore + ahorroScore).roundToInt().coerceIn(0, 100)
        val (healthLabel, healthDesc) = when {
            healthScore >= 80 -> Pair("Excelente 🌟", "Tus finanzas siguen una estructura 50/30/20 óptima.")
            healthScore >= 60 -> Pair("Saludable 👍", "Buen manejo general, revisa pequeños picos de gasto.")
            healthScore >= 40 -> Pair("En Riesgo ⚠️", "Tus gastos fijos o de estilo de vida superan el límite sugerido.")
            else -> Pair("Atención 🚨", "Tus gastos superan tus ingresos. Es necesario reducir egresos de inmediato.")
        }

        // 2. Daily Average & Month-End Projection
        val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysPassed = when (selectedPeriod) {
            "Últimos 3 Meses" -> 90
            "Año Actual" -> calendar.get(Calendar.DAY_OF_YEAR).coerceAtLeast(1)
            else -> currentDayOfMonth
        }

        val dailyAverageExpense = totalGastos / daysPassed

        val sdfPeriod = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val periodoActual = sdfPeriod.format(Date())
        val pagadosIds = movimientos
            .filter { it.observaciones?.startsWith("PAGO_FIJO|") == true }
            .filter { it.observaciones?.endsWith("|") == true }
            .mapNotNull { it.observaciones?.split("|")?.getOrNull(1) }
            .flatMap { listOf(it, it.removePrefix("F-"), "F-") }
            .toSet()

        val fijosPendientesMonto = fijos.filter { !pagadosIds.contains(it.id) }.sumOf { it.monto }
        val remainingDaysInMonth = (totalDaysInMonth - currentDayOfMonth).coerceAtLeast(0)

        val monthEndProjectedSpent = totalGastos + (dailyAverageExpense * remainingDaysInMonth) + fijosPendientesMonto
        val monthEndProjectedBalance = totalIngresos - monthEndProjectedSpent

        // 3. Category Breakdown
        val expensesByCategory = filteredMovimientos.filter { it.tipo == "gasto" }
            .groupBy { it.categoria }
            .mapValues { entry -> entry.value.sumOf { it.monto }.toDouble() }

        val categoryBreakdown = expensesByCategory.map { (cat, amount) ->
            val pct = if (totalGastos > 0) (amount / totalGastos) * 100.0 else 0.0
            val count = filteredMovimientos.count { it.tipo == "gasto" && it.categoria == cat }
            CategoryAnalyticItem(category = cat, amount = amount, percentage = pct, count = count)
        }.sortedByDescending { it.amount }

        // 4. Ant Expenses
        val antExpenses = filteredMovimientos.filter {
            it.tipo == "gasto" && (
                it.categoria == "Ocio" ||
                it.observaciones == "GASTO_HORMIGA" ||
                it.concepto.startsWith("Hormiga:")
            )
        }
        val antExpensesTotal = antExpenses.sumOf { it.monto }.toDouble()
        val antExpensesPercentageOfIncome = if (totalIngresos > 0) (antExpensesTotal / totalIngresos) * 100.0 else 0.0

        // 5. Monthly comparison chart data
        val sdfGroup = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthlyComparison = movimientos.groupBy {
            runCatching { sdfInput.parse(it.fecha)!! }.mapCatching { sdfGroup.format(it) }.getOrDefault("Unknown")
        }.mapValues { entry ->
            val inc = entry.value.filter { it.tipo == "ingreso" }.sumOf { it.monto }.toDouble()
            val exp = entry.value.filter { it.tipo == "gasto" }.sumOf { it.monto }.toDouble()
            Pair(inc, exp)
        }

        AnalyticsUiState(
            selectedPeriod = selectedPeriod,
            healthScore = healthScore,
            healthScoreLabel = healthLabel,
            healthScoreDescription = healthDesc,
            fixedExpenseRatio = fijosRatio * 100.0,
            lifestyleRatio = estiloVidaRatio * 100.0,
            savingsRatio = ahorroRatio * 100.0,
            dailyAverageExpense = dailyAverageExpense,
            monthEndProjectedSpent = monthEndProjectedSpent,
            monthEndProjectedBalance = monthEndProjectedBalance,
            remainingFixedExpenses = fijosPendientesMonto,
            daysPassed = daysPassed,
            remainingDaysInMonth = remainingDaysInMonth,
            totalIncome = totalIngresos,
            totalExpenses = totalGastos,
            antExpensesTotal = antExpensesTotal,
            antExpensesPercentageOfIncome = antExpensesPercentageOfIncome,
            categoryBreakdown = categoryBreakdown,
            expensesByCategory = expensesByCategory,
            monthlyComparison = monthlyComparison
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    val savingsGoalsState: StateFlow<SavingsGoalsUiState> = repository.getSavingsGoalsFlow().map { goals ->
        SavingsGoalsUiState(goals = goals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavingsGoalsUiState(isLoading = true))

    private val _addTransactionState = MutableStateFlow(AddTransactionUiState())
    val addTransactionState: StateFlow<AddTransactionUiState> = _addTransactionState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            getDashboardStatusUseCase()
            refreshAllData()
        }
    }

    fun updateUserProfile(
        name: String,
        currency: String,
        salary: Double,
        webAppUrl: String,
        geminiApiKey: String
    ) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
            userPreferencesRepository.setUserCurrency(currency)
            userPreferencesRepository.setAverageSalary(salary)
            userPreferencesRepository.setGoogleWebAppUrl(webAppUrl)
            userPreferencesRepository.setGeminiApiKey(geminiApiKey)
        }
    }

    fun dismissNotification(id: String) {
        viewModelScope.launch {
            userPreferencesRepository.dismissNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            val currentNotifs = notificationsState.value.notifications
            currentNotifs.forEach { notif ->
                userPreferencesRepository.dismissNotification(notif.id)
            }
        }
    }

    fun syncWithGoogleDrive() {
        viewModelScope.launch {
            _syncState.value = SyncUiState(isSyncing = true)
            val result = googleDriveSyncRepository.syncAllDataToDrive()
            if (result.isSuccess) {
                val syncRes = result.getOrNull()
                _syncState.value = SyncUiState(
                    isSyncing = false,
                    isSuccess = true,
                    message = syncRes?.message ?: "Sincronización exitosa con Google Drive"
                )
            } else {
                _syncState.value = SyncUiState(
                    isSyncing = false,
                    isSuccess = false,
                    error = result.exceptionOrNull()?.message ?: "Error al sincronizar con Google Drive"
                )
            }
        }
    }

    fun resetSyncStatus() {
        _syncState.value = SyncUiState()
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshData()
            getDashboardStatusUseCase()
            _isRefreshing.value = false
        }
    }

    fun setMovementsFilter(filter: String) {
        _filterType.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryBudgetLimit(category: String, limit: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetLimit(category, limit)
        }
    }

    fun addTransaction(
        concepto: String,
        monto: Int,
        tipo: String,
        categoria: String,
        cuenta: String,
        esPagoFijo: Boolean = false,
        fijoId: String? = null,
        periodo: String? = null,
    ) {
        viewModelScope.launch {
            _addTransactionState.update { it.copy(isSaving = true, isSuccess = false, error = null) }

            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdfPeriod = SimpleDateFormat("MM-yyyy", Locale.getDefault())

            val dateStr = sdfDate.format(Date())
            val timeStr = sdfTime.format(Date())
            val periodTag = periodo ?: sdfPeriod.format(Date())

            val observaciones = if (esPagoFijo && fijoId != null) {
                val fixedExpenseIdStr = String.format(Locale.getDefault(), "F-%04d", fijoId)
                payFixedExpenseUseCase(
                    fixedExpenseId = fixedExpenseIdStr,
                    amountPaid = monto.toDouble(),
                    paymentMethod = cuenta,
                    account = cuenta,
                    periodTag = periodTag,
                    fecha = dateStr,
                    hora = timeStr
                )
                String.format(SheetConstants.PAGO_FIJO_TAG_FORMAT, fixedExpenseIdStr, periodTag)
            } else {
                val pardoMovement = PardoMovement(
                    fecha = dateStr,
                    hora = timeStr,
                    tipo = if (tipo == "ingreso") MovementType.INGRESO else MovementType.EGRESO,
                    categoria = categoria,
                    subcategoria = "General",
                    descripcion = concepto,
                    valor = monto.toDouble(),
                    metodoPago = cuenta,
                    cuenta = cuenta
                )
                addMovementUseCase(pardoMovement)
                null
            }

            val nuevoMovimiento = Movimiento(
                fecha = dateStr,
                concepto = concepto,
                monto = monto,
                tipo = tipo,
                categoria = categoria,
                cuenta = cuenta,
                observaciones = observaciones
            )

            val result = repository.addMovimiento(nuevoMovimiento)

            if (result.isSuccess) {
                _addTransactionState.update { it.copy(isSaving = false, isSuccess = true) }
            } else {
                _addTransactionState.update {
                    it.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Error al guardar transacción"
                    )
                }
            }
        }
    }

    fun addAntExpense(name: String, amount: Double) {
        viewModelScope.launch {
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateStr = sdfDate.format(Date())
            val timeStr = sdfTime.format(Date())

            val pardoMovement = PardoMovement(
                fecha = dateStr,
                hora = timeStr,
                tipo = MovementType.EGRESO,
                categoria = "Ocio",
                subcategoria = "Hormiga",
                descripcion = "Hormiga: $name",
                valor = amount,
                metodoPago = "Efectivo",
                cuenta = "Efectivo"
            )
            addMovementUseCase(pardoMovement)

            val nuevoMovimiento = Movimiento(
                fecha = dateStr,
                concepto = "Hormiga: $name",
                monto = amount.toInt(),
                tipo = "gasto",
                categoria = "Ocio",
                cuenta = "Efectivo",
                observaciones = "GASTO_HORMIGA"
            )
            repository.addMovimiento(nuevoMovimiento)
        }
    }

    fun payFixedExpense(gastoFijo: GastoFijo) {
        viewModelScope.launch {
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val sdfPeriod = SimpleDateFormat("MM-yyyy", Locale.getDefault())

            val dateStr = sdfDate.format(Date())
            val timeStr = sdfTime.format(Date())
            val periodTag = sdfPeriod.format(Date())

            val idNum = gastoFijo.id.removePrefix("F-").toIntOrNull()
            val fixedExpenseIdStr = if (idNum != null) String.format(Locale.getDefault(), "F-%04d", idNum) else gastoFijo.id

            payFixedExpenseUseCase(
                fixedExpenseId = fixedExpenseIdStr,
                amountPaid = gastoFijo.monto,
                paymentMethod = "Efectivo",
                account = "Efectivo",
                periodTag = periodTag,
                fecha = dateStr,
                hora = timeStr
            )

            val tag = String.format(SheetConstants.PAGO_FIJO_TAG_FORMAT, fixedExpenseIdStr, periodTag)
            val nuevoMovimiento = Movimiento(
                fecha = dateStr,
                concepto = gastoFijo.concepto,
                monto = gastoFijo.monto.toInt(),
                tipo = "gasto",
                categoria = "Servicios",
                cuenta = "Efectivo",
                observaciones = tag
            )
            repository.addMovimiento(nuevoMovimiento)
        }
    }

    fun addSavingsGoal(name: String, target: Double) {
        viewModelScope.launch {
            repository.addSavingsGoal(SavingsGoalEntity(name = name, targetAmount = target, currentAmount = 0.0))
        }
    }

    fun contributeToGoal(goalId: Int, amount: Double) {
        viewModelScope.launch {
            repository.contributeToGoal(goalId, amount)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            repository.addMovimiento(
                Movimiento(
                    fecha = sdf.format(Date()),
                    concepto = "Ahorro: Contribución",
                    monto = amount.toInt(),
                    tipo = "gasto",
                    categoria = "Ahorro",
                    cuenta = "Efectivo"
                )
            )
        }
    }

    fun prefillFixedExpense(gastoFijo: GastoFijo) {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val periodoActual = sdf.format(Date())
        _addTransactionState.update {
            it.copy(
                prefill = PrefillData(
                    concepto = gastoFijo.concepto,
                    monto = gastoFijo.monto.toInt(),
                    tipo = "gasto",
                    categoria = "Servicios",
                    esPagoFijo = true,
                    fijoId = gastoFijo.id,
                    periodo = periodoActual
                )
            )
        }
    }

    fun clearPrefill() {
        _addTransactionState.update { it.copy(prefill = null) }
    }

    fun resetAddTransactionState() {
        _addTransactionState.value = AddTransactionUiState()
    }
}
private const val Calendar_YEAR_FIELD_DAY_OF_YEAR = Calendar.DAY_OF_YEAR
