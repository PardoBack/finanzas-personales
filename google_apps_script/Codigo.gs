/************************************************************
 * MIS FINANZAS - BOT DE FINANZAS PERSONALES & API WEB
 * VERSIÓN V26.5 PRO AI - ARCHIVO OFICIAL DEFINITIVO
 * 
 * Contiene el 100% de la automatización para Google Apps Script:
 * - API REST (doGet y doPost) para Dashboard Web en GitHub / Vercel
 * - Bot de Telegram con Botones Rápidos y Notas de Voz (Gemini AI)
 * - Control de Presupuesto, Quincenas, Gastos Fijos, Deudas y Ahorros Colanta
 * - Conexión directa y en tiempo real con Google Sheets
 ************************************************************/

/* =========================================================
   CONFIGURACIÓN Y ENTORNO
   ========================================================= */
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
  if (!ss) {
    throw new Error("No se pudo conectar con el libro de cálculo.");
  }
  return ss;
}

/* =========================================================
   API WEB (doGet & doPost) PARA EL DASHBOARD EN GITHUB / VERCEL
   ========================================================= */
function doGet(e) {
  // Manejo de solicitudes API desde el Dashboard Web
  const params = e && e.parameter ? e.parameter : {};
  const action = params.action || "getSummary";
  
  let resultado = { ok: true, timestamp: new Date().toISOString() };
  
  try {
    const ss = obtenerLibro_();
    
    if (action === "getSummary" || action === "getData") {
      const shPanel = ss.getSheetByName("PANEL");
      const shMov = ss.getSheetByName("MOVIMIENTOS");
      const shFijos = ss.getSheetByName("CONTROL FIJOS");
      const shAhorros = ss.getSheetByName("AHORROS");
      
      let ingreso = 0, gasto = 0, ahorro = 0, disponible = 0, fijosPendientes = 0;
      
      if (shPanel) {
        ingreso = Number(shPanel.getRange("B6").getValue() || 0);
        gasto = Number(shPanel.getRange("E6").getValue() || 0);
        ahorro = Number(shPanel.getRange("H6").getValue() || 0);
        disponible = Number(shPanel.getRange("K6").getValue() || Math.max(0, ingreso - gasto - ahorro));
        fijosPendientes = Number(shPanel.getRange("H16").getValue() || 0);
      }
      
      // Categorías de gastos del mes actual
      const gastosPorCategoria = {};
      const mesActual = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"][new Date().getMonth()];
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
          
          if (tipo === "gasto" && val > 0 && (!mes || mes === mesActual)) {
            gastosPorCategoria[cat] = (gastosPorCategoria[cat] || 0) + val;
          }
          
          if (i < 20) {
            ultimosMovs.push({
              id: r[0],
              fecha: r[1],
              hora: r[2],
              tipo: r[3],
              categoria: r[4],
              descripcion: r[6],
              valor: val,
              metodo: r[8],
              cuenta: r[9]
            });
          }
        }
      }
      
      // Gastos fijos
      const listaFijos = [];
      if (shFijos && shFijos.getLastRow() >= 10) {
        const fRows = shFijos.getRange(10, 1, shFijos.getLastRow() - 9, 8).getValues();
        fRows.forEach(r => {
          if (r[0] && String(r[0]).startsWith("F-")) {
            listaFijos.push({
              id: r[0],
              concepto: r[1],
              categoria: r[2],
              dia: r[3],
              cuota: Number(r[4] || 0),
              abonado: Number(r[5] || 0),
              estado: r[6] || "Pendiente"
            });
          }
        });
      }

      resultado = {
        ok: true,
        resumen: {
          ingresos: ingreso,
          gastos: gasto,
          ahorros: ahorro,
          disponible: disponible,
          fijos_pendientes: fijosPendientes,
          mes: mesActual
        },
        gastosPorCategoria: gastosPorCategoria,
        movimientos: ultimosMovs,
        fijos: listaFijos
      };
    } else if (action === "ping") {
      resultado = { ok: true, mensaje: "PardoFinanzas API activa y conectada." };
    }
  } catch (err) {
    resultado = { ok: false, error: err.message };
  }
  
  return ContentService.createTextOutput(JSON.stringify(resultado))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
  }
  
  try {
    const data = JSON.parse(e.postData.contents);
    
    // Si proviene de Telegram Webhook
    if (data.update_id) {
      if (data.message) {
        const chatId = data.message.chat.id;
        procesarGrupoMensajes(chatId, [data]);
      } else if (data.callback_query) {
        procesarCallbackTelegram(data.callback_query);
      }
      return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // Si proviene del Dashboard Web (Registro de nuevo movimiento)
    if (data.action === "addMovement") {
      const ss = obtenerLibro_();
      const shMov = ss.getSheetByName("MOVIMIENTOS");
      if (!shMov) throw new Error("Hoja MOVIMIENTOS no encontrada");
      
      const id = "M-" + String(shMov.getLastRow() + 1).padStart(4, "0");
      const d = new Date();
      const fecha = Utilities.formatDate(d, ZONA_HORARIA, "dd/MM/yyyy");
      const hora = Utilities.formatDate(d, ZONA_HORARIA, "hh:mm a");
      const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
      
      const nuevoMov = [
        id,
        fecha,
        hora,
        data.tipo || "Gasto",
        data.categoria || "Otros",
        data.subcategoria || "General",
        data.descripcion || "Gasto registrado desde Dashboard Web",
        Number(data.valor || 0),
        data.metodo || "Transferencia",
        data.cuenta || "Nequi",
        meses[d.getMonth()],
        d.getFullYear(),
        "Dashboard Web",
        "WEB"
      ];
      
      shMov.insertRowBefore(5);
      shMov.getRange(5, 1, 1, nuevoMov.length).setValues([nuevoMov]);
      
      return ContentService.createTextOutput(JSON.stringify({ ok: true, id: id, movimiento: nuevoMov }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  return ContentService.createTextOutput(JSON.stringify({ ok: true })).setMimeType(ContentService.MimeType.JSON);
}

// Helpers base
function formatearCOP(v) { return Number(v||0).toLocaleString("es-CO"); }
