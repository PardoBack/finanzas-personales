package com.example.appfinanzas.data.repository

import com.example.appfinanzas.data.UserPreferencesRepository
import com.example.appfinanzas.data.local.dao.FixedExpenseDao
import com.example.appfinanzas.data.local.dao.SavingsGoalDao
import com.example.appfinanzas.data.local.dao.TransactionDao
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SyncPayloadTransaction(
    val id: String? = null,
    val fecha: String = "",
    val hora: String = "",
    val tipo: String = "EGRESO",
    val categoria: String = "",
    val subcategoria: String = "",
    val descripcion: String = "",
    val valor: Double = 0.0,
    val metodo: String = "",
    val cuenta: String = "Efectivo",
    val mes: String = "",
    val ano: String = "",
    val origen: String = "Android App",
    val referencia: String = ""
)

@Serializable
data class SyncPayloadFixedExpense(
    val id: String = "",
    val concepto: String = "",
    val categoria: String = "",
    val diaCobro: Int = 1,
    val valorMensual: Double = 0.0,
    val ultimoMesPagado: String = "",
    val fechaUltimoPago: String = "",
    val notasDeuda: String = ""
)

@Serializable
data class SyncPayloadSavingsGoal(
    val id: Int,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val category: String = "General"
)

@Serializable
data class SyncFullRequest(
    val action: String = "sync",
    val movimientos: List<SyncPayloadTransaction> = emptyList(),
    val gastosFijos: List<SyncPayloadFixedExpense> = emptyList(),
    val metasAhorro: List<SyncPayloadSavingsGoal> = emptyList()
)

@Serializable
data class SyncFullResponse(
    val status: String? = null,
    val message: String? = null,
    val syncedCount: Int? = 0
)

data class SyncResult(
    val success: Boolean,
    val syncedMovementsCount: Int = 0,
    val syncedFixedCount: Int = 0,
    val syncedGoalsCount: Int = 0,
    val message: String
)

interface GoogleDriveSyncRepository {
    suspend fun syncAllDataToDrive(): Result<SyncResult>
    fun getAppsScriptTemplateCode(): String
}

@Singleton
class GoogleDriveSyncRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : GoogleDriveSyncRepository {

    override suspend fun syncAllDataToDrive(): Result<SyncResult> = runCatching {
        val webAppUrl = userPreferencesRepository.googleWebAppUrl.first()

        if (webAppUrl.isBlank()) {
            throw IllegalStateException("URL_NOT_CONFIGURED")
        }

        val transactions = transactionDao.getAllTransactions().first()
        val fixedExpenses = fixedExpenseDao.getAllFixedExpenses().first()
        val savingsGoals = savingsGoalDao.getAllSavingsGoals().first()

        val payload = SyncFullRequest(
            action = "sync",
            movimientos = transactions.map {
                SyncPayloadTransaction(
                    id = it.remoteId ?: "M-${it.id}",
                    fecha = it.fecha,
                    hora = "12:00:00",
                    tipo = it.tipo.uppercase(),
                    categoria = it.categoria,
                    subcategoria = "",
                    descripcion = it.concepto,
                    valor = it.monto.toDouble(),
                    metodo = "Efectivo",
                    cuenta = it.cuenta.ifBlank { "Efectivo" },
                    mes = "",
                    ano = "",
                    origen = "Android App",
                    referencia = it.observaciones ?: ""
                )
            },
            gastosFijos = fixedExpenses.map {
                SyncPayloadFixedExpense(
                    id = if (it.id.toString().startsWith("F-")) it.id.toString() else "F-${it.id}",
                    concepto = it.concepto,
                    categoria = "Servicios",
                    diaCobro = it.diaPago,
                    valorMensual = it.monto,
                    ultimoMesPagado = "",
                    fechaUltimoPago = "",
                    notasDeuda = ""
                )
            },
            metasAhorro = savingsGoals.map {
                SyncPayloadSavingsGoal(
                    id = it.id,
                    name = it.name,
                    targetAmount = it.targetAmount,
                    currentAmount = it.currentAmount,
                    category = it.category
                )
            }
        )

        val jsonString = json.encodeToString(SyncFullRequest.serializer(), payload)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(webAppUrl)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody.isNullOrBlank()) {
            throw Exception("Error de respuesta HTTP ${response.code}")
        }

        val syncResponse = runCatching {
            json.decodeFromString(SyncFullResponse.serializer(), responseBody)
        }.getOrNull()

