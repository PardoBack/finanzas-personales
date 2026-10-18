package com.example.appfinanzas.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.appfinanzas.data.remote.GastoFijo
import com.example.appfinanzas.data.remote.Movimiento
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialReportExporter {

    fun generateCsvReport(
        movimientos: List<Movimiento>,
        gastosFijos: List<GastoFijo>,
        totalBalance: Double,
        totalIngresos: Double,
        totalGastos: Double
    ): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("=== REPORTE FINANCIERO - FINANZAS PRO V25 ===\n")
        sb.append("Fecha de Generación: $currentDate\n")
        sb.append("Total Ingresos: ${CurrencyUtils.formatCOP(totalIngresos)}\n")
        sb.append("Total Gastos: ${CurrencyUtils.formatCOP(totalGastos)}\n")
        sb.append("Balance Neto: ${CurrencyUtils.formatCOP(totalBalance)}\n")
        sb.append("=============================================\n\n")

        sb.append("DETALLE DE MOVIMIENTOS\n")
        sb.append("Fecha,Concepto,Categoría,Cuenta,Tipo,Monto (COP),Observaciones\n")

        for (m in movimientos) {
            val fechaEscaped = escapeCsv(m.fecha)
            val conceptoEscaped = escapeCsv(m.concepto)
            val catEscaped = escapeCsv(m.categoria)
            val cuentaEscaped = escapeCsv(m.cuenta)
            val tipoEscaped = escapeCsv(m.tipo)
            val montoFormatted = m.monto
            val obsEscaped = escapeCsv(m.observaciones ?: "")

            sb.append("$fechaEscaped,$conceptoEscaped,$catEscaped,$cuentaEscaped,$tipoEscaped,$montoFormatted,$obsEscaped\n")
        }

        if (gastosFijos.isNotEmpty()) {
            sb.append("\n\nDETALLE DE GASTOS FIJOS\n")
            sb.append("ID,Concepto,Día de Pago,Monto (COP)\n")
            for (f in gastosFijos) {
                sb.append("${f.id},${escapeCsv(f.concepto)},Día ${f.diaPago},${f.monto}\n")
            }
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun shareReport(context: Context, reportText: String, filename: String = "Reporte_Finanzas_Pro.csv") {
        try {
            val cacheDir = File(context.cacheDir, "reports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val file = File(cacheDir, filename)
            file.writeText(reportText, Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Financiero - Finanzas Pro")
                putExtra(Intent.EXTRA_TEXT, "Adjunto reporte financiero generado por Finanzas Pro.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Exportar / Compartir Reporte Financiero")
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback plain text share if file sharing fails
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Financiero - Finanzas Pro")
                putExtra(Intent.EXTRA_TEXT, reportText)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Reporte Financiero"))
        }
    }
}
