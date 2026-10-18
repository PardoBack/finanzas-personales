package com.example.appfinanzas

import com.example.appfinanzas.data.remote.GastoFijo
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.util.FinancialReportExporter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityLogicTest {

    private fun isSecurityActive(prefEnabled: Boolean, pinCode: String): Boolean {
        return prefEnabled && pinCode.isNotBlank()
    }

    private fun shouldAppBeLocked(prefEnabled: Boolean, pinCode: String, requestedLock: Boolean): Boolean {
        return isSecurityActive(prefEnabled, pinCode) && requestedLock
    }

    @Test
    fun testDefaultSecurityStateIsDisabled() {
        val prefEnabled = false
        val pinCode = ""

        assertFalse(isSecurityActive(prefEnabled, pinCode))
        assertFalse(shouldAppBeLocked(prefEnabled, pinCode, requestedLock = true))
        assertFalse(shouldAppBeLocked(prefEnabled, pinCode, requestedLock = false))
    }

    @Test
    fun testSecurityEnabledWithEmptyPinIsDisabled() {
        // Even if setting says true, if PIN is empty, security must be disabled
        val prefEnabled = true
        val pinCode = ""

        assertFalse(isSecurityActive(prefEnabled, pinCode))
        assertFalse(shouldAppBeLocked(prefEnabled, pinCode, requestedLock = true))
        assertFalse(shouldAppBeLocked(prefEnabled, pinCode, requestedLock = false))
    }

    @Test
    fun testSecurityActiveOnlyWithValidPin() {
        val prefEnabled = true
        val pinCode = "1234"

        assertTrue(isSecurityActive(prefEnabled, pinCode))
        assertTrue(shouldAppBeLocked(prefEnabled, pinCode, requestedLock = true))
        assertFalse(shouldAppBeLocked(prefEnabled, pinCode, requestedLock = false))
    }
}

class FinancialReportExporterTest {

    @Test
    fun testGenerateCsvReport() {
        val movimientos = listOf(
            Movimiento(id = 1, fecha = "2026-09-01", concepto = "Sueldo", monto = 2500000, tipo = "ingreso", categoria = "Sueldo", cuenta = "Bancolombia"),
            Movimiento(id = 2, fecha = "2026-09-02", concepto = "Supermercado", monto = 200000, tipo = "gasto", categoria = "Alimentación", cuenta = "Nequi")
        )
        val fijos = listOf(
            GastoFijo(id = 1, concepto = "Internet Fenix", monto = 120000.0, diaPago = 15)
        )

        val csv = FinancialReportExporter.generateCsvReport(
            movimientos = movimientos,
            gastosFijos = fijos,
            totalBalance = 2300000.0,
            totalIngresos = 2500000.0,
            totalGastos = 200000.0
        )

        assertTrue(csv.contains("REPORTE FINANCIERO - FINANZAS PRO V25"))
        assertTrue(csv.contains("Sueldo"))
        assertTrue(csv.contains("Supermercado"))
        assertTrue(csv.contains("Internet Fenix"))
        assertTrue(csv.contains("Total Ingresos: $ 2.500.000"))
    }
}