        // Mark unsynced local transactions as synced upon successful push
        transactions.filter { !it.isSynced }.forEach { unsynced ->
            transactionDao.updateTransaction(unsynced.copy(isSynced = true))
        }

        SyncResult(
            success = true,
            syncedMovementsCount = transactions.size,
            syncedFixedCount = fixedExpenses.size,
            syncedGoalsCount = savingsGoals.size,
            message = syncResponse?.message ?: "Sincronización exitosa con tu Hoja de Cálculo en Google Drive"
        )
    }

    override fun getAppsScriptTemplateCode(): String {
        return """
// ====================================================================
// PLANTILLA OFICIAL GOOGLE APPS SCRIPT V25.3 PRO REST - PARDOFINANZAS V25
// INSTRUCCIONES (2 MINUTOS):
// 1. Ve a https://script.google.com o abre tu Google Sheets y selecciona 'Extensiones' > 'Apps Script'.
// 2. Borra el código por defecto y PEGA este script completo.
// 3. Haz clic en 'Implementar' > 'Nueva implementación'.
// 4. Selecciona 'Aplicación Web'.
// 5. En 'Quién tiene acceso', selecciona 'Cualquier persona' (Anyone).
// 6. Haz clic en 'Implementar', copia la URL de la Web App y pégala en Finanzas Pro > Perfil.
// ====================================================================

const HOJA_MOVIMIENTOS = "MOVIMIENTOS";
const HOJA_CONTROL_FIJOS = "CONTROL FIJOS";
const HOJA_PANEL = "PANEL";
const HOJA_PRESUPUESTO = "PRESUPUESTO Y ALERTAS";
const HOJA_REGLAS = "REGLAS COMERCIOS";
const HOJA_CXC = "CUENTAS POR COBRAR";
const HOJA_METAS = "METAS AHORRO";
const EXCLUIR_CUENTA = "Clara Aide";

function doGet(e) {
  try {
    const params = (e && e.parameter) ? e.parameter : {};
    const accion = params.accion || "resumen";
    const ss = SpreadsheetApp.getActiveSpreadsheet();

    switch (accion) {
      case "resumen": return jsonResponse(obtenerResumenFinanciero(ss));
      case "movimientos": return jsonResponse(obtenerListaMovimientos(ss));
      case "fijos": return jsonResponse(obtenerListaGastosFijos(ss));
      case "disponible": return jsonResponse(obtenerDisponibleReal(ss));
      case "fugas": return jsonResponse(obtenerGastosHormigaFugas(ss));
      default:
        return jsonResponse({ status: "ok", message: "Google Apps Script V25.3 PRO REST activo" });
    }
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

function doPost(e) {
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    let body = {};
    if (e && e.postData && e.postData.contents) {
      body = JSON.parse(e.postData.contents);
    }
    const params = (e && e.parameter) ? e.parameter : {};
    const accion = body.accion || body.action || params.accion || "sync";

    switch (accion) {
      case "nuevo_movimiento": return jsonResponse(registrarNuevoMovimiento(ss, body));
      case "pagar_fijo": return jsonResponse(registrarPagoGastoFijo(ss, body));
      case "borrar_ultimo": return jsonResponse(borrarUltimoConjunto(ss));
      case "sync":
      default: return jsonResponse(procesarSincronizacionCompleta(ss, body));
    }
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

function obtenerResumenFinanciero(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  let totalIngresos = 0, totalGastos = 0;

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    if (!row[0]) continue;
    const tipo = String(row[3] || "").toUpperCase();
    const cuenta = String(row[9] || "");
    const valor = Number(row[7]) || 0;
    if (cuenta.includes(EXCLUIR_CUENTA)) continue;
    if (tipo === "INGRESO") totalIngresos += valor;
    else if (tipo === "EGRESO" || tipo === "GASTO") totalGastos += valor;
  }
  return { balance: totalIngresos - totalGastos, disponible: obtenerDisponibleRealMonto(ss), ingresos: totalIngresos, gastos: totalGastos };
}

function obtenerListaMovimientos(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  const resultado = [];
  for (let i = 1; i < rows.length; i++) {
    const r = rows[i];
    if (!r[0]) continue;
    if (String(r[9] || "").includes(EXCLUIR_CUENTA)) continue;
    resultado.push({
      id: String(r[0]), fecha: formatFecha(r[1]), hora: String(r[2] || ""), tipo: String(r[3] || "EGRESO"),
      categoria: String(r[4] || ""), subcategoria: String(r[5] || ""), descripcion: String(r[6] || ""),
      valor: Number(r[7]) || 0, metodo: String(r[8] || ""), cuenta: String(r[9] || ""),
      mes: String(r[10] || ""), ano: String(r[11] || ""), origen: String(r[12] || "Android App"), referencia: String(r[13] || "")
    });
  }
  return resultado;
}

function obtenerListaGastosFijos(ss) {
  const sheetFijos = getOrCreateSheet(ss, HOJA_CONTROL_FIJOS);
  const rows = sheetFijos.getDataRange().getValues();
  const resultado = [];
  for (let i = 1; i < rows.length; i++) {
    const r = rows[i];
    if (!r[0]) continue;
    resultado.push({
      id: String(r[0]), concepto: String(r[1] || ""), categoria: String(r[2] || ""), diaCobro: Number(r[3]) || 1,
      valorMensual: Number(r[4]) || 0, ultimoMesPagado: String(r[5] || ""), fechaUltimoPago: formatFecha(r[6]), notasDeuda: String(r[7] || "")
    });
  }
  return resultado;
}

function obtenerDisponibleRealMonto(ss) {
  const res = obtenerResumenFinancieroPuro(ss);
  const fijos = obtenerListaGastosFijos(ss);
  const now = new Date();
  const mesAno = getMesNombre(now.getMonth()) + " " + now.getFullYear();
  let comp = 0;
  fijos.forEach(f => { if (f.ultimoMesPagado !== mesAno) comp += f.valorMensual; });
  return Math.max(0, res.balance - comp);
}

function obtenerDisponibleReal(ss) {
  return { disponibleReal: obtenerDisponibleRealMonto(ss), fechaConsulta: new Date().toISOString() };
}

function obtenerGastosHormigaFugas(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  let total = 0; const detalles = [];
  for (let i = 1; i < rows.length; i++) {
    const r = rows[i]; if (!r[0]) continue;
    const cat = String(r[4] || "").toLowerCase();
    const ref = String(r[13] || "").toLowerCase();
    if (cat.includes("hormiga") || ref.includes("gasto_hormiga")) {
      const val = Number(r[7]) || 0; total += val;
      detalles.push({ id: String(r[0]), descripcion: String(r[6] || ""), valor: val, fecha: formatFecha(r[1]) });
    }
  }
  return { totalFugasHormiga: total, cantidadRegistros: detalles.length, detalles: detalles };
}

function registrarNuevoMovimiento(ss, data) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const m = data.movimiento || data;
  const now = new Date();
  const idStr = m.id || "M-" + now.getTime();
  sheetMov.appendRow([
    idStr, m.fecha || now.toISOString().split('T')[0], m.hora || now.toTimeString().split(' ')[0],
    m.tipo || "EGRESO", m.categoria || "General", m.subcategoria || "", m.descripcion || m.concepto || "",
    Number(m.valor || m.monto) || 0, m.metodo || "Efectivo", m.cuenta || "Efectivo",
    m.mes || getMesNombre(now.getMonth()), m.ano || String(now.getFullYear()), "Android App", m.referencia || m.observaciones || ""
  ]);
  return { success: true, message: "Movimiento registrado", id: idStr };
}

function registrarPagoGastoFijo(ss, data) {
  const sheetFijos = getOrCreateSheet(ss, HOJA_CONTROL_FIJOS);
  const rows = sheetFijos.getDataRange().getValues();
  const idFijo = data.idFijo || data.id;
  const now = new Date();
  const mesAno = data.periodo || (getMesNombre(now.getMonth()) + " " + now.getFullYear());
  const fechaHoy = now.toISOString().split('T')[0];
  let encontrado = false, concepto = "Gasto Fijo", valor = Number(data.monto) || 0;
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(idFijo)) {
      concepto = String(rows[i][1]); if (valor === 0) valor = Number(rows[i][4]) || 0;
      sheetFijos.getRange(i + 1, 6).setValue(mesAno);
      sheetFijos.getRange(i + 1, 7).setValue(fechaHoy);
      encontrado = true; break;
    }
  }
  const tagRef = "PAGO_FIJO|" + idFijo + "|" + mesAno;
  registrarNuevoMovimiento(ss, {
    id: "M-" + now.getTime(), fecha: fechaHoy, hora: now.toTimeString().split(' ')[0], tipo: "EGRESO",
    categoria: "Servicios/Fijos", descripcion: "Pago Cuota: " + concepto, valor: valor,
    metodo: data.metodoPago || "Nequi", cuenta: data.cuenta || "Nequi", origen: "Android App", referencia: tagRef
  });
  return { success: true, message: "Pago registrado", idFijo: idFijo, tag: tagRef };
}

function borrarUltimoConjunto(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const lastRow = sheetMov.getLastRow();
  if (lastRow <= 1) return { success: false, message: "No hay movimientos" };
  const values = sheetMov.getRange(lastRow, 1, 1, 14).getValues()[0];
  const ref = String(values[13] || "");
  if (ref.startsWith("PAGO_FIJO|")) {
    const p = ref.split("|"); if (p.length >= 2) revertirEstadoGastoFijo(ss, p[1]);
  }
  sheetMov.deleteRow(lastRow);
  return { success: true, message: "Último movimiento eliminado", idEliminado: values[0] };
}

function revertirEstadoGastoFijo(ss, idFijo) {
  const sheetFijos = getOrCreateSheet(ss, HOJA_CONTROL_FIJOS);
  const rows = sheetFijos.getDataRange().getValues();
  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(idFijo)) {
      sheetFijos.getRange(i + 1, 6).setValue("");
      sheetFijos.getRange(i + 1, 7).setValue("");
      break;
    }
  }
}

function procesarSincronizacionCompleta(ss, data) {
  if (data.movimientos && Array.isArray(data.movimientos)) {
    const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
    sheetMov.clearContents();
    sheetMov.appendRow(["ID", "Fecha", "Hora", "Tipo", "Categoría", "Subcategoría", "Descripción", "Valor", "Método", "Cuenta", "Mes", "Año", "Origen", "Referencia"]);
    data.movimientos.forEach(m => {
      sheetMov.appendRow([
        m.id || "", m.fecha || "", m.hora || "", m.tipo || "EGRESO", m.categoria || "", m.subcategoria || "",
        m.descripcion || m.concepto || "", Number(m.valor || m.monto) || 0, m.metodo || "", m.cuenta || "Efectivo",
        m.mes || "", m.ano || "", "Android App", m.referencia || m.observaciones || ""
      ]);
    });
  }
  if (data.gastosFijos && Array.isArray(data.gastosFijos)) {
    const sheetFijos = getOrCreateSheet(ss, HOJA_CONTROL_FIJOS);
    sheetFijos.clearContents();
    sheetFijos.appendRow(["ID", "Concepto", "Categoría", "Día de cobro", "Valor mensual", "Último mes pagado", "Fecha último pago", "Notas/Deuda"]);
    data.gastosFijos.forEach(f => {
      sheetFijos.appendRow([
        f.id || "", f.concepto || "", f.categoria || "", Number(f.diaCobro || f.diaPago) || 1,
        Number(f.valorMensual || f.monto) || 0, f.ultimoMesPagado || "", f.fechaUltimoPago || "", f.notasDeuda || ""
      ]);
    });
  }
  if (data.metasAhorro && Array.isArray(data.metasAhorro)) {
    const sheetMetas = getOrCreateSheet(ss, HOJA_METAS);
    sheetMetas.clearContents();
    sheetMetas.appendRow(["ID", "Nombre", "MontoObjetivo", "MontoActual", "Categoría"]);
    data.metasAhorro.forEach(g => {
      sheetMetas.appendRow([g.id, g.name, g.targetAmount, g.currentAmount, g.category || 'General']);
    });
  }
  return { status: "success", message: "Sincronización exitosa", syncedCount: data.movimientos ? data.movimientos.length : 0 };
}

function obtenerResumenFinancieroPuro(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  let ing = 0, gas = 0;
  for (let i = 1; i < rows.length; i++) {
    const r = rows[i]; if (!r[0] || String(r[9] || "").includes(EXCLUIR_CUENTA)) continue;
    const t = String(r[3] || "").toUpperCase(), v = Number(r[7]) || 0;
    if (t === "INGRESO") ing += v; else if (t === "EGRESO" || t === "GASTO") gas += v;
  }
  return { balance: ing - gas, ingresos: ing, gastos: gas };
}

function getOrCreateSheet(ss, name) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) sheet = ss.insertSheet(name);
  return sheet;
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

function formatFecha(val) {
  if (!val) return "";
  if (val instanceof Date) return val.toISOString().split('T')[0];
  return String(val);
}

function getMesNombre(mesIdx) {
  const meses = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
  return meses[mesIdx] || "Enero";
}
        """.trimIndent()
    }
}
