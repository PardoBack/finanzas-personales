package com.example.finanzaspersonales.service

import java.util.Locale

data class ParsedFinancialTransaction(
    val monto: Double,
    val tipo: String, // "Gasto" or "Ingreso"
    val concepto: String,
    val categoria: String,
    val subcategoria: String,
    val cuenta: String, // "Nequi", "Google Wallet", "Bancolombia", etc.
    val rawText: String
)

object TransactionNotificationParser {

    private val KNOWN_PACKAGES = mapOf(
        "com.google.android.apps.walletnfcrel" to "Google Wallet",
        "com.google.android.gms" to "Google Wallet",
        "com.nequi.MobileApp" to "Nequi",
        "com.bancolombia.personas" to "Bancolombia",
        "co.com.bancolombia.alalamano" to "A la mano",
        "com.davivienda.daviplata" to "Daviplata",
        "com.davivienda.daviviendamovil" to "Davivienda",
        "co.nu.app" to "Nu",
        "co.com.rappipay.wallet" to "RappiPay",
        "com.lulobank.mobile" to "Lulo Bank",
        "co.com.bancobogota.dale" to "Dale"
    )

    private val INCOMING_KEYWORDS = listOf(
        "recibiste", "te transfirieron", "te enviaron", "recepción", "abono",
        "consignación", "te consignaron", "consignacion", "recepcion"
    )

    private val OUTGOING_KEYWORDS = listOf(
        "enviaste", "pagaste", "compra", "compraste", "pago de", "retiro",
        "retiraste", "aprobada", "transferencia a", "pasaste", "transferiste",
        "débito", "debito", "cargo", "pagó", "pago"
    )

    // Regex para montos: "$ 50.000", "$50.000,00", "$12000", "50.000 COP", "COP 50.000"
    private val AMOUNT_REGEX = Regex("""(?:\$|COP)\s*([0-9]{1,3}(?:\.[0-9]{3})+(?:,[0-9]{1,2})?|[0-9]{4,}(?:,[0-9]{1,2})?|[0-9]{1,3}(?:,[0-9]{3})+(?:\.[0-9]{1,2})?)|([0-9]{1,3}(?:\.[0-9]{3})+)\s*(?:COP|\$)""")

    fun isFinancialNotification(packageName: String, title: String, text: String): Boolean {
        if (KNOWN_PACKAGES.containsKey(packageName)) return true
        val combined = "$title $text".lowercase(Locale.getDefault())
        return combined.contains("nequi") ||
               combined.contains("google pay") ||
               combined.contains("google wallet") ||
               combined.contains("wallet") ||
               combined.contains("bancolombia") ||
               combined.contains("daviplata") ||
               combined.contains("tarjeta") ||
               combined.contains("pago con tarjeta") ||
               combined.contains("compra aprobada") ||
               combined.contains("visa") ||
               combined.contains("mastercard")
    }

    fun parse(packageName: String, title: String, text: String): ParsedFinancialTransaction? {
        val fullText = "$title $text".trim()
        val lowerText = fullText.lowercase(Locale.getDefault())

        // 1. Detectar Cuenta
        val cuenta = KNOWN_PACKAGES[packageName] ?: when {
            lowerText.contains("google pay") || lowerText.contains("wallet") -> "Google Wallet"
            lowerText.contains("nequi") -> "Nequi"
            lowerText.contains("bancolombia") -> "Bancolombia"
            lowerText.contains("daviplata") -> "Daviplata"
            lowerText.contains("nu") -> "Nu"
            else -> "Tarjeta Virtual"
        }

        // 2. Extraer Monto
        val amount = extractAmount(fullText) ?: return null
        if (amount <= 0.0) return null

        // 3. Detectar Tipo (Gasto vs Ingreso)
        val isIncome = INCOMING_KEYWORDS.any { lowerText.contains(it) }
        val isExpense = OUTGOING_KEYWORDS.any { lowerText.contains(it) }

        val tipo = when {
            isIncome && !isExpense -> "Ingreso"
            else -> "Gasto"
        }

        // 4. Extraer Comercio / Concepto
        val concepto = extractConcept(fullText, title, text, cuenta)

        // 5. Categorización Inteligente
        val (categoria, subcategoria) = categorize(concepto, lowerText, tipo)

        return ParsedFinancialTransaction(
            monto = amount,
            tipo = tipo,
            concepto = concepto,
            categoria = categoria,
            subcategoria = subcategoria,
            cuenta = cuenta,
            rawText = fullText
        )
    }

