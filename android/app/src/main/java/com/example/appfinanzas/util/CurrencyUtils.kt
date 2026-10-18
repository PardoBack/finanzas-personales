package com.example.appfinanzas.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
    }
    private val decimalFormat = DecimalFormat("#,###", symbols)

    fun formatCOP(amount: Double): String {
        return "$ ${decimalFormat.format(amount)}"
    }

    fun formatCOP(amount: Int): String {
        return formatCOP(amount.toDouble())
    }

    /**
     * Parses a string like "$ 150.000" or "150000" into a clean integer.
     */
    fun parseAmount(text: String): Int {
        val cleanString = text.replace(Regex("[^\\d]"), "")
        return cleanString.toIntOrNull() ?: 0
    }
}
