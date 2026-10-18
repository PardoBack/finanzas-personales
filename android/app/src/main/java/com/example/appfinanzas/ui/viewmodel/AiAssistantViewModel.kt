package com.example.appfinanzas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appfinanzas.data.UserPreferencesRepository
import com.example.appfinanzas.data.repository.FinanceRepository
import com.example.appfinanzas.data.repository.FinancialContext
import com.example.appfinanzas.data.repository.GeminiRepository
import com.example.appfinanzas.util.CurrencyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class MessageSender {
    USER, AI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isGeminiActive: Boolean = false,
    val quickPrompts: List<String> = listOf(
        "¿En qué estoy gastando más este mes?",
        "¿Puedo comprar algo de $150.000 hoy sin sobregirarme?",
        "¿Cómo reduzco mis gastos hormiga?",
        "Genera mi plan de ahorro para los próximos 3 meses"
    )
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val geminiRepository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        sendInitialWelcomeMessage()
        checkGeminiStatus()
    }

    private fun checkGeminiStatus() {
        viewModelScope.launch {
            val key = userPreferencesRepository.geminiApiKey.first()
            _uiState.update { it.copy(isGeminiActive = key.isNotBlank()) }
        }
    }

    private fun sendInitialWelcomeMessage() {
        viewModelScope.launch {
            val movimientos = repository.getMovimientosFlow().first()
            val totalIngresos = movimientos.filter { it.tipo == "ingreso" }.sumOf { it.monto }.toDouble()
            val totalGastos = movimientos.filter { it.tipo == "gasto" }.sumOf { it.monto }.toDouble()
            val balance = totalIngresos - totalGastos

            val welcomeText = """
                👋 ¡Hola! Soy **IA Pardo Assistant**, tu asesor financiero inteligente.
                
                Actualmente tienes un balance estimado de **${CurrencyUtils.formatCOP(balance)}**. Leo tus ingresos, gastos fijos y microgastos en tiempo real para darte recomendaciones personalizadas.
                
                ¿En qué te puedo orientar hoy? Toca una pregunta rápida o escríbeme directamente.
            """.trimIndent()

            _uiState.update { state ->
                state.copy(
                    messages = listOf(
                        ChatMessage(
                            sender = MessageSender.AI,
                            text = welcomeText
                        )
                    )
                )
            }
        }
    }

    fun sendMessage(userQuery: String) {
        if (userQuery.isBlank()) return

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = userQuery.trim()
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                isTyping = true
            )
        }

        viewModelScope.launch {
            val userName = userPreferencesRepository.userName.first()
            val movimientos = repository.getMovimientosFlow().first()
            val fijos = repository.getFijosFlow().first()

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

            val gastosPorCategoria = movimientos
                .filter { it.tipo == "gasto" }
                .groupBy { it.categoria }
                .mapValues { entry -> entry.value.sumOf { it.monto }.toDouble() }

            val topCat = gastosPorCategoria.maxByOrNull { it.value }

            val gastosHormiga = movimientos.filter {
                it.tipo == "gasto" && (
                    it.categoria == "Ocio" ||
                    it.observaciones == "GASTO_HORMIGA" ||
                    it.concepto.startsWith("Hormiga:")
                )
            }
            val totalGastosHormiga = gastosHormiga.sumOf { it.monto }.toDouble()

            val context = FinancialContext(
                userName = userName,
                totalBalance = totalBalance,
                disponibleReal = disponibleReal,
                comprometidoFijos = comprometidoFijos,
                totalIngresos = totalIngresos,
                totalGastos = totalGastos,
                fijosPendientesCount = fijosPendientes.size,
                totalGastosHormiga = totalGastosHormiga,
                topCategory = topCat?.key,
                topCategoryAmount = topCat?.value ?: 0.0
            )

            val geminiResult = geminiRepository.generateFinancialAdvice(userQuery, context)

            val finalResponseText = if (geminiResult.isSuccess) {
                geminiResult.getOrNull() ?: ""
            } else {
                delay(500)
                val fallbackText = generateContextAwareResponse(
                    query = userQuery,
                    totalBalance = totalBalance,
                    disponibleReal = disponibleReal,
                    comprometidoFijos = comprometidoFijos,
                    totalIngresos = totalIngresos,
                    totalGastos = totalGastos,
                    gastosPorCategoria = gastosPorCategoria,
                    fijosPendientesCount = fijosPendientes.size,
                    totalGastosHormiga = totalGastosHormiga
                )

                "$fallbackText\n\n💡 *Tip: Para respuestas con IA Real de Google Gemini, configura tu API Key sin costo en Perfil > Google Gemini API Key (aistudio.google.com)*"
            }

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + ChatMessage(
                        sender = MessageSender.AI,
                        text = finalResponseText
                    ),
                    isTyping = false
                )
            }
        }
    }

    private fun generateContextAwareResponse(
        query: String,
        totalBalance: Double,
        disponibleReal: Double,
        comprometidoFijos: Double,
        totalIngresos: Double,
        totalGastos: Double,
        gastosPorCategoria: Map<String, Double>,
        fijosPendientesCount: Int,
        totalGastosHormiga: Double
    ): String {
        val qLower = query.lowercase(Locale.getDefault())

        return when {
            qLower.contains("gastando más") || qLower.contains("mayor gasto") || qLower.contains("mas este mes") || qLower.contains("categoría") -> {
                if (gastosPorCategoria.isEmpty()) {
                    "📊 Aún no tienes gastos registrados en este período. ¡Agrega tus compras para clasificarlas!"
                } else {
                    val topCategory = gastosPorCategoria.maxByOrNull { it.value }!!
                    val percentage = if (totalGastos > 0) (topCategory.value / totalGastos) * 100 else 0.0

                    """
                    📊 **Análisis de Gastos Principales**
                    
                    Tu mayor categoría de gasto este mes es **${topCategory.key}** con **${CurrencyUtils.formatCOP(topCategory.value)}**, representando un **${String.format(Locale.getDefault(), "%.1f", percentage)}%** de tus egresos totales (${CurrencyUtils.formatCOP(totalGastos)}).
                    
                    📋 **Principales Categorías**:
                    ${gastosPorCategoria.entries.sortedByDescending { it.value }.take(4).joinToString("\n") { 
                        "• **${it.key}**: ${CurrencyUtils.formatCOP(it.value)}" 
                    }}
                    
                    💡 **Recomendación IA Pardo**: Puedes configurar una alerta de tope máximo para *${topCategory.key}* en el módulo de **Presupuestos**.
                    """.trimIndent()
                }
            }

            qLower.contains("150.000") || qLower.contains("150000") || (qLower.contains("puedo comprar") && qLower.contains("sobregirar")) -> {
                val montoCompra = 150000.0
                if (disponibleReal >= montoCompra) {
                    val restante = disponibleReal - montoCompra
                    """
                    ✅ **¡Sí! Puedes realizar la compra de ${CurrencyUtils.formatCOP(montoCompra)} sin sobregirarte.**
                    
                    • **Disponible Real libre**: ${CurrencyUtils.formatCOP(disponibleReal)}
                    • **Monto de la compra**: ${CurrencyUtils.formatCOP(montoCompra)}
                    • **Saldo restante tras la compra**: ${CurrencyUtils.formatCOP(restante)}
                    
                    🛡️ *Tus gastos fijos pendientes (${CurrencyUtils.formatCOP(comprometidoFijos)}) están totalmente apartados y protegidos.*
                    """.trimIndent()
                } else {
                    val faltante = montoCompra - disponibleReal
                    """
                    ⚠️ **No es recomendable realizar la compra de ${CurrencyUtils.formatCOP(montoCompra)} hoy.**
                    
                    • Tu **Disponible Real** (descontando fijos obligatorios) es de **${CurrencyUtils.formatCOP(disponibleReal)}**.
                    • Te faltan **${CurrencyUtils.formatCOP(faltante)}** para efectuar la compra sin invadir el dinero apartado para tus $fijosPendientesCount compromisos fijos pendientes (${CurrencyUtils.formatCOP(comprometidoFijos)}).
                    """.trimIndent()
                }
            }

            qLower.contains("gastos hormiga") || qLower.contains("reducir") || qLower.contains("microgastos") -> {
                val mensualProyectado = if (totalGastosHormiga > 0) totalGastosHormiga * 1.5 else 90000.0
                val anualImpacto = mensualProyectado * 12

                """
                🐜 **Plan de Acción para Eliminar Gastos Hormiga**
                
                Llevas **${CurrencyUtils.formatCOP(totalGastosHormiga)}** acumulados en microcompras. Al año esto representa **${CurrencyUtils.formatCOP(anualImpacto)}** de dinero que podrías ahorrar.
                
                🎯 **3 Pasos Clave**:
                1. **Pausa de 24 Horas**: Para compras imprevistas menores a $30.000 (cafés, antojos), espera 1 día antes de comprar.
                2. **Semáforo Diario**: Utiliza la sección *Fuga Hormiga* del Dashboard para monitorear el tope diario.
                3. **Ahorro Inverso**: Cada vez que evites un gasto hormiga, ve a **Metas** y abona el mismo valor. ¡Construirás tu fondo en semanas!
                """.trimIndent()
            }

            qLower.contains("plan de ahorro") || qLower.contains("3 meses") || qLower.contains("proximos 3 meses") -> {
                val baseIncome = if (totalIngresos > 0) totalIngresos else 1500000.0
                val cuotaMensual = baseIncome * 0.20
                val mes1 = cuotaMensual
                val mes2 = cuotaMensual * 2
                val mes3 = cuotaMensual * 3

                """
                🚀 **Plan de Ahorro a 3 Meses (IA Pardo)**
                
                Calculado según tu ingreso mensual (${CurrencyUtils.formatCOP(baseIncome)}):
                
                📌 **Hitos de Ahorro Recomendados**:
                • **Mes 1**: ${CurrencyUtils.formatCOP(mes1)} *(Ahorro del 20%)*
                • **Mes 2**: ${CurrencyUtils.formatCOP(mes2)} *(Acumulado)*
                • **Mes 3**: ${CurrencyUtils.formatCOP(mes3)} 🎉 *(Fondo de Reserva Alcanzado)*
                
                💡 **Siguiente Acción**: Abre el módulo de **Metas**, crea *"Fondo de Reserva 3M"* con objetivo ${CurrencyUtils.formatCOP(mes3)} y realiza tu primer aporte.
                """.trimIndent()
            }

            qLower.contains("saldo") || qLower.contains("balance") || qLower.contains("cuánto tengo") -> {
                """
                💰 **Estado Financiero Actual**
                
                • **Balance en Cuenta**: ${CurrencyUtils.formatCOP(totalBalance)}
                • **Comprometido en Fijos Pendientes**: ${CurrencyUtils.formatCOP(comprometidoFijos)}
                • **Disponible Real Líquido**: ${CurrencyUtils.formatCOP(disponibleReal)}
                • **Ingresos Registrados**: ${CurrencyUtils.formatCOP(totalIngresos)}
                • **Gastos Totales**: ${CurrencyUtils.formatCOP(totalGastos)}
                
                ${if (disponibleReal > 0) "✅ Tu disponible líquido está en terreno positivo." else "🚨 Tus compromisos fijos igualan o superan tu balance."}
                """.trimIndent()
            }

            else -> {
                """
                🤖 **Asistente IA Pardo**:
                
                He evaluado tus datos financieros. Actualmente tu **Disponible Real** libre es de **${CurrencyUtils.formatCOP(disponibleReal)}** tras reservar **${CurrencyUtils.formatCOP(comprometidoFijos)}** para tus fijos obligatorios.
                
                Has ingresado **${CurrencyUtils.formatCOP(totalIngresos)}** y gastado **${CurrencyUtils.formatCOP(totalGastos)}**.
                
                ¿Quieres analizar tu score de salud financiera, tus gastos por categoría o tus metas de ahorro?
                """.trimIndent()
            }
        }
    }
}
