package com.example.appfinanzas.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.appfinanzas.MainActivity
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.data.repository.FinanceRepository
import com.example.appfinanzas.util.CurrencyUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationListenerEntryPoint {
    fun financeRepository(): FinanceRepository
}

class TransactionNotificationListenerService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "TxNotifListener"
        private const val CHANNEL_ID = "pardo_auto_transactions"
        private const val CHANNEL_NAME = "Movimientos Detectados Automáticamente"

        // Deduplication cache: key -> timestamp (ms)
        private val processedCache = ConcurrentHashMap<String, Long>()
        private const val DEDUPLICATION_WINDOW_MS = 60_000L // 60 segundos
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "TransactionNotificationListenerService iniciado")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignorar notificaciones del sistema o en curso (llamadas, música, etc.)
        if (sbn.isOngoing) return
        val packageName = sbn.packageName ?: return

        // Evitar procesar nuestras propias notificaciones
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val contentText = if (bigText.isNotBlank()) bigText else text

        if (!TransactionNotificationParser.isFinancialApp(packageName, title, contentText)) {
            return
        }

        val parsed = TransactionNotificationParser.parse(packageName, title, contentText) ?: return

        // Control de duplicados: evita que notificaciones repetidas registren dos veces el mismo gasto
        val deduplicationKey = "${parsed.cuenta}_${parsed.monto}_${parsed.concepto}_${parsed.tipo}"
        val nowMs = System.currentTimeMillis()
        val lastSeen = processedCache[deduplicationKey] ?: 0L
        if (nowMs - lastSeen < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "Notificación duplicada ignorada: $deduplicationKey")
            return
        }
        processedCache[deduplicationKey] = nowMs

        // Limpiar caché viejo
        if (processedCache.size > 50) {
            val cutoff = nowMs - DEDUPLICATION_WINDOW_MS
            processedCache.entries.removeIf { it.value < cutoff }
        }

        Log.i(TAG, "Movimiento detectado: ${parsed.tipo} $${parsed.monto} en ${parsed.concepto} (${parsed.cuenta})")

        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    NotificationListenerEntryPoint::class.java
                )
                val repository = entryPoint.financeRepository()

                val now = Date()
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                val sdfMes = SimpleDateFormat("MMMM", Locale("es", "CO"))
                val sdfAno = SimpleDateFormat("yyyy", Locale.getDefault())

                val dateStr = sdfDate.format(now)
                val timeStr = sdfTime.format(now)
                val mesStr = sdfMes.format(now).lowercase(Locale.getDefault())
                val anoStr = sdfAno.format(now)

                val movimiento = Movimiento(
                    fecha = dateStr,
                    hora = timeStr,
                    tipo = if (parsed.tipo == "INGRESO") "ingreso" else "gasto",
                    categoria = parsed.categoria,
                    subcategoria = parsed.subcategoria,
                    descripcion = parsed.concepto,
                    valor = parsed.monto,
                    metodo = parsed.cuenta,
                    cuenta = parsed.cuenta,
                    mes = mesStr,
                    ano = anoStr,
                    origen = "Auto-detector Android",
                    referencia = "Auto: ${parsed.cuenta}"
                )

                // Guarda en base de datos local y sincroniza en segundo plano con Google Sheets
                repository.addMovimiento(movimiento)

                // Notificar al usuario que se detectó y registró automáticamente
                showConfirmationNotification(parsed)

            } catch (e: Exception) {
                Log.e(TAG, "Error guardando movimiento detectado", e)
            }
        }
    }

    private fun showConfirmationNotification(parsed: ParsedFinancialTransaction) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = CurrencyUtils.formatCOP(parsed.monto)
        val prefix = if (parsed.tipo == "INGRESO") "+ " else "- "
        val title = "⚡ Movimiento detectado: $prefix$formattedAmount"
        val message = "${parsed.concepto} (${parsed.cuenta}) registrado en ${parsed.categoria}."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando PardoFinanzas detecta pagos de Nequi, Google Wallet y Bancos"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}