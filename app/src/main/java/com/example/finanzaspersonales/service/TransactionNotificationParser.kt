package com.example.finanzaspersonales.service

import java.util.Locale

data class ParsedFinancialTransaction(
    val monto: Double,
    val tipo: String, // "Gasto" or "Ingreso"
    val concepto: String,
    val categoria: String,
    val subcategoria: String,
    val cuenta: String, // "Nequi", "Google Wallet", "Bancolombia", "AyC Colanta", etc.
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
        "consignación", "te consignaron", "consignacion", "recepcion", "recepcion transferencia"
    )

    private val OUTGOING_KEYWORDS = listOf(
        "enviaste", "pagaste", "compra", "compraste", "pago de", "retiro",
        "retiraste", "aprobada", "transferencia a", "pasaste", "transferiste",
        "débito", "debito", "cargo", "pagó", "pago", "compra exitosa",
        "transferencia por"
    )

    // Regex 1: Montos con símbolo $ o COP explícito ($198.000, $420,000.00, $ 198.000, COP 50.000, etc.)
    private val AMOUNT_WITH_CURRENCY_REGEX = Regex(
        """(?:\$|COP)\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]{3,}(?:[.,][0-9]{1,2})?)|([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]{3,}(?:[.,][0-9]{1,2})?)\s*(?:COP|\$)""",
        RegexOption.IGNORE_CASE
    )

    // Regex 2: Montos directamente tras verbos financieros sin símbolo ("Pagaste 198.000,00", "Enviaste 50.000,00", "transferencia por $420,000.00", etc.)
    private val AMOUNT_AFTER_VERB_REGEX = Regex(
        """\b(?:pagaste|enviaste|compraste|recibiste|retiro|retiraste|te transfirieron|te enviaron|abono de|compra por|compra de|transferencia por|por|de|valor|cargo)\s+(?:de\s+)?(?:\$|COP\s*)?([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]{4,}(?:[.,][0-9]{1,2})?)\b""",
        RegexOption.IGNORE_CASE
    )

    // Regex 3: Cualquier número con formato de miles ("198.000,00", "420,000.00", "198.000")
    private val AMOUNT_THOUSANDS_REGEX = Regex(
        """\b([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{1,2})?)\b"""
    )

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
               combined.contains("compra exitosa") ||
               combined.contains("pagaste") ||
               combined.contains("enviaste") ||
               combined.contains("transacción") ||
               combined.contains("transaccion") ||
               combined.contains("visa") ||
               combined.contains("mastercard") ||
               combined.contains("ayc colanta") ||
               combined.contains("colanta le informa") ||
               combined.contains("ahorro y credito") ||
               title.contains("55200")
    }

    fun parse(packageName: String, title: String, text: String): ParsedFinancialTransaction? {
        val fullText = "$title $text".trim()
        val lowerText = fullText.lowercase(Locale.getDefault())

        // 1. Detectar Cuenta
        val cuenta = when {
            lowerText.contains("ayc colanta") || lowerText.contains("colanta") || title.contains("55200") -> "AyC Colanta"
            lowerText.contains("nequi") -> "Nequi"
            lowerText.contains("bancolombia") -> "Bancolombia"
            lowerText.contains("daviplata") -> "Daviplata"
            lowerText.contains("nu") || lowerText.contains("nubank") -> "Nu"
            packageName == "com.google.android.apps.walletnfcrel" || lowerText.contains("wallet") -> "Google Wallet"
            else -> KNOWN_PACKAGES[packageName] ?: "Tarjeta Virtual"
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

    fun extractAmount(text: String): Double? {
        // Intento 1: Con $ o COP explícito
        val m1 = AMOUNT_WITH_CURRENCY_REGEX.find(text)
        if (m1 != null) {
            val numStr = if (m1.groupValues[1].isNotEmpty()) m1.groupValues[1] else m1.groupValues[2]
            cleanAmount(numStr)?.let { return it }
        }

        // Intento 2: Tras verbo financiero ("transferencia por $420,000.00", "Pagaste 198.000,00", etc.)
        val m2 = AMOUNT_AFTER_VERB_REGEX.find(text)
        if (m2 != null) {
            val numStr = m2.groupValues[1]
            cleanAmount(numStr)?.let { return it }
        }

        // Intento 3: Cualquier número con separador de miles ("198.000,00" o "420,000.00")
        val m3 = AMOUNT_THOUSANDS_REGEX.find(text)
        if (m3 != null) {
            val numStr = m3.groupValues[1]
            cleanAmount(numStr)?.let { return it }
        }

        return null
    }

    private fun cleanAmount(raw: String): Double? {
        val trimmed = raw.trim()
        val sanitized = when {
            trimmed.contains(',') && trimmed.contains('.') -> {
                val lastComma = trimmed.lastIndexOf(',')
                val lastDot = trimmed.lastIndexOf('.')
                if (lastComma > lastDot) {
                    // Formato latino: 198.000,00
                    trimmed.replace(".", "").replace(",", ".")
                } else {
                    // Formato anglo/Colanta: 420,000.00
                    trimmed.replace(",", "")
                }
            }
            trimmed.contains(',') -> {
                val parts = trimmed.split(',')
                if (parts.last().length == 2) {
                    trimmed.replace(',', '.')
                } else {
                    trimmed.replace(",", "")
                }
            }
            trimmed.contains('.') -> {
                val parts = trimmed.split('.')
                if (parts.size == 2 && parts.last().length == 2) {
                    trimmed
                } else {
                    trimmed.replace(".", "")
                }
            }
            else -> trimmed
        }
        return sanitized.toDoubleOrNull()
    }

    fun extractConcept(fullText: String, title: String, text: String, cuenta: String): String {
        // 0. Transferencias AyC Colanta a Llave ("AyC Colanta le informa transferencia por $420,000.00 a llave @3167142613")
        val matchLlave = Regex("""a\s+llave\s+(@[0-9A-Za-z]+|[0-9]{10})""", RegexOption.IGNORE_CASE).find(text)
        if (matchLlave != null) {
            val llave = matchLlave.groupValues[1].trim()
            return if (llave.contains("3167142613")) {
                "Transferencia a Nequi (Llave @3167142613)"
            } else {
                "Transferencia a $llave"
            }
        }

        // 1. Patrón "en <Comercio>" (Nequi, Google Wallet, Bancolombia)
        // Ej: "Pagaste 198.000,00 en HIDENTAL PUNTO CLAVE" -> "HIDENTAL PUNTO CLAVE"
        val matchEn = Regex("""\ben\s+([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]{2,40}?)(?:\s+con|\s+por|\s*\.|\s*,\s*|$)""", RegexOption.IGNORE_CASE).find(text)
        if (matchEn != null) {
            val candidate = matchEn.groupValues[1].trim()
            if (candidate.length in 2..40 && !candidate.contains("$") && !candidate.contains("tarjeta", ignoreCase = true)) {
                return candidate
            }
        }

        // 2. Patrón "a <Destinatario>" (Transferencias Nequi/Daviplata)
        val matchA = Regex("""\ba\s+([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]{2,40}?)(?:\s+con|\s+por|\s+desde|\s*\.|\s*,\s*|$)""", RegexOption.IGNORE_CASE).find(text)
        if (matchA != null) {
            val candidate = matchA.groupValues[1].trim()
            if (candidate.length in 2..40 && !candidate.contains("$") && !candidate.contains("tarjeta", ignoreCase = true)) {
                return "Transferencia a $candidate"
            }
        }

        // 3. Patrón "de <Remitente>" (Recepción de dinero)
        val matchDe = Regex("""\bde\s+([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]{2,40}?)(?:\s+con|\s+por|\s*\.|\s*,\s*|$)""", RegexOption.IGNORE_CASE).find(text)
        if (matchDe != null) {
            val candidate = matchDe.groupValues[1].trim()
            if (candidate.length in 2..40 && !candidate.contains("$") && !candidate.contains("cuenta", ignoreCase = true)) {
                return "Recibido de $candidate"
            }
        }

        // 4. Si el TÍTULO es el Comercio (Estilo Google Wallet: Title="HIDENTAL PUNTO CLAVE", Text="$ 198.000 con Tarjeta Nequi")
        val genericWords = listOf("google", "wallet", "billetera", "nequi", "bancolombia", "daviplata", "notificaci", "compra", "transferencia", "aviso", "pago", "55200")
        val titleClean = title.trim()
        if (titleClean.length in 3..45 && !genericWords.any { titleClean.contains(it, ignoreCase = true) }) {
            return titleClean
        }

        // 5. Comercio antes del monto al inicio del texto (Ej: "HIDENTAL PUNTO CLAVE $ 198.000")
        val matchPrefix = Regex("""^([A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s\.\-]{3,35}?)\s+(?:\$|COP|por\s+\$|por\s+[0-9])""", RegexOption.IGNORE_CASE).find(text)
        if (matchPrefix != null) {
            val cand = matchPrefix.groupValues[1].trim()
            if (cand.length >= 3 && !genericWords.any { cand.contains(it, ignoreCase = true) }) {
                return cand
            }
        }

        // 6. Detección específica de gasolineras si aparecen en cualquier parte del texto
        val gasRegex = Regex("""\b(terpel|primax|biomax|zeuss|texaco|eds|gasolinera|estacion de servicio)\b[A-Za-z0-9ÁÉÍÓÚáéíóúÑñ\s]*""", RegexOption.IGNORE_CASE).find(fullText)
        if (gasRegex != null) {
            val cand = gasRegex.value.trim().take(35)
            if (cand.length >= 3) return cand
        }

        return "Pago con $cuenta"
    }

    fun categorize(concepto: String, fullLower: String, tipo: String): Pair<String, String> {
        if (tipo == "Ingreso") {
            return if (fullLower.contains("nomina") || fullLower.contains("sueldo") || fullLower.contains("colanta")) {
                "Salario" to "Nómina Colanta"
            } else {
                "Ingresos" to "Transferencia Recibida"
            }
        }

        val target = "$concepto $fullLower".lowercase(Locale.getDefault())

        return when {
            // Transferencias entre cuentas propias (AyC Colanta a Nequi)
            target.contains("transferencia a nequi") || target.contains("3167142613") ->
                "Transferencias" to "Recarga Nequi / Entre Cuentas"

            // Moto & Transporte (Gasolineras, Combustible, Repuestos, Parqueaderos)
            Regex("""\b(gasolina|tanqueo|tanqueada|terpel|primax|zeuss|biomax|texaco|eds|combustible|estacion de servicio|brio|petrobras)\b""").containsMatchIn(target) ->
                "Moto" to "Combustible"
            Regex("""\b(taller|repuesto|mecanico|aceite|llanta|frenos|soat|revision|tecnomecanica|lavado moto|auteco|yamaha|honda|bajaj|suzuki|akt)\b""").containsMatchIn(target) ->
                "Moto" to "Mantenimiento"
            Regex("""\b(parqueadero|parking|peaje|flypass|copiloto)\b""").containsMatchIn(target) ->
                "Moto" to "Parqueadero"

            // Salud, Odontología y Farmacia (HIDENTAL, Dentales, Clínicas, Droguerías)
            Regex("""\b(salud|dental|dent|hidental|odontolog|odontolog[ií]a|diente|dientes|ortodoncia|clinica|cl[ií]nica|hospital|optica|óptica|laboratorio|farmacia|drogueria|droguer[ií]a|pasteur|rebaja|farmatodo|medicamento|medico|médico|eps|sura|colsanitas|sanitas|coomeva|terapia)\b""").containsMatchIn(target) ->
                "Salud" to "Odontología y Salud"

            // Alimentación & Supermercados
            Regex("""\b(d1|exito|carulla|euro|vaquita|supermercado|mercado|olimpica|jumbo|viveres|tienda|fruver|carniceria|plaza de mercado|isimo|ara)\b""").containsMatchIn(target) ->
                "Alimentación" to "Mercado"
            Regex("""\b(restaurante|almuerzo|cena|desayuno|burger|hamburguesa|pizza|frisby|kfc|mcdonalds|corrientazo|domicilio|rappi|asadero|crepes|subway|tacos|sushi|comida rapida|pollo|sandwich|picada|panaderia)\b""").containsMatchIn(target) ->
                "Alimentación" to "Restaurante"

            // Servicios Públicos & Telecomunicaciones
            Regex("""\b(epm|energia|luz|electricidad|prepago agua|prepago energia)\b""").containsMatchIn(target) ->
                "Servicios" to "EPM Energía"
            Regex("""\b(agua|acueducto|alcantarillado)\b""").containsMatchIn(target) ->
                "Servicios" to "Agua Domiciliaria"
            Regex("""\b(gas|gas natural|bombona)\b""").containsMatchIn(target) ->
                "Servicios" to "Gas Domiciliario"
            Regex("""\b(tigo|claro|movistar|wom|etb|internet|wifi|datos|celular|plan movil)\b""").containsMatchIn(target) ->
                "Servicios" to "Internet y Telefonía"

            // Gastos Hormiga & Antojos
            Regex("""\b(tinto|cafecito|cafe|pan|buñuelo|empanada|helado|dulce|chocolatina|mecato|snack|golosina|starbucks|tostao|juan valdez)\b""").containsMatchIn(target) ->
                "Antojos" to "Gasto hormiga"

            // Suscripciones y Entretenimiento
            Regex("""\b(spotify|netflix|disney|prime|youtube|cine|cinecolombia|playstation|steam|hbo|max|apple|paramount|twitch)\b""").containsMatchIn(target) ->
                "Entretenimiento" to "Suscripciones"
            Regex("""\b(bar|discoteca|cerveza|licor|licorera|fiesta|concierto|boleta|teatro|billar)\b""").containsMatchIn(target) ->
                "Entretenimiento" to "Ocio"

            // Vivienda & Hogar
            Regex("""\b(arriendo|alquiler|canon|administracion|inmobiliaria)\b""").containsMatchIn(target) ->
                "Vivienda" to "Arriendo"
            Regex("""\b(homecenter|sodimac|ferreteria|muebles|decoracion|pintura|easy|ikea)\b""").containsMatchIn(target) ->
                "Vivienda" to "Hogar y Reparaciones"

            // Compras y Vestuario
            Regex("""\b(falabella|zara|koaj|mattelsa|ropa|zapatos|tenis|amazon|mercado libre|temu|aliexpress|shein|pull&bear|bershka|arturo calle)\b""").containsMatchIn(target) ->
                "Compras" to "Compras generales"

            // Transporte Urbano
            Regex("""\b(uber|didi|cabify|indriver|taxi|metro|metroplus|civica|tarjeta civica|pasaje)\b""").containsMatchIn(target) ->
                "Transporte" to "Transporte Urbano"

            else -> "Alimentación" to "General"
        }
    }
}
