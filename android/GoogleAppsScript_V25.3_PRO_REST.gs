/**
 * ============================================================================
 * GOOGLE APPS SCRIPT V25.3 PRO REST - PARDOFINANZAS / FINANZAS PRO (2026)
 * ============================================================================
 * Edición con Extensión REST API (doGet y doPost) para sincronización con
 * Android App, Telegram Bot, Gmail Parser y Google Sheets.
 *
 * Estructuras de Hojas:
 * - MOVIMIENTOS (14 cols): ID, Fecha, Hora, Tipo, Categoría, Subcategoría, Descripción, Valor, Método, Cuenta, Mes, Año, Origen, Referencia
 * - CONTROL FIJOS (8 cols): ID, Concepto, Categoría, Día de cobro, Valor mensual, Último mes pagado, Fecha último pago, Notas/Deuda
 * - PANEL, PRESUPUESTO Y ALERTAS, REGLAS COMERCIOS, CUENTAS POR COBRAR, METAS AHORRO
 */

// Constantes de Hojas
const HOJA_MOVIMIENTOS = "MOVIMIENTOS";
const HOJA_CONTROL_FIJOS = "CONTROL FIJOS";
const HOJA_PANEL = "PANEL";
const HOJA_PRESUPUESTO = "PRESUPUESTO Y ALERTAS";
const HOJA_REGLAS = "REGLAS COMERCIOS";
const HOJA_CXC = "CUENTAS POR COBRAR";
const HOJA_METAS = "METAS AHORRO";

// Regla de Exclusión
const EXCLUIR_CUENTA = "Clara Aide";

/**
 * Handler HTTP GET para REST API
 */
function doGet(e) {
  try {
    const params = (e && e.parameter) ? e.parameter : {};
    const accion = params.accion || "resumen";
    const ss = SpreadsheetApp.getActiveSpreadsheet();

    switch (accion) {
      case "resumen":
        return jsonResponse(obtenerResumenFinanciero(ss));
      case "movimientos":
        return jsonResponse(obtenerListaMovimientos(ss));
      case "fijos":
        return jsonResponse(obtenerListaGastosFijos(ss));
      case "disponible":
        return jsonResponse(obtenerDisponibleReal(ss));
      case "fugas":
        return jsonResponse(obtenerGastosHormigaFugas(ss));
      default:
        return jsonResponse({
          status: "ok",
          message: "Google Apps Script V25.3 PRO REST activo",
          accion: accion
        });
    }
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

/**
 * Handler HTTP POST para REST API
 */
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
      case "nuevo_movimiento":
        return jsonResponse(registrarNuevoMovimiento(ss, body));
      case "pagar_fijo":
        return jsonResponse(registrarPagoGastoFijo(ss, body));
      case "borrar_ultimo":
        return jsonResponse(borrarUltimoConjunto(ss));
      case "sync":
      default:
        return jsonResponse(procesarSincronizacionCompleta(ss, body));
    }
  } catch (err) {
    return jsonResponse({ status: "error", message: err.toString() });
  }
}

// ----------------------------------------------------------------------------
// FUNCIONES CONSULTA (GET)
// ----------------------------------------------------------------------------

function obtenerResumenFinanciero(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  let totalIngresos = 0;
  let totalGastos = 0;

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i];
    if (!row[0] || row[0] === "") continue;

    const tipo = String(row[3] || "").toUpperCase();
    const cuenta = String(row[9] || "");
    const valor = Number(row[7]) || 0;

    if (cuenta.includes(EXCLUIR_CUENTA)) continue;

    if (tipo === "INGRESO") {
      totalIngresos += valor;
    } else if (tipo === "EGRESO" || tipo === "GASTO") {
      totalGastos += valor;
    }
  }

  const balance = totalIngresos - totalGastos;
  const disponible = obtenerDisponibleRealMonto(ss);

  return {
    balance: balance,
    disponible: disponible,
    ingresos: totalIngresos,
    gastos: totalGastos
  };
}

