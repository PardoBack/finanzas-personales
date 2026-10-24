package com.example.finanzaspersonales.service

import android.content.Context
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

        @Volatile
        var instance: TransactionNotificationListenerService? = null

        // Deduplication cache: key -> timestamp (ms)
        private val processedCache = ConcurrentHashMap<String, Long>()
        private const val DEDUPLICATION_WINDOW_MS = 60_000L // 60 segundos

        /**
         * Permite forzar un barrido del historial de notificaciones activas desde cualquier parte de la app
         */
        fun forceScanActiveNotifications(context: Context): Int {
            val listener = instance ?: return 0
            return listener.scanActiveNotifications()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "TransactionNotificationListenerService iniciado y activo")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "onListenerConnected: Servicio conectado al SO. Iniciando barrido de notificaciones activas...")
        scope.launch {
            scanActiveNotifications()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        job.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processNotification(sbn)
    }

    /**
     * Barre todas las notificaciones actualmente visibles en la barra de estado de Android
     * para recuperar movimientos de Nequi o Google Wallet que hayan llegado mientras la app estaba cerrada.
     */
    fun scanActiveNotifications(): Int {
        var count = 0
        try {
            val activeNotifs = activeNotifications ?: return 0
            Log.d(TAG, "Escaneando ${activeNotifs.size} notificaciones activas en el sistema...")
            for (sbn in activeNotifs) {
                if (processNotification(sbn)) {
                    count++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error escaneando activeNotifications", e)
        }
        return count
    }

    /**
     * Procesa una notificación individual. Retorna true si fue un pago financiero nuevo registrado.
     */
    private fun processNotification(sbn: StatusBarNotification): Boolean {
        // Ignorar notificaciones en curso (música, llamadas, etc.)
        if (sbn.isOngoing) return false
        val packageName = sbn.packageName ?: return false

        // Evitar procesar nuestras propias notificaciones
        if (packageName == applicationContext.packageName) return false

        val extras = sbn.notification?.extras ?: return false
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val contentText = if (bigText.isNotBlank()) bigText else text

        if (!TransactionNotificationParser.isFinancialNotification(packageName, title, contentText)) {
            return false
        }

        val parsed = TransactionNotificationParser.parse(packageName, title, contentText)
        if (parsed == null) {
            Log.w(TAG, "Notificación financiera pero no se pudo parsear: '$title' - '$contentText'")
            return false
        }

        val normConcept = parsed.concepto.trim().lowercase(Locale.getDefault())
        val valorLong = parsed.monto.toLong()
        val nowMs = System.currentTimeMillis()

        // Control de duplicados inteligente:
        // Evita duplicar cuando Google Wallet y Nequi emiten alerta casi simultánea para el mismo pago
        val isDuplicate = processedCache.entries.any { (key, timestamp) ->
            if (nowMs - timestamp > DEDUPLICATION_WINDOW_MS) return@any false
            val parts = key.split("|")
            if (parts.size >= 3) {
                val cachedAmount = parts[0].toLongOrNull() ?: 0L
                val cachedConcept = parts[1]
                val cachedTipo = parts[2]

                cachedAmount == valorLong &&
                cachedTipo == parsed.tipo &&
                (cachedConcept == normConcept ||
                 cachedConcept.contains(normConcept) ||
                 normConcept.contains(cachedConcept) ||
                 normConcept.take(7) == cachedConcept.take(7))
            } else false
        }

        if (isDuplicate) {
            Log.d(TAG, "Notificación duplicada omitida: $valorLong COP | $normConcept")
            return false
        }

        val deduplicationKey = "$valorLong|$normConcept|${parsed.tipo}"
        processedCache[deduplicationKey] = nowMs

        // Limpieza periódica de caché
        if (processedCache.size > 50) {
            val cutoff = nowMs - DEDUPLICATION_WINDOW_MS
            processedCache.entries.removeIf { it.value < cutoff }
        }

        Log.i(TAG, "✅ Transacción Verificada: ${parsed.tipo} $$valorLong COP en '${parsed.concepto}' [${parsed.categoria} > ${parsed.subcategoria}] (${parsed.cuenta})")

        scope.launch {
            try {
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
        return true
    }
}
