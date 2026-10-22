package com.example.finanzaspersonales.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.finanzaspersonales.MainActivity
import com.example.finanzaspersonales.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "pardo_finanzas_channel"
    const val CHANNEL_NAME = "Alertas PardoFinanzas"
    const val CHANNEL_DESC = "Facturas procesadas, pagos detectados y vencimientos"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 220, 120, 220)
                enableLights(true)
                lightColor = Color.CYAN
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatCop(valor: Long): String {
        return NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }.format(valor).replace("COP", "").trim()
    }

    /**
     * Muestra una notificación con diseño BigTextStyle ultra limpio y acción de apertura
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        subText: String = "PardoFinanzas en Vivo",
        bigText: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ): Boolean {
        init(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val fullBigText = bigText ?: message

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setSubText(subText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(subText)
                    .bigText(fullBigText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(Color.parseColor("#06B6D4"))
            .setColorized(true)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 220, 120, 220))
            .setContentIntent(getPendingIntent(context))

        return try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Notificación cuando se procesa una factura DIAN o archivo .ZIP desde Gmail
     */
    fun notifyFacturaProcesada(
        context: Context,
        comercio: String,
        monto: Long? = null,
        cantidad: Int = 1
    ) {
        val montoStr = if (monto != null && monto > 0L) "$${formatCop(monto)} COP" else "Importe extractado"
        val title = if (cantidad > 1) "🧾 $cantidad Facturas .ZIP Procesadas" else "🧾 Factura Procesada: $comercio"
        val shortMsg = if (cantidad > 1) {
            "Se descomprimieron y registraron $cantidad facturas nuevas en Google Sheets."
        } else {
            "Factura de $comercio por $montoStr procesada y guardada."
        }

        val bigMsg = """
            🧾 Factura Electrónica (.ZIP) Normalizada
            • Comercio / Emisor: $comercio
            • Valor Registrado: $montoStr
            • Hoja Sheets: Fila 5 de MOVIMIENTOS
            • Bandeja de Entrada: Queda sin marcar como visto y se moverá a FINANZAS apenas la leas.
        """.trimIndent()

        showNotification(
            context = context,
            title = title,
            message = shortMsg,
            subText = "Facturación DIAN / Gmail",
            bigText = bigMsg,
            notificationId = 3001
        )
    }

    /**
     * Notificación cuando se detecta o registra un pago/gasto
     */
    fun notifyPagoDetectado(
        context: Context,
        comercio: String,
        valor: Long,
        cuenta: String
    ) {
        val valorStr = formatCop(valor)
        val title = "💳 Pago Detectado: $comercio"
        val shortMsg = "Salida de $$valorStr COP debitada de $cuenta."
        val bigMsg = """
            💳 Movimiento Confirmado en Tiempo Real
            • Comercio: $comercio
            • Monto: $$valorStr COP
            • Medio / Cuenta: $cuenta
            • Estado: Sincronizado en tu Google Sheets y descontado del disponible.
        """.trimIndent()

        showNotification(
            context = context,
            title = title,
            message = shortMsg,
            subText = "Billetera & Cuentas",
            bigText = bigMsg,
            notificationId = (System.currentTimeMillis() % 10000).toInt()
        )
    }

    /**
     * Notificación cuando se registra un movimiento manual (Gasto o Ingreso)
     */
    fun notifyMovimientoRegistrado(
        context: Context,
        tipo: String,
        descripcion: String,
        valor: Long,
        cuenta: String
    ) {
        val valorStr = formatCop(valor)
        val esIngreso = tipo.contains("Ingreso", ignoreCase = true)
        val emoji = if (esIngreso) "💰" else "💸"
        val title = "$emoji $tipo: $$valorStr COP"
        val shortMsg = "$descripcion • Cuenta: $cuenta"
        val bigMsg = """
            $emoji $tipo Registrado con Éxito
            • Concepto: $descripcion
            • Valor: $$valorStr COP
            • Cuenta: $cuenta
            • Nube: Sincronizado en vivo en MOVIMIENTOS de Google Sheets.
        """.trimIndent()

        showNotification(
            context = context,
            title = title,
            message = shortMsg,
            subText = "Control Financiero",
            bigText = bigMsg,
            notificationId = (System.currentTimeMillis() % 10000).toInt()
        )
    }

    /**
     * Notificación de gasto fijo pagado
     */
    fun notifyGastoFijoPagado(context: Context, concepto: String, valor: Long) {
        val valorStr = formatCop(valor)
        val title = "✅ Compromiso Fijo Pagado: $concepto"
        val msg = "Abono de $$valorStr COP registrado en CONTROL FIJOS de Google Sheets."
        showNotification(
            context = context,
            title = title,
            message = msg,
            subText = "Gastos Fijos",
            notificationId = 4001
        )
    }

    /**
     * Notificación de gasto fijo revertido
     */
    fun notifyGastoFijoRevertido(context: Context, concepto: String, valor: Long) {
        val valorStr = formatCop(valor)
        val title = "↺ Pago Revertido: $concepto"
        val msg = "El pago de $$valorStr COP fue restaurado como pendiente en Google Sheets."
        showNotification(
            context = context,
            title = title,
            message = msg,
            subText = "Gastos Fijos",
            notificationId = 4002
        )
    }

    /**
     * Prueba instantánea de notificación de factura Gmail (.ZIP)
     */
    fun sendTestFacturaNotification(context: Context): Boolean {
        return showNotification(
            context = context,
            title = "🧾 Factura Procesada: EPM Medellín (.ZIP)",
            message = "Factura de servicios públicos por $197.000 COP procesada con éxito.",
            subText = "Motor DIAN / Gmail",
            bigText = """
                🧾 Factura Electrónica Normalizada
                • Comercio: Empresas Públicas de Medellín (EPM)
                • Importe: $197.000 COP
                • Adjunto .ZIP: Extraído XML UBL 2.1 y PDF oficial
                • Regla: Registrada en fila 5 de MOVIMIENTOS y etiquetada en FINANZAS.
            """.trimIndent(),
            notificationId = 5001
        )
    }

    /**
     * Prueba instantánea de notificación de pago detectado (Nequi / Datáfono)
     */
    fun sendTestPagoNotification(context: Context): Boolean {
        return showNotification(
            context = context,
            title = "💳 Pago Detectado: Éxito Colombia",
            message = "Pago de $68.500 COP realizado con Nequi Visa Débito.",
            subText = "Billetera & Cuentas",
            bigText = """
                💳 Transacción Detectada en Datáfono
                • Establecimiento: Éxito Colombia
                • Valor: $68.500 COP
                • Medio: Nequi Visa Débito
                • Estado: Balance actualizado y movimiento guardado.
            """.trimIndent(),
            notificationId = 5002
        )
    }

    fun sendTestNotification(context: Context): Boolean {
        return showNotification(
            context = context,
            title = "🔔 PardoFinanzas Alertas Activas",
            message = "¡Tus notificaciones financieras están funcionando al 100%! Recibirás avisos de facturas Gmail y registros.",
            subText = "Notificaciones Activas",
            notificationId = 1001
        )
    }
}
