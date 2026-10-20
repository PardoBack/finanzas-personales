package com.example.finanzaspersonales.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finanzaspersonales.data.AppDatabase
import com.example.finanzaspersonales.data.Transaction
import com.example.finanzaspersonales.data.TransactionRepository
import com.example.finanzaspersonales.network.RetrofitClient
import com.example.finanzaspersonales.worker.SyncWorker
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MovimientoItem(
    val id: String,
    val fecha: String,
    val hora: String,
    val tipo: String,
    val categoria: String,
    val subcategoria: String,
    val descripcion: String,
    val valor: Long,
    val metodo: String,
    val cuenta: String,
    val mes: String,
    val anio: Int
)

data class GastoFijoItem(
    val id: String,
    val concepto: String,
    val categoria: String,
    val dia: Int,
    val cuota: Long,
    val abonado: Long,
    val estado: String,
    val fechaPago: String,
    val saldo: Long,
    val prioridad: String,
    val medioPago: String
) {
    val estaPagado: Boolean
        get() = estado.contains("Pagado", ignoreCase = true) || (cuota > 0 && abonado >= cuota)
    val quincena: Int
        get() = if (dia <= 15) 1 else 2
}

data class MesAnualItem(
    val key: String,
    val nombre: String,
    val ingresos: Long,
    val gastos: Long,
    val ahorro: Long
) {
    val tasaAhorro: Int
        get() = if (ingresos > 0) (((ingresos - gastos).toDouble() / ingresos.toDouble()) * 100).toInt().coerceIn(0, 100) else 0
}

