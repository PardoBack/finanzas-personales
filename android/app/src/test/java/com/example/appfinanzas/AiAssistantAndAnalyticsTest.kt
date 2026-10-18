package com.example.appfinanzas

import com.example.appfinanzas.data.UserPreferencesRepository
import com.example.appfinanzas.data.local.SavingsGoalEntity
import com.example.appfinanzas.data.remote.GastoFijo
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.data.remote.ResumenResponse
import com.example.appfinanzas.data.remote.SimpleResponse
import com.example.appfinanzas.data.repository.FinanceRepository
import com.example.appfinanzas.data.repository.FinancialContext
import com.example.appfinanzas.data.repository.GeminiRepository
import com.example.appfinanzas.ui.viewmodel.AiAssistantViewModel
import com.example.appfinanzas.ui.viewmodel.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFinanceRepository : FinanceRepository {
    private val movimientos = mutableListOf(
        Movimiento(id = 1, fecha = "2026-09-01", concepto = "Sueldo", monto = 3000000, tipo = "ingreso", categoria = "Sueldo", cuenta = "Nequi"),
        Movimiento(id = 2, fecha = "2026-09-02", concepto = "Arriendo", monto = 1000000, tipo = "gasto", categoria = "Vivienda", cuenta = "Nequi"),
        Movimiento(id = 3, fecha = "2026-09-03", concepto = "Mercado", monto = 400000, tipo = "gasto", categoria = "Alimentación", cuenta = "Tarjeta"),
        Movimiento(id = 4, fecha = "2026-09-04", concepto = "Hormiga: Café", monto = 8000, tipo = "gasto", categoria = "Ocio", cuenta = "Efectivo", observaciones = "GASTO_HORMIGA")
    )

    private val fijos = mutableListOf(
        GastoFijo(id = 1, concepto = "Arriendo Depto", monto = 1000000.0, diaPago = 5),
        GastoFijo(id = 2, concepto = "Internet Fibra", monto = 100000.0, diaPago = 10)
    )

    override fun getResumenFlow(): Flow<ResumenResponse> = flowOf(ResumenResponse(balance = 1600000.0, disponible = 1600000.0, ingresos = 3000000.0, gastos = 1400000.0))
    override fun getMovimientosFlow(): Flow<List<Movimiento>> = flowOf(movimientos)
    override fun getFijosFlow(): Flow<List<GastoFijo>> = flowOf(fijos)
    override fun getSavingsGoalsFlow(): Flow<List<SavingsGoalEntity>> = flowOf(emptyList())

    override suspend fun checkAndSeedInitialData() {}
    override suspend fun refreshData() {}
    override suspend fun addMovimiento(movimiento: Movimiento): Result<SimpleResponse> {
        movimientos.add(movimiento)
        return Result.success(SimpleResponse(success = true, message = "OK"))
    }
    override suspend fun addSavingsGoal(goal: SavingsGoalEntity) {}
    override suspend fun contributeToGoal(goalId: Int, amount: Double) {}
    override suspend fun syncUnsyncedTransactions() {}
    override suspend fun getResumen(): Result<ResumenResponse> = Result.success(ResumenResponse())
    override suspend fun getMovimientos(): Result<List<Movimiento>> = Result.success(movimientos)
    override suspend fun getFijos(): Result<List<GastoFijo>> = Result.success(fijos)
}

class FakeGeminiRepository : GeminiRepository {
    override suspend fun generateFinancialAdvice(
        userPrompt: String,
        context: FinancialContext
    ): Result<String> {
        return Result.failure(IllegalStateException("NO_API_KEY"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AiAssistantAndAnalyticsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAiAssistantInitialWelcomeMessage() = runTest {
        val repo = FakeFinanceRepository()
        val fakeGemini = FakeGeminiRepository()
        val prefs = UserPreferencesRepository()
        val viewModel = AiAssistantViewModel(repo, fakeGemini, prefs)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.messages.isEmpty())
        assertEquals(MessageSender.AI, state.messages.first().sender)
        assertTrue(state.messages.first().text.contains("IA Pardo Assistant"))
    }

    @Test
    fun testAiAssistantQuickPromptTopSpendingCategory() = runTest {
        val repo = FakeFinanceRepository()
        val fakeGemini = FakeGeminiRepository()
        val prefs = UserPreferencesRepository()
        val viewModel = AiAssistantViewModel(repo, fakeGemini, prefs)

        advanceUntilIdle()

        viewModel.sendMessage("¿En qué estoy gastando más este mes?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.messages.size)
        val aiResponse = state.messages.last().text
        assertTrue(aiResponse.contains("Vivienda") || aiResponse.contains("Alimentación"))
        assertTrue(aiResponse.contains("Análisis de Gastos Principales"))
    }

    @Test
    fun testAiAssistantQuickPromptPurchaseFeasibility() = runTest {
        val repo = FakeFinanceRepository()
        val fakeGemini = FakeGeminiRepository()
        val prefs = UserPreferencesRepository()
        val viewModel = AiAssistantViewModel(repo, fakeGemini, prefs)

        advanceUntilIdle()

        viewModel.sendMessage("¿Puedo comprar algo de $150.000 hoy sin sobregirarme?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val aiResponse = state.messages.last().text
        assertNotNull(aiResponse)
        assertTrue(aiResponse.contains("150.000") || aiResponse.contains("Disponible Real"))
    }

    @Test
    fun testAiAssistantQuickPromptAntExpensesReduction() = runTest {
        val repo = FakeFinanceRepository()
        val fakeGemini = FakeGeminiRepository()
        val prefs = UserPreferencesRepository()
        val viewModel = AiAssistantViewModel(repo, fakeGemini, prefs)

        advanceUntilIdle()

        viewModel.sendMessage("¿Cómo reduzco mis gastos hormiga?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val aiResponse = state.messages.last().text
        assertTrue(aiResponse.contains("Gastos Hormiga") || aiResponse.contains("microcompras"))
    }

    @Test
    fun testAiAssistantQuickPromptSavingsPlan() = runTest {
        val repo = FakeFinanceRepository()
        val fakeGemini = FakeGeminiRepository()
        val prefs = UserPreferencesRepository()
        val viewModel = AiAssistantViewModel(repo, fakeGemini, prefs)

        advanceUntilIdle()

        viewModel.sendMessage("Genera mi plan de ahorro para los próximos 3 meses")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val aiResponse = state.messages.last().text
        assertTrue(aiResponse.contains("Plan de Ahorro") || aiResponse.contains("Mes 1"))
    }
}