function obtenerListaMovimientos(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  const resultado = [];

  for (let i = 1; i < rows.length; i++) {
    const r = rows[i];
    if (!r[0] || r[0] === "") continue;

    const cuenta = String(r[9] || "");
    if (cuenta.includes(EXCLUIR_CUENTA)) continue;

    resultado.push({
      id: String(r[0]),
      fecha: formatFecha(r[1]),
      hora: String(r[2] || ""),
      tipo: String(r[3] || "EGRESO"),
      categoria: String(r[4] || ""),
      subcategoria: String(r[5] || ""),
      descripcion: String(r[6] || ""),
      valor: Number(r[7]) || 0,
      metodo: String(r[8] || ""),
      cuenta: cuenta,
      mes: String(r[10] || ""),
      ano: String(r[11] || ""),
      origen: String(r[12] || "Android App"),
      referencia: String(r[13] || "")
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
    if (!r[0] || r[0] === "") continue;

    resultado.push({
      id: String(r[0]),
      concepto: String(r[1] || ""),
      categoria: String(r[2] || ""),
      diaCobro: Number(r[3]) || 1,
      valorMensual: Number(r[4]) || 0,
      ultimoMesPagado: String(r[5] || ""),
      fechaUltimoPago: formatFecha(r[6]),
      notasDeuda: String(r[7] || "")
    });
  }
  return resultado;
}

function obtenerDisponibleRealMonto(ss) {
  const resumen = obtenerResumenFinancieroPuro(ss);
  const fijos = obtenerListaGastosFijos(ss);

  const fechaActual = new Date();
  const mesAnoActual = getMesNombre(fechaActual.getMonth()) + " " + fechaActual.getFullYear();

  let comprometidoFijos = 0;
  fijos.forEach(fijo => {
    if (fijo.ultimoMesPagado !== mesAnoActual) {
      comprometidoFijos += fijo.valorMensual;
    }
  });

  return Math.max(0, resumen.balance - comprometidoFijos);
}

function obtenerDisponibleReal(ss) {
  const disponible = obtenerDisponibleRealMonto(ss);
  return {
    disponibleReal: disponible,
    fechaConsulta: new Date().toISOString()
  };
}

function obtenerGastosHormigaFugas(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  let totalHormiga = 0;
  const detalles = [];

  for (let i = 1; i < rows.length; i++) {
    const r = rows[i];
    if (!r[0] || r[0] === "") continue;

    const cat = String(r[4] || "").toLowerCase();
    const subcat = String(r[5] || "").toLowerCase();
    const ref = String(r[13] || "").toLowerCase();

    if (cat.includes("hormiga") || subcat.includes("hormiga") || ref.includes("gasto_hormiga")) {
      const val = Number(r[7]) || 0;
      totalHormiga += val;
      detalles.push({
        id: String(r[0]),
        descripcion: String(r[6] || ""),
        valor: val,
        fecha: formatFecha(r[1])
      });
    }
  }

  return {
    totalFugasHormiga: totalHormiga,
    cantidadRegistros: detalles.length,
    detalles: detalles
  };
}

// ----------------------------------------------------------------------------
// FUNCIONES ESCRITURA Y OPERACIONES (POST)
// ----------------------------------------------------------------------------

function registrarNuevoMovimiento(ss, data) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const m = data.movimiento || data;

  const now = new Date();
  const fechaStr = m.fecha || now.toISOString().split('T')[0];
  const horaStr = m.hora || now.toTimeString().split(' ')[0];
  const mesStr = m.mes || getMesNombre(now.getMonth());
  const anoStr = m.ano || String(now.getFullYear());
  const idStr = m.id || "M-" + now.getTime();
  const origenStr = m.origen || "Android App";
  const refStr = m.referencia || m.observaciones || "";

  sheetMov.appendRow([
    idStr,
    fechaStr,
    horaStr,
    m.tipo || "EGRESO",
    m.categoria || "General",
    m.subcategoria || "",
    m.descripcion || m.concepto || "",
    Number(m.valor || m.monto) || 0,
    m.metodo || "Efectivo",
    m.cuenta || "Efectivo",
    mesStr,
    anoStr,
    origenStr,
    refStr
  ]);

  actualizarPanelResumen(ss);

  return {
    success: true,
    message: "Movimiento registrado exitosamente",
    id: idStr
  };
}

function registrarPagoGastoFijo(ss, data) {
  const sheetFijos = getOrCreateSheet(ss, HOJA_CONTROL_FIJOS);
  const fijosRows = sheetFijos.getDataRange().getValues();
  const idFijo = data.idFijo || data.id;
  const now = new Date();
  const mesAnoActual = data.periodo || (getMesNombre(now.getMonth()) + " " + now.getFullYear());
  const fechaHoy = now.toISOString().split('T')[0];

  let encontrado = false;
  let concepto = "Gasto Fijo";
  let valor = Number(data.monto) || 0;

  for (let i = 1; i < fijosRows.length; i++) {
    if (String(fijosRows[i][0]) === String(idFijo)) {
      concepto = String(fijosRows[i][1]);
      if (valor === 0) valor = Number(fijosRows[i][4]) || 0;

      sheetFijos.getRange(i + 1, 6).setValue(mesAnoActual);
      sheetFijos.getRange(i + 1, 7).setValue(fechaHoy);
      encontrado = true;
      break;
    }
  }

  const tagRef = "PAGO_FIJO|" + idFijo + "|" + mesAnoActual;
  registrarNuevoMovimiento(ss, {
    id: "M-" + now.getTime(),
    fecha: fechaHoy,
    hora: now.toTimeString().split(' ')[0],
    tipo: "EGRESO",
    categoria: "Servicios/Fijos",
    descripcion: "Pago Cuota: " + concepto,
    valor: valor,
    metodo: data.metodoPago || "Nequi",
    cuenta: data.cuenta || "Nequi",
    origen: "Android App",
    referencia: tagRef
  });

  return {
    success: true,
    message: encontrado ? "Pago de gasto fijo registrado correctamente" : "Gasto registrado, pero ID fijo no encontrado",
    idFijo: idFijo,
    tag: tagRef
  };
}

function borrarUltimoConjunto(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const lastRow = sheetMov.getLastRow();

  if (lastRow <= 1) {
    return { success: false, message: "No hay movimientos para revertir" };
  }

  const rowValues = sheetMov.getRange(lastRow, 1, 1, 14).getValues()[0];
  const idEliminado = rowValues[0];
  const refEliminado = String(rowValues[13] || "");

  if (refEliminado.startsWith("PAGO_FIJO|")) {
    const partes = refEliminado.split("|");
    if (partes.length >= 2) {
      const idFijo = partes[1];
      revertirEstadoGastoFijo(ss, idFijo);
    }
  }

  sheetMov.deleteRow(lastRow);
  actualizarPanelResumen(ss);

  return {
    success: true,
    message: "Último movimiento eliminado de forma atómica",
    idEliminado: idEliminado
  };
}

function borrarRegistroCompletoPorID(ss, idTarget) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();

  for (let i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(idTarget)) {
      const ref = String(rows[i][13] || "");
      if (ref.startsWith("PAGO_FIJO|")) {
        const partes = ref.split("|");
        if (partes.length >= 2) revertirEstadoGastoFijo(ss, partes[1]);
      }
      sheetMov.deleteRow(i + 1);
      actualizarPanelResumen(ss);
      return { success: true, message: "Registro " + idTarget + " eliminado" };
    }
  }
  return { success: false, message: "ID no encontrado: " + idTarget };
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
    sheetMov.appendRow([
      "ID", "Fecha", "Hora", "Tipo", "Categoría", "Subcategoría",
      "Descripción", "Valor", "Método", "Cuenta", "Mes", "Año", "Origen", "Referencia"
    ]);

    data.movimientos.forEach(m => {
      sheetMov.appendRow([
        m.id || "",
        m.fecha || "",
        m.hora || "",
        m.tipo || "EGRESO",
        m.categoria || "",
        m.subcategoria || "",
        m.descripcion || m.concepto || "",
        Number(m.valor || m.monto) || 0,
        m.metodo || "",
        m.cuenta || "Efectivo",
        m.mes || "",
        m.ano || "",
        "Android App",
        m.referencia || m.observaciones || ""
      ]);
    });
  }

  if (data.gastosFijos && Array.isArray(data.gastosFijos)) {
    const sheetFijos = getOrCreateSheet(ss, HOJA_CONTROL_FIJOS);
    sheetFijos.clearContents();
    sheetFijos.appendRow([
      "ID", "Concepto", "Categoría", "Día de cobro", "Valor mensual",
      "Último mes pagado", "Fecha último pago", "Notas/Deuda"
    ]);

    data.gastosFijos.forEach(f => {
      sheetFijos.appendRow([
        f.id || "",
        f.concepto || "",
        f.categoria || "",
        Number(f.diaCobro || f.diaPago) || 1,
        Number(f.valorMensual || f.monto) || 0,
        f.ultimoMesPagado || "",
        f.fechaUltimoPago || "",
        f.notasDeuda || ""
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

  actualizarPanelResumen(ss);

  return {
    status: "success",
    message: "Sincronización masiva completada exitosamente",
    syncedCount: data.movimientos ? data.movimientos.length : 0
  };
}

// ----------------------------------------------------------------------------
// AUXILIARES
// ----------------------------------------------------------------------------

function actualizarPanelResumen(ss) {
  const sheetPanel = getOrCreateSheet(ss, HOJA_PANEL);
  const res = obtenerResumenFinanciero(ss);

  sheetPanel.getRange("A1:B5").setValues([
    ["Métrica", "Valor"],
    ["Ingresos Totales", res.ingresos],
    ["Gastos Totales", res.gastos],
    ["Balance Consolidado", res.balance],
    ["Disponible Real", res.disponible]
  ]);
}

function obtenerResumenFinancieroPuro(ss) {
  const sheetMov = getOrCreateSheet(ss, HOJA_MOVIMIENTOS);
  const rows = sheetMov.getDataRange().getValues();
  let totalIngresos = 0;
  let totalGastos = 0;

  for (let i = 1; i < rows.length; i++) {
    const r = rows[i];
    if (!r[0] || r[0] === "") continue;
    if (String(r[9] || "").includes(EXCLUIR_CUENTA)) continue;

    const tipo = String(r[3] || "").toUpperCase();
    const val = Number(r[7]) || 0;
    if (tipo === "INGRESO") totalIngresos += val;
    else if (tipo === "EGRESO" || tipo === "GASTO") totalGastos += val;
  }
  return { balance: totalIngresos - totalGastos, ingresos: totalIngresos, gastos: totalGastos };
}

function getOrCreateSheet(ss, name) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) sheet = ss.insertSheet(name);
  return sheet;
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
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
