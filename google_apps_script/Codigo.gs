/************************************************************
 * MIS FINANZAS - BOT DE FINANZAS PERSONALES & API WEB PRO
 * VERSIÓN V26.8 OFICIAL - ALINEADA 100% CON Panel_Finanzas.xlsx
 * 
 * Sincronizado exactamente con las hojas de cálculo:
 * - PANEL (Celdas B6, E6, H6, K6, H16, I32, J39, J40, J41)
 * - MOVIMIENTOS (14 columnas + Col O formula _IndexFiltro)
 * - HISTORICO FIJOS & CONTROL FIJOS (Sincronización quincenal Q1/Q2)
 * - AHORROS (Fondos de Ahorro Nómina Colanta)
 * - API REST ultrarrápida (doGet / doPost) para Dashboard Web y Telegram
 ************************************************************/

const TELEGRAM_TOKEN = String(PropertiesService.getScriptProperties().getProperty("TELEGRAM_TOKEN") || "").trim();
const SPREADSHEET_ID = String(PropertiesService.getScriptProperties().getProperty("SPREADSHEET_ID") || "1C9ifm7U8IYGOdY4IwAji9Z3McSlNy3B-e_2Jo-zvBFk").trim();
const WEBHOOK_URL = String(PropertiesService.getScriptProperties().getProperty("WEBHOOK_URL") || "").trim();
const GEMINI_API_KEY = String(PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") || "").trim();
const USUARIO_NOMBRE_COMPLETO = String(PropertiesService.getScriptProperties().getProperty("USUARIO_NOMBRE_COMPLETO") || "MARLON MUNOZ PARDO").trim();
const ZONA_HORARIA = "America/Bogota";

function obtenerLibro_() {
  let ss = null;
  if (SPREADSHEET_ID && SPREADSHEET_ID.trim().length > 10) {
    try { ss = SpreadsheetApp.openById(SPREADSHEET_ID.trim()); } catch (e) {}
  }
  if (!ss) {
    try { ss = SpreadsheetApp.getActiveSpreadsheet(); } catch (e) {}
  }
  if (!ss) throw new Error("No se pudo conectar con el libro Google Sheets.");
  return ss;
}

/* =========================================================
   API WEB (doGet & doPost) — TIEMPO DE RESPUESTA < 0.3s
   ========================================================= */
function doGet(e) {
  const params = e && e.parameter ? e.parameter : {};
  const action = params.action || "getData";
  let output = { ok: true, timestamp: new Date().toISOString() };

  try {
    const ss = obtenerLibro_();
    
    if (action === "getData" || action === "getSummary") {
      const shPanel = ss.getSheetByName("PANEL");
      const shMov = ss.getSheetByName("MOVIMIENTOS");
      const shFijos = ss.getSheetByName("CONTROL FIJOS");
      const shAhorros = ss.getSheetByName("AHORROS");

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

      // Gastos por categoría y últimos movimientos
      const gastosPorCategoria = {};
      const ultimosMovs = [];

      if (shMov && shMov.getLastRow() >= 5) {
        const numRows = Math.min(100, shMov.getLastRow() - 4);
        const data = shMov.getRange(5, 1, numRows, 14).getValues();

        for (let i = 0; i < data.length; i++) {
          const r = data[i];
          const tipo = String(r[3] || "").toLowerCase();
          const cat = String(r[4] || "Otros").trim();
          const val = Number(r[7] || 0);
          const mes = String(r[10] || "").toLowerCase();
          const anio = Number(r[11] || anioActivo);

          if (tipo === "gasto" && val > 0 && (!mes || mes === mesActivo) && anio === anioActivo) {
            gastosPorCategoria[cat] = (gastosPorCategoria[cat] || 0) + val;
          }

          if (i < 25) {
            ultimosMovs.push({
              id: r[0],
              fecha: r[1] instanceof Date ? Utilities.formatDate(r[1], ZONA_HORARIA, "dd/MM/yyyy") : String(r[1] || ""),
              hora: r[2],
              tipo: r[3],
              categoria: r[4],
              subcategoria: r[5],
              descripcion: r[6],
              valor: val,
              metodo: r[8],
              cuenta: r[9]
            });
          }
        }
      }

      // Compromisos Fijos de CONTROL FIJOS
      const fijosList = [];
      if (shFijos && shFijos.getLastRow() >= 10) {
        const numFijos = shFijos.getLastRow() - 9;
        const fRows = shFijos.getRange(10, 1, numFijos, 8).getValues();
        fRows.forEach(r => {
          if (r[0] && String(r[0]).startsWith("F-")) {
            fijosList.push({
              id: r[0],
              concepto: r[1],
              categoria: r[2],
              dia: Number(r[3] || 15),
              cuota: Number(r[4] || 0),
              abonado: Number(r[5] || 0),
              estado: r[6] || "Pendiente"
            });
          }
        });
      }

      output = {
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
          total: totalLiquidez
        },
        gastosPorCategoria: gastosPorCategoria,
        movimientos: ultimosMovs,
        fijos: fijosList
      };
    } else if (action === "ping") {
      output = { ok: true, mensaje: "API PardoFinanzas en línea y lista." };
    }
  } catch (err) {
    output = { ok: false, error: err.message };
  }

  return ContentService.createTextOutput(JSON.stringify(output))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
  }

  try {
    const data = JSON.parse(e.postData.contents);

    // 1. Webhook de Telegram
    if (data.update_id) {
      if (data.message) procesarGrupoMensajes(data.message.chat.id, [data]);
      else if (data.callback_query) procesarCallbackTelegram(data.callback_query);
      return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
    }

    // 2. Registro desde el Dashboard Web (Formulario o Chat)
    if (data.action === "addMovement" || data.tipo) {
      const ss = obtenerLibro_();
      const shMov = ss.getSheetByName("MOVIMIENTOS");
      if (!shMov) throw new Error("Hoja MOVIMIENTOS no encontrada");

      const d = new Date();
      const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
      const mesActual = meses[d.getMonth()];
      const anioActual = d.getFullYear();

      // Generar ID consecutivo M-xxxx
      let maxNum = 255;
      if (shMov.getLastRow() >= 5) {
        const primerId = String(shMov.getRange(5, 1).getValue() || "");
        const match = primerId.match(/^M-(\d+)$/);
        if (match) maxNum = Math.max(maxNum, parseInt(match[1], 10));
      }
      const nuevoId = "M-" + String(maxNum + 1).padStart(4, "0");

      const fechaStr = Utilities.formatDate(d, ZONA_HORARIA, "dd/MM/yyyy");
      const horaStr = Utilities.formatDate(d, ZONA_HORARIA, "hh:mm a");

      const filaMov = [
        nuevoId,
        fechaStr,
        horaStr,
        data.tipo || "Gasto",
        data.categoria || "Otros",
        data.subcategoria || "General",
        data.descripcion || "Movimiento registrado desde Dashboard",
        Number(data.valor || 0),
        data.metodo || (data.cuenta === "Efectivo" ? "Efectivo" : "Transferencia"),
        data.cuenta || "Nequi",
        data.mes || mesActual,
        data.anio || anioActual,
        data.origen || "Dashboard Web",
        data.notas || "WEB_CHAT"
      ];

      // Inserción exacta en Fila 5 (respetando encabezados en fila 4)
      shMov.insertRowBefore(5);
      shMov.getRange(5, 1, 1, filaMov.length).setValues([filaMov]);

      // ¡REGLA DE ORO DE Panel_Finanzas.xlsx!: Columna O (15) fórmula _IndexFiltro
      shMov.getRange(5, 15).setFormula('=IF(ISBLANK(A5), "", IF(AND(LOWER(TRIM(K5))=LOWER(TRIM(PANEL!$L$2)), L5=PANEL!$I$2), MAX($O$4:O4)+1, ""))');

      // Si es un abono a gasto fijo, actualizar HISTORICO FIJOS
      if (data.idFijo && data.tipo === "Gasto") {
        actualizarHistoricoFijos_(ss, data.idFijo, Number(data.valor || 0), mesActual, anioActual);
      }

      return ContentService.createTextOutput(JSON.stringify({
        ok: true,
        id: nuevoId,
        movimiento: filaMov
      })).setMimeType(ContentService.MimeType.JSON);
    }

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
}