    private fun extractAmount(text: String): Double? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        val rawNum = (if (match.groupValues[1].isNotEmpty()) match.groupValues[1] else match.groupValues[2])
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return rawNum.toDoubleOrNull()
    }

    private fun extractConcept(fullText: String, title: String, text: String, cuenta: String): String {
        // "Pagaste $ 50.000 en Terpel San Diego" -> "Terpel San Diego"
        // "Compra en Primax por $ 30.000" -> "Primax"
        val matchEn = Regex("""\ben\s+([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]+?)(?:\s+con|\s+por|\s*\.|$)""", RegexOption.IGNORE_CASE).find(text)
        if (matchEn != null) {
            val candidate = matchEn.groupValues[1].trim()
            if (candidate.length in 2..45 && !candidate.contains("$")) {
                return candidate
            }
        }

        val matchA = Regex("""\ba\s+([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]+?)(?:\s+por|\s*\.|$)""", RegexOption.IGNORE_CASE).find(text)
        if (matchA != null) {
            val candidate = matchA.groupValues[1].trim()
            if (candidate.length in 2..45 && !candidate.contains("$")) {
                return "Transferencia a $candidate"
            }
        }

        val matchDe = Regex("""\bde\s+([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]+?)(?:\s+por|\s*\.|$)""", RegexOption.IGNORE_CASE).find(text)
        if (matchDe != null) {
            val candidate = matchDe.groupValues[1].trim()
            if (candidate.length in 2..45 && !candidate.contains("$")) {
                return "Recibido de $candidate"
            }
        }

        // Búsqueda específica de gasolineras si aparecen en el texto
        val gasRegex = Regex("""\b(terpel|primax|biomax|zeuss|texaco|eds|gasolinera|estacion de servicio)\b[A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s]*""", RegexOption.IGNORE_CASE).find(fullText)
        if (gasRegex != null) {
            val cand = gasRegex.value.trim().take(35)
            if (cand.length >= 3) return cand
        }

        if (title.isNotBlank() && !title.contains(cuenta, ignoreCase = true) && title.length in 3..35) {
            return title.trim()
        }

        return "Pago con $cuenta"
    }

    private fun categorize(concepto: String, fullLower: String, tipo: String): Pair<String, String> {
        if (tipo == "Ingreso") {
            return if (fullLower.contains("nomina") || fullLower.contains("sueldo") || fullLower.contains("colanta")) {
                "Salario" to "Nómina Colanta"
            } else {
                "Ingresos" to "Transferencia Recibida"
            }
        }

        val target = "$concepto $fullLower".lowercase(Locale.getDefault())

        return when {
            // Moto & Transporte (Gasolineras, Combustible, Repuestos, Parqueaderos)
            Regex("""\b(gasolina|tanqueo|tanqueada|terpel|primax|zeuss|biomax|texaco|eds|combustible|estacion de servicio)\b""").containsMatchIn(target) ->
                "Moto" to "Combustible"
            Regex("""\b(taller|repuesto|mecanico|aceite|llanta|frenos|soat|revision|tecnomecanica|lavado moto)\b""").containsMatchIn(target) ->
                "Moto" to "Mantenimiento"
            Regex("""\b(parqueadero|parking|peaje)\b""").containsMatchIn(target) ->
                "Moto" to "Parqueadero"

            // Alimentación & Supermercados
            Regex("""\b(d1|exito|carulla|euro|vaquita|supermercado|mercado|olimpica|jumbo|viveres|tienda)\b""").containsMatchIn(target) ->
                "Alimentación" to "Mercado"
            Regex("""\b(restaurante|almuerzo|cena|desayuno|burger|pizza|frisby|kfc|mcdonalds|corrientazo|domicilio|rappi|asadero)\b""").containsMatchIn(target) ->
                "Alimentación" to "Restaurante"

            // Servicios Públicos & Telecomunicaciones
            Regex("""\b(epm|energia|luz|electricidad)\b""").containsMatchIn(target) ->
                "Servicios" to "EPM Energía"
            Regex("""\b(agua|acueducto|alcantarillado)\b""").containsMatchIn(target) ->
                "Servicios" to "Agua Domiciliaria"
            Regex("""\b(gas|gas natural|bombona)\b""").containsMatchIn(target) ->
                "Servicios" to "Gas Domiciliario"
            Regex("""\b(tigo|claro|movistar|wom|internet|wifi|datos|celular)\b""").containsMatchIn(target) ->
                "Servicios" to "Internet y Telefonía"

            // Gastos Hormiga & Antojos
            Regex("""\b(tinto|cafecito|cafe|pan|buñuelo|empanada|helado|dulce|chocolatina|mecato|snack)\b""").containsMatchIn(target) ->
                "Antojos" to "Gasto hormiga"

            // Salud & Farmacia
            Regex("""\b(farmacia|drogueria|pasteur|rebaja|farmatodo|medicamento|medico|eps|sura)\b""").containsMatchIn(target) ->
                "Salud" to "Medicamentos y Salud"

            // Suscripciones y Entretenimiento
            Regex("""\b(spotify|netflix|disney|prime|youtube|cine|cinecolombia|playstation|steam)\b""").containsMatchIn(target) ->
                "Entretenimiento" to "Suscripciones"

            // Vivienda
            Regex("""\b(arriendo|alquiler|canon|administracion)\b""").containsMatchIn(target) ->
                "Vivienda" to "Arriendo"

            // Compras generales
            Regex("""\b(falabella|zara|koaj|mattelsa|ropa|zapatos|tenis|amazon|mercado libre|temu)\b""").containsMatchIn(target) ->
                "Compras" to "Compras generales"

            else -> "Alimentación" to "General"
        }
    }
}