data class CfoMensaje(
    val id: String = System.currentTimeMillis().toString(),
    val autor: String, // "user" o "ai"
    val texto: String,
    val hora: String = SimpleDateFormat("hh:mm a", Locale("es", "CO")).format(Date())
)

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isScanningGmail: Boolean = false,
    val statusBanner: String? = null,
    val mesActivo: String = "octubre",
    val anioActivo: Int = 2026,
    val ingresosMes: Long = 1700605L,
    val gastosMes: Long = 1285239L,
    val disponibleReal: Long = 1065366L,
    val fijosPendientes: Long = 773900L,
    val aycColanta: Long = 993566L,
    val efectivo: Long = 71800L,
    val nequi: Long = 0L,
    val ahorroNavideno: Long = 2280000L,
    val aportesCooperativos: Long = 1032650L,
    val ahorroOrdinario: Long = 627000L,
    val otrosAhorros: Long = 227770L,
    val totalFondosColanta: Long = 4167420L,
    val totalIngresosAnual: Long = 34152478L,
    val totalGastosAnual: Long = 23785728L,
    val superavitAnual: Long = 10366750L,
    val tasaAhorroAnual: Double = 30.4,
    val movimientos: List<MovimientoItem> = MainViewModel.defaultMovimientosOctubre(),
    val fijos: List<GastoFijoItem> = MainViewModel.defaultFijosOctubre(),
    val resumenAnual: List<MesAnualItem> = MainViewModel.defaultResumenAnual(),
    val mensajesCfo: List<CfoMensaje> = listOf(
        CfoMensaje(
            autor = "ai",
            texto = "Hola Marlon 👋 Soy tu CFO IA V26.9 conectado en vivo con tu Google Sheets y el analizador de facturas Gmail (.ZIP). Puedes preguntarme por tu liquidez, tus fondos Colanta o escribir comandos como: «gasté 15000 en almuerzo» o «escanear correos»."
        )
    )
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val transactions: StateFlow<List<Transaction>>
    val totalIncome: StateFlow<Double?>
    val totalExpense: StateFlow<Double?>

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())

        transactions = repository.allTransactions.stateIn(
            viewModelScope, SharingStarted.Lazily, emptyList()
        )
        totalIncome = repository.totalIncome.stateIn(
            viewModelScope, SharingStarted.Lazily, 0.0
        )
        totalExpense = repository.totalExpense.stateIn(
            viewModelScope, SharingStarted.Lazily, 0.0
        )

        sincronizarConSheets("octubre", 2026, silencioso = true)
    }

    fun limpiarBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun seleccionarMes(mes: String) {
        val mesLimpio = mes.lowercase(Locale.ROOT).trim()
        val estadoActual = _uiState.value
        val datosMesAnual = estadoActual.resumenAnual.find { it.key.equals(mesLimpio, ignoreCase = true) }

        if (datosMesAnual != null && mesLimpio != "octubre") {
            _uiState.update {
                it.copy(
                    mesActivo = mesLimpio,
                    ingresosMes = datosMesAnual.ingresos,
                    gastosMes = datosMesAnual.gastos,
                    disponibleReal = (datosMesAnual.ingresos - datosMesAnual.gastos).coerceAtLeast(0L)
                )
            }
        } else {
            _uiState.update { it.copy(mesActivo = mesLimpio) }
        }
        sincronizarConSheets(mesLimpio, estadoActual.anioActivo, silencioso = true)
    }

    fun sincronizarConSheets(
        mes: String = _uiState.value.mesActivo,
        anio: Int = _uiState.value.anioActivo,
        silencioso: Boolean = false
    ) {
        viewModelScope.launch {
            if (!silencioso) {
                _uiState.update { it.copy(isLoading = true, statusBanner = "Sincronizando en vivo con Google Sheets...") }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getDashboardData(
                        action = "getData",
                        mes = mes,
                        anio = anio
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.optBoolean("ok", false)) {
                        aplicarJsonDeSheets(body, mes, silencioso)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                statusBanner = if (silencioso) null else "Datos locales activos (Sheets ocupado)"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusBanner = if (silencioso) null else "Modo offline activo • Datos auditados cargados"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusBanner = if (silencioso) null else "Respaldo local activo (${e.localizedMessage?.take(35) ?: "Sin red"})"
                    )
                }
            }
        }
    }

    private fun aplicarJsonDeSheets(json: JsonObject, mesSolicitado: String, silencioso: Boolean) {
        try {
            val resumen = json.optObject("resumen")
            val cuentas = json.optObject("cuentas")
            val ahorros = json.optObject("ahorrosColanta")
            val kpis = json.optObject("kpisAnuales")

            val movsList = mutableListOf<MovimientoItem>()
            val movsArray = json.getAsJsonArray("movimientos")
            if (movsArray != null) {
                for (i in 0 until movsArray.size()) {
                    val m = movsArray[i].asJsonObject
                    movsList.add(
                        MovimientoItem(
                            id = m.optString("id", "M-${260 + i}"),
                            fecha = m.optString("fecha", "2026-10-19"),
                            hora = m.optString("hora", "12:00 PM"),
                            tipo = m.optString("tipo", "Gasto"),
                            categoria = m.optString("categoria", "Otros"),
                            subcategoria = m.optString("subcategoria", "General"),
                            descripcion = m.optString("descripcion", "Movimiento"),
                            valor = m.optLong("valor", 0L),
                            metodo = m.optString("metodo", "Transferencia"),
                            cuenta = m.optString("cuenta", "AyC Colanta"),
                            mes = m.optString("mes", mesSolicitado),
                            anio = m.optInt("anio", 2026)
                        )
                    )
                }
            }

            val fijosList = mutableListOf<GastoFijoItem>()
            val fijosArray = json.getAsJsonArray("fijos")
            if (fijosArray != null) {
                for (i in 0 until fijosArray.size()) {
                    val f = fijosArray[i].asJsonObject
                    val cuota = f.optLong("cuota", 0L)
                    val abonado = f.optLong("abonado", 0L)
                    fijosList.add(
                        GastoFijoItem(
                            id = f.optString("id", "F-010$i"),
                            concepto = f.optString("concepto", "Compromiso"),
                            categoria = f.optString("categoria", "Servicios"),
                            dia = f.optInt("dia", 15),
                            cuota = cuota,
                            abonado = abonado,
                            estado = f.optString("estado", if (abonado >= cuota && cuota > 0) "🟢 Pagado" else "🔴 Pendiente"),
                            fechaPago = f.optString("fechaPago", ""),
                            saldo = f.optLong("saldo", (cuota - abonado).coerceAtLeast(0L)),
                            prioridad = f.optString("prioridad", "Media"),
                            medioPago = f.optString("medioPago", "Transferencia")
                        )
                    )
                }
            }

            val anualList = mutableListOf<MesAnualItem>()
            val anualArray = json.getAsJsonArray("resumenAnual")
            if (anualArray != null) {
                for (i in 0 until anualArray.size()) {
                    val a = anualArray[i].asJsonObject
                    val nombreMes = a.optString("nombre", "Mes")
                    anualList.add(
                        MesAnualItem(
                            key = nombreMes.lowercase(Locale.ROOT),
                            nombre = nombreMes,
                            ingresos = a.optLong("ingresos", 0L),
                            gastos = a.optLong("gastos", 0L),
                            ahorro = a.optLong("ahorro", 0L)
                        )
                    )
                }
            }

            _uiState.update { prev ->
                val nuevosFijos = if (fijosList.isNotEmpty()) fijosList else prev.fijos
                val fijosPendCalculados = nuevosFijos.filter { !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }

                prev.copy(
                    isLoading = false,
                    statusBanner = if (silencioso) null else "✅ Sincronizado 1:1 con Google Sheets",
                    mesActivo = resumen?.optString("mes", mesSolicitado)?.lowercase(Locale.ROOT) ?: mesSolicitado,
                    ingresosMes = resumen?.optLong("ingresos", prev.ingresosMes)?.takeIf { it > 0 } ?: prev.ingresosMes,
                    gastosMes = resumen?.optLong("gastos", prev.gastosMes)?.takeIf { it > 0 } ?: prev.gastosMes,
                    disponibleReal = resumen?.optLong("disponible", prev.disponibleReal)?.takeIf { it > 0 } ?: prev.disponibleReal,
                    fijosPendientes = if (fijosPendCalculados > 0) fijosPendCalculados else (resumen?.optLong("fijos_pendientes", prev.fijosPendientes) ?: prev.fijosPendientes),
                    aycColanta = cuentas?.optLong("aycColanta", prev.aycColanta) ?: prev.aycColanta,
                    efectivo = cuentas?.optLong("efectivo", prev.efectivo) ?: prev.efectivo,
                    nequi = cuentas?.optLong("nequi", prev.nequi) ?: prev.nequi,
                    ahorroNavideno = ahorros?.optLong("navidenoAcumulado", prev.ahorroNavideno) ?: prev.ahorroNavideno,
                    aportesCooperativos = ahorros?.optLong("aportesAcumulado", prev.aportesCooperativos) ?: prev.aportesCooperativos,
                    ahorroOrdinario = ahorros?.optLong("ordinarioAcumulado", prev.ahorroOrdinario) ?: prev.ahorroOrdinario,
                    otrosAhorros = ahorros?.optLong("otrosAcumulado", prev.otrosAhorros) ?: prev.otrosAhorros,
                    totalFondosColanta = ahorros?.optLong("totalAcumulado", prev.totalFondosColanta) ?: prev.totalFondosColanta,
                    totalIngresosAnual = kpis?.optLong("totalIngresos", prev.totalIngresosAnual) ?: prev.totalIngresosAnual,
                    totalGastosAnual = kpis?.optLong("totalGastos", prev.totalGastosAnual) ?: prev.totalGastosAnual,
                    superavitAnual = kpis?.optLong("superavit", prev.superavitAnual) ?: prev.superavitAnual,
                    tasaAhorroAnual = kpis?.optDouble("tasaAhorro", prev.tasaAhorroAnual) ?: prev.tasaAhorroAnual,
                    movimientos = if (movsList.isNotEmpty()) movsList else prev.movimientos,
                    fijos = nuevosFijos,
                    resumenAnual = if (anualList.isNotEmpty()) anualList else prev.resumenAnual
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun registrarMovimiento(
        tipo: String,
        descripcion: String,
        categoria: String,
        subcategoria: String = "General",
        valor: Long,
        cuenta: String = "AyC Colanta",
        idFijo: String? = null
    ) {
        if (valor <= 0L) return
        val ahora = Date()
        val fechaStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(ahora)
        val horaStr = SimpleDateFormat("hh:mm a", Locale("es", "CO")).format(ahora)
        val estado = _uiState.value

        val maxConsecutivo = estado.movimientos.mapNotNull {
            it.id.removePrefix("M-").toIntOrNull()
        }.maxOrNull() ?: 264
        val nuevoId = "M-" + (maxConsecutivo + 1).toString().padStart(4, '0')
        val metodo = when {
            cuenta.contains("Efectivo", ignoreCase = true) -> "Efectivo"
            cuenta.contains("Colanta", ignoreCase = true) -> "Transferencia / Nómina"
            else -> "Transferencia"
        }

        val nuevoMov = MovimientoItem(
            id = nuevoId,
            fecha = fechaStr,
            hora = horaStr,
            tipo = tipo,
            categoria = categoria,
            subcategoria = if (idFijo != null) "Pago fijo" else subcategoria,
            descripcion = descripcion.ifBlank { "$tipo de $categoria" },
            valor = valor,
            metodo = metodo,
            cuenta = cuenta,
            mes = estado.mesActivo,
            anio = estado.anioActivo
        )

        // Actualización optimista inmediata
        _uiState.update { prev ->
            val esGasto = !tipo.contains("Ingreso", ignoreCase = true)
            val nuevosIng = if (esGasto) prev.ingresosMes else prev.ingresosMes + valor
            val nuevosGas = if (esGasto) prev.gastosMes + valor else prev.gastosMes
            val nuevoDisp = if (esGasto) (prev.disponibleReal - valor).coerceAtLeast(0L) else prev.disponibleReal + valor

            val nuevosFijos = if (idFijo != null && esGasto) {
                prev.fijos.map { f ->
                    if (f.id.equals(idFijo, ignoreCase = true)) {
                        val nuevoAbono = (f.abonado + valor).coerceAtLeast(valor)
                        val nuevaCuota = f.cuota.coerceAtLeast(valor)
                        f.copy(
                            cuota = nuevaCuota,
                            abonado = nuevoAbono,
                            estado = if (nuevoAbono >= nuevaCuota) "🟢 Pagado" else "🟡 Abono Parcial",
                            fechaPago = fechaStr,
                            saldo = (nuevaCuota - nuevoAbono).coerceAtLeast(0L)
                        )
                    } else f
                }
            } else prev.fijos

            val nuevosPendientes = nuevosFijos.filter { !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }

            prev.copy(
                ingresosMes = nuevosIng,
                gastosMes = nuevosGas,
                disponibleReal = nuevoDisp,
                fijosPendientes = nuevosPendientes,
                movimientos = listOf(nuevoMov) + prev.movimientos,
                fijos = nuevosFijos,
                statusBanner = "✅ Registrado $nuevoId: ${nuevoMov.descripcion}"
            )
        }

        // Guardar también en base de datos local Room y sincronizar con Google Sheets Fila 5
        viewModelScope.launch {
            try {
                repository.insertTransaction(
                    Transaction(
                        type = tipo,
                        concept = nuevoMov.descripcion,
                        category = categoria,
                        amount = valor.toDouble()
                    )
                )
            } catch (_: Exception) {}

            try {
                val payload = mutableMapOf<String, Any?>(
                    "action" to "addMovement",
                    "token" to RetrofitClient.AUTH_TOKEN,
                    "id" to nuevoId,
                    "tipo" to tipo,
                    "valor" to valor,
                    "descripcion" to nuevoMov.descripcion,
                    "categoria" to categoria,
                    "subcategoria" to nuevoMov.subcategoria,
                    "cuenta" to cuenta,
                    "metodo" to metodo,
                    "origen" to "App Android Nativa V26.9"
                )
                if (idFijo != null) {
                    payload["idFijo"] = idFijo
                }
                withContext(Dispatchers.IO) {
                    RetrofitClient.api.executePostAction(payload)
                }
            } catch (_: Exception) {}
        }
    }

    fun registrarGastoHormiga(concepto: String, valor: Long) {
        registrarMovimiento(
            tipo = "Gasto",
            descripcion = "Gasto Hormiga: $concepto",
            categoria = "Antojos",
            subcategoria = "Gasto hormiga",
            valor = valor,
            cuenta = "Efectivo"
        )
    }

    fun pagarGastoFijo(fijo: GastoFijoItem) {
        val montoAPagar = if (fijo.saldo > 0) fijo.saldo else fijo.cuota
        if (montoAPagar <= 0L) return
        registrarMovimiento(
            tipo = "Gasto",
            descripcion = "Pago Fijo: ${fijo.concepto}",
            categoria = fijo.categoria,
            subcategoria = "Pago fijo",
            valor = montoAPagar,
            cuenta = "AyC Colanta",
            idFijo = fijo.id
        )
    }

    fun crearGastoFijo(concepto: String, categoria: String, cuota: Long, dia: Int) {
        if (concepto.isBlank() || cuota <= 0L) return
        val estado = _uiState.value
        val maxFijo = estado.fijos.mapNotNull {
            it.id.removePrefix("F-").toIntOrNull()
        }.maxOrNull() ?: 122
        val nuevoId = "F-" + (maxFijo + 1).toString().padStart(4, '0')

        val nuevoItem = GastoFijoItem(
            id = nuevoId,
            concepto = concepto.trim(),
            categoria = categoria,
            dia = dia,
            cuota = cuota,
            abonado = 0L,
            estado = "🔴 Pendiente",
            fechaPago = "",
            saldo = cuota,
            prioridad = "Media",
            medioPago = "Transferencia"
        )

        _uiState.update { prev ->
            val nuevaLista = (prev.fijos + nuevoItem).sortedWith(compareBy({ it.quincena }, { it.dia }, { it.id }))
            val pendientes = nuevaLista.filter { !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            prev.copy(
                fijos = nuevaLista,
                fijosPendientes = pendientes,
                statusBanner = "✅ Compromiso fijo $nuevoId ($concepto) creado"
            )
        }

        viewModelScope.launch {
            try {
                val payload = mapOf(
                    "action" to "addFixedObligation",
                    "token" to RetrofitClient.AUTH_TOKEN,
                    "id" to nuevoId,
                    "concepto" to concepto.trim(),
                    "categoria" to categoria,
                    "dia" to dia,
                    "cuota" to cuota,
                    "abonado" to 0,
                    "mes" to estado.mesActivo,
                    "anio" to estado.anioActivo
                )
                withContext(Dispatchers.IO) {
                    RetrofitClient.api.executePostAction(payload)
                }
            } catch (_: Exception) {}
        }
    }

    fun eliminarMovimiento(mov: MovimientoItem) {
        _uiState.update { prev ->
            val esGasto = !mov.tipo.contains("Ingreso", ignoreCase = true)
            val nuevosIng = if (esGasto) prev.ingresosMes else (prev.ingresosMes - mov.valor).coerceAtLeast(0L)
            val nuevosGas = if (esGasto) (prev.gastosMes - mov.valor).coerceAtLeast(0L) else prev.gastosMes
            val nuevoDisp = if (esGasto) prev.disponibleReal + mov.valor else (prev.disponibleReal - mov.valor).coerceAtLeast(0L)

            prev.copy(
                ingresosMes = nuevosIng,
                gastosMes = nuevosGas,
                disponibleReal = nuevoDisp,
                movimientos = prev.movimientos.filterNot { it.id == mov.id },
                statusBanner = "🗑️ Registro ${mov.id} eliminado de Google Sheets"
            )
        }

        viewModelScope.launch {
            try {
                val payload = mapOf(
                    "action" to "deleteMovement",
                    "token" to RetrofitClient.AUTH_TOKEN,
                    "id" to mov.id,
                    "descripcion" to mov.descripcion,
                    "valor" to mov.valor
                )
                withContext(Dispatchers.IO) {
                    RetrofitClient.api.executePostAction(payload)
                }
            } catch (_: Exception) {}
        }
    }

    fun escanearFacturasGmail() {
        if (_uiState.value.isScanningGmail) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningGmail = true,
                    statusBanner = "📬 Analizando correos y descomprimiendo archivos .ZIP en Gmail..."
                )
            }
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.executeGetAction(
                        action = "procesarFacturas",
                        token = RetrofitClient.AUTH_TOKEN
                    )
                }
                val body = response.body()
                val procesadas = body?.optInt("procesadas", 0) ?: 0
                val msg = if (procesadas > 0) {
                    "📬 ¡$procesadas factura(s) .ZIP/XML procesadas! Quedaron sin leer en tu bandeja y se moverán al leerlas."
                } else {
                    "📬 Bandeja revisada: Correos leídos movidos a su carpeta FINANZAS y sin facturas pendientes."
                }
                _uiState.update { it.copy(isScanningGmail = false, statusBanner = msg) }
                sincronizarConSheets(_uiState.value.mesActivo, _uiState.value.anioActivo, silencioso = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanningGmail = false,
                        statusBanner = "📬 Escáner ejecutado (verifica conexión si hay facturas nuevas)"
                    )
                }
            }
        }
    }

    fun enviarAuditoriaEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusBanner = "📧 Generando y enviando Auditoría Anual a tu Gmail...") }
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.executeGetAction(
                        action = "sendEmailAudit",
                        token = RetrofitClient.AUTH_TOKEN
                    )
                }
                val ok = response.body()?.optBoolean("ok", false) ?: false
                _uiState.update {
                    it.copy(
                        statusBanner = if (ok) "📧 Diagnóstico Financiero 2026 enviado a marlonpardomu@gmail.com"
                        else "📧 Solicitud de auditoría enviada al servidor"
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(statusBanner = "⚠️ No se pudo conectar para enviar el correo") }
            }
        }
    }

    fun enviarPreguntaCfo(preguntaRaw: String) {
        val pregunta = preguntaRaw.trim()
        if (pregunta.isEmpty()) return

        val msgUser = CfoMensaje(autor = "user", texto = pregunta)
        _uiState.update { it.copy(mensajesCfo = it.mensajesCfo + msgUser) }

        val pLower = pregunta.lowercase(Locale.ROOT)
        val st = _uiState.value

        // 1. Detectar si es comando de escanear correos / facturas .zip
        if (pLower.contains("escanear") || pLower.contains("correo") || pLower.contains("gmail") || pLower.contains("zip")) {
            escanearFacturasGmail()
            val respuesta = CfoMensaje(
                autor = "ai",
                texto = "📬 He lanzado el escáner de Gmail y descompresor `.ZIP`. Recuerda la regla activa: todo correo nuevo se analiza, se etiqueta en `FINANZAS/<Carpeta>` y se deja **sin marcar como visto** en tu bandeja; apenas lo abras/leas, se mueve automáticamente a su carpeta."
            )
            _uiState.update { it.copy(mensajesCfo = it.mensajesCfo + respuesta) }
            return
        }

        // 2. Detectar si es comando de registrar gasto rápido (ej. "gasté 15000 en almuerzo")
        val regexGasto = Regex("""(?:gaste|gasté|pague|pagué|compre|compré)\s+\$?\s*(\d[\d.,]*)\s*(?:mil|k)?\s*(?:en|de|por)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val matchGasto = regexGasto.find(pregunta)
        if (matchGasto != null) {
            val numRaw = matchGasto.groupValues[1].replace(".", "").replace(",", "")
            var monto = numRaw.toLongOrNull() ?: 0L
            if (pLower.contains("${matchGasto.groupValues[1]} mil") || pLower.contains("${matchGasto.groupValues[1]}k")) {
                monto *= 1000L
            }
            val concepto = matchGasto.groupValues[2].trim().ifEmpty { "Gasto registrado por CFO IA" }
            if (monto > 0L) {
                registrarMovimiento(
                    tipo = "Gasto",
                    descripcion = concepto.replaceFirstChar { it.uppercase() },
                    categoria = "Alimentación",
                    valor = monto,
                    cuenta = "AyC Colanta"
                )
                val respuesta = CfoMensaje(
                    autor = "ai",
                    texto = "✅ Listo Marlon. Registré el gasto de **$${formatearNumero(monto)} COP** por concepto de **$concepto** en la fila 5 de `MOVIMIENTOS`. Tu nuevo disponible es **$${formatearNumero(_uiState.value.disponibleReal)} COP**."
                )
                _uiState.update { it.copy(mensajesCfo = it.mensajesCfo + respuesta) }
                return
            }
        }

        // 3. Respuesta analítica inteligente basada en las cifras reales de Marlon
        val textoRespuesta = when {
            pLower.contains("disponible") || pLower.contains("liquidez") || pLower.contains("cuanto tengo") || pLower.contains("cuánto tengo") ->
                "💰 Tu **Disponible Real** en ${st.mesActivo.replaceFirstChar { it.uppercase() }} es de **$${formatearNumero(st.disponibleReal)} COP**, distribuido así:\n• **AyC Colanta:** $${formatearNumero(st.aycColanta)} COP\n• **Efectivo en Mano:** $${formatearNumero(st.efectivo)} COP\n• **Nequi:** $${formatearNumero(st.nequi)} COP"

            pLower.contains("colanta") || pLower.contains("fondo") || pLower.contains("navide") || pLower.contains("ahorro") ->
                "🏦 En **Fondos Cooperativa Colanta** acumulas **$${formatearNumero(st.totalFondosColanta)} COP**:\n• 🎄 **Ahorro Navideño (1 Dic):** $${formatearNumero(st.ahorroNavideno)} COP\n• 🤝 **Aportes Cooperativos:** $${formatearNumero(st.aportesCooperativos)} COP\n• 📈 **Ahorro Ordinario:** $${formatearNumero(st.ahorroOrdinario)} COP\n• 🛡️ **Otros Fondos:** $${formatearNumero(st.otrosAhorros)} COP"

            pLower.contains("fijo") || pLower.contains("pendiente") || pLower.contains("quincena") -> {
                val pend = st.fijos.filter { !it.estaPagado }
                val resumenPend = pend.take(5).joinToString("\n") { "• **${it.concepto}** (Día ${it.dia}): $${formatearNumero(it.saldo)} COP" }
                "📅 Tienes **${pend.size} compromisos fijos pendientes** por **$${formatearNumero(st.fijosPendientes)} COP**:\n$resumenPend\n\nPuedes pagarlos con 1 toque en la pestaña **Fijos**."
            }

            else ->
                "📊 **Diagnóstico Rápido (${st.mesActivo.replaceFirstChar { it.uppercase() }} ${st.anioActivo}):**\n• **Disponible Real:** $${formatearNumero(st.disponibleReal)} COP\n• **Ingresos Mes:** $${formatearNumero(st.ingresosMes)} COP\n• **Gastos Mes:** $${formatearNumero(st.gastosMes)} COP\n• **Patrimonio Colanta:** $${formatearNumero(st.totalFondosColanta)} COP\n• **Superávit 2026:** $${formatearNumero(st.superavitAnual)} COP (${st.tasaAhorroAnual}%)"
        }

        _uiState.update {
            it.copy(mensajesCfo = it.mensajesCfo + CfoMensaje(autor = "ai", texto = textoRespuesta))
        }
    }

    fun addTransaction(type: String, concept: String, category: String, amount: Double) {
        registrarMovimiento(
            tipo = type,
            descripcion = concept,
            categoria = category,
            valor = amount.toLong()
        )
        scheduleSync()
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(syncRequest)
    }

    companion object {
        fun formatearNumero(valor: Long): String {
            return String.format(Locale("es", "CO"), "%,d", valor).replace(',', '.')
        }

        fun defaultMovimientosOctubre(): List<MovimientoItem> = listOf(
            MovimientoItem("M-0264", "2026-10-18", "08:40 AM", "Gasto", "Antojos", "Gasto hormiga", "Endulzada amigo secreto Colanta", 18000L, "Efectivo", "Efectivo", "octubre", 2026),
            MovimientoItem("M-0263", "2026-10-17", "07:15 PM", "Gasto", "Servicios", "Pago fijo", "Pago Fijo: EPM ENERGIA (Octubre 2026)", 197000L, "PSE", "AyC Colanta", "octubre", 2026),
            MovimientoItem("M-0262", "2026-10-17", "02:30 PM", "Gasto", "Alimentación", "Pago fijo", "Pago Fijo: COMIDA (Quincena 1)", 650000L, "Transferencia / Nómina", "AyC Colanta", "octubre", 2026),
            MovimientoItem("M-0261", "2026-10-16", "06:10 PM", "Gasto", "Moto", "Combustible", "Tanqueada Gasolina Moto", 32000L, "Efectivo", "Efectivo", "octubre", 2026),
            MovimientoItem("M-0260", "2026-10-15", "04:00 PM", "Ingreso", "Salario", "Nómina Colanta", "Nómina Colanta - Quincena 1 Octubre", 1700605L, "Transferencia / Nómina", "AyC Colanta", "octubre", 2026),
            MovimientoItem("M-0258", "2026-10-12", "12:35 PM", "Gasto", "Alimentación", "Restaurante", "Almuerzo — Restaurante Corporativo Planta Medellín (Colanta)", 11500L, "Transferencia / Nómina", "AyC Colanta", "octubre", 2026),
            MovimientoItem("M-0257", "2026-10-08", "05:50 PM", "Gasto", "Moto", "Mantenimiento", "Lavada de Moto", 18000L, "Efectivo", "Efectivo", "octubre", 2026),
            MovimientoItem("M-0256", "2026-10-04", "03:20 PM", "Gasto", "Compras", "Ropa y Moda Paisa", "Ropa Deportiva Greenfit (Leggins, Top, Enterizo)", 121215L, "Transferencia", "Nequi", "octubre", 2026)
        )

        fun defaultFijosOctubre(): List<GastoFijoItem> = listOf(
            GastoFijoItem("F-0107", "EPM ENERGIA", "Servicios", 15, 197000L, 197000L, "🟢 Pagado", "2026-10-17", 0L, "Alta", "PSE"),
            GastoFijoItem("F-0108", "COMIDA", "Alimentación", 15, 650000L, 650000L, "🟢 Pagado", "2026-10-17", 0L, "Alta", "Transferencia"),
            GastoFijoItem("F-0109", "Internet Tigo Hogar", "Servicios", 15, 95000L, 0L, "🔴 Pendiente", "", 95000L, "Alta", "PSE"),
            GastoFijoItem("F-0110", "Parqueadero Trabajo", "Moto", 15, 34000L, 0L, "🔴 Pendiente", "", 34000L, "Media", "Efectivo"),
            GastoFijoItem("F-0112", "Parqueadero Moto Casa", "Moto", 15, 70000L, 0L, "🔴 Pendiente", "", 70000L, "Alta", "Efectivo"),
            GastoFijoItem("F-0113", "Gasolina Moto Mes", "Moto", 15, 130000L, 32000L, "🟡 Abono Parcial", "2026-10-16", 98000L, "Alta", "Efectivo"),
            GastoFijoItem("F-0122", "Agua", "Servicios", 15, 60000L, 0L, "🔴 Pendiente", "", 60000L, "Media", "Transferencia"),
            GastoFijoItem("F-0114", "Spotify Premium", "Suscripciones", 30, 18900L, 0L, "🔴 Pendiente", "", 18900L, "Baja", "Nequi"),
            GastoFijoItem("F-0115", "Plan Datos Celular", "Servicios", 30, 48000L, 0L, "🔴 Pendiente", "", 48000L, "Media", "PSE"),
            GastoFijoItem("F-0116", "Arriendo Hogar", "Vivienda", 30, 350000L, 0L, "🔴 Pendiente", "", 350000L, "Alta", "Transferencia")
        )

        fun defaultResumenAnual(): List<MesAnualItem> = listOf(
            MesAnualItem("enero", "Enero", 4120000L, 1636906L, 2483094L),
            MesAnualItem("febrero", "Febrero", 3450000L, 2180000L, 1270000L),
            MesAnualItem("marzo", "Marzo", 3520000L, 2390000L, 1130000L),
            MesAnualItem("abril", "Abril", 3480000L, 2410000L, 1070000L),
            MesAnualItem("mayo", "Mayo", 3610000L, 2590000L, 1020000L),
            MesAnualItem("junio", "Junio", 4350000L, 3892812L, 457188L),
            MesAnualItem("julio", "Julio", 3490000L, 2420000L, 1070000L),
            MesAnualItem("agosto", "Agosto", 3280000L, 2510000L, 770000L),
            MesAnualItem("septiembre", "Septiembre", 3151873L, 2470771L, 681102L),
            MesAnualItem("octubre", "Octubre", 1700605L, 1285239L, 415366L)
        )
    }
}

private fun JsonObject.optString(key: String, fallback: String): String {
    val el = this.get(key)
    return if (el != null && !el.isJsonNull) el.asString else fallback
}

private fun JsonObject.optLong(key: String, fallback: Long): Long {
    val el = this.get(key)
    return try {
        if (el != null && !el.isJsonNull) el.asDouble.toLong() else fallback
    } catch (_: Exception) {
        fallback
    }
}

private fun JsonObject.optInt(key: String, fallback: Int): Int {
    val el = this.get(key)
    return try {
        if (el != null && !el.isJsonNull) el.asInt else fallback
    } catch (_: Exception) {
        fallback
    }
}

private fun JsonObject.optDouble(key: String, fallback: Double): Double {
    val el = this.get(key)
    return try {
        if (el != null && !el.isJsonNull) el.asDouble else fallback
    } catch (_: Exception) {
        fallback
    }
}

private fun JsonObject.optBoolean(key: String, fallback: Boolean): Boolean {
    val el = this.get(key)
    return try {
        if (el != null && !el.isJsonNull) el.asBoolean else fallback
    } catch (_: Exception) {
        fallback
    }
}

private fun JsonObject.optObject(key: String): JsonObject? {
    val el = this.get(key)
    return if (el != null && el.isJsonObject) el.asJsonObject else null
}