function actualizarHistoricoFijos_(ss, idFijo, valorAbono, mes, anio) {
  const shHist = ss.getSheetByName("HISTORICO FIJOS");
  if (!shHist || shHist.getLastRow() < 4) return;
  const numRows = shHist.getLastRow() - 3;
  const hRows = shHist.getRange(4, 1, numRows, 11).getValues();

  for (let i = 0; i < hRows.length; i++) {
    const rowId = String(hRows[i][0] || "").trim().toUpperCase();
    const rowMes = String(hRows[i][9] || "").trim().toLowerCase();
    const rowAnio = Number(hRows[i][10] || 0);

    if (rowId === idFijo.toUpperCase() && rowMes === mes.toLowerCase() && rowAnio === anio) {
      const prevAbono = Number(hRows[i][5] || 0);
      const cuota = Number(hRows[i][4] || 0);
      const nuevoAbono = prevAbono + valorAbono;
      shHist.getRange(i + 4, 6).setValue(nuevoAbono);
      shHist.getRange(i + 4, 7).setValue(Utilities.formatDate(new Date(), ZONA_HORARIA, "yyyy-MM-dd"));
      shHist.getRange(i + 4, 8).setValue(nuevoAbono >= cuota ? "🟢 Pagado" : "🟡 Abono Parcial");
      break;
    }
  }
}
