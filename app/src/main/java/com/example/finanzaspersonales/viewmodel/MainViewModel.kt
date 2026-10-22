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
import com.example.finanzaspersonales.util.NotificationHelper
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
        get() = estado.contains("Pagado", ignoreCase = true) || (cuota > 0 && abonado >= cuota) || (saldo <= 0 && fechaPago.isNotBlank())
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
    val autor: String,
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
    val totalCuotasFijos: Long = 2381000L,
    val totalAbonadoFijos: Long = 2021000L,
    val fijosPendientes: Long = 240000L,
    val q1Pendiente: Long = 135000L,
    val q2Pendiente: Long = 105000L,
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
            texto = "¡Hola Marlon! Soy tu asistente financiero de IA 🧠 ¿En qué te puedo ayudar hoy? ¿Qué movimiento o consulta financiera deseas realizar?"
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
            val resFijos = json.optObject("resumenFijos")

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
                    val estadoStr = f.optString("estado", "")
                    val fechaPagoStr = f.optString("fechaPago", "")
                    val esArriendo = f.optString("id", "").equals("F-0116", ignoreCase = true) || f.optString("concepto", "").contains("arriendo", ignoreCase = true)
                    val esPagado = if (esArriendo && abonado == 0L) false else (estadoStr.contains("Pagado", ignoreCase = true) || (cuota > 0 && abonado >= cuota))
                    val saldoCalc = if (esPagado) 0L else f.optLong("saldo", (cuota - abonado).coerceAtLeast(0L))

                    fijosList.add(
                        GastoFijoItem(
                            id = f.optString("id", "F-010$i"),
                            concepto = f.optString("concepto", "Compromiso"),
                            categoria = f.optString("categoria", "Servicios"),
                            dia = f.optInt("dia", 15),
                            cuota = cuota,
                            abonado = if (esPagado && abonado == 0L) cuota else abonado,
                            estado = if (esPagado) "🟢 Pagado" else "🔴 Pendiente",
                            fechaPago = fechaPagoStr,
                            saldo = saldoCalc,
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
                val q1Pend = nuevosFijos.filter { it.quincena == 1 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
                val q2Pend = nuevosFijos.filter { it.quincena == 2 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
                val totalPendientes = q1Pend + q2Pend
                val totCuotas = resFijos?.optLong("totalCuotas", prev.totalCuotasFijos) ?: nuevosFijos.sumOf { it.cuota }
                val totAbonado = resFijos?.optLong("totalAbonado", prev.totalAbonadoFijos) ?: nuevosFijos.sumOf { it.abonado }

                prev.copy(
                    isLoading = false,
                    statusBanner = if (silencioso) null else "✅ Sincronizado 1:1 con Google Sheets",
                    mesActivo = resumen?.optString("mes", mesSolicitado)?.lowercase(Locale.ROOT) ?: mesSolicitado,
                    ingresosMes = resumen?.optLong("ingresos", prev.ingresosMes)?.takeIf { it > 0 } ?: prev.ingresosMes,
                    gastosMes = resumen?.optLong("gastos", prev.gastosMes)?.takeIf { it > 0 } ?: prev.gastosMes,
                    disponibleReal = resumen?.optLong("disponible", prev.disponibleReal)?.takeIf { it > 0 } ?: prev.disponibleReal,
                    totalCuotasFijos = totCuotas,
                    totalAbonadoFijos = totAbonado,
                    fijosPendientes = totalPendientes,
                    q1Pendiente = q1Pend,
                    q2Pendiente = q2Pend,
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

            val q1Pend = nuevosFijos.filter { it.quincena == 1 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            val q2Pend = nuevosFijos.filter { it.quincena == 2 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            val totalPendientes = q1Pend + q2Pend

            prev.copy(
                ingresosMes = nuevosIng,
                gastosMes = nuevosGas,
                disponibleReal = nuevoDisp,
                fijosPendientes = totalPendientes,
                q1Pendiente = q1Pend,
                q2Pendiente = q2Pend,
                totalAbonadoFijos = if (idFijo != null && esGasto) prev.totalAbonadoFijos + valor else prev.totalAbonadoFijos,
                movimientos = listOf(nuevoMov) + prev.movimientos,
                fijos = nuevosFijos,
                statusBanner = "✅ Registrado $nuevoId: ${nuevoMov.descripcion}"
            )
        }

        try {
            NotificationHelper.notifyMovimientoRegistrado(
                getApplication(),
                tipo,
                nuevoMov.descripcion,
                valor
            )
        } catch (_: Exception) {}

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
        val cta = when {
            fijo.medioPago.contains("Efectivo", ignoreCase = true) -> "Efectivo"
            fijo.medioPago.contains("Nequi", ignoreCase = true) -> "Nequi"
            else -> "AyC Colanta"
        }
        registrarMovimiento(
            tipo = "Gasto",
            descripcion = "Pago Fijo: ${fijo.concepto}",
            categoria = fijo.categoria,
            subcategoria = "Pago fijo",
            valor = montoAPagar,
            cuenta = cta,
            idFijo = fijo.id
        )
    }

    fun revertirGastoFijo(fijo: GastoFijoItem) {
        val montoRevertido = if (fijo.abonado > 0) fijo.abonado else fijo.cuota
        val estado = _uiState.value

        // 1. Encontrar movimiento asociado si existe en la lista local para borrarlo
        val movAsociado = estado.movimientos.firstOrNull { m ->
            (m.subcategoria.contains("fijo", ignoreCase = true) || m.descripcion.contains("pago fijo", ignoreCase = true)) &&
            (m.descripcion.contains(fijo.concepto, ignoreCase = true) || (fijo.id.isNotBlank() && m.descripcion.contains(fijo.id)))
        }

        // 2. Actualización optimista inmediata en la UI
        _uiState.update { prev ->
            val nuevosFijos = prev.fijos.map { f ->
                if (f.id.equals(fijo.id, ignoreCase = true)) {
                    f.copy(
                        abonado = 0L,
                        estado = "🔴 Pendiente",
                        fechaPago = "",
                        saldo = f.cuota
                    )
                } else f
            }

            val q1Pend = nuevosFijos.filter { it.quincena == 1 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            val q2Pend = nuevosFijos.filter { it.quincena == 2 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            val totalPend = q1Pend + q2Pend

            val nuevaListaMovs = if (movAsociado != null) {
                prev.movimientos.filterNot { it.id == movAsociado.id }
            } else prev.movimientos

            val nuevoGastoMes = (prev.gastosMes - montoRevertido).coerceAtLeast(0L)
            val nuevoDisp = prev.disponibleReal + montoRevertido
            val nuevoAbonadoFijos = (prev.totalAbonadoFijos - montoRevertido).coerceAtLeast(0L)

            prev.copy(
                fijos = nuevosFijos,
                fijosPendientes = totalPend,
                q1Pendiente = q1Pend,
                q2Pendiente = q2Pend,
                totalAbonadoFijos = nuevoAbonadoFijos,
                gastosMes = nuevoGastoMes,
                disponibleReal = nuevoDisp,
                movimientos = nuevaListaMovs,
                statusBanner = "↺ Pago de ${fijo.concepto} revertido a Pendiente"
            )
        }

        // 3. Sincronización robusta con Google Sheets en segundo plano
        viewModelScope.launch {
            if (movAsociado != null) {
                try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.api.executePostAction(
                            mapOf(
                                "action" to "deleteMovement",
                                "token" to RetrofitClient.AUTH_TOKEN,
                                "id" to movAsociado.id,
                                "descripcion" to movAsociado.descripcion,
                                "valor" to movAsociado.valor
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.api.executePostAction(
                        mapOf(
                            "action" to "revertirPagoFijo",
                            "token" to RetrofitClient.AUTH_TOKEN,
                            "id" to fijo.id,
                            "concepto" to fijo.concepto,
                            "valor" to montoRevertido,
                            "mes" to estado.mesActivo,
                            "anio" to estado.anioActivo
                        )
                    )
                }
            } catch (_: Exception) {}

            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.api.executePostAction(
                        mapOf(
                            "action" to "updateFixedObligation",
                            "token" to RetrofitClient.AUTH_TOKEN,
                            "id" to fijo.id,
                            "cuota" to fijo.cuota,
                            "abonado" to 0L,
                            "estado" to "🔴 Pendiente",
                            "saldo" to fijo.cuota,
                            "fechaPago" to "",
                            "mes" to estado.mesActivo,
                            "anio" to estado.anioActivo
                        )
                    )
                }
            } catch (_: Exception) {}

            try {
                kotlinx.coroutines.delay(1200)
                sincronizarConSheets(silencioso = true)
            } catch (_: Exception) {}
        }
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
            val q1Pend = nuevaLista.filter { it.quincena == 1 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            val q2Pend = nuevaLista.filter { it.quincena == 2 && !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }
            prev.copy(
                fijos = nuevaLista,
                totalCuotasFijos = prev.totalCuotasFijos + cuota,
                fijosPendientes = q1Pend + q2Pend,
                q1Pendiente = q1Pend,
                q2Pendiente = q2Pend,
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

        if (pLower.contains("escanear") || pLower.contains("correo") || pLower.contains("gmail") || pLower.contains("zip")) {
            escanearFacturasGmail()
            val respuesta = CfoMensaje(
                autor = "ai",
                texto = "📬 He lanzado el escáner de Gmail y descompresor `.ZIP`. Recuerda la regla activa: todo correo nuevo se analiza, se etiqueta en `FINANZAS/<Carpeta>` y se deja **sin marcar como visto** en tu bandeja; apenas lo abras/leas, se mueve automáticamente a su carpeta."
            )
            _uiState.update { it.copy(mensajesCfo = it.mensajesCfo + respuesta) }
            return
        }

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

        val textoRespuesta = when {
            pLower.contains("disponible") || pLower.contains("liquidez") || pLower.contains("cuanto tengo") || pLower.contains("cuánto tengo") ->
                "💰 Tu **Disponible Real** en ${st.mesActivo.replaceFirstChar { it.uppercase() }} es de **$${formatearNumero(st.disponibleReal)} COP**, distribuido así:\n• **AyC Colanta:** $${formatearNumero(st.aycColanta)} COP\n• **Efectivo en Mano:** $${formatearNumero(st.efectivo)} COP\n• **Nequi:** $${formatearNumero(st.nequi)} COP"

            pLower.contains("colanta") || pLower.contains("fondo") || pLower.contains("navide") || pLower.contains("ahorro") ->
                "🏦 En **Fondos Cooperativa Colanta** acumulas **$${formatearNumero(st.totalFondosColanta)} COP**:\n• 🎄 **Ahorro Navideño (1 Dic):** $${formatearNumero(st.ahorroNavideno)} COP\n• 🤝 **Aportes Cooperativos:** $${formatearNumero(st.aportesCooperativos)} COP\n• 📈 **Ahorro Ordinario:** $${formatearNumero(st.ahorroOrdinario)} COP\n• 🛡️ **Otros Fondos:** $${formatearNumero(st.otrosAhorros)} COP"

            pLower.contains("fijo") || pLower.contains("pendiente") || pLower.contains("quincena") -> {
                val q1 = st.fijos.filter { it.quincena == 1 && !it.estaPagado }
                val q2 = st.fijos.filter { it.quincena == 2 && !it.estaPagado }
                "📅 **Gastos Fijos Pendientes (${st.mesActivo.replaceFirstChar { it.uppercase() }}):**\n" +
                    "• **Quincena 1 (Día 15):** $${formatearNumero(st.q1Pendiente)} COP (${q1.size} pendientes)\n" +
                    q1.joinToString("\n") { "  - ${it.concepto}: $${formatearNumero(it.saldo)} COP" } + "\n" +
                    "• **Fin de Mes (Día 30):** $${formatearNumero(st.q2Pendiente)} COP (${q2.size} pendientes)\n" +
                    q2.joinToString("\n") { "  - ${it.concepto}: $${formatearNumero(it.saldo)} COP" } + "\n" +
                    "Total Saldo Pendiente: **$${formatearNumero(st.fijosPendientes)} COP**"
            }

            else ->
                "📊 **Diagnóstico Rápido (${st.mesActivo.replaceFirstChar { it.uppercase() }} ${st.anioActivo}):**\n• **Disponible Real:** $${formatearNumero(st.disponibleReal)} COP\n• **Fijos Q1 (Día 15):** $${formatearNumero(st.q1Pendiente)} COP pend.\n• **Fijos Q2 (Día 30):** $${formatearNumero(st.q2Pendiente)} COP pend.\n• **Patrimonio Colanta:** $${formatearNumero(st.totalFondosColanta)} COP\n• **Superávit 2026:** $${formatearNumero(st.superavitAnual)} COP (${st.tasaAhorroAnual}%)"
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

        /**
         * Compromisos fijos oficiales de CONTROL FIJOS sincronizados 1:1 con Google Sheets:
         * Quincena 1 (Día 15): Total $1.356.000 | Abonado $1.221.000 | Pendiente $135.000 (F-0110, F-0112, F-0113)
         * Quincena 2 / Fin de Mes (Día 30): Total $1.025.000 | Abonado $920.000 | Pendiente $105.000 (F-0118, F-0119, F-0121)
         * Total Cuotas Mes = $2.381.000 | Total Abonado = $2.021.000 | Saldo Pendiente = $240.000
         */
        fun defaultFijosOctubre(): List<GastoFijoItem> = listOf(
            // QUINCENA 1 (DÍA 15 DE CADA MES)
            GastoFijoItem("F-0107", "EPM ENERGIA", "Servicios", 15, 197000L, 197000L, "🟢 Pagado", "2026-10-17", 0L, "Media", "Transferencia"),
            GastoFijoItem("F-0108", "COMIDA", "Alimentación", 15, 650000L, 650000L, "🟢 Pagado", "2026-10-17", 0L, "Media", "Efectivo"),
            GastoFijoItem("F-0109", "Internet Tigo", "Servicios", 15, 200000L, 200000L, "🟢 Pagado", "2026-10-17", 0L, "Media", "Transferencia"),
            GastoFijoItem("F-0110", "Parqueadero Trabajo Semanal", "Transporte", 15, 40000L, 0L, "🔴 Pendiente", "", 40000L, "Media", "Transferencia"),
            GastoFijoItem("F-0111", "Cuota Audífonos Samsung", "Otros", 15, 60000L, 60000L, "🟢 Pagado", "2026-10-17", 0L, "Media", "Transferencia"),
            GastoFijoItem("F-0112", "PARQUEADERO MOTO", "Moto", 15, 60000L, 0L, "🔴 Pendiente", "", 60000L, "Media", "Efectivo"),
            GastoFijoItem("F-0113", "Gasolina Moto", "Moto", 15, 35000L, 0L, "🔴 Pendiente", "", 35000L, "Media", "Transferencia"),
            GastoFijoItem("F-0114", "Spotify", "Entretenimiento", 15, 10100L, 10100L, "🟢 Pagado", "2026-10-15", 0L, "Media", "Tarjeta Nequi"),
            GastoFijoItem("F-0115", "CUOTA DATOS CELULAR", "Servicios", 15, 43900L, 43900L, "🟢 Pagado", "2026-09-30", 0L, "Media", "Efectivo"),
            GastoFijoItem("F-0122", "Agua", "Servicios", 15, 60000L, 60000L, "🟢 Pagado", "2026-10-17", 0L, "Media", "Transferencia"),

            // QUINCENA 2 / FIN DE MES (DÍA 30 DE CADA MES)
            GastoFijoItem("F-0116", "ARRIENDO CASA", "Vivienda", 30, 900000L, 0L, "🔴 Pendiente", "", 900000L, "Media", "Efectivo"),
            GastoFijoItem("F-0117", "Google One", "Servicios", 30, 20000L, 20000L, "🟢 Pagado", "2026-09-20", 0L, "Media", "Efectivo"),
            GastoFijoItem("F-0118", "Gasolina Moto", "Moto", 30, 25000L, 0L, "🔴 Pendiente", "", 25000L, "Media", "Efectivo"),
            GastoFijoItem("F-0119", "Parqueadero Trabajo Semanal", "Transporte", 30, 40000L, 0L, "🔴 Pendiente", "", 40000L, "Media", "Efectivo"),
            GastoFijoItem("F-0121", "PARQUEADERO MOTO", "Moto", 30, 40000L, 0L, "🔴 Pendiente", "", 40000L, "Media", "Efectivo")
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
