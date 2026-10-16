# -*- coding: utf-8 -*-
"""
Script que unifica el Bot de Telegram + Escáner Gmail con la API Web del Dashboard
y el módulo de procesamiento de Colillas con Gemini.
"""

import os
import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

def main():
    root = Path(__file__).resolve().parent.parent
    colilla_path = root / "colilla_original_full.txt"
    with open(colilla_path, "r", encoding="utf-8") as f:
        orig = f.read()

    pos_doPost = orig.find("function doPost(e) {")
    pos_after_doGet = orig.find("/* =========================================================\n\n   ACTIVADORES GLOBALES", pos_doPost)
    if pos_after_doGet == -1:
        pos_after_doGet = orig.find("/* =========================================================", pos_doPost + 500)

    nuevo_bloque = """/* =========================================================
   API WEB (doGet & doPost) — CONEXIÓN DASHBOARD & TELEGRAM V26.9
   ========================================================= */

function doGet(e) {
  const params = e && e.parameter ? e.parameter : {};
  const action = params.action || "ping";

  if (action === "getData" || action === "getSummary") {
    try {
      const data = obtenerDatosCompletosDashboard_();
      return ContentService.createTextOutput(JSON.stringify(data))
        .setMimeType(ContentService.MimeType.JSON);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "ping") {
    return ContentService.createTextOutput(JSON.stringify({ 
      ok: true, 
      version: "V26.9 PRO AI", 
      timestamp: new Date().toISOString(),
      mensaje: "API PardoFinanzas y Bot de Telegram en línea y sincronizados."
    })).setMimeType(ContentService.MimeType.JSON);
  }

  return ContentService.createTextOutput("Bot de finanzas V26.9 PRO activo y sincronizado con Dashboard Web");
}

function doPost(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
  }

  try {
    const rawContents = e.postData.contents;
    const data = JSON.parse(rawContents);

    // =========================================================
    // 🌐 1. OPERACIONES DESDE EL DASHBOARD WEB
    // =========================================================
    if (data.action === "addMovement" || (data.tipo && !data.update_id)) {
      return registrarMovimientoDesdeDashboard_(data);
    }

    if (data.action === "procesarColilla") {
      return procesarColillaDashboard_(data);
    }

    if (data.action === "testConnection") {
      const ss = obtenerLibro_();
      const shMov = ss.getSheetByName("MOVIMIENTOS");
      return ContentService.createTextOutput(JSON.stringify({
        ok: true,
        nombreLibro: ss.getName(),
        filasMovimientos: shMov ? shMov.getLastRow() : 0,
        fechaServidor: new Date().toISOString()
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // =========================================================
    // 📱 2. OPERACIONES DESDE BOT DE TELEGRAM
    // =========================================================
    const update = data;
    const cache = CacheService.getScriptCache();
    if (update.update_id) {
      const keyUpdate = "TG_UPD_" + update.update_id;
      const propsWh = PropertiesService.getScriptProperties();
      const ultimoWh = Number(propsWh.getProperty("TG_ULTIMO_UPDATE_WEBHOOK") || 0);

      if (cache.get(keyUpdate) || (ultimoWh && update.update_id <= ultimoWh)) {
        return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
      }
      try { cache.put(keyUpdate, "1", 21600); } catch (errC) {}
      try { propsWh.setProperty("TG_ULTIMO_UPDATE_WEBHOOK", String(update.update_id)); } catch (errP) {}
    }

    if (update.callback_query) {
      procesarCallbackTelegram(update.callback_query);
    } else if (update.message) {
      const chatId = update.message.chat.id;
      try {
        procesarGrupoMensajes(chatId, [update]);
      } catch (errProc) {
        console.error("Error en procesarGrupoMensajes:", errProc);
        try {
          enviarMensajeTelegram(chatId, "⚠️ *Aviso del Sistema:* " + errProc.message);
        } catch (eSend) {}
      }
    }
  } catch (err) {
    console.error("Error en doPost:", err);
    return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
}

/* =========================================================
   FUNCIONES AUXILIARES DEL DASHBOARD WEB V26.9
   ========================================================= */

function registrarMovimientoDesdeDashboard_(data) {
  const ss = obtenerLibro_();
  const shMov = ss.getSheetByName("MOVIMIENTOS");
  if (!shMov) throw new Error("No se encontró la hoja MOVIMIENTOS en Panel_Finanzas.");

  const d = new Date();
  const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
  const mesActual = meses[d.getMonth()];
  const anioActual = d.getFullYear();

  // Generar ID consecutivo M-xxxx respetando el último registro
  let maxNum = 255;
  if (shMov.getLastRow() >= 5) {
    const idsRange = shMov.getRange(5, 1, Math.min(20, shMov.getLastRow() - 4), 1).getValues();
    idsRange.forEach(row => {
      const m = String(row[0] || "").match(/^M-(\\d+)$/);
      if (m) maxNum = Math.max(maxNum, parseInt(m[1], 10));
    });
  }
  const nuevoId = "M-" + String(maxNum + 1).padStart(4, "0");

  const fechaStr = Utilities.formatDate(d, ZONA_HORARIA, "yyyy-MM-dd");
  const horaStr = Utilities.formatDate(d, ZONA_HORARIA, "hh:mm a");

  const tipo = data.tipo || "Gasto";
  const valor = Number(data.valor || data.monto || 0);
  const descripcion = String(data.descripcion || "Movimiento Dashboard").trim();
  const categoria = String(data.categoria || (tipo === "Gasto" ? "Otros Gastos" : "Otros Ingresos")).trim();
  const subcategoria = String(data.subcategoria || "General").trim();
  const cuenta = String(data.cuenta || "Nequi").trim();
  const metodo = String(data.metodo || (cuenta === "Efectivo" ? "Efectivo" : "Transferencia")).trim();

  const filaMov = [
    nuevoId,
    fechaStr,
    horaStr,
    tipo,
    categoria,
    subcategoria,
    descripcion,
    valor,
    metodo,
    cuenta,
    mesActual,
    anioActual,
    data.origen || "Dashboard Web",
    data.notas || "CFO_AI_CHAT"
  ];

  // Inserción exacta en Fila 5 (inmediatamente debajo del encabezado en fila 4)
  shMov.insertRowBefore(5);
  shMov.getRange(5, 1, 1, filaMov.length).setValues([filaMov]);

  // Aplicar fórmula de filtrado en Columna O (_IndexFiltro)
  shMov.getRange(5, 15).setFormula('=IF(ISBLANK(A5), "", IF(AND(LOWER(TRIM(K5))=LOWER(TRIM(PANEL!$L$2)), L5=PANEL!$I$2), MAX($O$4:O4)+1, ""))');

  // Si corresponde a un gasto fijo, registrar abono en HISTORICO FIJOS
  if (data.idFijo && tipo === "Gasto") {
    try {
      actualizarHistoricoFijos_(ss, data.idFijo, valor, mesActual, anioActual);
    } catch (eFijo) {
      console.warn("Aviso fijos:", eFijo);
    }
  }

  return ContentService.createTextOutput(JSON.stringify({
    ok: true,
    id: nuevoId,
    mensaje: "Movimiento registrado exitosamente en la fila 5 de MOVIMIENTOS",
    fila: 5,
    movimiento: {
      id: nuevoId,
      fecha: fechaStr,
      hora: horaStr,
      tipo: tipo,
      valor: valor,
      descripcion: descripcion,
      categoria: categoria,
      cuenta: cuenta
    }
  })).setMimeType(ContentService.MimeType.JSON);
}

function procesarColillaDashboard_(data) {
  const ss = obtenerLibro_();
  const shCol = ss.getSheetByName("COLILLAS DE PAGO");
  const shAho = ss.getSheetByName("AHORROS");
  const shMov = ss.getSheetByName("MOVIMIENTOS");

  if (!shCol) throw new Error("No se encontró la hoja COLILLAS DE PAGO.");

  const base64Data = data.fileBase64 || data.base64;
  const mimeType = data.mimeType || "application/pdf";
  const fileName = data.fileName || "Colilla_Nomina.pdf";

  if (!base64Data) throw new Error("No se recibió archivo base64 de la colilla.");

  // Prompt estructurado para Gemini multimodal
  const promptAuditoria = 
    "Eres un auditor financiero experto en nóminas de Cooperativa Colanta para Marlon Muñoz Pardo.\\n" +
    "Analiza con máxima precisión este desprendible / colilla de nómina (PDF o imagen).\\n" +
    "Extrae y calcula los valores exactos devolviendo ÚNICAMENTE un objeto JSON válido con esta estructura:\\n" +
    "{\\n" +
    '  "idColilla": "CP-0019",\\n' +
    '  "mes": "octubre",\\n' +
    '  "anio": 2026,\\n' +
    '  "quincena": "Q1",\\n' +
    '  "periodoLiquidado": "01/10/2026 al 15/10/2026",\\n' +
    '  "dias": 15,\\n' +
    '  "basicoQuincenal": 1878342,\\n' +
    '  "extrasRecargos": 320000,\\n' +
    '  "otrosDevengados": 95956,\\n' +
    '  "totalDevengado": 2294298,\\n' +
    '  "salud": 88154,\\n' +
    '  "pension": 88154,\\n' +
    '  "fondoEmpleados": 236121,\\n' +
    '  "otrasDeducciones": 180000,\\n' +
    '  "netoACobrar": 1701869,\\n' +
    '  "ahorroOrdinario": 33000,\\n' +
    '  "ahorroNavideno": 120000,\\n' +
    '  "aportesCooperativos": 37567,\\n' +
    '  "otrosAhorros": 45554,\\n' +
    '  "cuentaConsignacion": "AyC Colanta",\\n' +
    '  "observaciones": "Nómina procesada con Gemini"\\n' +
    "}\\n" +
    "Nota: Solo devuelve el JSON sin formato markdown ni explicaciones adicionales.";

  const respuestaIA = llamarGeminiAPI({
    contents: [{
      parts: [
        { inlineData: { mimeType: mimeType, data: base64Data } },
        { text: promptAuditoria }
      ]
    }],
    generationConfig: { responseMimeType: "application/json", temperature: 0.1 }
  });

  if (!respuestaIA || respuestaIA.error || !respuestaIA.candidates || !respuestaIA.candidates[0].content) {
    throw new Error("Gemini no pudo procesar la colilla: " + (respuestaIA && respuestaIA.error ? respuestaIA.error.message : "Sin respuesta"));
  }

  const jsonText = limpiarJSON(respuestaIA.candidates[0].content.parts[0].text);
  const p = JSON.parse(jsonText);

  // 1. Consecutivo para Colilla CP-xxxx
  let nextCpNum = 19;
  if (shCol.getLastRow() >= 5) {
    const rawIds = shCol.getRange(5, 1, Math.min(10, shCol.getLastRow() - 4), 1).getValues();
    rawIds.forEach(r => {
      const m = String(r[0] || "").match(/^CP-(\\d+)$/);
      if (m) nextCpNum = Math.max(nextCpNum, parseInt(m[1], 10) + 1);
    });
  }
  const idColilla = "CP-" + String(nextCpNum).padStart(4, "0");

  const d = new Date();
  const fechaHoyStr = Utilities.formatDate(d, ZONA_HORARIA, "dd/MM/yyyy");

  // Inserción en COLILLAS DE PAGO (Fila 5)
  const filaColilla = [
    idColilla,
    p.mes || "octubre",
    Number(p.anio || 2026),
    p.quincena || "Q1",
    p.periodoLiquidado || "",
    Number(p.dias || 15),
    Number(p.basicoQuincenal || 0),
    Number(p.extrasRecargos || 0),
    Number(p.otrosDevengados || 0),
    Number(p.totalDevengado || 0),
    Number(p.salud || 0),
    Number(p.pension || 0),
    Number(p.fondoEmpleados || 0),
    Number(p.otrasDeducciones || 0),
    Number(p.netoACobrar || 0),
    p.cuentaConsignacion || "AyC Colanta",
    fechaHoyStr,
    "Pagado",
    p.observaciones || "Procesado desde Dashboard Gemini",
    fileName
  ];

  shCol.insertRowBefore(5);
  shCol.getRange(5, 1, 1, filaColilla.length).setValues([filaColilla]);

  // Inserción en AHORROS si hay retenciones de fondo de empleados
  if (shAho && shAho.getLastRow() >= 7) {
    const ahorrosItems = [
      { tipo: "Ahorro Ordinario", valor: Number(p.ahorroOrdinario || 33000) },
      { tipo: "Ahorro Navideño", valor: Number(p.ahorroNavideno || 120000) },
      { tipo: "Aportes Cooperativos", valor: Number(p.aportesCooperativos || 37567) }
    ];

    if (p.otrosAhorros && Number(p.otrosAhorros) > 0) {
      ahorrosItems.push({ tipo: "Aporte Afiliación Cooperativa", valor: Number(p.otrosAhorros) });
    }

    let nextANum = 61;
    ahorrosItems.forEach(item => {
      if (item.valor > 0) {
        const idAhorro = "A-" + String(nextANum++).padStart(4, "0");
        const filaAhorro = [
          idAhorro,
          fechaHoyStr,
          "Fondo de Empleados Colanta",
          item.tipo,
          item.valor,
          p.mes || "octubre",
          Number(p.anio || 2026),
          "Ref Colilla: " + idColilla + " | " + item.tipo
        ];
        const nextAhoRow = shAho.getLastRow() + 1;
        shAho.getRange(nextAhoRow, 1, 1, filaAhorro.length).setValues([filaAhorro]);
      }
    });
  }

  // Inserción automática del Neto como Ingreso en MOVIMIENTOS
  if (shMov && Number(p.netoACobrar) > 0) {
    registrarMovimientoDesdeDashboard_({
      tipo: "Ingreso",
      valor: Number(p.netoACobrar),
      descripcion: "Nómina Colanta - " + (p.quincena || "Quincena") + " " + (p.mes || ""),
      categoria: "Salario",
      subcategoria: "Nómina Colanta",
      cuenta: p.cuentaConsignacion || "AyC Colanta",
      metodo: "Transferencia / Nómina",
      origen: "Colilla Nómina (Gemini)",
      notas: "Ref Colilla: " + idColilla
    });
  }

  return ContentService.createTextOutput(JSON.stringify({
    ok: true,
    idColilla: idColilla,
    datosExtraidos: p,
    mensaje: "Colilla auditada con Gemini e insertada con éxito en COLILLAS DE PAGO, AHORROS y MOVIMIENTOS."
  })).setMimeType(ContentService.MimeType.JSON);
}

function obtenerDatosCompletosDashboard_() {
  const ss = obtenerLibro_();
  const shPanel = ss.getSheetByName("PANEL");
  const shMov = ss.getSheetByName("MOVIMIENTOS");
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const shAho = ss.getSheetByName("AHORROS");
  const shCol = ss.getSheetByName("COLILLAS DE PAGO");

  let ingreso = 0, gasto = 0, ahorro = 0, disponible = 0, fijosPendientes = 0;
  let saldoColanta = 0, saldoNequi = 0, saldoEfectivo = 0, totalLiquidez = 0;
  let mesActivo = "octubre", anioActivo = 2026;

  if (shPanel) {
    mesActivo = String(shPanel.getRange("L2").getValue() || "octubre").toLowerCase();
    anioActivo = Number(shPanel.getRange("I2").getValue() || 2026);
    ingreso = Number(shPanel.getRange("B6").getValue() || 0);
    gasto = Number(shPanel.getRange("E6").getValue() || 0);
    ahorro = Number(shPanel.getRange("H6").getValue() || 0);
    disponible = Number(shPanel.getRange("K6").getValue() || 0);
    fijosPendientes = Number(shPanel.getRange("H16").getValue() || 0);

    saldoColanta = Number(shPanel.getRange("J39").getValue() || 0);
    saldoNequi = Number(shPanel.getRange("J40").getValue() || 0);
    saldoEfectivo = Number(shPanel.getRange("J41").getValue() || 0);
    totalLiquidez = Number(shPanel.getRange("J42").getValue() || (saldoColanta + saldoNequi + saldoEfectivo));
  }

  const ultimosMovs = [];
  const gastosPorCategoria = {};

  if (shMov && shMov.getLastRow() >= 5) {
    const numRows = Math.min(60, shMov.getLastRow() - 4);
    const data = shMov.getRange(5, 1, numRows, 14).getValues();

    for (let i = 0; i < data.length; i++) {
      const r = data[i];
      if (!r[0]) continue;
      const mTipo = String(r[3] || "");
      const mCat = String(r[4] || "Otros").trim();
      const mVal = Number(r[7] || 0);
      const mMes = String(r[10] || "").toLowerCase();

      if (mTipo.toLowerCase() === "gasto" && mVal > 0 && (!mMes || mMes === mesActivo)) {
        gastosPorCategoria[mCat] = (gastosPorCategoria[mCat] || 0) + mVal;
      }

      if (i < 30) {
        ultimosMovs.push({
          id: r[0],
          fecha: r[1] instanceof Date ? Utilities.formatDate(r[1], ZONA_HORARIA, "dd/MM/yyyy") : String(r[1] || ""),
          hora: r[2] instanceof Date ? Utilities.formatDate(r[2], ZONA_HORARIA, "hh:mm a") : String(r[2] || ""),
          tipo: mTipo,
          categoria: mCat,
          subcategoria: String(r[5] || "General"),
          descripcion: String(r[6] || ""),
          valor: mVal,
          metodo: String(r[8] || ""),
          cuenta: String(r[9] || ""),
          mes: mMes,
          anio: Number(r[11] || anioActivo)
        });
      }
    }
  }

  // Fondos Colanta
  let navidenoAcum = 2160000, ordinarioAcum = 594000, aportesAcum = 995083, otrosAcum = 182216, totalFondos = 3931299;
  if (shAho) {
    try {
      totalFondos = Number(shAho.getRange("A5").getValue() || 3931299);
      navidenoAcum = Number(shAho.getRange("E5").getValue() || 2160000);
      ordinarioAcum = Number(shAho.getRange("C8").getValue() || 594000);
      aportesAcum = Number(shAho.getRange("C10").getValue() || 995083);
    } catch(eAho) {}
  }

  return {
    ok: true,
    resumen: {
      ingresos: ingreso,
      gastos: gasto,
      ahorros: ahorro,
      disponible: totalLiquidez > 0 ? totalLiquidez : disponible,
      fijos_pendientes: fijosPendientes,
      mes: mesActivo,
      anio: anioActivo
    },
    cuentas: {
      aycColanta: saldoColanta,
      nequi: saldoNequi,
      efectivo: saldoEfectivo,
      total: totalLiquidez > 0 ? totalLiquidez : (saldoColanta + saldoNequi + saldoEfectivo)
    },
    ahorrosColanta: {
      ordinarioAcumulado: ordinarioAcum,
      navidenoAcumulado: navidenoAcum,
      aportesAcumulado: aportesAcum,
      otrosAcumulado: otrosAcum,
      totalAcumulado: totalFondos
    },
    gastosPorCategoria: gastosPorCategoria,
    movimientos: ultimosMovs
  };
}
"""

    codigo_completo = orig[:pos_doPost] + nuevo_bloque + orig[pos_after_doGet:]

    out_file = root / "google_apps_script" / "Codigo.gs"
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(codigo_completo)

    print(f"✅ Generado exitosamente {out_file} ({len(codigo_completo)} bytes)")

if __name__ == "__main__":
    main()
