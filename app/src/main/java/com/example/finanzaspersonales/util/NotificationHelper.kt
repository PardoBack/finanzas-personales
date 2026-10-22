package com.example.finanzaspersonales.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
    const val CHANNEL_NAME = "Notificaciones PardoFinanzas"
    const val CHANNEL_DESC = "Alertas de gastos fijos, balance y confirmación de registros"

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
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

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        init(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permiso no otorgado
        }
    }

    fun sendTestNotification(context: Context) {
        showNotification(
            context = context,
            title = "🔔 PardoFinanzas Alertas Activas",
            message = "¡Tus notificaciones financieras están funcionando al 100%! Recibirás avisos de vencimientos de quincena y registros.",
            notificationId = 1001
        )
    }

    fun notifyGastoFijoPendiente(context: Context, concepto: String, valor: Long, dia: Int) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        val valorStr = formatter.format(valor).replace("COP", "").trim()
        showNotification(
            context = context,
            title = "⏰ Vencimiento Próximo: $concepto",
            message = "Recuerda pagar $valorStr COP programado para el día $dia de este mes.",
            notificationId = 2000 + dia
        )
    }

    fun notifyMovimientoRegistrado(context: Context, tipo: String, descripcion: String, valor: Long) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        val valorStr = formatter.format(valor).replace("COP", "").trim()
        val emoji = if (tipo.contains("Ingreso", ignoreCase = true)) "💰" else "💸"
        showNotification(
            context = context,
            title = "$emoji $tipo Registrado: $valorStr COP",
            message = "$descripcion • Sincronizado en Google Sheets y en tu saldo disponible.",
            notificationId = (System.currentTimeMillis() % 10000).toInt()
        )
    }
}
