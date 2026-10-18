package com.example.appfinanzas.data.repository

import com.example.appfinanzas.data.UserPreferencesRepository
import com.example.appfinanzas.data.remote.GeminiContent
import com.example.appfinanzas.data.remote.GeminiPart
import com.example.appfinanzas.data.remote.GeminiRequest
import com.example.appfinanzas.data.remote.GeminiResponse
import com.example.appfinanzas.util.CurrencyUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class FinancialContext(
    val userName: String = "Pro",
    val totalBalance: Double = 0.0,
    val disponibleReal: Double = 0.0,
    val comprometidoFijos: Double = 0.0,
    val totalIngresos: Double = 0.0,
    val totalGastos: Double = 0.0,
    val fijosPendientesCount: Int = 0,
    val totalGastosHormiga: Double = 0.0,
    val topCategory: String? = null,
    val topCategoryAmount: Double = 0.0
)

interface GeminiRepository {
    suspend fun generateFinancialAdvice(
        userPrompt: String,
        context: FinancialContext
    ): Result<String>
}

@Singleton
class GeminiRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val userPreferencesRepository: UserPreferencesRepository
) : GeminiRepository {

    override suspend fun generateFinancialAdvice(
        userPrompt: String,
        context: FinancialContext
    ): Result<String> = runCatching {
        val apiKey = userPreferencesRepository.geminiApiKey.first()

        if (apiKey.isBlank()) {
            throw IllegalStateException("NO_API_KEY")
        }

        val promptText = """
            Eres 'IA Pardo Assistant', un asesor financiero inteligente altamente empático y preciso para la app Finanzas Pro / PardoFinanzas V25.
            
            Información financiera real del usuario en tiempo real:
            - Nombre del usuario: ${context.userName}
            - Balance Total en cuenta: ${CurrencyUtils.formatCOP(context.totalBalance)}
            - Disponible Real Líquido: ${CurrencyUtils.formatCOP(context.disponibleReal)}
            - Comprometido en Gastos Fijos Pendientes: ${CurrencyUtils.formatCOP(context.comprometidoFijos)} (${context.fijosPendientesCount} compromisos pendientes)
            - Total Ingresos del mes: ${CurrencyUtils.formatCOP(context.totalIngresos)}
            - Total Gastos del mes: ${CurrencyUtils.formatCOP(context.totalGastos)}
            - Total Gastos Hormiga acumulados: ${CurrencyUtils.formatCOP(context.totalGastosHormiga)}
            ${if (context.topCategory != null) "- Mayor categoría de gasto: ${context.topCategory} (${CurrencyUtils.formatCOP(context.topCategoryAmount)})" else ""}
            
            Instrucciones para responder:
            1. Responde siempre en español, de forma clara, concisa y estructurada usando viñetas (*, •) y negritas.
            2. Utiliza los datos cuantitativos del contexto financiero real provisto para dar respuestas exactas y personalizadas.
            3. Brinda consejos prácticos enfocados en proteger su Disponible Real y reducir gastos hormiga.
            
            Pregunta o instrucción del usuario:
            "${userPrompt}"
        """.trimIndent()

        val requestBodyData = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = promptText)
                    )
                )
            )
        )

        val jsonString = json.encodeToString(GeminiRequest.serializer(), requestBodyData)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody.isNullOrBlank()) {
            throw Exception("HTTP ${response.code}: ${responseBody ?: "Respuesta vacía de Google Gemini"}")
        }

        val geminiResponse = json.decodeFromString(GeminiResponse.serializer(), responseBody)

        val generatedText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: geminiResponse.error?.message
            ?: throw Exception("No se pudo obtener texto generado por Gemini.")

        generatedText
    }
}
