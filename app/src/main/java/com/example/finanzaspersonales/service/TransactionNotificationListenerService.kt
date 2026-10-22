package com.example.finanzaspersonales.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.finanzaspersonales.data.AppDatabase
import com.example.finanzaspersonales.data.Transaction
import com.example.finanzaspersonales.network.RetrofitClient
import com.example.finanzaspersonales.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TransactionNotificationListenerService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "PardoNotifListener"

        // Deduplication cache: key -> timestamp (ms)
        private val processedCache = ConcurrentHashMap<String, Long>()
        private const val DEDUPLICATION_WINDOW_MS = 60_000L // 60 segundos
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "TransactionNotificationListenerService iniciado correctamente")
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

        if (!TransactionNotificationParser.isFinancialNotification(packageName, title, contentText)) {
            return
        }

        val parsed = TransactionNotificationParser.parse(packageName, title, contentText) ?: return

        // Control de duplicados: evita registrar dos veces el mismo pago si la app repite notificación
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

        Log.i(TAG, "Pago/Movimiento detectado: ${parsed.tipo} $${parsed.monto} en ${parsed.concepto} (${parsed.cuenta})")

        scope.launch {
            try {
                val valorLong = parsed.monto.toLong()
                val ahora = Date()
                val sdfId = SimpleDateFormat("HHmmss", Locale.US).format(ahora)
                val nuevoId = "M-AUTO-$sdfId"

                // 1. Guardar en Base de Datos Local Room
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.transactionDao().insertTransaction(
                        Transaction(
                            type = parsed.tipo,
                            concept = parsed.concepto,
                            category = parsed.categoria,
                            amount = parsed.monto
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo insertar en Room DB local", e)
                }

                // 2. Sincronizar directamente con Google Sheets (Fila 5)
                try {
                    val payload = mutableMapOf<String, Any?>(
                        "action" to "addMovement",
                        "token" to RetrofitClient.AUTH_TOKEN,
                        "id" to nuevoId,
                        "tipo" to parsed.tipo,
                        "valor" to valorLong,
                        "descripcion" to parsed.concepto,
                        "categoria" to parsed.categoria,
                        "subcategoria" to parsed.subcategoria,
                        "cuenta" to parsed.cuenta,
                        "metodo" to "Tarjeta Virtual / Google Wallet",
                        "origen" to "Detector Notificaciones Android"
                    )
                    val response = RetrofitClient.api.executePostAction(payload)
                    Log.i(TAG, "Sincronizado con Sheets: $response")
                } catch (e: Exception) {
                    Log.w(TAG, "Error enviando a Google Sheets Webhook", e)
                }

                // 3. Emitir notificación enriquecida confirmando la captura del pago
                NotificationHelper.notifyPagoDetectado(
                    context = applicationContext,
                    comercio = parsed.concepto,
                    valor = valorLong,
                    cuenta = parsed.cuenta
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error procesando pago detectado", e)
            }
        }
    }
}
