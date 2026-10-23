package com.example.finanzaspersonales

import com.example.finanzaspersonales.service.TransactionNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionNotificationParserTest {

    @Test
    fun testNequiPaymentWithoutCurrencySymbol() {
        val parsed = TransactionNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "Compra exitosa con Tarjeta Nequi",
            text = "Pagaste 198.000,00 en HIDENTAL PUNTO CLAVE"
        )

        assertNotNull("Nequi notification should be parsed", parsed)
        assertEquals(198000.0, parsed!!.monto, 0.01)
        assertEquals("Gasto", parsed.tipo)
        assertEquals("HIDENTAL PUNTO CLAVE", parsed.concepto)
        assertEquals("Salud", parsed.categoria)
        assertEquals("Odontología y Salud", parsed.subcategoria)
        assertEquals("Nequi", parsed.cuenta)
    }

    @Test
    fun testGoogleWalletWithNequiCard() {
        val parsed = TransactionNotificationParser.parse(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "HIDENTAL PUNTO CLAVE",
            text = "$ 198.000 con Tarjeta Nequi Mama"
        )

        assertNotNull("Google Wallet notification should be parsed", parsed)
        assertEquals(198000.0, parsed!!.monto, 0.01)
        assertEquals("Gasto", parsed.tipo)
        assertEquals("HIDENTAL PUNTO CLAVE", parsed.concepto)
        assertEquals("Salud", parsed.categoria)
        assertEquals("Odontología y Salud", parsed.subcategoria)
        assertEquals("Nequi", parsed.cuenta)
    }

    @Test
    fun testGoogleWalletGasolinera() {
        val parsed = TransactionNotificationParser.parse(
            packageName = "com.google.android.apps.walletnfcrel",
            title = "Billetera de Google",
            text = "Pagaste $ 35.000 en Primax Las Palmas con Tarjeta Virtual"
        )

        assertNotNull(parsed)
        assertEquals(35000.0, parsed!!.monto, 0.01)
        assertEquals("Gasto", parsed.tipo)
        assertTrue(parsed.concepto.contains("Primax"))
        assertEquals("Moto", parsed.categoria)
        assertEquals("Combustible", parsed.subcategoria)
    }

    @Test
    fun testNequiTransferencia() {
        val parsed = TransactionNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "Enviaste plata",
            text = "Enviaste 45.000,00 a Carlos Gomez"
        )

        assertNotNull(parsed)
        assertEquals(45000.0, parsed!!.monto, 0.01)
        assertEquals("Gasto", parsed.tipo)
        assertEquals("Transferencia a Carlos Gomez", parsed.concepto)
        assertEquals("Nequi", parsed.cuenta)
    }

    @Test
    fun testBancolombiaExito() {
        val parsed = TransactionNotificationParser.parse(
            packageName = "com.bancolombia.personas",
            title = "Bancolombia le informa",
            text = "Compra por $120.000 en EXITO POBLADO con tarjeta terminada en 4321"
        )

        assertNotNull(parsed)
        assertEquals(120000.0, parsed!!.monto, 0.01)
        assertEquals("Gasto", parsed.tipo)
        assertEquals("EXITO POBLADO", parsed.concepto)
        assertEquals("Alimentación", parsed.categoria)
        assertEquals("Mercado", parsed.subcategoria)
    }
}
