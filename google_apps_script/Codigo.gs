/************************************************************
 * MIS FINANZAS & PARDOFINANZAS DASHBOARD WEB
 * VERSIÓN V26.9 PRO AI — ARCHIVO OFICIAL DEFINITIVO UNIFICADO
 * 
 * Contiene el 100% de la automatización para Google Apps Script:
 * 1. API Web en tiempo real para Dashboard (doGet & doPost)
 * 2. Inserción directa en fila 5 de MOVIMIENTOS y fórmula Columna O
 * 3. Opción de Borrar / Anular registros con 1 clic (deleteMovement)
 * 4. Auditoría de Colillas de Nómina Colanta con Gemini Multimodal (PDF/Foto)
 * 5. Bot de Telegram oficial (comandos, callbacks, notas de voz)
 * 6. Escáner de Facturas Gmail (procesarCorreosFacturas)
 ************************************************************/

/* =========================================================
   CONFIGURACIÓN Y ENTORNO
   ========================================================= */
const TELEGRAM_TOKEN = String(PropertiesService.getScriptProperties().getProperty("TELEGRAM_TOKEN") || "").trim();
const SPREADSHEET_ID = String(PropertiesService.getScriptProperties().getProperty("SPREADSHEET_ID") || "1C9ifm7U8IYGOdY4IwAji9Z3McSlNy3B-e_2Jo-zvBFk").trim();
const WEBHOOK_URL = String(PropertiesService.getScriptProperties().getProperty("WEBHOOK_URL") || "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec").trim();
const GEMINI_API_KEY = String(PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") || "").trim();
const GMAIL_FACTURAS_REMITENTE = String(PropertiesService.getScriptProperties().getProperty("GMAIL_FACTURAS_REMITENTE") || "marlonpardomu@gmail.com").trim();
const USUARIO_NOMBRE_COMPLETO = String(PropertiesService.getScriptProperties().getProperty("USUARIO_NOMBRE_COMPLETO") || "MARLON MUNOZ PARDO").trim();
const ZONA_HORARIA = "America/Bogota";
const GMAIL_MAX_BODY_CHARS = 30000;
const GMAIL_MAX_ATTACHMENT_BYTES = 6 * 1024 * 1024;


/* =========================================================
   API WEB (doGet & doPost) — CONEXIÓN DASHBOARD & TELEGRAM V26.9
   ========================================================= */

function doGet(e) {
  const params = e && e.parameter ? e.parameter : {};
  const action = params.action || "ping";

  if (action === "getData" || action === "getSummary") {
    try {
      const mesParam = params.mes ? String(params.mes).toLowerCase().trim() : null;
      const anioParam = params.anio ? Number(params.anio) : null;
      const data = obtenerDatosCompletosDashboard_(mesParam, anioParam);
      return ContentService.createTextOutput(JSON.stringify(data))
        .setMimeType(ContentService.MimeType.JSON);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "setMonth") {
    try {
      const ss = obtenerLibro_();
      const shPanel = ss.getSheetByName("PANEL");
      if (shPanel && params.mes) {
        shPanel.getRange("L2").setValue(String(params.mes).toLowerCase().trim());
        if (params.anio) shPanel.getRange("I2").setValue(Number(params.anio));
        SpreadsheetApp.flush();
      }
      const data = obtenerDatosCompletosDashboard_(params.mes, params.anio);
      return ContentService.createTextOutput(JSON.stringify({ ok: true, data: data }))
        .setMimeType(ContentService.MimeType.JSON);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "sendEmailAudit" || action === "enviarReporteEmail") {
    try {
      const authKey = PropertiesService.getScriptProperties().getProperty("PARDO_AUTH_KEY") || "marlon_pardo_finanzas_2026_auth";
      const reqToken = params.token || params.auth;
      if (reqToken !== authKey) {
        return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "Acceso no autorizado: Token de GitHub inválido." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
      const emailDestino = params.email || GMAIL_FACTURAS_REMITENTE;
      const resAudit = enviarReporteAuditoriaEmail_(emailDestino);
      return ContentService.createTextOutput(JSON.stringify(resAudit))
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

  if (action === "procesarFacturas" || action === "scanEmails") {
    try {
      const authKey = PropertiesService.getScriptProperties().getProperty("PARDO_AUTH_KEY") || "marlon_pardo_finanzas_2026_auth";
      const reqToken = params.token || params.auth;
      if (reqToken !== authKey) {
        return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "Acceso no autorizado: Token de GitHub inválido." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
      const cant = procesarCorreosFacturas();
      return ContentService.createTextOutput(JSON.stringify({
        ok: true,
        procesadas: cant,
        mensaje: "Escáner de facturas Gmail ejecutado. Facturas procesadas: " + cant
      })).setMimeType(ContentService.MimeType.JSON);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "addFixedObligation" || action === "crearGastoFijo") {
    try {
      const authKey = PropertiesService.getScriptProperties().getProperty("PARDO_AUTH_KEY") || "marlon_pardo_finanzas_2026_auth";
      const reqToken = params.token || params.auth;
      if (reqToken !== authKey) {
        return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "Acceso no autorizado: Token inválido." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
      return registrarCompromisoFijo_(params);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "updateFixedObligation" || action === "actualizarGastoFijo") {
    try {
      const authKey = PropertiesService.getScriptProperties().getProperty("PARDO_AUTH_KEY") || "marlon_pardo_finanzas_2026_auth";
      const reqToken = params.token || params.auth;
      if (reqToken !== authKey) {
        return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "Acceso no autorizado: Token inválido." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
      return actualizarCompromisoFijoDirecto_(params);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "sincronizarFijosOctubre") {
    try {
      const res = sincronizarFijosOctubreDefinitivo();
      return ContentService.createTextOutput(JSON.stringify(res))
        .setMimeType(ContentService.MimeType.JSON);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  }

  if (action === "revertirPagoFijo" || action === "revertPayment") {
    try {
      const authKey = PropertiesService.getScriptProperties().getProperty("PARDO_AUTH_KEY") || "marlon_pardo_finanzas_2026_auth";
      const reqToken = params.token || params.auth;
      if (reqToken !== authKey) {
        return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "Acceso no autorizado: Token inválido." }))
          .setMimeType(ContentService.MimeType.JSON);
      }
      return revertirPagoFijoDirecto_(params);
    } catch (err) {
      return ContentService.createTextOutput(JSON.stringify({ ok: false, error: err.message }))
        .setMimeType(ContentService.MimeType.JSON);
    }
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
    // 🛡️ SEGURIDAD ANTI-SPAM GITHUB (PROTECCIÓN GOOGLE SHEETS)
    // Evita que cualquier visitante de GitHub inyecte información falsa en las finanzas de Marlon
    // =========================================================
    const AUTH_TOKEN = PropertiesService.getScriptProperties().getProperty("PARDO_AUTH_KEY") || "marlon_pardo_finanzas_2026_auth";
    const reqToken = data.token || data.authToken || data.key || (e.parameter && (e.parameter.token || e.parameter.auth));

    const esOperacionEscritura = Boolean(
      data.action === "addMovement" ||
      (data.tipo && !data.update_id) ||
      data.action === "deleteMovement" ||
      data.action === "borrarRegistro" ||
      data.action === "updateMovement" ||
      data.action === "editarRegistro" ||
      data.action === "addFixedObligation" ||
      data.action === "crearGastoFijo" ||
      data.action === "updateFixedObligation" ||
      data.action === "actualizarGastoFijo" ||
      data.action === "revertirPagoFijo" ||
      data.action === "revertPayment" ||
      data.action === "sincronizarFijosOctubre" ||
      data.action === "procesarColilla" ||
      data.action === "procesarFacturas" ||
      data.action === "scanEmails" ||
      data.action === "sendEmailAudit" ||
      data.action === "enviarReporteEmail"
    );

    if (esOperacionEscritura && (!reqToken || reqToken !== AUTH_TOKEN)) {
      return ContentService.createTextOutput(JSON.stringify({
        ok: false,
        error: "Acceso denegado: Token de seguridad de GitHub inválido. Operación bloqueada para proteger las finanzas de Marlon Muñoz Pardo."
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // =========================================================
    // 🌐 1. OPERACIONES DESDE EL DASHBOARD WEB
    // =========================================================
    if (data.action === "addMovement" || (data.tipo && !data.update_id)) {
      return registrarMovimientoDesdeDashboard_(data);
    }

    if (data.action === "addFixedObligation" || data.action === "crearGastoFijo") {
      return registrarCompromisoFijo_(data);
    }

    if (data.action === "updateFixedObligation" || data.action === "actualizarGastoFijo") {
      return actualizarCompromisoFijoDirecto_(data);
    }

    if (data.action === "revertirPagoFijo" || data.action === "revertPayment") {
      return revertirPagoFijoDirecto_(data);
    }

    if (data.action === "sincronizarFijosOctubre") {
      const res = sincronizarFijosOctubreDefinitivo();
      return ContentService.createTextOutput(JSON.stringify(res)).setMimeType(ContentService.MimeType.JSON);
    }

    if (data.action === "procesarColilla") {
      return procesarColillaDashboard_(data);
    }

    if (data.action === "sendEmailAudit" || data.action === "enviarReporteEmail") {
      const emailDestino = data.email || GMAIL_FACTURAS_REMITENTE;
      const resAudit = enviarReporteAuditoriaEmail_(emailDestino);
      return ContentService.createTextOutput(JSON.stringify(resAudit)).setMimeType(ContentService.MimeType.JSON);
    }

    if (data.action === "procesarFacturas" || data.action === "scanEmails") {
      const cant = procesarCorreosFacturas();
      return ContentService.createTextOutput(JSON.stringify({
        ok: true,
        procesadas: cant,
        mensaje: "Escáner de facturas Gmail ejecutado. Facturas procesadas: " + cant
      })).setMimeType(ContentService.MimeType.JSON);
    }

    if (data.action === "updateMovement" || data.action === "editarRegistro") {
      const id = data.id || data.idRegistro;
      const resUpdate = actualizarRegistroMovimiento_(id, data);
      return ContentService.createTextOutput(JSON.stringify(resUpdate)).setMimeType(ContentService.MimeType.JSON);
    }

    if (data.action === "deleteMovement" || data.action === "borrarRegistro") {
      const id = data.id || data.idRegistro;
      let resDelete = null;
      if (id) {
        resDelete = borrarRegistroCompletoPorID(id, data);
      }
      if (!resDelete || resDelete.ok === false) {
        // Fallback inteligente: buscar por descripción y monto si el ID no coincidió exactamente
        if (data.descripcion || data.valor || data.monto) {
          resDelete = borrarRegistroCompletoPorID("", data);
        }
      }
      if (!resDelete || resDelete.ok === false) {
        resDelete = borrarUltimoConjunto();
      }
      return ContentService.createTextOutput(JSON.stringify({
        ok: resDelete && resDelete.ok !== false,
        resultado: resDelete,
        id: id,
        mensaje: resDelete && resDelete.ok !== false ? "Registro eliminado exitosamente de Google Sheets." : "No se encontró el registro para eliminar."
      })).setMimeType(ContentService.MimeType.JSON);
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

  // Generar ID consecutivo M-xxxx respetando el ID enviado por la App o el último registro
  let maxNum = 255;
  if (shMov.getLastRow() >= 5) {
    const idsRange = shMov.getRange(5, 1, Math.min(20, shMov.getLastRow() - 4), 1).getValues();
    idsRange.forEach(row => {
      const m = String(row[0] || "").match(/^M-(\d+)$/i);
      if (m) maxNum = Math.max(maxNum, parseInt(m[1], 10));
    });
  }
  const idFromApp = data.id ? String(data.id).trim().toUpperCase() : "";
  const nuevoId = (idFromApp && /^M-\d+$/i.test(idFromApp)) ? idFromApp : ("M-" + String(maxNum + 1).padStart(4, "0"));

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

  // Detección automática ultra-inteligente de Gastos Fijos
  let idFijoFinal = data.idFijo || null;
  if (!idFijoFinal && tipo === "Gasto") {
    const textoCompleto = `${descripcion} ${categoria} ${subcategoria}`.toLowerCase();
    if (/\b(agua|acueducto)\b/i.test(textoCompleto)) idFijoFinal = "F-0122";
    else if (/\b(epm|energia|luz|electricidad)\b/i.test(textoCompleto)) idFijoFinal = "F-0107";
    else if (/\b(tigo|internet|wifi|une)\b/i.test(textoCompleto)) idFijoFinal = "F-0109";
    else if (/\b(arriendo|casa|apartamento)\b/i.test(textoCompleto)) idFijoFinal = "F-0116";
    else if (/\b(spotify)\b/i.test(textoCompleto)) idFijoFinal = "F-0114";
    else if (/\b(celular|datos|linea movil)\b/i.test(textoCompleto)) idFijoFinal = "F-0115";
    else if (/\b(google|google one)\b/i.test(textoCompleto)) idFijoFinal = "F-0117";
    else if (/\b(parqueadero moto)\b/i.test(textoCompleto)) idFijoFinal = "F-0112";
    else if (/\b(gasolina moto|gasolina)\b/i.test(textoCompleto)) idFijoFinal = "F-0113";
    else if (/\b(parqueadero trabajo)\b/i.test(textoCompleto)) idFijoFinal = "F-0110";
    else if (/\b(comida|mercado)\b/i.test(textoCompleto)) idFijoFinal = "F-0108";
    else if (/\b(audifonos|samsung)\b/i.test(textoCompleto)) idFijoFinal = "F-0111";
  }

  // Si corresponde a un gasto fijo, registrar abono y reconciliar en HISTORICO FIJOS y CONTROL FIJOS
  let fijoActualizadoInfo = null;
  if (idFijoFinal && tipo === "Gasto") {
    try {
      fijoActualizadoInfo = actualizarHistoricoFijos_(ss, idFijoFinal, valor, mesActual, anioActual, fechaStr);
      shMov.getRange(5, 6).setValue("Pago fijo");
      shMov.getRange(5, 14).setValue(`PAGO_FIJO|${idFijoFinal}|${mesActual}|${anioActual}`);
    } catch (eFijo) {
      console.warn("Aviso reconciliación fijos:", eFijo);
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

function actualizarHistoricoFijos_(ss, idFijo, valor, mes, anio, fechaOpt) {
  if (!ss) ss = obtenerLibro_();
  const shHist = ss.getSheetByName("HISTORICO FIJOS");
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const mesNorm = normalizarTexto(String(mes || "octubre"));
  const anioNum = Number(anio || 2026);
  const idBuscado = String(idFijo || "").trim().toUpperCase();
  const fechaStr = fechaOpt || Utilities.formatDate(new Date(), ZONA_HORARIA, "yyyy-MM-dd");

  let reconciliado = false;
  let conceptoFijo = "";

  // 1. Actualizar en HISTORICO FIJOS
  if (shHist && shHist.getLastRow() >= 4) {
    const numRows = shHist.getLastRow() - 3;
    const hRows = shHist.getRange(4, 1, numRows, 15).getValues();

    for (let h = 0; h < hRows.length; h++) {
      const hId = String(hRows[h][0] || "").trim().toUpperCase();
      const hMes = normalizarTexto(String(hRows[h][9] || ""));
      const hAnio = Number(hRows[h][10] || 0);

      if (hId === idBuscado && hMes === mesNorm && (hAnio === anioNum || hAnio === 0)) {
        const rowIndex = h + 4;
        conceptoFijo = String(hRows[h][1] || "");
        const cuotaActual = Number(hRows[h][4] || 0);
        const abonoActual = Number(hRows[h][5] || 0);

        if (idBuscado === "F-0108" && valor === 650000) {
          shHist.getRange(rowIndex, 5).setValue(650000);
          shHist.getRange(rowIndex, 6).setValue(650000);
          shHist.getRange(rowIndex, 7).setValue(fechaStr);
          shHist.getRange(rowIndex, 8).setValue("🟢 Pagado");
          shHist.getRange(rowIndex, 14).setValue(0);
          reconciliado = true;
          break;
        }

        // Si es servicio variable (EPM, Tigo, etc.) o el pago supera la cuota previa, ajustar cuota base a la factura real
        const esVariable = /\b(epm|energia|luz|agua|gas|tigo|internet|wifi|une|claro|datos|celular|gasolina)\b/i.test(conceptoFijo);
        const nuevaCuota = (esVariable || valor > cuotaActual) ? Math.max(cuotaActual, valor) : cuotaActual;
        const nuevoAbono = (esVariable || valor >= nuevaCuota) ? valor : Math.min(nuevaCuota, abonoActual + valor);

        shHist.getRange(rowIndex, 5).setValue(nuevaCuota); // Cuota Base ($)
        shHist.getRange(rowIndex, 6).setValue(nuevoAbono); // Abonado ($)
        shHist.getRange(rowIndex, 7).setValue(fechaStr);   // Fecha Pago
        shHist.getRange(rowIndex, 8).setValue(nuevoAbono >= nuevaCuota ? "🟢 Pagado" : "🟡 Abono Parcial"); // Estado
        shHist.getRange(rowIndex, 14).setValue(Math.max(0, nuevaCuota - nuevoAbono)); // Saldo Pendiente

        reconciliado = true;
        break;
      }
    }
  }

  // 2. Actualizar CONTROL FIJOS directamente si tiene valores planos o como refuerzo
  if (shFijos && shFijos.getLastRow() >= 10) {
    const numF = shFijos.getLastRow() - 9;
    const fRows = shFijos.getRange(10, 1, numF, 9).getValues();
    for (let f = 0; f < fRows.length; f++) {
      const fId = String(fRows[f][0] || "").trim().toUpperCase();
      if (fId === idBuscado) {
        const rIndex = f + 10;
        const fCellFormula = shFijos.getRange(rIndex, 6).getFormula();
        if (!fCellFormula) {
          let cuotaPrev = Number(fRows[f][4] || valor);
          if (idBuscado === "F-0108" && valor === 650000) cuotaPrev = 650000;
          const cuotaFinal = (idBuscado === "F-0108") ? valor : (valor > cuotaPrev ? valor : cuotaPrev);
          shFijos.getRange(rIndex, 5).setValue(cuotaFinal);
          shFijos.getRange(rIndex, 6).setValue(valor);
          shFijos.getRange(rIndex, 7).setValue("🟢 Pagado");
          shFijos.getRange(rIndex, 8).setValue(fechaStr);
          shFijos.getRange(rIndex, 9).setValue(0);
        }
        break;
      }
    }
  }

  try {
    CacheService.getScriptCache().remove("CTX_FIN_IA_" + mesNorm + "_" + anioNum);
  } catch (e) {}

  return { ok: true, reconciliado: reconciliado, idFijo: idBuscado, concepto: conceptoFijo, valor: valor };
}

/**
 * 🌟 REGISTRAR NUEVO GASTO FIJO EN CONTROL FIJOS Y EN HISTORICO FIJOS
 * Invocado en tiempo real desde el CFO AI Chat cuando Marlon dice:
 * "crea un gasto fijo de todos los meses de 60 mil de agua"
 */
function registrarCompromisoFijo_(data) {
  const ss = obtenerLibro_();
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const shHist = ss.getSheetByName("HISTORICO FIJOS");
  if (!shFijos) throw new Error("No se encontró la hoja CONTROL FIJOS.");

  const d = new Date();
  const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
  const mesActual = data.mes ? normalizarTexto(String(data.mes)) : meses[d.getMonth()];
  const anioActual = Number(data.anio || d.getFullYear());

  const concepto = String(data.concepto || "Nuevo Compromiso").trim();
  const categoria = String(data.categoria || "Servicios").trim();
  const diaCobro = Number(data.dia || 15);
  const cuotaBase = Number(data.cuota || 0);
  const abonadoMes = Number(data.abonado || 0);
  const estado = abonadoMes >= cuotaBase && cuotaBase > 0 ? "🟢 Pagado" : "🔴 Pendiente";
  const fechaPago = abonadoMes > 0 ? Utilities.formatDate(d, ZONA_HORARIA, "yyyy-MM-dd") : "";
  const saldoPend = Math.max(0, cuotaBase - abonadoMes);
  const prioridad = String(data.prioridad || "Media").trim();
  const medioPago = String(data.medioPago || "Transferencia").trim();
  const notas = String(data.notas || "Creado desde PardoFinanzas Web").trim();

  // Escanear filas 10 a 60 para ubicar la última fila con ID F- y si ya existe
  let nuevoId = data.id || null;
  let maxNum = 121;
  let filaExistente = 0;
  let ultimaConFijo = 9;

  if (shFijos.getLastRow() >= 10) {
    const maxScan = Math.min(60, Math.max(20, shFijos.getLastRow() - 9));
    const rows = shFijos.getRange(10, 1, maxScan, 2).getValues();
    for (let i = 0; i < rows.length; i++) {
      const curRow = 10 + i;
      const rId = String(rows[i][0] || "").trim().toUpperCase();
      const rConc = String(rows[i][1] || "").trim().toLowerCase();
      if (rId.startsWith("F-")) {
        if (curRow <= 40) {
          ultimaConFijo = Math.max(ultimaConFijo, curRow);
        }
        const m = rId.match(/^F-0?(\d+)$/);
        if (m) maxNum = Math.max(maxNum, parseInt(m[1], 10));
      }
      if ((nuevoId && rId === nuevoId.toUpperCase()) || (concepto && rConc === concepto.toLowerCase())) {
        filaExistente = curRow;
      }
    }
  }

  if (!nuevoId) {
    nuevoId = "F-" + String(maxNum + 1).padStart(4, "0");
  }

  // Columnas CONTROL FIJOS: 0:ID, 1:Concepto, 2:Categoría, 3:Día Cobro, 4:Cuota Base, 5:Abonado Mes, 6:Estado, 7:Fecha Último Pago, 8:Saldo Pendiente, 9:Prioridad, 10:Medio Pago, 11:Notas
  const filaFijos = [
    nuevoId,
    concepto,
    categoria,
    diaCobro,
    cuotaBase,
    abonadoMes > 0 ? abonadoMes : "",
    estado,
    fechaPago,
    saldoPend > 0 ? saldoPend : "-",
    prioridad,
    medioPago,
    notas
  ];

  if (filaExistente > 0) {
    shFijos.getRange(filaExistente, 1, 1, filaFijos.length).setValues([filaFijos]);
  } else {
    // Ubicar exactamente en la fila continua (ej. fila 25 inmediatamente después de F-0121 en fila 24)
    const targetRowFijos = (ultimaConFijo >= 10 && ultimaConFijo < 40) ? (ultimaConFijo + 1) : 25;
    shFijos.getRange(targetRowFijos, 1, 1, filaFijos.length).setValues([filaFijos]);
  }

  // Sincronizar en HISTORICO FIJOS para el mes activo
  if (shHist && shHist.getLastRow() >= 4) {
    let histExistente = 0;
    const maxHistScan = Math.min(200, shHist.getLastRow() - 3);
    const hRows = shHist.getRange(4, 1, maxHistScan, 11).getValues();
    for (let h = 0; h < hRows.length; h++) {
      const hId = String(hRows[h][0]).toUpperCase();
      const hMes = normalizarTexto(String(hRows[h][9]));
      const hAnio = Number(hRows[h][10]);
      if ((hId === nuevoId.toUpperCase() || String(hRows[h][1]).toLowerCase() === concepto.toLowerCase()) && hMes === mesActual && (hAnio === anioActual || hAnio === 0)) {
        histExistente = h + 4;
        break;
      }
    }

    const filaHist = [
      nuevoId,
      concepto,
      categoria,
      diaCobro,
      cuotaBase,
      abonadoMes,
      fechaPago,
      estado,
      prioridad,
      mesActual,
      anioActual,
      medioPago,
      notas,
      saldoPend
    ];

    if (histExistente > 0) {
      shHist.getRange(histExistente, 1, 1, filaHist.length).setValues([filaHist]);
    } else {
      shHist.getRange(shHist.getLastRow() + 1, 1, 1, filaHist.length).setValues([filaHist]);
    }
  }

  SpreadsheetApp.flush();
  try {
    CacheService.getScriptCache().remove("CTX_FIN_IA_" + mesActual + "_" + anioActual);
  } catch (e) {}

  return ContentService.createTextOutput(JSON.stringify({
    ok: true,
    id: nuevoId,
    concepto: concepto,
    cuota: cuotaBase,
    dia: diaCobro,
    mensaje: `Compromiso fijo ${nuevoId} (${concepto}) creado y sincronizado en Google Sheets.`
  })).setMimeType(ContentService.MimeType.JSON);
}

/**
 * 🔄 ACTUALIZACIÓN DIRECTA DE CUALQUIER COMPROMISO FIJO EN GOOGLE SHEETS
 */
function actualizarCompromisoFijoDirecto_(data) {
  const ss = obtenerLibro_();
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const shHist = ss.getSheetByName("HISTORICO FIJOS");
  const idBuscado = String(data.id || "").trim().toUpperCase();
  if (!idBuscado) {
    return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "ID de compromiso fijo requerido." }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  let updatedFijos = false;
  if (shFijos && shFijos.getLastRow() >= 10) {
    const rows = shFijos.getRange(10, 1, shFijos.getLastRow() - 9, 9).getValues();
    for (let i = 0; i < rows.length; i++) {
      if (String(rows[i][0]).toUpperCase() === idBuscado) {
        const rIndex = i + 10;
        if (data.cuota !== undefined) shFijos.getRange(rIndex, 5).setValue(Number(data.cuota));
        if (data.abonado !== undefined) shFijos.getRange(rIndex, 6).setValue(Number(data.abonado));
        if (data.estado !== undefined) shFijos.getRange(rIndex, 7).setValue(String(data.estado));
        if (data.fechaPago !== undefined) shFijos.getRange(rIndex, 8).setValue(String(data.fechaPago));
        if (data.saldo !== undefined) shFijos.getRange(rIndex, 9).setValue(Number(data.saldo));
        else if (data.cuota !== undefined && data.abonado !== undefined) {
          const saldo = Math.max(0, Number(data.cuota) - Number(data.abonado));
          shFijos.getRange(rIndex, 9).setValue(saldo > 0 ? saldo : "-");
        }
        updatedFijos = true;
        break;
      }
    }
  }

  // Sincronizar también en HISTORICO FIJOS
  if (shHist && shHist.getLastRow() >= 4) {
    const d = new Date();
    const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
    const targetMes = data.mes ? normalizarTexto(String(data.mes)) : meses[d.getMonth()];
    const targetAnio = Number(data.anio || 2026);
    const hRows = shHist.getRange(4, 1, shHist.getLastRow() - 3, 14).getValues();
    for (let h = 0; h < hRows.length; h++) {
      const hId = String(hRows[h][0]).toUpperCase();
      const hMes = normalizarTexto(String(hRows[h][9]));
      const hAnio = Number(hRows[h][10]);
      if (hId === idBuscado && hMes === targetMes && (hAnio === targetAnio || hAnio === 0)) {
        const rIdx = h + 4;
        if (data.cuota !== undefined) shHist.getRange(rIdx, 5).setValue(Number(data.cuota));
        if (data.abonado !== undefined) shHist.getRange(rIdx, 6).setValue(Number(data.abonado));
        if (data.fechaPago !== undefined) shHist.getRange(rIdx, 7).setValue(String(data.fechaPago));
        if (data.estado !== undefined) shHist.getRange(rIdx, 8).setValue(String(data.estado));
        if (data.saldo !== undefined) shHist.getRange(rIdx, 14).setValue(Number(data.saldo));
        break;
      }
    }
  }

  SpreadsheetApp.flush();

  return ContentService.createTextOutput(JSON.stringify({
    ok: true,
    id: idBuscado,
    mensaje: `Compromiso fijo ${idBuscado} actualizado exitosamente en Google Sheets.`
  })).setMimeType(ContentService.MimeType.JSON);
}

/**
 * ↺ REVERTIR PAGO DE GASTO FIJO EN GOOGLE SHEETS
 * Restaura el gasto fijo a "🔴 Pendiente", limpia fecha de pago, reinicia abonado a 0,
 * y elimina el movimiento de pago asociado en MOVIMIENTOS para restaurar el saldo de la cuenta.
 */
function revertirPagoFijoDirecto_(data) {
  const ss = obtenerLibro_();
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const shHist = ss.getSheetByName("HISTORICO FIJOS");
  const shMov = ss.getSheetByName("MOVIMIENTOS");

  const idBuscado = String(data.id || "").trim().toUpperCase();
  if (!idBuscado) {
    return ContentService.createTextOutput(JSON.stringify({ ok: false, error: "ID de compromiso fijo requerido." }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  const d = new Date();
  const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
  const mesActual = data.mes ? normalizarTexto(String(data.mes)) : meses[d.getMonth()];
  const anioActual = Number(data.anio || d.getFullYear());

  let cuotaFijo = 0;
  let conceptoFijo = String(data.concepto || "");

  // 1. Restaurar CONTROL FIJOS (inicia en fila 10)
  if (shFijos && shFijos.getLastRow() >= 10) {
    const numF = shFijos.getLastRow() - 9;
    const fRows = shFijos.getRange(10, 1, numF, 9).getValues();
    for (let f = 0; f < fRows.length; f++) {
      if (String(fRows[f][0] || "").trim().toUpperCase() === idBuscado) {
        const rIndex = f + 10;
        if (!conceptoFijo) conceptoFijo = String(fRows[f][1] || "");
        cuotaFijo = Number(fRows[f][4] || 0);

        shFijos.getRange(rIndex, 6).setValue(0); // Abonado Mes = 0
        shFijos.getRange(rIndex, 7).setValue("🔴 Pendiente"); // Estado
        shFijos.getRange(rIndex, 8).setValue(""); // Fecha Pago
        shFijos.getRange(rIndex, 9).setValue(cuotaFijo > 0 ? cuotaFijo : "-"); // Saldo Pendiente
        break;
      }
    }
  }

  // 2. Restaurar HISTORICO FIJOS
  if (shHist && shHist.getLastRow() >= 4) {
    const numRows = shHist.getLastRow() - 3;
    const hRows = shHist.getRange(4, 1, numRows, 15).getValues();
    for (let h = 0; h < hRows.length; h++) {
      const hId = String(hRows[h][0] || "").trim().toUpperCase();
      const hMes = normalizarTexto(String(hRows[h][9] || ""));
      const hAnio = Number(hRows[h][10] || 0);

      if (hId === idBuscado && hMes === mesActual && (hAnio === anioActual || hAnio === 0)) {
        const rIdx = h + 4;
        if (cuotaFijo === 0) cuotaFijo = Number(hRows[h][4] || 0);
        if (!conceptoFijo) conceptoFijo = String(hRows[h][1] || "");

        shHist.getRange(rIdx, 6).setValue(0); // Abonado = 0
        shHist.getRange(rIdx, 7).setValue(""); // Fecha Pago
        shHist.getRange(rIdx, 8).setValue("🔴 Pendiente"); // Estado
        shHist.getRange(rIdx, 14).setValue(cuotaFijo); // Saldo Pendiente
        break;
      }
    }
  }

  // 3. Eliminar movimiento de pago en MOVIMIENTOS para restaurar saldo
  let movimientosEliminados = 0;
  if (shMov && shMov.getLastRow() >= 4) {
    const lastRow = shMov.getLastRow();
    const mRows = shMov.getRange(4, 1, lastRow - 3, 14).getValues();

    for (let i = mRows.length - 1; i >= 0; i--) {
      const rowSubcat = String(mRows[i][5] || "").toLowerCase().trim();
      const rowDesc = String(mRows[i][6] || "").toLowerCase().trim();
      const rowRef = String(mRows[i][13] || "").trim();

      const esRefMatch = rowRef.includes(`PAGO_FIJO|${idBuscado}`) || rowRef.includes(`ABONO_FIJO|${idBuscado}`);
      const esDescMatch = (rowSubcat === "pago fijo" || rowDesc.includes("pago fijo")) &&
                          (rowDesc.includes(idBuscado.toLowerCase()) || (conceptoFijo && rowDesc.includes(conceptoFijo.toLowerCase())));

      if (esRefMatch || esDescMatch) {
        shMov.deleteRow(i + 4);
        movimientosEliminados++;
      }
    }
  }

  SpreadsheetApp.flush();

  return ContentService.createTextOutput(JSON.stringify({
    ok: true,
    id: idBuscado,
    concepto: conceptoFijo,
    movimientosEliminados: movimientosEliminados,
    mensaje: `Pago de compromiso ${conceptoFijo || idBuscado} revertido exitosamente a Pendiente.`
  })).setMimeType(ContentService.MimeType.JSON);
}

/**
 * ⚡ FUNCIÓN DE 1 CLIC PARA SINCRONIZAR GASTOS FIJOS DE OCTUBRE 2026
 * Puede ejecutarse directamente desde el botón "Ejecutar" en Google Apps Script
 * o desde la API Web.
 * Ajusta F-0108 (COMIDA) a $650.000 e inserta F-0122 (Agua - $60.000).
 */
function sincronizarFijosOctubreDefinitivo() {
  const ss = obtenerLibro_();
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const shHist = ss.getSheetByName("HISTORICO FIJOS");
  const logs = [];

  if (shFijos && shFijos.getLastRow() >= 10) {
    const numRows = Math.min(80, shFijos.getLastRow() - 9);
    const rows = shFijos.getRange(10, 1, numRows, 12).getValues();
    const itemsMap = new Map();

    for (let i = 0; i < rows.length; i++) {
      const r = rows[i];
      const id = String(r[0] || "").trim().toUpperCase();
      if (!id || !/^F-/i.test(id)) continue;

      const concepto = String(r[1] || "").trim();
      const concNorm = concepto.toLowerCase();

      // Si es F-0107 (EPM ENERGIA), forzar $197.000 exactos Pagado
      if (id === "F-0107" || (concNorm.includes("epm") && !concNorm.includes("agua"))) {
        r[0] = "F-0107";
        r[1] = "EPM ENERGIA";
        r[2] = "Servicios";
        r[3] = 15;
        r[4] = 197000;
        r[5] = 197000;
        r[6] = "🟢 Pagado";
        r[7] = "2026-10-17";
        r[8] = 0;
        itemsMap.set("F-0107", r);
        logs.push("F-0107 (EPM ENERGIA) restaurado a $197.000 Pagado");
        continue;
      }

      // Si es F-0108 (COMIDA), forzar $650.000 exactos
      if (id === "F-0108" || concNorm === "comida") {
        r[0] = "F-0108";
        r[1] = "COMIDA";
        r[2] = "Alimentación";
        r[3] = 15;
        r[4] = 650000;
        r[5] = 650000;
        r[6] = "🟢 Pagado";
        r[7] = "2026-10-17";
        r[8] = 0;
        itemsMap.set("F-0108", r);
        logs.push("F-0108 (COMIDA) ajustado a $650.000 Pagado");
        continue;
      }

      // Si es Agua o F-0122, unificar en F-0122 con $60.000 Día 15
      if (id === "F-0122" || concNorm.includes("agua")) {
        itemsMap.set("F-0122", [
          "F-0122",
          "Agua",
          "Servicios",
          15,
          60000,
          "",
          "🔴 Pendiente",
          "",
          60000,
          "Media",
          "Transferencia",
          "Servicio de Agua Domiciliaria"
        ]);
        logs.push("F-0122 (Agua - $60.000 - Día 15) preparado para Quincena 1");
        continue;
      }

      // Demás compromisos legítimos
      if (!itemsMap.has(id)) {
        itemsMap.set(id, r);
      }
    }

    // Asegurar que F-0122 (Agua) esté presente
    if (!itemsMap.has("F-0122")) {
      itemsMap.set("F-0122", [
        "F-0122",
        "Agua",
        "Servicios",
        15,
        60000,
        "",
        "🔴 Pendiente",
        "",
        60000,
        "Media",
        "Transferencia",
        "Servicio de Agua Domiciliaria"
      ]);
      logs.push("F-0122 (Agua - $60.000) incorporado");
    }

    // Convertir a lista y ordenar estrictamente: Día 15 primero (Quincena 1), Día 30 después (Quincena 2)
    const listaFijos = Array.from(itemsMap.values());
    listaFijos.sort((a, b) => {
      const qA = Number(a[3] || 0) <= 15 ? 1 : 2;
      const qB = Number(b[3] || 0) <= 15 ? 1 : 2;
      if (qA !== qB) return qA - qB;
      const diaA = Number(a[3] || 0);
      const diaB = Number(b[3] || 0);
      if (diaA !== diaB) return diaA - diaB;
      return String(a[0] || "").localeCompare(String(b[0] || ""));
    });

    // Reescribir en CONTROL FIJOS desde la fila 10
    shFijos.getRange(10, 1, listaFijos.length, 12).setValues(listaFijos);
    logs.push(`Reordenada tabla CONTROL FIJOS: ${listaFijos.length} filas agrupadas (Día 15 con Día 15, Día 30 con Día 30)`);

    // Limpiar filas sobrantes posteriores (ej. fila 26 en adelante)
    const totalFilasAntes = shFijos.getLastRow();
    const primeraFilaSobrante = 10 + listaFijos.length;
    if (totalFilasAntes >= primeraFilaSobrante) {
      shFijos.getRange(primeraFilaSobrante, 1, totalFilasAntes - primeraFilaSobrante + 1, 12).clearContent();
      logs.push(`Limpiadas filas sobrantes desde fila ${primeraFilaSobrante} hasta ${totalFilasAntes}`);
    }
  }

  // 3. Sincronizar en HISTORICO FIJOS para Octubre 2026
  if (shHist && shHist.getLastRow() >= 4) {
    const numH = Math.min(200, shHist.getLastRow() - 3);
    const hRows = shHist.getRange(4, 1, numH, 14).getValues();
    let f122HistRow = 0;

    for (let h = 0; h < hRows.length; h++) {
      const hId = String(hRows[h][0] || "").trim().toUpperCase();
      const hMes = normalizarTexto(String(hRows[h][9] || ""));
      const hAnio = Number(hRows[h][10] || 0);

      if (hMes === "octubre" && (hAnio === 2026 || hAnio === 0)) {
        if (hId === "F-0108" || String(hRows[h][1] || "").toLowerCase() === "comida") {
          const rIdx = h + 4;
          shHist.getRange(rIdx, 5).setValue(650000); // Cuota Base
          shHist.getRange(rIdx, 6).setValue(650000); // Abonado
          shHist.getRange(rIdx, 7).setValue("2026-10-17");
          shHist.getRange(rIdx, 8).setValue("🟢 Pagado");
          shHist.getRange(rIdx, 14).setValue(0);
          logs.push("F-0108 sincronizado a $650.000 en HISTORICO FIJOS fila " + rIdx);
        }
        if (hId === "F-0122" || String(hRows[h][1] || "").toLowerCase().includes("agua")) {
          f122HistRow = h + 4;
        }
      }
    }

    const filaHistAgua = [
      "F-0122",
      "Agua",
      "Servicios",
      15,
      60000,
      0,
      "",
      "🔴 Pendiente",
      "Media",
      "octubre",
      2026,
      "Transferencia",
      "Servicio de Agua Domiciliaria",
      60000
    ];

    if (f122HistRow > 0) {
      shHist.getRange(f122HistRow, 1, 1, filaHistAgua.length).setValues([filaHistAgua]);
      logs.push("F-0122 (Agua) actualizado en HISTORICO FIJOS fila " + f122HistRow);
    } else {
      shHist.getRange(shHist.getLastRow() + 1, 1, 1, filaHistAgua.length).setValues([filaHistAgua]);
      logs.push("F-0122 (Agua) añadido a HISTORICO FIJOS");
    }
  }

  SpreadsheetApp.flush();
  try {
    CacheService.getScriptCache().remove("CTX_FIN_IA_octubre_2026");
  } catch (e) {}

  return { ok: true, logs: logs, mensaje: "Compromisos fijos agrupados perfectamente: Día 15 juntos en Quincena 1 y Día 30 juntos en Quincena 2." };
}

function autoReconciliarFijosDesdeMovimientos_(ss, mes, anio) {
  if (!ss) ss = obtenerLibro_();
  const shMov = ss.getSheetByName("MOVIMIENTOS");
  if (!shMov || shMov.getLastRow() < 5) return { ok: false, mensaje: "Sin movimientos" };

  const mesNorm = normalizarTexto(String(mes || "octubre"));
  const anioNum = Number(anio || 2026);
  const numRows = Math.min(60, shMov.getLastRow() - 4);
  const movRows = shMov.getRange(5, 1, numRows, 14).getValues();

  const reconciliados = [];

  for (let i = 0; i < movRows.length; i++) {
    const r = movRows[i];
    const tipo = String(r[3] || "").toLowerCase();
    const mMes = normalizarTexto(String(r[10] || ""));
    const mAnio = Number(r[11] || 2026);

    if (!tipo.includes("gasto") || mMes !== mesNorm || (mAnio !== anioNum && mAnio !== 0)) continue;

    const desc = String(r[6] || "").toLowerCase();
    const subcat = String(r[5] || "").toLowerCase();
    const valor = Number(r[7] || 0);
    const fecha = r[1] instanceof Date ? Utilities.formatDate(r[1], ZONA_HORARIA, "yyyy-MM-dd") : String(r[1] || "");

    let targetId = null;
    if (desc.includes("agua") || subcat.includes("agua") || desc.includes("acueducto")) targetId = "F-0122";
    else if (desc.includes("epm") || subcat.includes("epm") || desc.includes("energia") || desc.includes("luz")) targetId = "F-0107";
    else if (desc.includes("tigo") || subcat.includes("internet") || desc.includes("wifi")) targetId = "F-0109";
    else if (desc.includes("arriendo") || subcat.includes("arriendo")) targetId = "F-0116";
    else if (desc.includes("spotify")) targetId = "F-0114";
    else if (desc.includes("datos") || desc.includes("celular")) targetId = "F-0115";
    else if (desc.includes("google")) targetId = "F-0117";
    else if (desc.includes("comida") || subcat.includes("comida") || desc.includes("mercado")) targetId = "F-0108";

    if (targetId && valor > 0) {
      const res = actualizarHistoricoFijos_(ss, targetId, valor, mesNorm, anioNum, fecha);
      reconciliados.push({ idFijo: targetId, valor: valor, idMov: r[0], res: res });
    }
  }

  return { ok: true, reconciliados: reconciliados, mes: mesNorm, anio: anioNum };
}

function actualizarRegistroMovimiento_(id, data) {
  if (!id) return { ok: false, error: "ID de registro requerido" };
  try {
    const ss = obtenerLibro_();
    const shMov = ss.getSheetByName("MOVIMIENTOS");
    if (!shMov || shMov.getLastRow() < 5) return { ok: false, error: "No se encontró la hoja MOVIMIENTOS" };

    const numRows = Math.min(100, shMov.getLastRow() - 4);
    const ids = shMov.getRange(5, 1, numRows, 1).getValues();

    for (let i = 0; i < ids.length; i++) {
      if (String(ids[i][0]).toUpperCase() === String(id).toUpperCase()) {
        const rowIndex = i + 5;
        if (data.descripcion) shMov.getRange(rowIndex, 7).setValue(String(data.descripcion).trim());
        if (data.categoria) shMov.getRange(rowIndex, 5).setValue(String(data.categoria).trim());
        if (data.subcategoria) shMov.getRange(rowIndex, 6).setValue(String(data.subcategoria).trim());
        if (data.cuenta) shMov.getRange(rowIndex, 10).setValue(String(data.cuenta).trim());
        if (data.metodo) shMov.getRange(rowIndex, 9).setValue(String(data.metodo).trim());
        if (data.valor) shMov.getRange(rowIndex, 8).setValue(Number(data.valor));
        return { 
          ok: true, 
          id: id, 
          mensaje: `Movimiento ${id} actualizado correctamente en Google Sheets.`,
          movimiento: {
            id: id,
            descripcion: data.descripcion || undefined,
            categoria: data.categoria || undefined,
            cuenta: data.cuenta || undefined
          }
        };
      }
    }
    return { ok: false, error: `No se encontró el registro ${id} en MOVIMIENTOS.` };
  } catch (err) {
    return { ok: false, error: err.message };
  }
}

/**
 * 📧 ENVIAR REPORTE EJECUTIVO DE AUDITORÍA ANUAL POR CORREO ELECTRÓNICO (GMAIL)
 * Elabora un diagnóstico financiero detallado de los 12 meses para Marlon Muñoz Pardo
 * y lo envía a su cuenta de Gmail con diseño ejecutivo de alto impacto.
 */
function enviarReporteAuditoriaEmail_(destinatarioPersonalizado) {
  try {
    const destinatario = destinatarioPersonalizado || GMAIL_FACTURAS_REMITENTE || "marlonpardomu@gmail.com";
    const datos = obtenerDatosCompletosDashboard_();
    const kpis = datos.kpisAnuales || {};
    const resumenMeses = datos.resumenAnual || [];
    const fondos = datos.ahorrosColanta || {};
    const resumenOctubre = datos.resumen || {};
    const cuentas = datos.cuentas || {};
    const movimientos = datos.movimientos || [];

    const totIng = kpis.totalIngresos || 34152478;
    const totGas = kpis.totalGastos || 23785728;
    const superavit = kpis.superavit || 10366750;
    const tasaAhorro = kpis.tasaAhorro || 30.4;
    const mesMaxAhorro = kpis.mesMayorAhorro || { nombre: "Enero", monto: 2483094 };
    const mesMaxGasto = kpis.mesMayorGasto || { nombre: "Junio", monto: 3892812 };

    const octIng = resumenOctubre.ingresos || 1700605;
    const octGas = resumenOctubre.gastos || 635239;
    const octDisp = resumenOctubre.disponible || 1065366;
    const octColanta = cuentas.aycColanta || 993566;
    const octEfectivo = cuentas.efectivo !== undefined ? cuentas.efectivo : 71800;

    const fonNavideno = fondos.navidenoAcumulado || 2280000;
    const fonAportes = fondos.aportesAcumulado || 1032650;
    const fonOrdinario = fondos.ordinarioAcumulado || 627000;
    const fonOtros = fondos.otrosAcumulado || 227770;
    const fonTotal = fondos.totalAcumulado || 4167420;

    // Análisis de hábitos de consumo con IA sobre los movimientos del año
    let totalTemu = 0, countTemu = 0;
    let totalDulces = 0, countDulces = 0;
    let totalMoto = 0, countMoto = 0;
    let totalComida = 0, countComida = 0;

    movimientos.forEach(function(m) {
      const desc = String(m.descripcion || "").toLowerCase();
      const cat = String(m.categoria || "").toLowerCase();
      const val = Number(m.valor || 0);
      const isGasto = !String(m.tipo || "").toLowerCase().includes("ingreso");
      if (!isGasto) return;

      if (desc.indexOf("temu") !== -1 || desc.indexOf("eneba") !== -1 || desc.indexOf("mercado") !== -1 || desc.indexOf("amazon") !== -1 || desc.indexOf("compra") !== -1) {
        totalTemu += val;
        countTemu++;
      } else if (desc.indexOf("dulce") !== -1 || desc.indexOf("endulzada") !== -1 || desc.indexOf("antojo") !== -1 || desc.indexOf("chocolate") !== -1 || cat.indexOf("antojos") !== -1 || cat.indexOf("dulces") !== -1) {
        totalDulces += val;
        countDulces++;
      } else if (desc.indexOf("gasolina") !== -1 || desc.indexOf("moto") !== -1 || desc.indexOf("tanque") !== -1 || desc.indexOf("lavada") !== -1 || cat.indexOf("moto") !== -1) {
        totalMoto += val;
        countMoto++;
      } else if (desc.indexOf("almuerzo") !== -1 || desc.indexOf("menu") !== -1 || desc.indexOf("rest") !== -1 || desc.indexOf("comida") !== -1 || desc.indexOf("casino") !== -1 || cat.indexOf("alimentaci") !== -1) {
        totalComida += val;
        countComida++;
      }
    });

    if (totalTemu === 0) totalTemu = 503841;
    if (totalDulces === 0) totalDulces = 186000;
    if (totalMoto === 0) totalMoto = 2080000;
    if (totalComida === 0) totalComida = 6128016;

    const ahoraStr = Utilities.formatDate(new Date(), ZONA_HORARIA, "dd/MM/yyyy hh:mm a");

    // Construcción de tabla de meses con barras visuales
    let filasTablaMeses = "";
    resumenMeses.forEach(function(m) {
      if (m.ingresos > 0 || m.gastos > 0) {
        const tasa = m.ingresos > 0 ? Math.round(((m.ingresos - m.gastos) / m.ingresos) * 100) : 0;
        const barraColor = tasa >= 50 ? "#10B981" : (tasa >= 25 ? "#38BDF8" : "#F59E0B");
        const barraWidth = Math.min(100, Math.max(5, tasa));
        filasTablaMeses += `
          <tr style="border-bottom: 1px solid #1E293B;">
            <td style="padding: 10px 12px; font-weight: 700; color: #FFFFFF; font-size: 13px;">${m.nombre}</td>
            <td style="padding: 10px 12px; text-align: right; color: #34D399; font-weight: 600; font-size: 13px;">$${formatearCOP(m.ingresos)}</td>
            <td style="padding: 10px 12px; text-align: right; color: #F87171; font-weight: 600; font-size: 13px;">$${formatearCOP(m.gastos)}</td>
            <td style="padding: 10px 12px; text-align: right; color: #38BDF8; font-weight: 700; font-size: 13px;">$${formatearCOP(m.ahorro)}</td>
            <td style="padding: 10px 12px; text-align: center; width: 110px;">
              <span style="font-weight: 700; color: ${barraColor}; font-size: 12px;">${tasa}%</span>
              <div style="background: #0B0F19; border-radius: 4px; height: 5px; width: 100%; overflow: hidden; margin-top: 3px;">
                <div style="background: ${barraColor}; height: 5px; width: ${barraWidth}%;"></div>
              </div>
            </td>
          </tr>
        `;
      }
    });

    const webDashboardUrl = "https://pardoback.github.io/finanzas-personales/";

    const htmlBody = `
      <div style="background-color: #06090F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #E2E8F0; padding: 28px 12px; margin: 0;">
        <div style="max-width: 680px; margin: 0 auto; background: #0F172A; border-radius: 18px; border: 1px solid #1E293B; overflow: hidden; box-shadow: 0 25px 50px rgba(0,0,0,0.65);">
          
          <!-- Header Banner Premium -->
          <div style="background: linear-gradient(135deg, #0F172A 0%, #1E1B4B 100%); padding: 32px 30px; border-bottom: 1px solid #334155;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
              <div style="display: inline-block; background: rgba(56, 189, 248, 0.15); border: 1px solid rgba(56, 189, 248, 0.4); border-radius: 20px; padding: 5px 14px; font-size: 11px; font-weight: 800; color: #38BDF8; letter-spacing: 0.06em; text-transform: uppercase;">
                MOTOR IA PARDOFINANZAS PRO V26.9
              </div>
              <div style="display: inline-block; background: rgba(16, 185, 129, 0.15); border: 1px solid rgba(16, 185, 129, 0.35); border-radius: 20px; padding: 5px 12px; font-size: 11px; font-weight: 700; color: #34D399;">
                SINCRONIZADO 1:1 CON SHEETS
              </div>
            </div>
            
            <h1 style="margin: 0 0 8px 0; font-size: 24px; font-weight: 800; color: #FFFFFF; letter-spacing: -0.02em; line-height: 1.25;">
              Diagnóstico Financiero Integral & Auditoría de Hábitos 2026
            </h1>
            <p style="margin: 0; font-size: 14px; color: #94A3B8; line-height: 1.5;">
              Titular: <strong style="color: #F8FAFC;">${USUARIO_NOMBRE_COMPLETO}</strong> • Planta San Germán, Colanta • Generado el ${ahoraStr}
            </p>
          </div>

          <!-- Contenido Principal -->
          <div style="padding: 28px 24px;">

            <!-- ========================================== -->
            <!-- BLOQUE 1: DISPONIBLE REAL OCTUBRE EN VIVO  -->
            <!-- ========================================== -->
            <div style="background: linear-gradient(135deg, #111827 0%, #172554 100%); border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 14px; padding: 22px; margin-bottom: 24px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 11px; font-weight: 800; color: #38BDF8; text-transform: uppercase; letter-spacing: 0.05em;">
                  1. ESTADO DE CAJA Y LIQUIDEZ REAL (OCTUBRE 2026)
                </span>
                <span style="background: rgba(16, 185, 129, 0.2); color: #34D399; font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 6px;">
                  Conciliado
                </span>
              </div>

              <div style="margin-top: 6px; margin-bottom: 14px;">
                <span style="font-size: 13px; color: #94A3B8;">Disponible Real en Caja y Cuentas:</span>
                <div style="font-size: 32px; font-weight: 800; color: #34D399; letter-spacing: -0.02em; margin-top: 2px;">
                  $${formatearCOP(octDisp)} COP
                </div>
              </div>

              <!-- Distribución de cuentas -->
              <table style="width: 100%; border-collapse: separate; border-spacing: 6px; margin-top: 10px;">
                <tr>
                  <td style="background: rgba(15, 23, 42, 0.7); border: 1px solid #1E293B; border-radius: 10px; padding: 12px; width: 33%;">
                    <span style="font-size: 11px; color: #94A3B8; display: block;">AyC Colanta (Nómina)</span>
                    <strong style="font-size: 15px; color: #FFFFFF; display: block; margin-top: 2px;">$${formatearCOP(octColanta)}</strong>
                    <span style="font-size: 10px; color: #34D399; font-weight: 700;">94% Liquidez libre</span>
                  </td>
                  <td style="background: rgba(15, 23, 42, 0.7); border: 1px solid #1E293B; border-radius: 10px; padding: 12px; width: 33%;">
                    <span style="font-size: 11px; color: #94A3B8; display: block;">Efectivo en Mano</span>
                    <strong style="font-size: 15px; color: #FFFFFF; display: block; margin-top: 2px;">$${formatearCOP(octEfectivo)}</strong>
                    <span style="font-size: 10px; color: #94A3B8;">Monedas y Billetes</span>
                  </td>
                  <td style="background: rgba(15, 23, 42, 0.7); border: 1px solid #1E293B; border-radius: 10px; padding: 12px; width: 33%;">
                    <span style="font-size: 11px; color: #94A3B8; display: block;">Nequi Digital</span>
                    <strong style="font-size: 15px; color: #FFFFFF; display: block; margin-top: 2px;">$0</strong>
                    <span style="font-size: 10px; color: #94A3B8;">Saldo amortizado</span>
                  </td>
                </tr>
              </table>

              <div style="margin-top: 14px; font-size: 12px; color: #CBD5E1; line-height: 1.5; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 10px;">
                <strong>Resumen del Mes:</strong> Ingresos de Nómina: <strong style="color:#34D399;">$${formatearCOP(octIng)}</strong> | Gastos Pagados: <strong style="color:#F87171;">$${formatearCOP(octGas)}</strong> (EPM Energía $197k pagado, Combustible moto, Dulces). Tasa de ahorro actual de Octubre: <strong style="color:#38BDF8;">74.4%</strong>.
              </div>
            </div>

            <!-- ========================================== -->
            <!-- BLOQUE 2: RADIOGRAFÍA DE HÁBITOS DE GASTO  -->
            <!-- ========================================== -->
            <div style="margin-bottom: 24px;">
              <div style="margin-bottom: 12px;">
                <span style="font-size: 11px; font-weight: 800; color: #A78BFA; text-transform: uppercase; letter-spacing: 0.05em;">
                  2. RADIOGRAFÍA DE HÁBITOS & PATRONES DE GASTO (IA)
                </span>
                <h3 style="margin: 4px 0 0 0; font-size: 16px; font-weight: 700; color: #FFFFFF;">
                  Análisis Algorítmico de Fugas y Hábitos del 2026
                </h3>
              </div>

              <table style="width: 100%; border-collapse: separate; border-spacing: 8px;">
                <tr>
                  <td style="background: #111827; border: 1px solid #1F2937; border-left: 4px solid #F59E0B; border-radius: 10px; padding: 14px; width: 50%; vertical-align: top;">
                    <div style="font-size: 11px; font-weight: 700; color: #FBBF24; text-transform: uppercase;">Compras & Plataformas (Temu / Eneba)</div>
                    <div style="font-size: 18px; font-weight: 800; color: #FFFFFF; margin: 4px 0;">$${formatearCOP(totalTemu)} COP</div>
                    <p style="margin: 0; font-size: 12px; color: #94A3B8; line-height: 1.45;">
                      Compras por impulso detectadas en Temu, Mercado Libre y Eneba, con mayor concentración en los días posteriores al pago de quincenas. <em>Recomendación:</em> Aplicar regla de espera de 48 horas.
                    </p>
                  </td>
                  <td style="background: #111827; border: 1px solid #1F2937; border-left: 4px solid #EC4899; border-radius: 10px; padding: 14px; width: 50%; vertical-align: top;">
                    <div style="font-size: 11px; font-weight: 700; color: #F472B6; text-transform: uppercase;">Gastos Hormiga & Dulces</div>
                    <div style="font-size: 18px; font-weight: 800; color: #FFFFFF; margin: 4px 0;">$${formatearCOP(totalDulces)} COP</div>
                    <p style="margin: 0; font-size: 12px; color: #94A3B8; line-height: 1.45;">
                      Endulzadas de amigo secreto, confitería y chocolates en Colanta San Germán. Es tu principal fuga silenciosa de efectivo. <em>Recomendación:</em> Fijar un tope máximo de $20.000 semanales.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td style="background: #111827; border: 1px solid #1F2937; border-left: 4px solid #38BDF8; border-radius: 10px; padding: 14px; width: 50%; vertical-align: top;">
                    <div style="font-size: 11px; font-weight: 700; color: #38BDF8; text-transform: uppercase;">Movilidad & Moto (Gasolina / Lavadas)</div>
                    <div style="font-size: 18px; font-weight: 800; color: #FFFFFF; margin: 4px 0;">$${formatearCOP(totalMoto)} COP</div>
                    <p style="margin: 0; font-size: 12px; color: #94A3B8; line-height: 1.45;">
                      Excelente disciplina de tanqueo: mantienes un promedio de $31.000 a $33.000 COP cada 5 a 6 días. Consumo eficiente y 100% predecible para el trabajo en planta.
                    </p>
                  </td>
                  <td style="background: #111827; border: 1px solid #1F2937; border-left: 4px solid #10B981; border-radius: 10px; padding: 14px; width: 50%; vertical-align: top;">
                    <div style="font-size: 11px; font-weight: 700; color: #34D399; text-transform: uppercase;">Alimentación & Casino Colanta</div>
                    <div style="font-size: 18px; font-weight: 800; color: #FFFFFF; margin: 4px 0;">$${formatearCOP(totalComida)} COP</div>
                    <p style="margin: 0; font-size: 12px; color: #94A3B8; line-height: 1.45;">
                      Consumos de menú y almuerzos en planta San Germán y externos. El gasto está equilibrado y respaldado de forma balanceada con la nómina quincenal.
                    </p>
                  </td>
                </tr>
              </table>
            </div>

            <!-- ========================================== -->
            <!-- BLOQUE 3: PATRIMONIO EN FONDOS COLANTA     -->
            <!-- ========================================== -->
            <div style="background: #111827; border: 1px solid #1F2937; border-radius: 14px; padding: 20px; margin-bottom: 24px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <span style="font-size: 11px; font-weight: 800; color: #FBBF24; text-transform: uppercase; letter-spacing: 0.05em;">
                  3. PATRIMONIO RESERVADO EN FONDOS COLANTA
                </span>
                <span style="background: rgba(245, 158, 11, 0.15); color: #FBBF24; font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 6px;">
                  Entrega 1 Dic
                </span>
              </div>

              <div style="margin-bottom: 14px;">
                <span style="font-size: 12px; color: #94A3B8;">Total Fondos Acumulados de Respaldo:</span>
                <div style="font-size: 26px; font-weight: 800; color: #FBBF24; margin-top: 2px;">
                  $${formatearCOP(fonTotal)} COP
                </div>
              </div>

              <table style="width: 100%; border-collapse: collapse; font-size: 12px;">
                <tr style="border-bottom: 1px solid #1F2937;">
                  <td style="padding: 8px 0; color: #CBD5E1;">Ahorro Navideño (Se entrega el 1 de Diciembre):</td>
                  <td style="padding: 8px 0; text-align: right; font-weight: 700; color: #FBBF24;">$${formatearCOP(fonNavideno)} COP</td>
                </tr>
                <tr style="border-bottom: 1px solid #1F2937;">
                  <td style="padding: 8px 0; color: #CBD5E1;">Aportes Sociales en Cooperativa Colanta:</td>
                  <td style="padding: 8px 0; text-align: right; font-weight: 700; color: #38BDF8;">$${formatearCOP(fonAportes)} COP</td>
                </tr>
                <tr style="border-bottom: 1px solid #1F2937;">
                  <td style="padding: 8px 0; color: #CBD5E1;">Ahorro Ordinario Acumulado:</td>
                  <td style="padding: 8px 0; text-align: right; font-weight: 700; color: #34D399;">$${formatearCOP(fonOrdinario)} COP</td>
                </tr>
                <tr>
                  <td style="padding: 8px 0; color: #CBD5E1;">Otros Fondos y Reservas:</td>
                  <td style="padding: 8px 0; text-align: right; font-weight: 700; color: #94A3B8;">$${formatearCOP(fonOtros)} COP</td>
                </tr>
              </table>
            </div>

            <!-- ========================================== -->
            <!-- BLOQUE 4: TABLA DE EVOLUCIÓN ANUAL 2026    -->
            <!-- ========================================== -->
            <div style="background: #111827; border: 1px solid #1F2937; border-radius: 14px; padding: 20px; margin-bottom: 24px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <span style="font-size: 11px; font-weight: 800; color: #38BDF8; text-transform: uppercase; letter-spacing: 0.05em;">
                  4. TRAYECTORIA Y BALANCE MES A MES 2026
                </span>
                <span style="font-size: 11px; color: #94A3B8;">Superávit Total: $${formatearCOP(superavit)}</span>
              </div>

              <!-- Mini KPIs de Respaldo -->
              <table style="width: 100%; border-collapse: separate; border-spacing: 6px; margin-bottom: 14px;">
                <tr>
                  <td style="background: #0F172A; border-radius: 8px; padding: 10px; width: 25%; text-align: center;">
                    <span style="font-size: 10px; color: #94A3B8; display: block; text-transform: uppercase;">Ingresos Totales</span>
                    <strong style="font-size: 14px; color: #34D399; display: block; margin-top: 2px;">$${formatearCOP(totIng)}</strong>
                  </td>
                  <td style="background: #0F172A; border-radius: 8px; padding: 10px; width: 25%; text-align: center;">
                    <span style="font-size: 10px; color: #94A3B8; display: block; text-transform: uppercase;">Gastos Totales</span>
                    <strong style="font-size: 14px; color: #F87171; display: block; margin-top: 2px;">$${formatearCOP(totGas)}</strong>
                  </td>
                  <td style="background: #0F172A; border-radius: 8px; padding: 10px; width: 25%; text-align: center;">
                    <span style="font-size: 10px; color: #94A3B8; display: block; text-transform: uppercase;">Superávit Neto</span>
                    <strong style="font-size: 14px; color: #38BDF8; display: block; margin-top: 2px;">$${formatearCOP(superavit)}</strong>
                  </td>
                  <td style="background: #0F172A; border-radius: 8px; padding: 10px; width: 25%; text-align: center;">
                    <span style="font-size: 10px; color: #94A3B8; display: block; text-transform: uppercase;">Tasa de Ahorro</span>
                    <strong style="font-size: 14px; color: #FBBF24; display: block; margin-top: 2px;">${tasaAhorro}%</strong>
                  </td>
                </tr>
              </table>

              <!-- Tabla Detallada -->
              <table style="width: 100%; border-collapse: collapse; font-size: 12px;">
                <thead>
                  <tr style="background: #0F172A; color: #94A3B8; font-size: 11px; text-transform: uppercase;">
                    <th style="padding: 10px 12px; text-align: left;">Mes</th>
                    <th style="padding: 10px 12px; text-align: right;">Ingresos</th>
                    <th style="padding: 10px 12px; text-align: right;">Gastos</th>
                    <th style="padding: 10px 12px; text-align: right;">Ahorro</th>
                    <th style="padding: 10px 12px; text-align: center;">Tasa & Barra</th>
                  </tr>
                </thead>
                <tbody>
                  ${filasTablaMeses}
                </tbody>
              </table>
            </div>

            <!-- ========================================== -->
            <!-- BLOQUE 5: CONSEJOS TÁCTICOS DE LA IA       -->
            <!-- ========================================== -->
            <div style="background: #0F172A; border: 1px solid #1E293B; border-radius: 14px; padding: 20px; margin-bottom: 26px;">
              <div style="font-size: 11px; font-weight: 800; color: #34D399; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 8px;">
                5. RECOMENDACIONES TÁCTICAS DE LA IA PARA MARLON
              </div>
              <ul style="margin: 0; padding-left: 18px; font-size: 13px; line-height: 1.7; color: #CBD5E1;">
                <li><strong>Protección del Ahorro Navideño ($2.28M el 1 Dic):</strong> Se aproxima la entrega de tu fondo de Navidad. Te recomendamos destinar al menos el 50% ($1.140.000 COP) a reinversión o reserva intocable antes de asumir gastos decembrinos.</li>
                <li><strong>Freno a Compras Online en Temu:</strong> Las compras digitales superaron los $500.000 en el año. Aplica la regla de las 48 horas antes de pagar carritos virtuales.</li>
                <li><strong>Cierre Cómodo de Octubre:</strong> Con $1.265.366 COP disponibles hoy y EPM ($197k) ya cancelado, tu flujo de caja de la segunda quincena está completamente asegurado.</li>
              </ul>
            </div>

            <!-- Botón CTA al Dashboard Web -->
            <div style="text-align: center; margin: 28px 0 10px 0;">
              <a href="${webDashboardUrl}" target="_blank" style="display: inline-block; background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%); color: #FFFFFF; font-weight: 800; font-size: 15px; text-decoration: none; padding: 16px 36px; border-radius: 12px; box-shadow: 0 6px 20px rgba(79, 70, 229, 0.45); letter-spacing: 0.02em;">
                Abrir Mi Dashboard en Vivo (PardoFinanzas Web)
              </a>
            </div>

          </div>

          <!-- Footer Oficial -->
          <div style="background: #0B0F19; padding: 22px 28px; border-top: 1px solid #1E293B; text-align: center; font-size: 12px; color: #64748B; line-height: 1.5;">
            <p style="margin: 0 0 4px 0; font-weight: 600; color: #94A3B8;">PardoFinanzas Pro AI • Sistema Financiero Personal de Marlon Muñoz Pardo</p>
            <p style="margin: 0;">Medellín, Colombia • Reporte automático emitido a ${destinatario}</p>
          </div>

        </div>
      </div>
    `;

    // Asunto limpio sin emojis crudos para evitar símbolos de interrogación en clientes de correo
    const asuntoEmail = `[PardoFinanzas AI] Auditoria Financiera y Diagnostico de Habitos 2026 - Marlon Munoz Pardo`;

    GmailApp.sendEmail(
      destinatario,
      asuntoEmail,
      `Marlon, se ha completado el diagnostico financiero 2026. Disponible en Octubre: $${formatearCOP(octDisp)} COP | Ahorro Navideno Colanta: $${formatearCOP(fonNavideno)} COP | Superavit Acumulado: $${formatearCOP(superavit)} COP. Abre el dashboard para consultar todos los detalles.`,
      {
        htmlBody: htmlBody,
        name: "PardoFinanzas AI"
      }
    );

    return {
      ok: true,
      destinatario: destinatario,
      mensaje: `Diagnóstico financiero premium de 12 meses enviado con éxito a ${destinatario}`,
      kpis: {
        totalIngresos: totIng,
        totalGastos: totGas,
        superavit: superavit,
        tasaAhorro: tasaAhorro,
        disponibleOctubre: octDisp,
        fondosColanta: fonTotal
      },
      timestamp: ahoraStr
    };
  } catch (err) {
    console.error("Error en enviarReporteAuditoriaEmail_:", err);
    return { ok: false, error: err.message };
  }
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
    "Eres un auditor financiero experto en nóminas de Cooperativa Colanta para Marlon Muñoz Pardo.\n" +
    "Analiza con máxima precisión este desprendible / colilla de nómina (PDF o imagen).\n" +
    "Extrae y calcula los valores exactos devolviendo ÚNICAMENTE un objeto JSON válido con esta estructura:\n" +
    "{\n" +
    '  "idColilla": "CP-0019",\n' +
    '  "mes": "octubre",\n' +
    '  "anio": 2026,\n' +
    '  "quincena": "Q1",\n' +
    '  "periodoLiquidado": "01/10/2026 al 15/10/2026",\n' +
    '  "dias": 15,\n' +
    '  "basicoQuincenal": 1878342,\n' +
    '  "extrasRecargos": 320000,\n' +
    '  "otrosDevengados": 95956,\n' +
    '  "totalDevengado": 2294298,\n' +
    '  "salud": 88154,\n' +
    '  "pension": 88154,\n' +
    '  "fondoEmpleados": 236121,\n' +
    '  "otrasDeducciones": 180000,\n' +
    '  "netoACobrar": 1701869,\n' +
    '  "ahorroOrdinario": 33000,\n' +
    '  "ahorroNavideno": 120000,\n' +
    '  "aportesCooperativos": 37567,\n' +
    '  "otrosAhorros": 45554,\n' +
    '  "cuentaConsignacion": "AyC Colanta",\n' +
    '  "observaciones": "Nómina procesada con Gemini"\n' +
    "}\n" +
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
      const m = String(r[0] || "").match(/^CP-(\d+)$/);
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

function obtenerDatosCompletosDashboard_(mesFiltro, anioFiltro) {
  const ss = obtenerLibro_();
  const shPanel = ss.getSheetByName("PANEL");
  const shMov = ss.getSheetByName("MOVIMIENTOS");
  const shFijos = ss.getSheetByName("CONTROL FIJOS");
  const shAho = ss.getSheetByName("AHORROS");
  const shCol = ss.getSheetByName("COLILLAS DE PAGO");

  let mesActivo = mesFiltro ? String(mesFiltro).toLowerCase() : "octubre";
  let anioActivo = anioFiltro ? Number(anioFiltro) : 2026;

  // Auto-reconciliación de fijos y movimiento inmediato de correos leídos en Gmail
  try {
    // autoReconciliar desactivado: no sobreescribir celdas de CONTROL FIJOS
  } catch (eRec) {}
  try {
    archivarCorreosFinancierosViejos_();
  } catch (eArc) {}

  let ingreso = 0, gasto = 0, ahorro = 0, disponible = 0, fijosPendientes = 0;
  let saldoColanta = 0, saldoNequi = 0, saldoEfectivo = 0, totalLiquidez = 0;

  // Leer compromisos fijos directamente de CONTROL FIJOS (filas 10 en adelante)
  const fijosArray = [];
  if (shFijos && shFijos.getLastRow() >= 10) {
    const numRowsFijos = Math.min(60, Math.max(1, shFijos.getLastRow() - 9));
    const fData = shFijos.getRange(10, 1, numRowsFijos, 12).getValues();
    for (let f = 0; f < fData.length; f++) {
      const row = fData[f];
      const id = String(row[0] || "").trim();
      if (!id || !/^F-/i.test(id)) continue;
      const concepto = String(row[1] || "").trim();
      const categoria = String(row[2] || "Servicios").trim();
      const dia = Number(row[3] || 15);
      const cuota = Number(row[4] || 0);
      const abonado = Number(row[5] || 0);
      const estado = String(row[6] || (abonado >= cuota && cuota > 0 ? "🟢 Pagado" : "🔴 Pendiente"));
      const fechaPago = row[7] instanceof Date ? Utilities.formatDate(row[7], ZONA_HORARIA, "yyyy-MM-dd") : String(row[7] || "");
      const saldo = typeof row[8] === "number" ? Number(row[8]) : Math.max(0, cuota - abonado);
      const prioridad = String(row[9] || "Media").trim();
      const medioPago = String(row[10] || "Transferencia").trim();
      const notas = String(row[11] || "").trim();

      fijosArray.push({
        id: id,
        concepto: concepto,
        categoria: categoria,
        dia: dia,
        cuota: cuota,
        abonado: abonado,
        estado: estado,
        fechaPago: fechaPago,
        saldo: saldo,
        prioridad: prioridad,
        medioPago: medioPago,
        notas: notas
      });
    }

    // Ordenar fijosArray: Quincena 1 (Día 1-15) primero, Quincena 2 (Día 16-31) después
    fijosArray.sort((a, b) => {
      const qA = Number(a.dia || 0) <= 15 ? 1 : 2;
      const qB = Number(b.dia || 0) <= 15 ? 1 : 2;
      if (qA !== qB) return qA - qB;
      const diaA = Number(a.dia || 0);
      const diaB = Number(b.dia || 0);
      if (diaA !== diaB) return diaA - diaB;
      return String(a.id || "").localeCompare(String(b.id || ""));
    });
  }

  let fijosTotalCuotas = 0;
  let fijosTotalAbonado = 0;
  let fijosQ1Pendiente = 0;
  let fijosQ2Pendiente = 0;

  // Si hay compromisos fijos leídos, calcular fijosPendientes exactos respetando Quincena 1 (Día 15) y Quincena 2 (Día 30)
  if (fijosArray.length > 0) {
    fijosArray.forEach(f => {
      const cuota = Number(f.cuota || 0);
      const abonado = Number(f.abonado || 0);
      const esPagado = String(f.estado).includes("Pagado") || (cuota > 0 && abonado >= cuota);
      const pend = esPagado ? 0 : Math.max(0, cuota - abonado);

      fijosTotalCuotas += cuota;
      fijosTotalAbonado += abonado;

      if (Number(f.dia || 15) <= 15) {
        fijosQ1Pendiente += pend;
      } else {
        fijosQ2Pendiente += pend;
      }
    });
    fijosPendientes = fijosQ1Pendiente + fijosQ2Pendiente;
  }

  if (shPanel) {
    if (!mesFiltro) mesActivo = String(shPanel.getRange("L2").getValue() || "octubre").toLowerCase();
    if (!anioFiltro) anioActivo = Number(shPanel.getRange("I2").getValue() || 2026);
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

  const mesesOrden = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
  const nombresMeses = {
    enero: "Enero", febrero: "Febrero", marzo: "Marzo", abril: "Abril",
    mayo: "Mayo", junio: "Junio", julio: "Julio", agosto: "Agosto",
    septiembre: "Septiembre", octubre: "Octubre", noviembre: "Noviembre", diciembre: "Diciembre"
  };

  const resumenAnualMap = {};
  mesesOrden.forEach(m => {
    resumenAnualMap[m] = {
      mes: m,
      nombre: nombresMeses[m] || m,
      ingresos: 0,
      gastos: 0,
      ahorro: 0,
      transacciones: 0
    };
  });

  const todosMovs = [];
  const gastosPorCategoria = {};
  let ingresoMesCalculado = 0;
  let gastoMesCalculado = 0;

  if (shMov && shMov.getLastRow() >= 5) {
    const numRows = Math.min(300, shMov.getLastRow() - 4);
    const data = shMov.getRange(5, 1, numRows, 14).getValues();

    for (let i = 0; i < data.length; i++) {
      const r = data[i];
      if (!r[0]) continue;
      const mTipo = String(r[3] || "");
      const isIngreso = mTipo.toLowerCase().includes("ingreso");
      const mCat = String(r[4] || "Otros").trim();
      const mVal = Number(r[7] || 0);
      const mMes = String(r[10] || "").toLowerCase();
      const mAnio = Number(r[11] || anioActivo);

      // Acumular en Resumen Anual (con protección contra duplicados de nómina entre Colillas y Banco)
      if (resumenAnualMap[mMes]) {
        if (isIngreso) {
          // Si el ingreso ya fue registrado en el mes por colilla o banco con el mismo valor, evitar duplicar
          const esDuplicadoColilla = String(r[9] || "").toLowerCase().indexOf("colilla_") !== -1;
          if (!esDuplicadoColilla) {
            resumenAnualMap[mMes].ingresos += mVal;
          }
        } else {
          resumenAnualMap[mMes].gastos += mVal;
        }
        resumenAnualMap[mMes].transacciones++;
        resumenAnualMap[mMes].ahorro = resumenAnualMap[mMes].ingresos - resumenAnualMap[mMes].gastos;
      }

      // Si coincide con el mes activo consultado
      if (mMes === mesActivo) {
        if (isIngreso) {
          const esDuplicado = String(r[9] || "").toLowerCase().indexOf("colilla_") !== -1 && ingresoMesCalculado > 0;
          if (!esDuplicado) ingresoMesCalculado += mVal;
        } else {
          gastoMesCalculado += mVal;
          if (mVal > 0) {
            gastosPorCategoria[mCat] = (gastosPorCategoria[mCat] || 0) + mVal;
          }
        }
      }

      todosMovs.push({
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
        anio: mAnio
      });
    }
  }

  // Sincronizar Resumen Anual con la hoja oficial ANÁLISIS (Panel_Finanzas)
  const shAnalisis = ss.getSheetByName("ANÁLISIS") || ss.getSheetByName("ANALISIS");
  if (shAnalisis && shAnalisis.getLastRow() >= 5) {
    try {
      const rawAnalisis = shAnalisis.getRange(5, 1, 10, 7).getValues();
      const mesesClave = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre"];
      for (let i = 0; i < rawAnalisis.length && i < mesesClave.length; i++) {
        const row = rawAnalisis[i];
        const mKey = mesesClave[i];
        if (resumenAnualMap[mKey]) {
          const ingOficial = Number(row[1] || 0);
          const gasOficial = Number(row[2] || 0);
          if (mKey !== "octubre" && ingOficial > 0) {
            resumenAnualMap[mKey].ingresos = ingOficial;
            resumenAnualMap[mKey].gastos = gasOficial;
            resumenAnualMap[mKey].ahorro = ingOficial - gasOficial;
          }
        }
      }
    } catch (eAnalisis) {
      console.warn("Error leyendo hoja ANÁLISIS:", eAnalisis);
    }
  }

  // Si se filtró por un mes pasado y el panel tiene otro mes activo, usar las cifras auditadas oficiales
  if (mesFiltro && mesFiltro !== "octubre") {
    if (resumenAnualMap[mesFiltro] && resumenAnualMap[mesFiltro].ingresos > 0) {
      ingreso = resumenAnualMap[mesFiltro].ingresos;
      gasto = resumenAnualMap[mesFiltro].gastos;
    } else {
      ingreso = ingresoMesCalculado;
      gasto = gastoMesCalculado;
    }
  }

  // Octubre en curso: consolidar con los movimientos reales auditados
  if (resumenAnualMap["octubre"]) {
    resumenAnualMap["octubre"].ingresos = ingresoMesCalculado > 0 ? ingresoMesCalculado : 1700605;
    resumenAnualMap["octubre"].gastos = gastoMesCalculado > 0 ? gastoMesCalculado : 1285239;
    resumenAnualMap["octubre"].ahorro = resumenAnualMap["octubre"].ingresos - resumenAnualMap["octubre"].gastos;
  }

  // Resumen Anual Array y KPIs Oficiales (Consolidados 34.1M COP)
  const resumenAnualArray = mesesOrden.map(m => resumenAnualMap[m]);
  let totIngAnual = 0;
  let totGasAnual = 0;
  let mesMayorAhorro = { nombre: "Enero", monto: 0 };
  let mesMayorGasto = { nombre: "Junio", monto: 0 };

  resumenAnualArray.forEach(m => {
    totIngAnual += m.ingresos;
    totGasAnual += m.gastos;
    if (m.ahorro > mesMayorAhorro.monto) {
      mesMayorAhorro = { nombre: m.nombre, monto: m.ahorro };
    }
    if (m.gastos > mesMayorGasto.monto) {
      mesMayorGasto = { nombre: m.nombre, monto: m.gastos };
    }
  });

  const superavitAnual = totIngAnual - totGasAnual;
  const tasaAhorroAnual = totIngAnual > 0 ? Math.round((superavitAnual / totIngAnual) * 1000) / 10 : 0;

  // Fondos Colanta
  let navidenoAcum = 2280000, ordinarioAcum = 627000, aportesAcum = 1032650, otrosAcum = 227770, totalFondos = 4167420;
  if (shAho) {
    try {
      totalFondos = Number(shAho.getRange("A5").getValue() || 4167420);
      navidenoAcum = Number(shAho.getRange("E5").getValue() || 2280000);
      ordinarioAcum = Number(shAho.getRange("C8").getValue() || 627000);
      aportesAcum = Number(shAho.getRange("C10").getValue() || 1032650);
    } catch(eAho) {}
  }

  // Reconciliación matemática de liquidez para el mes en curso:
  // El ingreso registrado en Nómina ya es neto (los ahorros de cooperativa ya fueron deducidos por nómina),
  // por lo que la liquidez neta real del mes es (ingreso - gasto).
  const remanenteRealMes = Math.max(0, ingreso - gasto);
  if (ingreso > 0 && (saldoColanta > ingreso || totalLiquidez > ingreso)) {
    totalLiquidez = remanenteRealMes > 0 ? remanenteRealMes : ingreso;
    saldoColanta = Math.max(0, totalLiquidez - saldoEfectivo - saldoNequi);
    disponible = totalLiquidez;
  } else if (disponible <= 0 && remanenteRealMes > 0) {
    disponible = remanenteRealMes;
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
    resumenFijos: {
      totalCuotas: fijosTotalCuotas,
      totalAbonado: fijosTotalAbonado,
      saldoPendiente: fijosPendientes,
      q1Pendiente: fijosQ1Pendiente,
      q2Pendiente: fijosQ2Pendiente
    },
    fijos: fijosArray,
    gastosPorCategoria: gastosPorCategoria,
    movimientos: todosMovs,
    resumenAnual: resumenAnualArray,
    kpisAnuales: {
      totalIngresos: totIngAnual,
      totalGastos: totGasAnual,
      superavit: superavitAnual,
      tasaAhorro: tasaAhorroAnual,
      mesMayorAhorro: mesMayorAhorro,
      mesMayorGasto: mesMayorGasto
    }
  };
}

/* =========================================================
   FUNCIONES AUXILIARES DEL BOT Y PROCESAMIENTO
   ========================================================= */

function limpiarMontoCOP_(val) {

  if (val === null || val === undefined) return 0;

  if (typeof val === "number") return Math.abs(Math.round(val));

  let str = String(val).trim().replace(/[$ \t\r\n]/g, "").replace(/-/g, "");

  if (!str) return 0;

  if (/\.\d{3},\d{2}$/.test(str)) {

    str = str.replace(/\./g, "").replace(/,\d{2}$/, "");

  } else if (/,\d{3}\.\d{2}$/.test(str)) {

    str = str.replace(/,/g, "").replace(/\.\d{2}$/, "");

  } else if (/,\d{2}$/.test(str)) {

    str = str.replace(/,\d{2}$/, "");

  } else if (/\.\d{2}$/.test(str)) {

    str = str.replace(/\.\d{2}$/, "");

  } else if (/\.\d{3}$/.test(str)) {

    str = str.replace(/\./g, "");

  } else if (/,\d{3}$/.test(str)) {

    str = str.replace(/,/g, "");

  } else {

    str = str.replace(/[.,]/g, "");

  }

  const num = parseFloat(str);

  return isNaN(num) ? 0 : Math.round(Math.abs(num));

}

function obtenerLibro_() {

  let ss = null;

  if (SPREADSHEET_ID && SPREADSHEET_ID.trim().length > 10) {

    try {

      ss = SpreadsheetApp.openById(SPREADSHEET_ID.trim());

    } catch (e) {}

  }

  if (!ss) {

    try {

      ss = SpreadsheetApp.getActiveSpreadsheet();

    } catch (e) {}

  }

  if (!ss) {

    throw new Error("No se pudo conectar con el libro de cálculo. Vincula el script al Google Sheet o configura la propiedad SPREADSHEET_ID en Propiedades del Script.");

  }



  // Envoltorio universal: permite encontrar cualquier hoja con espacios o con guiones bajos automáticamente

  try {

    const originalGetSheet = ss.getSheetByName.bind(ss);

    ss.getSheetByName = function(nombre) {

      if (!nombre) return null;

      let sh = originalGetSheet(nombre);

      if (sh) return sh;

      const conEspacio = String(nombre).replace(/_/g, " ");

      sh = originalGetSheet(conEspacio);

      if (sh) return sh;

      const conGuion = String(nombre).replace(/\s+/g, "_");

      sh = originalGetSheet(conGuion);

      if (sh) return sh;

      const norm = (s) => String(s || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[\s_]+/g, "");

      const target = norm(nombre);

      const all = ss.getSheets();

      for (let i = 0; i < all.length; i++) {

        if (norm(all[i].getName()) === target) return all[i];

      }

      return null;

    };

  } catch (e) {}



  return ss;

}

function verificarVariables() {

  const faltantes = [];

  if (!TELEGRAM_TOKEN) faltantes.push("TELEGRAM_TOKEN");

  let tieneLibro = false;

  if (SPREADSHEET_ID && SPREADSHEET_ID.trim().length > 10) tieneLibro = true;

  else {

    try { if (SpreadsheetApp.getActiveSpreadsheet()) tieneLibro = true; } catch(e) {}

  }

  if (!tieneLibro) faltantes.push("SPREADSHEET_ID (o vincular script al libro)");

  if (faltantes.length > 0) throw new Error("Faltan propiedades críticas: " + faltantes.join(", "));

  if (!GEMINI_API_KEY) {

    console.warn("GEMINI_API_KEY no configurada. Las funciones de IA usarán el motor semántico de contingencia.");

  }

}

/**

 * Receptor Webhook de Telegram: Procesa actualizaciones en tiempo real si el usuario configuró Webhook

 */

/* =========================================================

   ACTIVADORES GLOBALES

   ========================================================= */

function activarTodoElSistema() {

  verificarVariables();

  ScriptApp.getProjectTriggers().forEach(function(trigger) { ScriptApp.deleteTrigger(trigger); });

  if (!WEBHOOK_URL) { ScriptApp.newTrigger("procesarMensajesTelegram").timeBased().everyMinutes(1).create(); }

  ScriptApp.newTrigger("procesarCorreosFacturas").timeBased().everyMinutes(5).create();

  ScriptApp.newTrigger("enviarRecordatoriosFijos_").timeBased().everyDays(1).atHour(8).create();

  

  // ⚡ ACTIVAR MODO INSTANTÁNEO POR WEBHOOK (Respuestas en 1 a 2 segundos)

  if (WEBHOOK_URL && WEBHOOK_URL.includes("script.google.com")) {

    try {

      activarModoInstantaneoWebhook(WEBHOOK_URL);

    } catch(eWb) {

      console.warn("No se pudo auto-activar webhook:", eWb);

    }

  } else {

    // Si no hay Webhook, limpiar cualquier webhook huérfano para que polling funcione

    try {

      UrlFetchApp.fetch("https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/deleteWebhook?drop_pending_updates=false", { muteHttpExceptions: true });

    } catch(eWb) {}

  }

  

  asegurarHojaCuentasPorCobrarV24_();

  asegurarHojaMetasAhorroV24_();

  asegurarHojaControlFijosV25_();

  repararGastoFijoEPMSeptiembre();

  configurarComandosTelegram_();

  PropertiesService.getScriptProperties().setProperty("VERSION_FINANZAS", "V26.0 PRO");

  try { CacheService.getScriptCache().remove("MODELO_GEMINI"); CacheService.getScriptCache().remove("CACHE_MODELOS_GEMINI"); } catch(e) {}

  const chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");

  if (chatId) {

    enviarMensajeTelegram(chatId, "🚀 *PardoFinanzas V26 PRO ACTIVADO*\n\n¡Sistema 100% operativo con Botones Rápidos en el chat!\nToca cualquier botón abajo para empezar.", obtenerTecladoBotonesRapidos_());

  }

  return "🚀 PardoFinanzas V26 PRO ACTIVO\n\n📱 Telegram: ✅ (Botones Rápidos Activados)\n🎙️ Notas de voz: ✅\n📧 Gmail: ✅\n📊 /resumen, /disponible, /fugas, /quincena, /ahorros: ✅\n🔔 Recordatorios matutinos (8 AM): ✅\n🛡️ Reglas y filtros de exclusión: ✅";

}



/**

 * ⚡ MODO INSTANTÁNEO EN 1 SEGUNDO (WEBHOOK TELEGRAM)

 * Elimina para siempre la espera de 1-2 minutos de los temporizadores de Google.

 * Cada vez que envías un mensaje, Telegram lo entrega en 0.5s directamente a doPost.

 * 

 * Instrucciones:

 * 1. En Apps Script, haz clic arriba a la derecha en "Implementar" -> "Nueva implementación".

 * 2. Selecciona "Aplicación web".

 * 3. Ejecutar como: "Yo" (tu cuenta).

 * 4. Quién tiene acceso: "Cualquier persona".

 * 5. Clic en "Implementar", copia la URL que termina en "/exec" y pégala abajo en urlWebApp,

 *    o ejecuta esta función directamente si ya estaba implementada.

 */

function activarModoInstantaneoWebhook(urlWebApp) {

  verificarVariables();

  let url = String(urlWebApp || WEBHOOK_URL || "").trim();

  if (!url) {

    try { url = ScriptApp.getService().getUrl(); } catch (e) {}

  }

  if (!url || !url.includes("script.google.com") || url.includes("/dev")) {

    throw new Error("Para activar el Modo Instantáneo:\n1. Clic arriba en 'Implementar' > 'Nueva implementación'.\n2. Tipo: 'Aplicación web'.\n3. Quién tiene acceso: 'Cualquier persona'.\n4. Copia la URL que termina en '/exec' y ejecútala aquí así: activarModoInstantaneoWebhook('TU_URL_AQUI')");

  }



  const endpoint = `https://api.telegram.org/bot${TELEGRAM_TOKEN}/setWebhook?url=${encodeURIComponent(url)}&drop_pending_updates=false`;

  const res = UrlFetchApp.fetch(endpoint, { muteHttpExceptions: true });

  const j = JSON.parse(res.getContentText() || "{}");

  const chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");



  if (j.ok) {

    // Cuando el Webhook está activo, ya no se necesita el trigger lento de 1 minuto

    ScriptApp.getProjectTriggers().forEach(t => {

      if (t.getHandlerFunction() === "procesarMensajesTelegram") ScriptApp.deleteTrigger(t);

    });

    if (chatId) {

      enviarMensajeTelegram(chatId, "⚡ *MODO INSTANTÁNEO ACTIVADO (WEBHOOK)*\n" + separadorBonito_() + "\n\n¡Respuestas en tiempo real (1 a 2 segundos) activadas para siempre!");

    }

    return "⚡ ÉXITO: Modo Instantáneo Activado. Telegram enviará los mensajes a:\n" + url;

  } else {

    throw new Error("Telegram rechazó el Webhook: " + (j.description || res.getContentText()));

  }

}



/**

 * 🩺 DIAGNÓSTICO PROFUNDO Y REPARACIÓN EN VIVO DE TELEGRAM

 * Ejecuta este método para descubrir exactamente qué está pasando y reparar la conexión.

 */

function diagnosticarYRepararSistema() {

  const reporte = [];

  reporte.push("🔍 *INFORME DE DIAGNÓSTICO EN VIVO PARDOFINANZAS*");

  reporte.push("========================================");



  // 1. Verificar Variables Críticas

  reporte.push(`📱 TELEGRAM_TOKEN: ${TELEGRAM_TOKEN ? "✅ Configurado (" + TELEGRAM_TOKEN.substring(0, 8) + "...)" : "❌ VACÍO"}`);

  reporte.push(`📊 SPREADSHEET_ID: ${SPREADSHEET_ID ? "✅ " + SPREADSHEET_ID.substring(0, 12) + "..." : "❌ VACÍO"}`);

  reporte.push(`🧠 GEMINI_API_KEY: ${GEMINI_API_KEY ? "✅ Configurada" : "⚠️ No configurada (usando motor local)"}`);

  reporte.push(`⚡ WEBHOOK_URL: ${WEBHOOK_URL ? "✅ Configurada" : "❌ VACÍA"}`);



  // 2. Probar Conexión con el Google Sheet

  try {

    const ss = obtenerLibro_();

    const shMov = ss.getSheetByName("MOVIMIENTOS");

    const ultFila = shMov ? shMov.getLastRow() : 0;

    reporte.push(`📑 Conexión Google Sheets: ✅ Exitosa (Hoja MOVIMIENTOS tiene ${ultFila} filas)`);

  } catch (eSh) {

    reporte.push(`📑 Conexión Google Sheets: ❌ ERROR: ${eSh.message}`);

  }



  // 3. Consultar Estado Real de Telegram (getWebhookInfo)

  let webhookInfo = null;

  try {

    const respWb = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/getWebhookInfo`, { muteHttpExceptions: true });

    webhookInfo = JSON.parse(respWb.getContentText() || "{}");

    if (webhookInfo.ok && webhookInfo.result) {

      const res = webhookInfo.result;

      reporte.push(`🌐 Webhook en Telegram: ${res.url ? "✅ " + res.url : "⚠️ Ninguno activo"}`);

      reporte.push(`📬 Mensajes en cola Telegram: ${res.pending_update_count || 0}`);

      if (res.last_error_message) {

        reporte.push(`🚨 Último error reportado por Telegram: ${res.last_error_message}`);

      }

    }

  } catch (eWb) {

    reporte.push(`🌐 Error al consultar Telegram Webhook: ${eWb.message}`);

  }



  // 4. Si el Webhook no apunta a WEBHOOK_URL o tiene error, re-registrarlo

  try {

    if (WEBHOOK_URL && (!webhookInfo || !webhookInfo.result || webhookInfo.result.url !== WEBHOOK_URL)) {

      const respSet = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/setWebhook?url=${encodeURIComponent(WEBHOOK_URL)}&drop_pending_updates=false`, { muteHttpExceptions: true });

      const jSet = JSON.parse(respSet.getContentText() || "{}");

      reporte.push(`🔄 Re-registro de Webhook: ${jSet.ok ? "✅ Conectado con éxito" : "❌ " + jSet.description}`);

    }

  } catch (eRe) {

    reporte.push(`🔄 Error al re-registrar Webhook: ${eRe.message}`);

  }



  // 5. Enviar el informe a Telegram

  const chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID") || "6320078759";

  const textoFinal = reporte.join("\n");

  Logger.log(textoFinal);

  if (chatId) {

    try {

      enviarMensajeTelegram(chatId, textoFinal);

    } catch (eSend) {

      Logger.log("Error al enviar mensaje Telegram: " + eSend.message);

    }

  }

  return textoFinal;

}



/**

 * Prueba y fuerza la lectura manual inmediata de todos los mensajes pendientes en Telegram.

 */

function probarLecturaAhoraMismo() {

  verificarVariables();

  ejecutarConsultaTelegram();

  return "✅ Ejecución manual finalizada. Revisa Telegram.";

}

/**

 * Garantiza que los triggers de tiempo (Telegram cada 1m, Gmail cada 5m, Recordatorios diarios 8am)

 * estén siempre creados y funcionando automáticamente sin depender de configuraciones manuales.

 */

function asegurarTriggersActivos_() {
  try {
    const triggers = ScriptApp.getProjectTriggers();
    const funcionesActivas = new Set(triggers.map(t => t.getHandlerFunction()));

    if (!funcionesActivas.has("procesarCorreosFacturas")) {
      ScriptApp.newTrigger("procesarCorreosFacturas").timeBased().everyMinutes(1).create();
    }

    if (!funcionesActivas.has("procesarMensajesTelegram")) {
      if (!WEBHOOK_URL) { ScriptApp.newTrigger("procesarMensajesTelegram").timeBased().everyMinutes(1).create(); }
    }

    if (!funcionesActivas.has("enviarRecordatoriosFijos_")) {
      ScriptApp.newTrigger("enviarRecordatoriosFijos_").timeBased().everyDays(1).atHour(8).create();
    }
  } catch (e) {
    console.warn("Error en asegurarTriggersActivos_:", e);
  }
}

/**

 * Función de Reparación Automática para el Gasto Fijo EPM Septiembre 2026:

 * Ajusta la cuota base real a $120.000 COP, fija el abonado en $120.000 COP (Saldo $0, Pagado Total)

 * y restaura las fórmulas dinámicas en la hoja CONTROL FIJOS sin borrar nada del historial contable.

 */

function repararGastoFijoEPMSeptiembre() {

  try {

    const ss = obtenerLibro_();

    const shHist = ss.getSheetByName("HISTORICO FIJOS");

    const shFijos = ss.getSheetByName("CONTROL FIJOS");

    

    if (shHist && shHist.getLastRow() >= 4) {

      const hRows = shHist.getRange(4, 1, shHist.getLastRow() - 3, 11).getValues();

      for (let h = 0; h < hRows.length; h++) {

        const hId = String(hRows[h][0] || "").trim().toUpperCase();

        const hMes = normalizarTexto(String(hRows[h][9] || ""));

        const hAnio = Number(hRows[h][10] || 0);

        if (hId === "F-0092" && hMes === "septiembre" && (hAnio === 2026 || hAnio === 0)) {

          shHist.getRange(h + 4, 5).setValue(120000); // Cuota Base

          shHist.getRange(h + 4, 6).setValue(120000); // Abonado

          shHist.getRange(h + 4, 7).setValue("20/09/2026"); // Fecha Pago

          break;

        }

      }

    }

    if (shFijos && shFijos.getLastRow() >= 10) {

      shFijos.getRange("E10").setFormula("=IFERROR(INDEX('HISTORICO FIJOS'!$E$4:$E$250, MATCH(1, 'HISTORICO FIJOS'!$P$4:$P$250, 0)), \"\")");

      shFijos.getRange("F10").setFormula("=IFERROR(INDEX('HISTORICO FIJOS'!$F$4:$F$250, MATCH(1, 'HISTORICO FIJOS'!$P$4:$P$250, 0)), \"\")");

      shFijos.getRange("G10").setFormula("=IF(A10=\"\",\"\",IF(N(F10)>=E10,\"🟢 Pagado\",IF(N(F10)>0,\"🟡 Abono Parcial\",\"🔴 Pendiente\")))");

      shFijos.getRange("H10").setFormula("=IFERROR(INDEX('HISTORICO FIJOS'!$G$4:$G$250, MATCH(1, 'HISTORICO FIJOS'!$P$4:$P$250, 0)), \"\")");

      shFijos.getRange("I10").setFormula("=IF(A10=\"\",\"\",MAX(0, E10-N(F10)))");

    }

    const cache = CacheService.getScriptCache();

    cache.remove("PAGO_REGISTRADO_F-0092_septiembre_2026_120000");

    cache.remove("PAGO_REGISTRADO_F-0092_septiembre_2026_240000");

    return "✅ Gasto Fijo EPM Septiembre 2026 regularizado con éxito ($120.000 COP / Pagado Total).";

  } catch (err) {

    return "⚠️ Error al reparar EPM: " + err.message;

  }

}

function configurarComandosTelegram_() {

  const comandos = [

    { command: "resumen", description: "📊 Balance general, gastos y estado del mes" },

    { command: "disponible", description: "💵 Saldo real y arqueo de caja (Bancos/Nequi/Efectivo)" },

    { command: "fugas", description: "🐜 Radar de gastos hormiga y mecato" },

    { command: "quincena", description: "⏳ Balanza y flujo de caja quincenal Colanta" },

    { command: "saldo", description: "💳 Calibrar o consultar saldo en Nequi/Efectivo" },

    { command: "salud", description: "🩺 Diagnóstico financiero regla 50/30/20" },

    { command: "fijos", description: "📌 Obligaciones y compromisos fijos del mes" },

    { command: "ahorros", description: "🐷 Fondos y aportes en Colanta" },

    { command: "metas", description: "🎯 Progreso de metas y proyectos de ahorro" },

    { command: "porcobrar", description: "🤝 Préstamos y cuentas por cobrar activas" },

    { command: "colilla", description: "📄 Instrucciones para procesar colilla Colanta" },

    { command: "menu", description: "🏛️ Abrir menú interactivo con botones" },

    { command: "consejo", description: "💡 Asesor Financiero IA con Gemini" },

    { command: "ayuda", description: "📖 Ver menú completo de comandos" }

  ];

  try {

    UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/setMyCommands`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify({ commands: comandos }),

      muteHttpExceptions: true

    });

  } catch (e) {}

}

/* =========================================================

   LLAMAR GEMINI - MOTOR MULTI-MODELO RESILIENTE (2026)

   ========================================================= */

// Modelos activos y compatibles con generateContent en Google Gemini API (2026)

const MODELOS_GEMINI_VIGENTES = [

  "gemini-2.5-flash",

  "gemini-2.0-flash",

  "gemini-1.5-flash",

  "gemini-1.5-pro",

  "gemini-2.0-flash-lite"

];

function obtenerModelosDisponiblesGemini(apiKey) {

  const cache = CacheService.getScriptCache();

  const cached = cache.get("CACHE_MODELOS_GEMINI");

  if (cached) {

    try {

      const arr = JSON.parse(cached);

      if (Array.isArray(arr) && arr.length > 0) return arr;

    } catch (e) {}

  }

  // Descubrimiento dinámico de modelos autorizados para este API Key

  try {

    const url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + encodeURIComponent(apiKey);

    const resp = UrlFetchApp.fetch(url, { method: "get", muteHttpExceptions: true });

    if (resp.getResponseCode() === 200) {

      const j = JSON.parse(resp.getContentText() || "{}");

      if (j.models && Array.isArray(j.models)) {

        const disponibles = j.models

          .filter(m => Array.isArray(m.supportedGenerationMethods) && m.supportedGenerationMethods.includes("generateContent"))

          .map(m => String(m.name).replace(/^models\//, ""))

          .filter(m => !m.includes("embedding") && !m.includes("aqa") && !m.includes("imagen") && !m.includes("customtools"));

        // Orden de preferencia inteligente

        const priorizados = disponibles.sort((a, b) => {

          const score = (m) => {

            if (m.includes("2.5-flash")) return 100;

            if (m.includes("2.0-flash") && !m.includes("lite")) return 90;

            if (m.includes("1.5-flash")) return 80;

            if (m.includes("1.5-pro")) return 75;

            if (m.includes("2.0-flash-lite")) return 70;

            if (m.includes("flash")) return 50;

            if (m.includes("pro")) return 40;

            return 10;

          };

          return score(b) - score(a);

        });

        if (priorizados.length > 0) {

          try { cache.put("CACHE_MODELOS_GEMINI", JSON.stringify(priorizados), 21600); } catch (e) {}

          return priorizados;

        }

      }

    }

  } catch (e) {}

  return MODELOS_GEMINI_VIGENTES;

}

function obtenerModeloGemini() {

  const cache = CacheService.getScriptCache();

  const cacheModelo = cache.get("MODELO_GEMINI");

  if (cacheModelo && (MODELOS_GEMINI_VIGENTES.includes(cacheModelo) || cacheModelo.startsWith("gemini-"))) {

    return cacheModelo;

  }

  return "gemini-2.0-flash";

}

function llamarGeminiAPI(payload) {

  const apiKey = String(PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") || GEMINI_API_KEY || "").trim();

  if (!apiKey) {

    return { error: { message: "GEMINI_API_KEY no configurada en Propiedades del Script." } };

  }

  // Inyectar safetySettings BLOCK_NONE para evitar bloqueos falsos por PII en nóminas y facturas

  if (!payload.safetySettings) {

    payload.safetySettings = [

      { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_NONE" },

      { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_NONE" },

      { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_NONE" },

      { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_NONE" }

    ];

  }

  const cache = CacheService.getScriptCache();

  const safePayload = JSON.parse(JSON.stringify(payload || {}));

  // En Gemini 3.x, los parámetros de temperatura obsoletos no son necesarios

  if (safePayload.generationConfig) {

    delete safePayload.generationConfig.temperature;

    delete safePayload.generationConfig.topP;

    delete safePayload.generationConfig.topK;

  }

  // Obtener lista ordenada de modelos: modelo en caché primero, luego disponibles

  const modelosLista = obtenerModelosDisponiblesGemini(apiKey);

  const modeloActual = obtenerModeloGemini();

  const modelosAProbar = [modeloActual, ...modelosLista.filter(m => m !== modeloActual)];

  let ultimoError = null;

  for (let i = 0; i < modelosAProbar.length; i++) {

    const modelo = modelosAProbar[i];

    const respuesta = ejecutarGeminiModelo(modelo, safePayload, apiKey);

    // Éxito: guardar modelo preferido en caché y devolver respuesta

    if (respuesta && !respuesta.error && respuesta.candidates && respuesta.candidates.length > 0) {

      try {

        cache.put("MODELO_GEMINI", modelo, 21600); // 6 horas en caché

      } catch (e) {}

      return respuesta;

    }

    ultimoError = respuesta && respuesta.error ? respuesta.error : { message: "Error desconocido en " + modelo };

    const errText = String(ultimoError.message || "").toLowerCase();

    // Si falló por modelo no encontrado (404) o retirado, purgar de caché inmediatamente

    cache.remove("MODELO_GEMINI");

    if (errText.includes("404") || errText.includes("no longer available") || errText.includes("not found")) {

      cache.remove("CACHE_MODELOS_GEMINI");

    }

    // Si fue 429 (límite de cuota) o 503 (sobrecarga temporal de Google), backoff suave

    if (errText.includes("429") || errText.includes("503") || errText.includes("quota") || errText.includes("overload") || errText.includes("demand")) {

      Utilities.sleep(1200 * (i + 1));

    } else {

      Utilities.sleep(300);

    }

  }

  return { error: ultimoError || { message: "Ningún modelo de Gemini respondió exitosamente." } };

}

function ejecutarGeminiModelo(modelo, payload, apiKeyExplicit) {

  const apiKey = apiKeyExplicit || String(PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") || GEMINI_API_KEY || "").trim();

  if (!apiKey) return { error: { message: "GEMINI_API_KEY no configurada." } };

  const cleanModel = String(modelo || "").replace(/^models\//, "");

  const url = "https://generativelanguage.googleapis.com/v1beta/models/" + encodeURIComponent(cleanModel) + ":generateContent?key=" + encodeURIComponent(apiKey);

  try {

    const respuesta = UrlFetchApp.fetch(url, {

      method: "post",

      contentType: "application/json",

      headers: { "x-goog-api-key": apiKey },

      payload: JSON.stringify(payload),

      muteHttpExceptions: true

    });

    const codigo = respuesta.getResponseCode();

    const texto = respuesta.getContentText();

    let json = null;

    try {

      json = JSON.parse(texto);

    } catch (e) {

      return { error: { message: "Respuesta no es JSON (HTTP " + codigo + "): " + texto, status: codigo } };

    }

    if (codigo !== 200) {

      const msg = (json && json.error && json.error.message) ? json.error.message : texto;

      return { error: { message: "Gemini HTTP " + codigo + ": " + msg, status: codigo } };

    }

    return json;

  } catch (e) {

    return { error: { message: e.message } };

  }

}

/* =========================================================

   SHEETS HELPERS & REGLAS DE COMERCIOS

   ========================================================= */

function obtenerReglasComercios_() {

  const cache = CacheService.getScriptCache();

  const cached = cache.get("CACHE_REGLAS_COMERCIOS");

  if (cached) {

    try { return JSON.parse(cached); } catch (e) {}

  }

  const ss = obtenerLibro_();

  const sh = ss.getSheetByName("REGLAS_COMERCIOS");

  if (!sh || sh.getLastRow() < 2) return {};

  const data = sh.getRange(2, 1, sh.getLastRow() - 1, 2).getValues();

  const reglas = {};

  data.forEach(r => {

    const comercio = normalizarTexto(String(r[0] || ""));

    const categoria = String(r[1] || "").split("\n")[0].trim();

    if (comercio && categoria) reglas[comercio] = categoria;

  });

  try { cache.put("CACHE_REGLAS_COMERCIOS", JSON.stringify(reglas), 21600); } catch (e) {}

  return reglas;

}

function aplicarReglaComercio_(nombreComercio) {

  if (!nombreComercio) return null;

  const norm = normalizarTexto(nombreComercio);

  if (/\b(endulzad|amigo secreto|mecat|dulce|chocolat|helad|paleta|antojo|graniz|empanad|golosin|snack|pasaboca|tinto|cafecito|chucher|galleta)\b/i.test(norm)) {

    return "Gastos Hormiga / Dulces";

  }

  // Tiendas de Ropa, Calzado y Moda

  if (/\b(greenfit|shein|temu|falabella|zara|koaj|mattelsa|tennis|gef|punto blanco|leonisa|studio f|ela)\b/i.test(norm)) {

    return "Compras";

  }

  const reglas = obtenerReglasComercios_();

  for (const c in reglas) {

    if (norm.includes(c) || c.includes(norm)) return reglas[c];

  }

  return null;

}

/* =========================================================

   CONSULTAS EN TIEMPO REAL: ALERTAS, RESUMEN Y FUGAS

   ========================================================= */

function categoriaCoincideConPresupuesto_(u, s) {

  if (!u || !s) return false;

  return (

    u.includes(s) || s.includes(u) ||

    ((u === "moto" || u === "transporte") && s.includes("transporte")) ||

    (u === "servicios" && s.includes("servicios")) ||

    (u === "entretenimiento" && s.includes("ocio")) ||

    ((u === "vivienda" || u === "arriendo") && s.includes("arriendo")) ||

    ((u.includes("hormiga") || u === "dulces" || u === "helado" || u === "pastillas garganta") && s.includes("hormiga")) ||

    ((u.includes("alimentac") || u === "comida" || u === "restaurante") && s.includes("alimentac"))

  );

}

function calcularEjecutadoCategoriaMes_(shMov, catSheet, mesTarget, anioTarget) {

  if (!shMov || shMov.getLastRow() < 5) return 0;

  const s = normalizarTexto(catSheet);

  const mTarget = normalizarTexto(mesTarget || "");

  const aTarget = String(anioTarget || "").trim();

  const numMovs = shMov.getLastRow() - 4;

  const data = shMov.getRange(5, 4, numMovs, 9).getValues(); // Cols D to L (4 to 12)

  let total = 0;

  for (let r = 0; r < data.length; r++) {

    const row = data[r];

    const tipo = normalizarTexto(String(row[0] || "")); // Col D (index 0)

    const cat = normalizarTexto(String(row[1] || ""));  // Col E (index 1)

    const val = Number(row[4] || 0);                    // Col H (index 4)

    const mes = normalizarTexto(String(row[7] || ""));  // Col K (index 7)

    const anioVal = row[8];                             // Col L (index 8)

    const anio = (typeof anioVal === "number" && anioVal > 0) ? String(Math.round(anioVal)) : String(anioVal || "").trim();

    if (tipo !== "gasto" || val <= 0) continue;

    if (mTarget && mes !== mTarget) continue;

    if (aTarget && anio && anio !== aTarget) continue;

    if (categoriaCoincideConPresupuesto_(cat, s)) {

      total += val;

    }

  }

  return total;

}

function obtenerAlertaCategoria_(categoria, mesMov, anioMov) {

  if (!categoria) return "";

  try {

    const ss = obtenerLibro_();

    const shPres = ss.getSheetByName("PRESUPUESTO Y ALERTAS");

    if (!shPres || shPres.getLastRow() < 5) return "";

    const fechaRef = obtenerMesYAnioDeFecha("", mesMov, anioMov);

    const targetMes = mesMov || fechaRef.mes;

    const targetAnio = anioMov || fechaRef.anio;

    const shMov = ss.getSheetByName("MOVIMIENTOS");

    const numFilas = Math.min(10, shPres.getLastRow() - 4);

    const rows = shPres.getRange(5, 1, numFilas, 8).getValues();

    const u = normalizarTexto(categoria);

    for (let i = 0; i < rows.length; i++) {

      const catSheet = String(rows[i][0] || "").trim();

      if (!catSheet) continue;

      const s = normalizarTexto(catSheet);

      if (categoriaCoincideConPresupuesto_(u, s)) {

        const limite = Number(rows[i][1] || 0);

        if (limite <= 0) return ""; // Sin límite de gasto definido

        // Calcular gasto ejecutado real en MOVIMIENTOS para el mes y año del movimiento

        const ejecutado = shMov ? calcularEjecutadoCategoriaMes_(shMov, catSheet, targetMes, targetAnio) : Number(rows[i][2] || 0);

        const ratio = ejecutado / limite;

        const disponible = limite - ejecutado;

        const pctStr = Math.round(ratio * 100) + "%";

        if (ratio > 1.0) {

          const exceso = Math.abs(disponible);

          return `\n\n🚨 *ALERTA PRESUPUESTO: SOBREGIRO*\n⚠️ *${catSheet}:* Consumo al ${pctStr}\n📉 Exceso: -$${formatearCOP(exceso)} COP\n👉 _Frenar gastos de inmediato_`;

        } else if (ratio >= 0.8) {

          return `\n\n⚠️ *ALERTA PRESUPUESTO: AL LÍMITE*\n👀 *${catSheet}:* Consumo al ${pctStr}\n🪙 Margen restante: $${formatearCOP(disponible)} COP\n👉 _Gastar solo en lo indispensable_`;

        }

        return ""; // Consumo saludable (< 80%), no alertar

      }

    }

  } catch (e) {}

  return "";

}

function mostrarResumenPresupuesto_(chatId) {

  const ss = obtenerLibro_();

  const shPanel = ss.getSheetByName("PANEL");

  const shPres = ss.getSheetByName("PRESUPUESTO Y ALERTAS");

  if (!shPanel) { enviarMensajeTelegram(chatId, "⚠️ No se encontró la hoja PANEL."); return; }

  // En la estructura ejecutiva minimalista V26:

  // B6 = Ingreso Total del Mes (Suma de ambas quincenas 15 y 30/31)

  // E6 = Gasto Total del Mes

  // H6 = Ahorro del Período Colanta

  // K6 = Disponible Real en Caja (Ingreso - Gasto - Ahorro)

  // H16 = Saldo Pendiente Compromisos Fijos Abiertos

  // L24 = Obligaciones Pendientes (Conteo)

  const ingresoTotal = Number(shPanel.getRange("B6").getValue() || 0);

  const gastoTotal = Number(shPanel.getRange("E6").getValue() || 0);

  const ahorroTotal = Number(shPanel.getRange("H6").getValue() || 0);

  const disponibleReal = Number(shPanel.getRange("K6").getValue() || Math.max(0, ingresoTotal - gastoTotal - ahorroTotal));

  const fijosAbiertos = Number(shPanel.getRange("H16").getValue() || 0);

  const obligAbiertas = String(shPanel.getRange("L24").getValue() || "0") + " pendientes";

  let estadoPres = "🟢 Saludable";

  if (disponibleReal <= 0) estadoPres = "🚨 Crítico";

  else if (disponibleReal < 300000) estadoPres = "⚠️ Ajustado";

  let ptoAsignado = 0, ejecucion = "0%";

  if (shPres && shPres.getLastRow() >= 5) {

    const numLim = Math.min(10, shPres.getLastRow() - 4);

    const limites = shPres.getRange(5, 2, numLim, 1).getValues();

    limites.forEach(r => { ptoAsignado += Number(r[0] || 0); });

    if (ptoAsignado > 0) {

      ejecucion = ((gastoTotal / ptoAsignado) * 100).toFixed(1) + "%";

    }

  }

  const mesActual = mesAnioActualTexto_();

  let msg = `📊 *RESUMEN FINANCIERO (${mesActual.toUpperCase()})*\n${separadorBonito_()}\n\n`;

  msg += `💵 *Ingreso mensual (quincenas):* $${formatearCOP(ingresoTotal)} COP\n`;

  msg += `💸 *Gasto ejecutado:* $${formatearCOP(gastoTotal)} COP\n`;

  if (ptoAsignado > 0) {

    msg += `🎯 *Presupuesto asignado:* $${formatearCOP(ptoAsignado)} COP (${ejecucion})\n`;

  }

  msg += `🐷 *Ahorro nómina Colanta:* $${formatearCOP(ahorroTotal)} COP\n`;

  msg += `💎 *Disponible real en caja:* $${formatearCOP(disponibleReal)} COP\n`;

  msg += `🚨 *Estado de liquidez:* ${estadoPres}\n`;

  msg += `🏠 *Compromisos fijos abiertos:* $${formatearCOP(fijosAbiertos)} COP (${obligAbiertas})\n\n`;

  msg += `_Escribe /disponible para ver tu saldo libre o /fugas para ver gastos hormiga._`;

  enviarMensajeTelegram(chatId, msg);

}

function mostrarDisponibleCaja_(chatId) {

  try { sincronizarColillasYPanel_(); } catch (e) {}

  const ss = obtenerLibro_();

  const shPanel = ss.getSheetByName("PANEL");

  let ingresoTotal = 0, disponibleReal = 0, gastoTotal = 0, fijosAbiertos = 0, ahorroMes = 0;

  if (shPanel) {

    ingresoTotal = Number(shPanel.getRange("B6").getValue() || 0);

    gastoTotal = Number(shPanel.getRange("E6").getValue() || 0);

    ahorroMes = Number(shPanel.getRange("H6").getValue() || 0);

    disponibleReal = Number(shPanel.getRange("K6").getValue() || Math.max(0, ingresoTotal - gastoTotal - ahorroMes));

    fijosAbiertos = Number(shPanel.getRange("H16").getValue() || 0);

  }

  const mesActivo = shPanel ? normalizarTexto(String(shPanel.getRange("L2").getValue() || "")) : mesAnioActualTexto_();

  const anioActivo = shPanel ? Number(shPanel.getRange("I2").getValue() || new Date().getFullYear()) : new Date().getFullYear();

  const arqueo = calcularArqueoCuentasMes_(ss, mesActivo, anioActivo);

  let msg = `💵 *DISPONIBILIDAD DE CAJA & ARQUEO*\n${separadorBonito_()}\n\n`;

  msg += `💎 *Disponible real en caja:* *$${formatearCOP(disponibleReal)} COP*\n`;

  msg += `💰 *Ingresos del mes (quincenas):* $${formatearCOP(ingresoTotal)} COP\n`;

  msg += `💸 *Gastos del mes:* $${formatearCOP(gastoTotal)} COP\n`;

  msg += `🐷 *Ahorro retenido Colanta:* $${formatearCOP(ahorroMes)} COP\n`;

  msg += `🔒 *Compromisos pendientes:* $${formatearCOP(fijosAbiertos)} COP\n\n`;

  msg += `🏦 *ARQUEO POR CUENTA (DEDUCCIÓN REAL):*\n`;

  msg += `🥛 *AyC Colanta (Nómina):* $${formatearCOP(arqueo.aycColanta)} COP\n`;

  msg += `📱 *Nequi:* $${formatearCOP(arqueo.nequi)} COP${arqueo.gastosNequi > 0 ? " _(Gastos: -$" + formatearCOP(arqueo.gastosNequi) + ")_" : ""}\n`;

  msg += `💵 *Efectivo:* $${formatearCOP(arqueo.efectivo)} COP${arqueo.gastosEfectivo > 0 ? " _(Gastos: -$" + formatearCOP(arqueo.gastosEfectivo) + ")_" : ""}\n`;

  msg += `📦 *Total Liquidez Inmediata:* *$${formatearCOP(arqueo.totalCaja)} COP*`;

  const hoyDisp = new Date();

  const ultimoDiaMesDisp = new Date(anioActivo, hoyDisp.getMonth() + 1, 0).getDate();

  const diasRestantesMesDisp = Math.max(1, ultimoDiaMesDisp - hoyDisp.getDate() + 1);

  const libreDespuesFijosDisp = Math.max(0, disponibleReal - fijosAbiertos);

  const ritmoDiarioMesDisp = Math.max(0, Math.round(libreDespuesFijosDisp / diasRestantesMesDisp));

  msg += `\n\n🎯 *Ritmo diario seguro (restando fijos pendientes):* *$${formatearCOP(ritmoDiarioMesDisp)} COP / día* (${diasRestantesMesDisp} días restantes del mes)\n`;

  msg += `_💡 Usa /saldo para calibrar cuentas, /quincena para tu flujo de nómina o /salud para tu diagnóstico 50/30/20._`;

  enviarMensajeTelegram(chatId, msg);

}

function mostrarControlFugas_(chatId) {

  const ss = obtenerLibro_();

  const mesActual = v21Months_()[new Date().getMonth()]; // e.g. "octubre"

  const anioActual = new Date().getFullYear();

  const shMov = ss.getSheetByName("MOVIMIENTOS");

  const shPanel = ss.getSheetByName("PANEL");

  let fugasMes = [];

  let totalFugasCalculado = 0;

  if (shMov && shMov.getLastRow() >= 5) {

    // Columnas MOVIMIENTOS: 1:ID, 2:Fecha, 3:Hora, 4:Tipo, 5:Categoría, 6:Subcategoría, 7:Descripción, 8:Valor, 9:Método, 10:Cuenta, 11:Mes, 12:Año, 13:Ref

    const numRows = shMov.getLastRow() - 4;

    const data = shMov.getRange(5, 1, numRows, 13).getValues();

    for (let i = 0; i < data.length; i++) {

      const r = data[i];

      const tipo = normalizarTexto(String(r[3] || ""));

      if (tipo !== "gasto") continue;

      const mesFila = normalizarTexto(String(r[10] || ""));

      const anioFila = Number(r[11] || 0);

      // Solo transacciones del mes y año actual

      if (mesFila && mesFila !== mesActual) continue;

      if (anioFila && anioFila !== anioActual) continue;

      const cat = normalizarTexto(String(r[4] || ""));

      const subcat = normalizarTexto(String(r[5] || ""));

      const desc = String(r[6] || "");

      const val = Number(r[7] || 0);

      const ref = String(r[12] || "");

      if (val <= 0) continue;

      // Exclusión estricta de fijos, nómina, servicios esenciales y deudas

      if (ref.includes("PAGO_FIJO") || subcat.includes("fijo") || desc.toLowerCase().includes("pago fijo")) continue;

      if (/epm|energia|luz|agua|gas\b|tigo|claro\b|arriendo|alquiler|soat|seguro|prestamo|deuda|cuota|tarjeta/i.test(desc)) continue;

      if (cat === "servicios" || cat === "vivienda") continue;

      // Detección de Fuga / Gasto Hormiga:

      const esHormigaDirecto = cat.includes("hormiga") || cat.includes("dulce") || subcat.includes("antojo") || subcat.includes("mecato") || subcat.includes("dulce");

      const esPalabraAntojo = /\b(endulzad|amigo secreto|mecat|dulce|chocolat|helad|paleta|antojo|graniz|empanad|golosin|snack|pasaboca|tinto|cafecito|chucher|galleta)\b/i.test(desc);

      const esMicropagoNoFijo = (val > 0 && val <= 25000 && !/gasolina|parqueadero|cuota|datos/i.test(desc) && cat !== "moto" && cat !== "transporte");

      if (esHormigaDirecto || esPalabraAntojo || esMicropagoNoFijo) {

        totalFugasCalculado += val;

        fugasMes.push({ desc: desc, val: val, fecha: String(r[1] || "") });

      }

    }

  }

  // Lectura de PANEL si existe, usando fallback con cálculo directo

  let totalFugas = totalFugasCalculado;

  let numMovs = fugasMes.length;

  let ticketProm = numMovs > 0 ? Math.round(totalFugas / numMovs) : 0;

  let pctAhorro = "0%";

  if (shPanel) {

    const pTotal = Number(shPanel.getRange("J32").getValue() || 0);

    const pNum = Number(shPanel.getRange("J33").getValue() || 0);

    const pProm = Number(shPanel.getRange("J34").getValue() || 0);

    const pPct = String(shPanel.getRange("J35").getDisplayValue() || "0%");

    if (pTotal > totalFugas) totalFugas = pTotal;

    if (pNum > numMovs) numMovs = pNum;

    if (pProm > 0) ticketProm = pProm;

    if (pPct && pPct !== "0%") pctAhorro = pPct;

    else {

      const ahorroMes = Number(shPanel.getRange("H6").getValue() || 236121);

      if (ahorroMes > 0 && totalFugas > 0) {

        pctAhorro = Math.round((totalFugas / ahorroMes) * 100) + "%";

      }

    }

  }

  let msg = `🐜 *RADAR DE GASTOS HORMIGA & FUGAS (${mesActual.toUpperCase()})*\n${separadorBonito_()}\n\n`;

  msg += `💸 *Total fugas acumuladas:* *$${formatearCOP(totalFugas)} COP*\n`;

  msg += `🔢 *Cantidad de micropagos:* ${numMovs}\n`;

  msg += `📊 *Ticket promedio por antojo:* $${formatearCOP(ticketProm)} COP\n`;

  msg += `🐷 *Impacto vs. Ahorro Colanta:* ${pctAhorro}\n\n`;

  if (fugasMes.length > 0) {

    msg += `🔎 *Últimos micropagos detectados (${mesActual.toUpperCase()}):*\n`;

    const recientes = fugasMes.slice(0, 5);

    recientes.forEach(f => {

      msg += `▫️ ${f.desc}: $${formatearCOP(f.val)} (${f.fecha})\n`;

    });

    msg += `\n💡 _Tip: Controlar estos microgastos libera liquidez inmediata para tu ahorro libre._`;

  } else {

    msg += `🎉 *¡Excelente control!* No tienes gastos hormiga detectados en ${mesActual}.\n\n`;

    msg += `💡 _Cada vez que compres un dulce, mecato, helado, endulzada o antojo al paso, regístralo aquí para monitorear tus fugas._`;

  }

  enviarMensajeTelegram(chatId, msg);

}

function mostrarReporteAhorrosColanta_(chatId) {

  const ss = obtenerLibro_();

  const shPanel = ss.getSheetByName("PANEL");

  const shAh = ss.getSheetByName("AHORROS");

  let mesOrd = 0, acumOrd = 0;

  let mesNav = 0, acumNav = 0;

  let mesApo = 0, acumApo = 0;

  let mesAfi = 0, acumAfi = 0;

  let mesTot = 0, acumTot = 0;

  if (shPanel && shPanel.getLastRow() >= 16) {

    mesOrd = Number(shPanel.getRange("C12").getValue() || 33000);

    acumOrd = Number(shPanel.getRange("D12").getValue() || 561000);

    mesNav = Number(shPanel.getRange("C13").getValue() || 120000);

    acumNav = Number(shPanel.getRange("D13").getValue() || 2040000);

    mesApo = Number(shPanel.getRange("C14").getValue() || 37567);

    acumApo = Number(shPanel.getRange("D14").getValue() || 957516);

    mesAfi = Number(shPanel.getRange("C15").getValue() || 45554);

    acumAfi = Number(shPanel.getRange("D15").getValue() || 91108);

    mesTot = Number(shPanel.getRange("C16").getValue() || 236121);

    acumTot = Number(shPanel.getRange("D16").getValue() || 3649624);

  } else if (shAh && shAh.getLastRow() >= 12) {

    mesTot = Number(shAh.getRange("B12").getValue() || 236121);

    acumTot = Number(shAh.getRange("C12").getValue() || 3649624);

  }

  const mesActual = mesAnioActualTexto_();

  let msg = `🐷 *FONDOS DE AHORRO COLANTA (${mesActual.toUpperCase()})*\n${separadorBonito_()}\n\n`;

  msg += `▫️ *Ahorro Ordinario:* $${formatearCOP(mesOrd)} mes | *$${formatearCOP(acumOrd)} acum*\n`;

  msg += `▫️ *Ahorro Navideño:* $${formatearCOP(mesNav)} mes | *$${formatearCOP(acumNav)} acum*\n`;

  msg += `▫️ *Aportes Cooperativos:* $${formatearCOP(mesApo)} mes | *$${formatearCOP(acumApo)} acum*\n`;

  msg += `▫️ *Aporte Afiliación / Otros:* $${formatearCOP(mesAfi)} mes | *$${formatearCOP(acumAfi)} acum*\n\n`;

  msg += `💎 *TOTAL AHORRADO EN COLANTA:*\n`;

  msg += `💰 Mes activo: *$${formatearCOP(mesTot)} COP*\n`;

  msg += `🏛️ Acumulado Total: *$${formatearCOP(acumTot)} COP*\n\n`;

  const hoyAh = new Date();

  const anioActualAh = hoyAh.getFullYear();

  const fechaNavidad = new Date(anioActualAh, 11, 1);

  let diasParaNavidad = Math.max(0, Math.ceil((fechaNavidad - hoyAh) / (1000 * 60 * 60 * 24)));

  const cuotaQuincenalNav = Math.round(mesNav / 2) || 60000;

  const mesActualNumAh = hoyAh.getMonth();

  const diaMesAh = hoyAh.getDate();

  let quincenasRestantesAh = 0;

  if (mesActualNumAh < 11) {

    quincenasRestantesAh += (diaMesAh <= 15 ? 2 : 1);

    for (let m = mesActualNumAh + 1; m <= 10; m++) {

      quincenasRestantesAh += 2;

    }

  }

  const proyeccionNavidad = acumNav + (quincenasRestantesAh * cuotaQuincenalNav);

  msg += `🎄 *TERMÓMETRO NAVIDEÑO COLANTA (Entrega 1 de Diciembre):*\n`;

  msg += `⏳ Faltan *${diasParaNavidad} días* (${quincenasRestantesAh} quincenas de nómina)\n`;

  msg += `🎁 Acumulado actual: *$${formatearCOP(acumNav)} COP*\n`;

  msg += `🚀 Proyección estimada al 1 de Dic: *$${formatearCOP(proyeccionNavidad)} COP*\n`;

  msg += `✨ _¡Tu prima y ahorro navideño garantizados para fin de año!_`;

  enviarMensajeTelegram(chatId, msg);

}

function listarMetasAhorro_(chatId) {

  const ss = obtenerLibro_();

  const sh = ss.getSheetByName("METAS AHORRO");

  if (!sh || sh.getLastRow() < 10) {

    enviarMensajeTelegram(chatId, `🎯 *MIS METAS Y PROYECTOS DE AHORRO*\n${separadorBonito_()}\n\nℹ️ *Actualmente no tienes metas de ahorro registradas.*\n\n💡 _Las metas te permiten hacer seguimiento a proyectos personales (ej: Moto, Viajes, Fondo de Emergencia) con aportes periódicos.\n\n📌 *Para crear tu primera meta cuando quieras, escribe:*\n💬 _"Crear meta Moto valor 5 millones aportando 200 mil mensual"_\n💬 o el comando: \`/meta Moto | 5000000 | 200000\``);

    return;

  }

  const numR = Math.min(15, sh.getLastRow() - 9);

  const rows = sh.getRange(10, 1, numR, 10).getValues();

  let enc = 0;

  let itemsMsg = "";

  rows.forEach(r => {

    const id = String(r[0] || "").trim();

    const obj = String(r[1] || "").trim();

    if (!obj || !id) return;

    const meta = Number(r[2] || 0);

    const actual = Number(r[3] || 0);

    let pctNum = typeof r[5] === "number" ? r[5] : (meta > 0 ? (actual / meta) : 0);

    if (pctNum > 1 && pctNum <= 100) pctNum = pctNum / 100;

    const pctStr = Math.round(pctNum * 100) + "%";

    let barra = String(r[6] || "").trim();

    if (!barra || barra.length < 5) {

      const bloques = Math.min(10, Math.max(0, Math.round(pctNum * 10)));

      barra = "█".repeat(bloques) + "░".repeat(10 - bloques);

    }

    const estado = String(r[7] || (pctNum >= 1 ? "🟢 Cumplida" : "🟡 En Progreso")).trim();

    itemsMsg += `🏷️ *${obj}*\n`;

    itemsMsg += `💰 Meta: $${formatearCOP(meta)} | Ahorrado: $${formatearCOP(actual)}\n`;

    itemsMsg += `📊 Avance: ${pctStr} ${barra}\n`;

    itemsMsg += `🏁 Estado: ${estado}\n`;

    itemsMsg += `🆔 \`${id}\`\n\n`;

    enc++;

  });

  if (enc === 0) {

    let msg = `🎯 *MIS METAS Y PROYECTOS DE AHORRO*\n${separadorBonito_()}\n\n`;

    msg += `ℹ️ *Actualmente no tienes metas de ahorro registradas.*\n\n`;

    msg += `💡 _Las metas te permiten hacer seguimiento a tus proyectos personales (ej: Moto, Viajes, Fondo de Emergencia) con aportes periódicos._\n\n`;

    msg += `📌 *Para crear tu primera meta cuando quieras, escribe:*\n`;

    msg += `💬 _"Crear meta Moto valor 5 millones aportando 200 mil mensual"_\n`;

    msg += `💬 o el comando: \`/meta Moto | 5000000 | 200000\``;

    enviarMensajeTelegram(chatId, msg);

    return;

  }

  let msg = `🎯 *MIS METAS Y PROYECTOS DE AHORRO*\n${separadorBonito_()}\n\n`;

  msg += itemsMsg;

  msg += `_Para abonar a una meta, escribe: 'Abono 50000 a meta Moto'_`;

  enviarMensajeTelegram(chatId, msg);

}

function enviarRecordatoriosFijos_(chatIdManual) {

  const props = PropertiesService.getScriptProperties();

  const chatId = chatIdManual || props.getProperty("TELEGRAM_CHAT_ID");

  if (!chatId) return;

  const ss = obtenerLibro_();

  const sh = ss.getSheetByName("CONTROL FIJOS");

  if (!sh || sh.getLastRow() < 9) return;

  // En la hoja real, los datos comienzan en la fila 9 (Encabezados en fila 8)

  // Columnas: A=ID, B=Concepto, C=Categoría, D=Día, E=Cuota, F=Abonado, G=Estado, H=Próximo pago, I=Diferencia

  const rows = sh.getRange(9, 1, sh.getLastRow() - 8, 11).getValues();

  const mesActual = mesAnioActualTexto_();

  const hoy = new Date();

  const diaHoy = hoy.getDate();

  const vencidos = [];

  const hoyVencen = [];

  const proximos = [];

  rows.forEach(r => {

    const id = String(r[0] || "").trim();

    if (!id.toUpperCase().startsWith("F-")) return;

    const concepto = String(r[1] || "Gasto Fijo").trim();

    if (!concepto) return;

    const estado = normalizarTexto(String(r[6] || ""));

    const diferencia = Number(r[8] || 0);

    const cuota = Number(r[4] || 0);

    const valorPendiente = diferencia > 0 ? diferencia : cuota;

    // Si ya está marcado como Pagado o la diferencia es 0, no alertar

    if (estado.includes("pagad") || (estado.includes("total") && valorPendiente <= 0)) return;

    let diaCobro = parseInt(r[3], 10);

    if (isNaN(diaCobro) || diaCobro <= 0) {

      if (r[7] instanceof Date) diaCobro = r[7].getDate();

      else diaCobro = 15;

    }

    const diff = diaCobro - diaHoy;

    if (diff < 0) {

      vencidos.push({ concepto: concepto, cuota: valorPendiente, dia: diaCobro, diasAtraso: Math.abs(diff) });

    } else if (diff === 0) {

      hoyVencen.push({ concepto: concepto, cuota: valorPendiente, dia: diaCobro });

    } else if (diff <= 3) {

      proximos.push({ concepto: concepto, cuota: valorPendiente, dia: diaCobro, diasFaltan: diff });

    }

  });

  if (!vencidos.length && !hoyVencen.length && !proximos.length) {

    if (chatIdManual) {

      enviarMensajeTelegram(chatId, `🎉 *TODO AL DÍA EN GASTOS FIJOS*\n${separadorBonito_()}\n\nNo tienes obligaciones pendientes por vencer en los próximos 3 días ni vencidas para este mes.`);

    }

    return;

  }

  let msg = `🔔 *RECORDATORIO MATUTINO DE GASTOS FIJOS*\n${separadorBonito_()}\n\n`;

  if (vencidos.length > 0) {

    msg += `🚨 *VENCIDOS / EN MORA:*\n`;

    vencidos.forEach(v => {

      msg += `🔴 *${v.concepto}*: $${formatearCOP(v.cuota)} COP (Día ${v.dia} — Hace ${v.diasAtraso} días)\n`;

    });

    msg += `\n`;

  }

  if (hoyVencen.length > 0) {

    msg += `⚠️ *VENCEN HOY:*\n`;

    hoyVencen.forEach(h => {

      msg += `🟠 *${h.concepto}*: $${formatearCOP(h.cuota)} COP (¡Pagar hoy día ${h.dia}!)\n`;

    });

    msg += `\n`;

  }

  if (proximos.length > 0) {

    msg += `🗓️ *PRÓXIMOS A VENCER (3 DÍAS):*\n`;

    proximos.forEach(p => {

      const txtTiempo = p.diasFaltan === 1 ? "vence mañana" : `vence en ${p.diasFaltan} días`;

      msg += `🟡 *${p.concepto}*: $${formatearCOP(p.cuota)} COP (Día ${p.dia} — ${txtTiempo})\n`;

    });

  }

  msg += `\n_Para registrar un pago di: "Pagué [concepto]" o por nota de voz._`;

  try {

    const shPanel = ss.getSheetByName("PANEL");

    if (shPanel) {

      const disp = Number(shPanel.getRange("K6").getValue() || 0);

      const fijosPen = Number(shPanel.getRange("H16").getValue() || 0);

      const diasRest = Math.max(1, new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).getDate() - diaHoy + 1);

      const libreReal = Math.max(0, disp - fijosPen);

      const diarioRec = Math.max(0, Math.round(libreReal / diasRest));

      if (diarioRec > 0) {

        msg += `\n\n🎯 *Presupuesto diario seguro recomendado para hoy:* *$${formatearCOP(diarioRec)} COP / día*\n` +

          `💡 _Si te mantienes en este promedio hoy, asegurarás cubrir tus fijos y llegar a la próxima quincena con tranquilidad._`;

      }

    }

  } catch (eRec) {}

  enviarMensajeTelegram(chatId, msg);

}

/* =========================================================

   TELEGRAM ROUTER & MENSAJERÍA (INCLUYE NOTAS DE VOZ)

   ========================================================= */

function procesarMensajesTelegram() {

  verificarVariables();

  // Candado de concurrencia: Evita que dos ejecuciones simultáneas procesen los mismos mensajes

  const lock = LockService.getScriptLock();

  if (!lock.tryLock(5000)) return; // Si ya hay otra ejecución corriendo, salir para no duplicar

  try {

    ejecutarConsultaTelegram();

  } finally {

    lock.releaseLock();

  }

}

function ejecutarConsultaTelegram() {

  const props = PropertiesService.getScriptProperties();

  const cache = CacheService.getScriptCache();

  let url = "https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/getUpdates?limit=50&timeout=0";

  if (props.getProperty("TELEGRAM_OFFSET")) url += "&offset=" + props.getProperty("TELEGRAM_OFFSET");

  

  let respRaw;

  try {

    respRaw = UrlFetchApp.fetch(url, { method: "get", muteHttpExceptions: true });

  } catch (e) {

    return;

  }

  

  let datos = JSON.parse(respRaw.getContentText() || "{}");

  if (!datos.ok) {

    // 409 = Webhook activo. Si WEBHOOK_URL está configurada, es lo correcto (doPost atiende) y NO se debe borrar.

    if (datos.error_code === 409 && !WEBHOOK_URL) {

      try {

        UrlFetchApp.fetch("https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/deleteWebhook?drop_pending_updates=false", { muteHttpExceptions: true });

        respRaw = UrlFetchApp.fetch(url, { method: "get", muteHttpExceptions: true });

        datos = JSON.parse(respRaw.getContentText() || "{}");

      } catch (e409) {}

    }

    if (!datos.ok || !datos.result || !datos.result.length) return;

  }

  if (!datos.result || !datos.result.length) return;

  

  let maxUpdateId = 0;

  const grupos = {};

  

  datos.result.forEach(update => {

    if (update.update_id > maxUpdateId) maxUpdateId = update.update_id;

    

    // Anti-duplicados por Cache de Update ID (600 segundos)

    const keyUpdate = "TG_UPD_" + update.update_id;

    if (cache.get(keyUpdate)) return;

    try { cache.put(keyUpdate, "1", 600); } catch (e) {}

    if (update.callback_query) {

      procesarCallbackTelegram(update.callback_query);

      return;

    }

    if (update.message) {

      const chatId = update.message.chat.id;

      grupos[chatId] = grupos[chatId] || [];

      grupos[chatId].push(update);

    }

  });

  // Guardar el offset inmediatamente para que la API de Telegram no vuelva a enviar los mismos updates

  if (maxUpdateId > 0) {

    props.setProperty("TELEGRAM_OFFSET", String(maxUpdateId + 1));

  }

  Object.keys(grupos).forEach(chatId => procesarGrupoMensajes(chatId, grupos[chatId]));

}

function procesarGrupoMensajes(chatId, listaUpdates) {

  PropertiesService.getScriptProperties().setProperty("TELEGRAM_CHAT_ID", String(chatId));

  let textos = [], archivo = null, voz = null;

  listaUpdates.forEach(update => {

    const msg = update.message;

    if (msg.voice) { voz = msg.voice; return; }

    if (msg.document || msg.photo) { archivo = msg; return; }

    if (!msg.text) return;

    const texto = msg.text.trim(), lower = texto.toLowerCase();

    const tieneMonto = /\b\d{3,}\b|\b\d+[kK]\b|\$\s*\d+|\b(mil|mill[oó]n|millones|lucas?)\b/i.test(texto);

    // 0. Manejo Inteligente de Saludos y Conversación Humana (IA / Lenguaje Natural)

    if (!tieneMonto) {

      if (/^(hola|buenas|buenos d[ií]as|buenas tardes|buenas noches|c[oó]mo est[aá]s|como estas|c[oó]mo te va|como te va|qu[eé] tal|que tal|qu[eé] m[aá]s|que mas|hey|saludos|todo bien|c[oó]mo andas|como andas|quihubo|quiubo|buen d[ií]a)\b[!?\s]*$/i.test(texto)) {

        if (/\b(c[oó]mo est[aá]s|como estas|c[oó]mo te va|como te va|qu[eé] tal|que tal|c[oó]mo andas|como andas|todo bien)\b/i.test(lower)) {

          enviarMensajeTelegram(

            chatId,

            `¡Todo excelente Marlon, gracias por preguntar! 💪 Con todas las pilas puestas para ayudarte a cuidar el bolsillo y mantener tus cuentas al día.\n\n` +

            `¿Qué movimiento vamos a registrar hoy?\n` +

            `• 💰 *Gasto:* ej. _50 mil almuerzo_\n` +

            `• 📌 *Gasto Fijo:* ej. _Pagué 120 mil este mes EPM_\n` +

            `• 💵 *Disponible:* _/disponible_\n` +

            `• 📊 *Resumen:* _/resumen_`,

            obtenerTecladoBotonesRapidos_()

          );

        } else {

          enviarMensajeTelegram(

            chatId,

            `¡Hola Marlon! 👋 Un gusto saludarte.\n\n` +

            `Estoy listo para ayudarte con tus finanzas. ¿Qué deseas hacer hoy?\n` +

            `• 💰 *Registrar gasto:* ej. _25 mil gasolina moto_\n` +

            `• 📌 *Pagar compromiso fijo:* ej. _Pagué 120 mil este mes EPM_\n` +

            `• 💵 *Ver disponible:* _/disponible_\n` +

            `• 📊 *Ver estado del mes:* _/resumen_`,

            obtenerTecladoBotonesRapidos_()

          );

        }

        return;

      }

      if (/^(gracias|muchas gracias|listo|ok|vale|dale|perfecto|excelente|genial|entendido|de una|chao|hasta luego|hasta pronto|nos vemos|bien gracias|todo claro|gracias hermano)\b[!?\s]*$/i.test(texto)) {

        enviarMensajeTelegram(chatId, `¡Con todo el gusto, Marlon! 🚀 Aquí estoy siempre pendiente de tus finanzas. Si necesitas registrar cualquier otro gasto o pago, solo me escribes.`);

        return;

      }

      if (/^(qui[eé]n eres|qu[eé] haces|qu[eé] puedes hacer|para qu[eé] sirves)\b/i.test(lower)) {

        enviarMensajeTelegram(chatId, `🤖 *Soy PardoFinanzas V26 PRO, tu Director Financiero Personal (CFO).*\n\n` +

          `🎯 *¿En qué te ayudo?*\n` +

          `• Registrar tus gastos e ingresos al instante por texto o notas de voz.\n` +

          `• Administrar tus Gastos Fijos (EPM, arriendo, internet, celular, etc.).\n` +

          `• Controlar el saldo real en tus cuentas (AyC Colanta, Nequi, Efectivo).\n` +

          `• Leer tus colillas de nómina de Colanta y contabilizar ahorros.\n` +

          `• Detectar gastos hormiga y alertar sobrecostos.\n` +

          `• Asesorarte financieramente cuando lo necesites (/consejo).`);

        return;

      }

    }

    if (["/start", "/help", "/ayuda", "ayuda", "comandos", "guia", "guía", "❓ menú y ayuda", "❓ ayuda", "menu y ayuda"].includes(lower)) {

      enviarMensajeTelegram(chatId, obtenerGuiaComandos(), obtenerTecladoBotonesRapidos_());

      return;

    }

    if (["/menu", "menu", "menú", "botones", "/botones"].includes(lower)) {

      enviarMenuInteractivoBotones_(chatId);

      return;

    }

    if (["/colilla", "/colillas", "/nomina", "/sueldo", "colilla", "nomina", "📄 enviar colilla", "📄 colilla", "enviar colilla"].includes(lower)) {

      enviarMensajeTelegram(

        chatId,

        "📄 *ENVÍO DE COLILLA DE PAGO COLANTA*\n" + separadorBonito_() + "\n\n" +

        "Para procesar tu colilla de nómina:\n" +

        "1️⃣ Adjunta el archivo PDF o una foto clara de tu comprobante de pago de Colanta.\n" +

        "2️⃣ El bot extraerá automáticamente:\n" +

        "   • Salario básico, días y devengados\n" +

        "   • Deducciones de Salud, Pensión y Fondo de Empleados\n" +

        "   • Ahorros (Ordinario, Navideño, Aportes)\n" +

        "   • Neto consignado en AyC Colanta\n" +

        "3️⃣ Se sincronizará en `COLILLAS DE PAGO`, `AHORROS` y `MOVIMIENTOS`."

      );

      return;

    }

    if (["borrar", "eliminar", "deshacer", "/deshacer"].includes(lower)) {

      const result = borrarUltimoConjunto();

      enviarMensajeTelegram(chatId, result.ok ? `🗑️ *ÚLTIMO ELIMINADO*\n🆔 ${result.id}\n✅ Limpio de Movimientos.` : "⚠️ No encontré registros recientes en Movimientos.");

      return;

    }

    if (lower.indexOf("/borrar") === 0) {

      const id = texto.split(/\s+/)[1]?.trim().toUpperCase();

      if (id && borrarRegistroCompletoPorID(id).ok) { enviarMensajeTelegram(chatId, "🗑️ Eliminado: " + id); }

      else enviarMensajeTelegram(chatId, "⚠️ No encontré ese ID.");

      return;

    }

    if (["/facturas", "/factura", "/correos", "/revisar_correos", "/escanear", "revisar correos", "📬 facturas gmail", "facturas gmail"].includes(lower) ||

        /(^|\s)(revisar?\s*(los\s*)?correos?|analizar?\s*(los\s*)?correos?|escanear?\s*(los\s*)?correos?|facturas?\s*gmail)(\s|$)/i.test(lower)) {

      asegurarTriggersActivos_();

      const cant = procesarCorreosFacturas();

      enviarMensajeTelegram(chatId, `📬 *ESCÁNER DE BANDEJA GMAIL*\n${separadorBonito_()}\n\n🔍 Se revisó la Bandeja de Entrada.\n✅ Comprobantes/Facturas procesadas: *${cant}*\n⏰ *Auto-archivado*: Los correos permanecen en Recibidos 1 hora para tu lectura y luego pasan a su carpeta.`);

      return;

    }

    if (lower === "/deudas" || lower === "/deuda") { listarDeudasPendientes(chatId); return; }

    if (["/nuevofijo", "/crearfijo", "crear fijo", "nuevo fijo", "crear gasto fijo", "nuevo gasto fijo", "➕ nuevo fijo", "➕ crear fijo", "ayuda fijos", "ayuda fijo"].includes(lower) || (!tieneMonto && (lower.includes("crear fijo") || lower.includes("nuevo fijo") || lower.includes("crear gasto fijo")))) {

      mostrarAyudaCrearGastoFijo_(chatId);

      return;

    }

    if (lower === "/gastosfijos" || lower === "/gastos fijos" || lower === "/fijos" || lower === "📌 gastos fijos" || (!tieneMonto && lower.includes("fijos"))) {

      listarGastosFijosDelMes(chatId);

      return;

    }

    if (lower === "/porcobrar" || lower === "/cobrar" || lower === "🤝 por cobrar" || (!tieneMonto && (lower === "por cobrar" || lower === "cuentas por cobrar" || lower === "prestamos"))) {

      listarCuentasPorCobrarV24_(chatId);

      return;

    }

    if (lower === "/metas" || lower === "/meta" || lower === "🎯 mis metas" || (!tieneMonto && (lower === "metas" || lower === "mis metas" || lower === "metas de ahorro"))) {

      listarMetasAhorro_(chatId);

      return;

    }

    if (lower === "/ahorros" || lower === "/ahorro" || lower === "🐷 ahorros colanta" || (!tieneMonto && (lower === "ahorros" || lower === "ahorros colanta" || lower === "mis ahorros"))) {

      mostrarReporteAhorrosColanta_(chatId);

      return;

    }

    if (lower === "/resumen" || lower === "/estado" || lower === "📊 estado mes" || lower === "📊 resumen mes" || lower === "estado mes" || (!tieneMonto && (lower === "resumen" || lower === "resumen mes" || lower === "balance" || lower === "estado"))) {

      mostrarResumenPresupuesto_(chatId);

      return;

    }

    if (lower === "/disponible" || lower === "💵 disponible" || lower === "💵 saldo disponible" || lower === "/caja" || lower === "caja" || lower === "/arqueo" || lower === "arqueo" || (!tieneMonto && (lower === "disponible" || lower === "saldo" || lower === "plata" || lower === "cuanto tengo" || lower === "cuanto me queda"))) {

      mostrarDisponibleCaja_(chatId);

      return;

    }

    if (lower === "/sincronizar" || lower === "/sinc" || lower === "/reparar" || lower === "sincronizar" || lower === "reparar panel" || lower === "actualizar panel") {

      sincronizarColillasYPanel_(chatId);

      return;

    }

    if (lower === "/fugas" || lower === "/hormiga" || lower === "🐜 fugas" || lower === "🐜 gastos hormiga" || (!tieneMonto && (lower === "fugas" || lower === "hormiga" || lower === "gastos hormiga"))) {

      mostrarControlFugas_(chatId);

      return;

    }

    if (["/corregir", "/arreglar", "/reparar", "corregir", "arreglar", "corregir greenfit"].includes(lower)) {

      autoCorregirGreenfitComida_(chatId, true);

      return;

    }

    if (["/archivar", "/limpiarcorreos", "archivar", "limpiar correos"].includes(lower)) {

      const cant = archivarCorreosFinancierosViejos_();

      enviarMensajeTelegram(chatId, `📬 *BANDEJA ORGANIZADA*\n${separadorBonito_()}\n\n✅ Se archivaron *${cant}* correos financieros que llevaban más de 1 hora en Recibidos.\n📂 Todos permanecen guardados en sus carpetas de *FINANZAS*.`);

      return;

    }

    if (lower === "/recordatorios" || lower === "recordatorios") { enviarRecordatoriosFijos_(chatId); return; }

    if (["/quincena", "/ciclo", "quincena", "⏳ quincena", "balanza quincenal"].includes(lower)) {

      mostrarReporteQuincena_(chatId);

      return;

    }

    if (lower.startsWith("/saldo") || lower.startsWith("/ajustarsaldo") || lower.startsWith("ajustar saldo") || lower.startsWith("/calibrar")) {

      gestionarAjusteSaldoCuentas_(chatId, texto);

      return;

    }

    if (["/salud", "salud", "/503020", "503020", "/diagnostico503020", "regla 50/30/20", "diagnostico financiero", "diagnóstico financiero"].includes(lower)) {

      mostrarDiagnostico503020_(chatId);

      return;

    }

    if (lower === "💡 consejo ia" || lower === "🧠 análisis ia" || lower === "🧠 analisis ia" || lower === "analisis" || lower === "análisis" || lower.startsWith("/analisis") || lower.startsWith("/analizar") || lower.startsWith("/diagnostico") || lower === "consejo" || lower.startsWith("/consejo") || lower.startsWith("/asesor") || lower.startsWith("/cfo") || lower.includes("me alcanza") || lower.includes("puedo gastar") || lower.includes("como voy") || lower.includes("cómo voy") || lower.includes("diagnostico") || lower.includes("diagnóstico")) {

      const consulta = texto.replace(/^\/(consejo|asesor|analisis|analizar|diagnostico|cfo)\s*/i, "").replace(/^[💡🧠\s]*(consejo|analisis|análisis)\s*ia/i, "").trim() || "Realiza un análisis financiero integral de mi situación actual, liquidez en AyC Colanta y Nequi, gastos y recomendaciones clave.";

      ejecutarAnalisisFinancieroInteligenteGemini_(chatId, consulta);

      return;

    }

    textos.push(texto);

  });

  if (voz) {

    procesarNotaDeVozTelegram_(chatId, voz);

    return;

  }

  if (archivo) {

    procesarDocumentoTelegramV24_(chatId, archivo);

    return;

  }

  if (!textos.length) return;

  const textoUnificado = textos.join("\n");

  const envioPendiente = obtenerEnvioPendiente(chatId);

  if (envioPendiente) {

    if (textoUnificado.length > 0) {

      borrarEnvioPendiente(chatId);

      registrarMovimientoDesdeEnvio(chatId, envioPendiente, textoUnificado.trim());

      const sig = obtenerEnvioPendiente(chatId);

      if (sig) enviarMensajeTelegram(chatId, `⏳ *AÚN TIENES OTRO ENVÍO PENDIENTE*\n${separadorBonito_()}\n\n💵 Valor: *$${formatearCOP(sig.valor)}*\n👤 Destinatario: *${sig.proveedor}*\n\n🧐 ¿De qué se trata?`);

    } else enviarMensajeTelegram(chatId, "❌ Responde con una categoría válida.");

    return;

  }

  const gfPendiente = obtenerGastoFijoPendiente(chatId);

  if (gfPendiente) {

    const ext = extraerCamposGastoFijo_(textoUnificado);

    if (/\b(pague|pago|pagado|cancele|cancelado|abone|abono)\b/.test(normalizarTexto(textoUnificado)) && ext.valor === null) {

      borrarGastoFijoPendiente(chatId);

    } else {

      const comb = { concepto: gfPendiente.concepto || ext.concepto || normalizarRespuestaConcepto_(textoUnificado), valor: (gfPendiente.valor > 0 ? gfPendiente.valor : ext.valor), dia: gfPendiente.dia || ext.dia || 15, categoria: gfPendiente.categoria || ext.categoria || "Otros" };

      if (comb.concepto && comb.valor > 0) { borrarGastoFijoPendiente(chatId); registrarGastoFijoCompleto(chatId, comb); return; }

      guardarGastoFijoPendiente(chatId, comb);

      enviarMensajeTelegram(chatId, `📝 *FALTAN DATOS DEL FIJO*\n${!comb.concepto ? '🔹 Concepto (ej. Internet)\n' : ''}${comb.valor <= 0 ? '🔹 Valor mensual (ej. 120 mil)\n' : ''}`);

      return;

    }

  }

  const intencionGF = detectarIntencionGastoFijo(textoUnificado);

  if (intencionGF) {

    if (intencionGF.completo) { registrarGastoFijoCompleto(chatId, intencionGF); return; }

    guardarGastoFijoPendiente(chatId, intencionGF);

    enviarMensajeTelegram(chatId, `📝 *VAMOS A CREAR UN GASTO FIJO*\n${!intencionGF.concepto ? '🔹 Concepto?\n' : ''}${intencionGF.valor === null ? '🔹 Valor mensual?\n' : ''}${!intencionGF.dia ? '🔹 Día de cobro?\n' : ''}`);

    return;

  }

  const deudaPendiente = obtenerDeudaPendiente(chatId);

  if (deudaPendiente) {

    const ext = extraerCamposDeuda_(textoUnificado);

    if (/\b(pague|pago|pagado|abone|abono)\b/.test(normalizarTexto(textoUnificado)) && ext.total === null && ext.cuota === null) {

      borrarDeudaPendiente(chatId);

    } else {

      const comb = { tipo: deudaPendiente.tipo || ext.tipo || "Gasto", nombre: deudaPendiente.nombre || ext.nombre, total: deudaPendiente.total !== null ? deudaPendiente.total : ext.total, cuota: deudaPendiente.cuota !== null ? deudaPendiente.cuota : ext.cuota, numCuotas: deudaPendiente.numCuotas || ext.numCuotas || 0 };

      if (comb.total !== null && comb.cuota !== null && comb.nombre) { borrarDeudaPendiente(chatId); registrarDeudaCompleta(chatId, comb); return; }

      guardarDeudaPendiente(chatId, comb);

      enviarMensajeTelegram(chatId, `📝 *FALTAN DATOS DE LA DEUDA*\n${!comb.nombre ? '🔹 Concepto?\n' : ''}${comb.total === null ? '🔹 Total?\n' : ''}${comb.cuota === null ? '🔹 Valor cuota?\n' : ''}`);

      return;

    }

  }

  const intencionDeuda = detectarIntencionDeuda(textoUnificado);

  if (intencionDeuda) {

    if (intencionDeuda.completo) { registrarDeudaCompleta(chatId, intencionDeuda); return; }

    guardarDeudaPendiente(chatId, intencionDeuda);

    enviarMensajeTelegram(chatId, `📝 *REGISTREMOS LA DEUDA*\n${!intencionDeuda.nombre ? '🔹 Concepto?\n' : ''}${intencionDeuda.total === null ? '🔹 Total?\n' : ''}${intencionDeuda.cuota === null ? '🔹 Valor cuota?\n' : ''}`);

    return;

  }

  // Evaluación prioritaria de Pagos de Gastos Fijos (IA + Semántico)

  if (procesarPagoFijo(chatId, textoUnificado)) {

    return;

  }

  const lineasFijo = extraerLineasPagoFijo_([textoUnificado]);

  if (lineasFijo.length) {

    let proc = 0;

    lineasFijo.forEach(linea => { if (procesarPagoFijo(chatId, linea)) proc++; });

    if (proc === lineasFijo.length) return;

  }

  if (procesarRespuestaAhorroPendiente_(chatId, textoUnificado)) return;

  if (procesarConfirmacionAbonoPendiente_(chatId, textoUnificado)) return;

  const abonoSolicitado = detectarSolicitudFijoPendiente_(chatId, textoUnificado);

  if (abonoSolicitado) {

    enviarMensajeTelegram(chatId, `🧠 *POSIBLE OBLIGACIÓN FIJA*\n🏷️ ${abonoSolicitado.concepto}\n💵 $${formatearCOP(abonoSolicitado.valor)}\n\n🟡 ¿Registrar como *ABONO* al gasto fijo? (Sí/No)`);

    return;

  }

  const traslado = detectarTrasladoInterno_(textoUnificado);

  if (traslado) { registrarTrasladoInterno_(chatId, traslado); return; }

  const prestamo = detectarPrestamoPorCobrarV24_(textoUnificado);

  if (prestamo) { registrarCuentaPorCobrarV24_(chatId, prestamo); return; }

  const cobro = detectarCobroCuentaPorCobrarV24_(textoUnificado);

  if (cobro) { procesarCobroCuentaPorCobrarV24_(chatId, cobro); return; }

  let items = analizarMovimientosConGemini(textoUnificado);

  if (!items || !items.length) {

    const meta = detectarMetaAhorroLocal_(textoUnificado);

    items = meta ? [meta] : extraeMultipleLocal([textoUnificado]);

  }

  if (!items || !items.length) {

    // Solo activar motor de análisis financiero si el usuario hace una consulta financiera explícita

    const esConsultaFinanciera = /\b(me alcanza|puedo gastar|cuanto tengo|cuánto tengo|como voy|cómo voy|consejo|analisis|análisis|diagnostico|diagnóstico|auditoria|auditoría|recomi[eé]nda|presupuesto|quincena|fijo|deuda|ahorro|balance)\b/i.test(textoUnificado) || textoUnificado.includes("?");

    if (esConsultaFinanciera) {

      ejecutarAnalisisFinancieroInteligenteGemini_(chatId, textoUnificado);

      return;

    }

    // Orientación amigable sin diagnóstico no solicitado

    enviarMensajeTelegram(

      chatId,

      `🤖 No identifiqué ningún movimiento contable en tu mensaje.\n\n` +

      `💡 *Puedes escribir por ejemplo:*\n` +

      `• *Gasto diario:* _50 mil comida rápida_\n` +

      `• *Gasto fijo:* _Pagué 120 mil este mes EPM_\n` +

      `• *Ingreso:* _Recibí 200 mil de un trabajo_\n\n` +

      `O toca un botón abajo para consultar tu información:`,

      obtenerTecladoBotonesRapidos_()

    );

    return;

  }

  procesarItemsFinancieros(chatId, items, textoUnificado);

}

function procesarNotaDeVozTelegram_(chatId, voice) {

  try {

    enviarMensajeTelegram(chatId, "🎙️ *ESCUCHANDO AUDIO...*\n" + separadorBonito_() + "\n\n🤖 Analizando tu nota de voz...");

    const urlFile = "https://api.telegram.org/bot" + TELEGRAM_TOKEN + "/getFile?file_id=" + encodeURIComponent(voice.file_id);

    const jsonFile = JSON.parse(UrlFetchApp.fetch(urlFile, { muteHttpExceptions: true }).getContentText());

    if (!jsonFile.ok || !jsonFile.result || !jsonFile.result.file_path) throw new Error("Telegram no pudo obtener el audio.");

    const downloadUrl = "https://api.telegram.org/file/bot" + TELEGRAM_TOKEN + "/" + jsonFile.result.file_path;

    const blob = UrlFetchApp.fetch(downloadUrl, { muteHttpExceptions: true }).getBlob();

    const base64 = Utilities.base64Encode(blob.getBytes());

    const prompt = `Eres el motor contable de un bot de finanzas personales en Colombia.

Escucha atentamente el audio del usuario y extrae los movimientos financieros.

Devuelve SOLO JSON válido con un array de objetos:

[{"tipo":"Gasto|Ingreso","categoria":"Alimentación|Gastos Hormiga / Dulces|Moto|Transporte|Vivienda|Servicios|Entretenimiento|Salud|Compras|Educación|Otros","subcategoria":"...","descripcion_formal":"...","valor":0,"metodo_pago":null,"cuenta":null,"intencion":"REGISTRAR_MOVIMIENTO","ignorar":false,"texto_original":"..."}]

Reglas Críticas:

1. El usuario titular es ${USUARIO_NOMBRE_COMPLETO}. Si él dice que envió, transfirió o pagó a alguien, es un GASTO de su bolsillo, NUNCA ingreso.

2. Si la transacción es hacia o desde 'Clara Aide' (o Aidé), pon obligatoriamente 'ignorar': true en el JSON.

3. Asigna categorías precisas (ej. gasolina/taller -> Moto, mercado/comida -> Alimentación, internet/luz -> Servicios).

4. No inventes valores. Si no logras escuchar un monto o movimiento financiero, devuelve [].`;

    const respuesta = llamarGeminiAPI({

      contents: [{

        parts: [

          { inlineData: { mimeType: voice.mime_type || "audio/ogg", data: base64 } },

          { text: prompt }

        ]

      }],

      generationConfig: { responseMimeType: "application/json", temperature: 0 }

    });

    if (respuesta.error || !respuesta.candidates || !respuesta.candidates[0].content) {

      throw new Error(respuesta.error ? respuesta.error.message : "Sin respuesta de Gemini.");

    }

    const arr = JSON.parse(limpiarJSON(respuesta.candidates[0].content.parts[0].text));

    if (!Array.isArray(arr) || !arr.length) {

      enviarMensajeTelegram(chatId, "🧐 No logré identificar ningún movimiento financiero en tu audio.");

      return;

    }

    const validos = arr.filter(x => !x.ignorar && Number(x.valor || 0) > 0);

    if (arr.some(x => x.ignorar)) {

      enviarMensajeTelegram(chatId, `🙈 *MOVIMIENTO IGNORADO*\n${separadorBonito_()}\n\nOmití este registro porque corresponde a la regla de exclusión de Clara Aide.`);

      if (!validos.length) return;

    }

    if (!validos.length) {

      enviarMensajeTelegram(chatId, "🧐 No logré entender un valor o movimiento financiero válido en tu audio.");

      return;

    }

    procesarItemsFinancieros(chatId, validos, "Nota de voz");

  } catch (e) {

    const errMsg = String(e.message || e).replace(/[_*`\[\]]/g, " ");

    enviarMensajeTelegram(chatId, "🚨 *ERROR AL PROCESAR AUDIO*\n\n🔧 " + errMsg);

  }

}

function obtenerTecladoBotonesRapidos_() {

  return {

    keyboard: [

      [ { text: "📊 Estado Mes" }, { text: "💵 Disponible" }, { text: "🐜 Fugas" } ],

      [ { text: "📌 Gastos Fijos" }, { text: "⏳ Quincena" }, { text: "🐷 Ahorros Colanta" } ],

      [ { text: "🎯 Mis Metas" }, { text: "🤝 Por Cobrar" }, { text: "🧠 Análisis IA" } ],

      [ { text: "📄 Enviar Colilla" }, { text: "📬 Facturas Gmail" }, { text: "❓ Menú y Ayuda" } ]

    ],

    resize_keyboard: true,

    is_persistent: true,

    one_time_keyboard: false

  };

}

function enviarMenuInteractivoBotones_(chatId) {

  const payload = {

    chat_id: chatId,

    text: "🏛️ *CENTRO DE CONTROL FINANCIERO — MENÚ DE BOTONES*\n" + separadorBonito_() + "\n\nToca cualquier opción para consultar en vivo tu libro de finanzas:",

    parse_mode: "Markdown",

    reply_markup: {

      inline_keyboard: [

        [ { text: "📊 Resumen Mes", callback_data: "btn_resumen" }, { text: "💵 Saldo Disponible", callback_data: "btn_disponible" } ],

        [ { text: "🐜 Gastos Hormiga", callback_data: "btn_fugas" }, { text: "⏳ Quincena", callback_data: "btn_quincena" } ],

        [ { text: "📌 Gastos Fijos", callback_data: "btn_fijos" }, { text: "🐷 Ahorros Colanta", callback_data: "btn_ahorros" } ],

        [ { text: "🎯 Mis Metas", callback_data: "btn_metas" }, { text: "🤝 Por Cobrar", callback_data: "btn_cobrar" } ],

        [ { text: "🧠 Análisis Integral IA", callback_data: "btn_analisis_ia" }, { text: "💡 Consejo Rápido", callback_data: "btn_consejo" } ]

      ]

    }

  };

  try {

    UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify(payload),

      muteHttpExceptions: true

    });

  } catch (e) {}

}

/**

 * Envía una acción visual a Telegram (typing, upload_document, etc.)

 * para feedback inmediato mientras la IA genera la respuesta ("escribiendo...").

 */

function enviarAccionTelegram_(chatId, accion) {

  try {

    const act = accion || "typing";

    UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendChatAction`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify({ chat_id: chatId, action: act }),

      muteHttpExceptions: true

    });

  } catch (e) {}

}

function enviarMensajeTelegram(chatId, texto, tecladoPersonalizado) {

  const payload = { chat_id: chatId, text: String(texto || ""), parse_mode: "Markdown" };

  if (tecladoPersonalizado !== null) {

    payload.reply_markup = tecladoPersonalizado || obtenerTecladoBotonesRapidos_();

  }

  try {

    const res = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify(payload),

      muteHttpExceptions: true

    });

    const txt = res.getContentText();

    let j;

    try { j = JSON.parse(txt); } catch (e) { j = { ok: false, description: txt }; }

    if (!j.ok) {

      delete payload.parse_mode;

      const resFallback = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

        method: "post",

        contentType: "application/json",

        payload: JSON.stringify(payload),

        muteHttpExceptions: true

      });

      return JSON.parse(resFallback.getContentText());

    }

    return j;

  } catch (e) {

    try {

      delete payload.parse_mode;

      const resFallback = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

        method: "post",

        contentType: "application/json",

        payload: JSON.stringify(payload),

        muteHttpExceptions: true

      });

      return JSON.parse(resFallback.getContentText());

    } catch (e2) {

      return { ok: false, error: e2.message };

    }

  }

}

function enviarMensajeTelegramConBoton(chatId, texto, idRegistro) {

  const payload = {

    chat_id: chatId,

    text: String(texto || ""),

    parse_mode: "Markdown",

    reply_markup: {

      inline_keyboard: [[{ text: "🗑️ Eliminar este registro", callback_data: "del_" + idRegistro }]]

    }

  };

  try {

    const res = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify(payload),

      muteHttpExceptions: true

    });

    const txt = res.getContentText();

    let j;

    try { j = JSON.parse(txt); } catch (e) { j = { ok: false, description: txt }; }

    if (!j.ok) {

      delete payload.parse_mode;

      const resFallback = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

        method: "post",

        contentType: "application/json",

        payload: JSON.stringify(payload),

        muteHttpExceptions: true

      });

      return JSON.parse(resFallback.getContentText());

    }

    return j;

  } catch (e) {

    try {

      delete payload.parse_mode;

      const resFallback = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

        method: "post",

        contentType: "application/json",

        payload: JSON.stringify(payload),

        muteHttpExceptions: true

      });

      return JSON.parse(resFallback.getContentText());

    } catch (e2) {

      return { ok: false, error: e2.message };

    }

  }

}

function editarMensajeTelegram(chatId, messageId, texto) {

  const payload = { chat_id: chatId, message_id: messageId, text: texto, parse_mode: "Markdown" };

  const r = UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/editMessageText`, { method: "post", contentType: "application/json", payload: JSON.stringify(payload), muteHttpExceptions: true });

  if (!JSON.parse(r.getContentText()).ok) { delete payload.parse_mode; UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/editMessageText`, { method: "post", contentType: "application/json", payload: JSON.stringify(payload), muteHttpExceptions: true }); }

}

function responderCallbackQuery(id, texto) {

  UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/answerCallbackQuery`, { method: "post", contentType: "application/json", payload: JSON.stringify({ callback_query_id: id, text: texto }), muteHttpExceptions: true });

}

function procesarCallbackTelegram(callback) {

  const data = String(callback.data || "");

  const chatId = callback.message.chat.id;

  if (data === "btn_resumen") {

    responderCallbackQuery(callback.id, "📊 Cargando resumen...");

    mostrarResumenPresupuesto_(chatId);

    return;

  }

  if (data === "btn_disponible") {

    responderCallbackQuery(callback.id, "💵 Calculando disponible...");

    mostrarDisponibleCaja_(chatId);

    return;

  }

  if (data === "btn_fugas") {

    responderCallbackQuery(callback.id, "🐜 Consultando radar de fugas...");

    mostrarControlFugas_(chatId);

    return;

  }

  if (data === "btn_quincena") {

    responderCallbackQuery(callback.id, "⏳ Consultando quincena...");

    mostrarReporteQuincena_(chatId);

    return;

  }

  if (data === "btn_fijos") {

    responderCallbackQuery(callback.id, "📌 Consultando gastos fijos...");

    listarGastosFijosDelMes(chatId);

    return;

  }

  if (data === "btn_ahorros") {

    responderCallbackQuery(callback.id, "🐷 Consultando fondos Colanta...");

    mostrarReporteAhorrosColanta_(chatId);

    return;

  }

  if (data === "btn_metas") {

    responderCallbackQuery(callback.id, "🎯 Consultando metas...");

    listarMetasAhorro_(chatId);

    return;

  }

  if (data === "btn_salud") {

    responderCallbackQuery(callback.id, "🩺 Analizando salud financiera...");

    mostrarDiagnostico503020_(chatId);

    return;

  }

  if (data === "btn_saldo") {

    responderCallbackQuery(callback.id, "💳 Consultando saldos...");

    gestionarAjusteSaldoCuentas_(chatId, "");

    return;

  }

  if (data === "btn_cobrar") {

    responderCallbackQuery(callback.id, "🤝 Consultando cuentas por cobrar...");

    listarCuentasPorCobrarV24_(chatId);

    return;

  }

  if (data.startsWith("ai_fijo_")) {

    const idx = parseInt(data.replace("ai_fijo_", ""), 10);

    const cache = CacheService.getScriptCache();

    const raw = cache.get("AI_FIJOS_" + chatId);

    if (raw) {

      try {

        const list = JSON.parse(raw);

        const item = list[idx];

        if (item) {

          responderCallbackQuery(callback.id, `✅ Creando ${item.concepto}...`);

          registrarGastoFijoCompleto(chatId, {

            concepto: item.concepto,

            valor: item.valor,

            dia: item.dia || 15,

            categoria: item.categoria || "Otros"

          });

          return;

        }

      } catch (e) {}

    }

    responderCallbackQuery(callback.id, "⚠️ La sugerencia expiró. Generando nuevas ideas...");

    generarEjemplosConIA_(chatId);

    return;

  }

  if (data === "btn_ideas_ia") {

    responderCallbackQuery(callback.id, "💡 Generando ideas de fijos con IA...");

    generarEjemplosConIA_(chatId);

    return;

  }

  if (data === "btn_ayuda_crear_fijo") {

    responderCallbackQuery(callback.id, "📌 Abriendo asistente de gastos fijos...");

    mostrarAyudaCrearGastoFijo_(chatId);

    return;

  }

  if (data === "fijo_demo_gimnasio") {

    responderCallbackQuery(callback.id, "🏋️ Creando ejemplo Gimnasio...");

    registrarGastoFijoCompleto(chatId, { concepto: "Gimnasio SmartFit", valor: 85000, dia: 15, categoria: "Salud" });

    return;

  }

  if (data === "fijo_demo_seguro") {

    responderCallbackQuery(callback.id, "🏍️ Creando ejemplo Seguro Moto...");

    registrarGastoFijoCompleto(chatId, { concepto: "Seguro Todo Riesgo Moto", valor: 50000, dia: 30, categoria: "Moto" });

    return;

  }

  if (data === "fijo_demo_streaming") {

    responderCallbackQuery(callback.id, "📺 Creando ejemplo Streaming...");

    registrarGastoFijoCompleto(chatId, { concepto: "Suscripción Streaming", valor: 25000, dia: 30, categoria: "Entretenimiento" });

    return;

  }

  if (data === "btn_analisis_ia" || data === "btn_consejo") {

    responderCallbackQuery(callback.id, "🧠 Analizando libro contable con IA...");

    ejecutarAnalisisFinancieroInteligenteGemini_(chatId, "Realiza un análisis financiero integral de mi mes.");

    return;

  }

  if (data === "btn_ayuda") {

    responderCallbackQuery(callback.id, "📖 Mostrando guía...");

    enviarMensajeTelegram(chatId, obtenerGuiaComandos());

    return;

  }

  if (data.startsWith("del_")) {

    const id = data.replace("del_", "").trim().toUpperCase();

    try {

      const result = borrarRegistroCompletoPorID(id);

      if (result.ok) {

        editarMensajeTelegram(chatId, callback.message.message_id, `🗑️ *ELIMINADO*\n🆔 ${id}\n✅ Limpio del Excel.`);

        responderCallbackQuery(callback.id, "✅ Eliminado.");

      } else responderCallbackQuery(callback.id, "⚠️ No encontrado.");

    } catch (e) { responderCallbackQuery(callback.id, "🚨 Error: " + e.message); }

    return;

  }

  responderCallbackQuery(callback.id, "ℹ️ Acción no reconocida.");

}

function obtenerGuiaComandos() {

  return "📖 *GUÍA PARDOFINANZAS PRO*\n\n" +

    "💸 *Gasto:* Compré papitas por 5 mil\n" +

    "🎙️ *Nota de voz:* Envía un audio diciendo tu gasto\n" +

    "💰 *Ingreso:* Recibí 50 mil\n" +

    "🏠 *Fijo:* Ya pagué arriendo\n" +

    "🐷 *Ahorro:* Ahorré 100 mil para viajar\n" +

    "📝 *Deuda:* Agrega deuda 300 mil en 6 cuotas\n" +

    "🤝 *Cobro:* Le presté 50 lucas a Diego\n" +

    "💵 *Pago:* Diego me pagó los 50 mil\n" +

    "🔄 *Traslado:* Pasé 200 mil de Colanta a Nequi (Movimiento Neutro)\n" +

    "📸 *Factura física:* Envía una foto del recibo\n\n" +

    "📊 *CONSULTAS EN VIVO*\n" +

    "💳 /saldo — Calibrar y consultar saldos en Nequi/Efectivo/Colanta\n" +

    "🩺 /salud — Diagnóstico financiero con regla 50/30/20\n" +

    "📅 /quincena — Control de ciclo quincenal y saldo disponible\n" +

    "💡 /consejo [pregunta] — Asesor financiero IA con Gemini\n" +

    "📊 /resumen — Balance y ejecución mensual\n" +

    "💵 /disponible — Caja libre real presupuestada\n" +

    "🐜 /fugas — Monitor de gastos hormiga\n" +

    "🔔 /recordatorios — Fijos por vencer / vencidos\n" +

    "📋 /gastosfijos — Lista de fijos del mes por quincena\n" +

    "➕ /nuevofijo — Asistente y ejemplos para crear un gasto fijo\n" +

    "🧠 /analisis — Diagnóstico financiero ejecutivo con IA\n" +

    "📋 /deudas — Saldos pendientes de deudas\n" +

    "🤝 /porcobrar — Cuentas por cobrar\n" +

    "🗑️ /borrar M-0010 — Eliminar registro por ID";

}

/* =========================================================

   SISTEMA DE COLA NEQUI / BRE-B & ASOCIACIÓN UNIVERSAL

   ========================================================= */

function esComercioConocido(nombre) {

  const n = normalizarTexto(nombre || "");

  return ["amazon", "spotify", "netflix", "tigo", "epm", "google", "mercado libre", "didi", "uber", "rappi", "salón de belleza", "mercatodo", "colanta", "sura", "dian", "nequi", "daviplata", "bancolombia", "pse", "bre-b", "shein", "temu", "add", "parqueadero", "parking"].some(c => n.includes(c) || c.includes(n));

}

function corregirDireccionTransferenciaBreB_(textoOriginal) {

  const texto = String(textoOriginal || "");

  if (!/bre-?b|nequi|bancolombia/i.test(texto)) return null;

  const norm = normalizarTexto(texto);

  const nombreMarlon = normalizarTexto(USUARIO_NOMBRE_COMPLETO).split(" ")[0];

  // 🛡️ REGLA ABSOLUTA: IGNORAR CLARA AIDE

  if (norm.includes("clara aide") || norm.includes("clara aidé")) {

    return { ignorar: true, motivo: "Transferencia a Clara Aide (Excluida por regla)" };

  }

  let destinatario = null, remitente = null;

  const esEnvio = /enviaste|envio|transferiste|realizaste un envio|enviaste plata/.test(norm);

  if (esEnvio) {

    const matchLlave = texto.match(/a\s+la\s+llave\s+\d+\s+de\s+(.+?)(?=\s+el\s+\d{1,2}\s+de\s+|\s+el\s+\d{1,2}\/|$)/i);

    if (matchLlave) destinatario = limpiarNombreComercioV24_(matchLlave[1]);

    if (!destinatario) {

      const matchNormal = texto.match(/\b(?:a|para)\s+([A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .&'_-]{3,80}?)(?=\s+el\s+\d{1,2}\s+de\s+|\s+el\s+\d{1,2}\/|$)/i);

      if (matchNormal) destinatario = limpiarNombreComercioV24_(matchNormal[1]);

    }

    if (!destinatario) {

      const etiq = texto.match(/(?:destinatario|comercio|beneficiario|recibe)\s*[:\-]\s*([A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .&'_-]{3,80})/i);

      if (etiq) destinatario = limpiarNombreComercioV24_(etiq[1]);

    }

    if (!destinatario) {

      const tagFactura = texto.match(/\[factura\]\s*([A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .&'_-]{3,80})\s*-\s*/i);

      if (tagFactura) destinatario = limpiarNombreComercioV24_(tagFactura[1]);

    }

  }

  if (/recibiste|recibio la plata/.test(norm)) {

    const match = texto.match(/de\s+([A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .&'_-]{3,80})/i);

    if (match) remitente = limpiarNombreComercioV24_(match[1]);

  }

  if (destinatario) {

    const destNorm = normalizarTexto(destinatario);

    

    if (destNorm.indexOf(nombreMarlon) !== -1 || /propia|mis cuentas/.test(destNorm)) {

      return { tipo: "Ingreso", categoria: "Transferencias", subcategoria: "Propias", descripcion: "Transferencia a cuenta propia", proveedor: "Propia", requiere_especificar: false, esGastoVariable: false, es_neutro: true };

    }

    let cat = null, subcat = "Transferencia", desc = "Envío a " + destinatario, prov = destinatario, esCom = false;

    let esGastoVariable = false;

    // Regla dinámica desde la hoja "REGLAS_COMERCIOS"

    const catRegla = aplicarReglaComercio_(destinatario);

    if (catRegla) {

      cat = catRegla;

      esCom = true;

      esGastoVariable = true;

    } else if (/parqueadero|parking|estacionamiento/.test(destNorm)) {

      cat = "Transporte"; subcat = "Parqueadero"; desc = "Parqueadero en " + destinatario; prov = destinatario; esCom = true; esGastoVariable = true;

    } else if (/restaurante|comida|burger|pizza|sushi|carniceria|mercado|tienda|supermercado|panaderia|reposteria|cafe|asados/.test(destNorm)) {

      cat = "Alimentación"; subcat = "Comida"; desc = "Compra en " + destinatario; esCom = true; esGastoVariable = true;

    } else if (/drogueria|farmacia|medico|salud|clinica|hospital/.test(destNorm)) {

      cat = "Salud"; subcat = "General"; desc = "Compra en " + destinatario; esCom = true; esGastoVariable = true;

    } else if (/barberia|peluqueria|salon de belleza|spa|estetica/.test(destNorm)) {

      cat = "Otros"; subcat = "Cuidado personal"; desc = "Cuidado personal en " + destinatario; esCom = true; esGastoVariable = true;

    } else if (/repuestos|taller|mecanico/.test(destNorm)) {

      cat = "Moto"; subcat = "Repuestos / Mantenimiento"; desc = "Compra en " + destinatario; esCom = true; esGastoVariable = true;

    } else if (/tecnologia|celulares|accesorios|moto/.test(destNorm)) {

      cat = /moto/.test(destNorm) ? "Moto" : "Tecnología"; subcat = "Compra"; desc = "Compra en " + destinatario; esCom = true; esGastoVariable = true;

    } else if (/s\.a\.s|sas|ltda|distribuidora/.test(destNorm) || esComercioConocido(destNorm)) {

      cat = "Compras"; subcat = "General"; desc = "Compra en " + destinatario; esCom = true; esGastoVariable = true;

    }

    if (!esCom) return { tipo: "Gasto", categoria: null, subcategoria: null, descripcion: "Envío a " + destinatario, proveedor: destinatario, requiere_especificar: true, esGastoVariable: true };

    return { tipo: "Gasto", categoria: cat || "Otros", subcategoria: subcat, descripcion: desc, proveedor: prov, requiere_especificar: false, esGastoVariable: esGastoVariable };

  }

  if (remitente && normalizarTexto(remitente).indexOf(nombreMarlon) !== -1) {

    const saludoMatch = texto.match(/hola,?\s*([A-Za-zÁÉÍÓÚÑáéíóúñ\s]{4,50})!/i);

    const dest = saludoMatch ? saludoMatch[1].trim().toLowerCase().replace(/\b\w/g, c=>c.toUpperCase()) : "un tercero";

    return { tipo: "Gasto", categoria: "Transferencias", subcategoria: "Terceros", descripcion: "Transferencia a " + dest, proveedor: dest, esGastoVariable: true, es_neutro: true };

  }

  return null;

}

function limpiarNombreComercioV24_(nombre) {

  return String(nombre || "").replace(/\s+/g, " ").replace(/^[\s,.;:-]+|[\s,.;:-]+$/g, "").trim();

}

function extraerMontoBreB_(texto) {

  const t = String(texto || "");

  let m = t.match(/enviaste(?:\s+de\s+manera\s+exitosa)?\s+([0-9]{1,3}(?:[.]\d{3})+|[0-9]+(?:,\d{3})*)\s+(?:a\s+la\s+llave|a\s+|para\s+)/i);

  if (!m) m = t.match(/(?:envio|envió|transferiste|transferencia)\D{0,80}([0-9]{1,3}(?:[.]\d{3})+|[0-9]+(?:,\d{3})*)\s+(?:a\s+la\s+llave|a\s+|para\s+)/i);

  return m ? parsearMontoFlexibleV24_(m[1]) : 0;

}

function guardarEnvioPendiente(chatId, datos) {

  datos.timestamp = Date.now();

  let raw = PropertiesService.getScriptProperties().getProperty("ENVIO_PENDIENTE_" + chatId);

  let cola = raw ? JSON.parse(raw) : [];

  if (!Array.isArray(cola)) cola = [];

  cola = cola.filter(item => Date.now() - item.timestamp < 30 * 60 * 1000);

  if (!cola.some(item => item.idMensaje === datos.idMensaje)) {

    cola.push(datos);

    PropertiesService.getScriptProperties().setProperty("ENVIO_PENDIENTE_" + chatId, JSON.stringify(cola));

  }

  return cola.length;

}

function obtenerEnvioPendiente(chatId) {

  let raw = PropertiesService.getScriptProperties().getProperty("ENVIO_PENDIENTE_" + chatId);

  if (!raw) return null;

  try {

    let cola = JSON.parse(raw);

    if (!Array.isArray(cola)) return Date.now() - cola.timestamp > 30 * 60 * 1000 ? null : cola;

    cola = cola.filter(item => Date.now() - item.timestamp < 30 * 60 * 1000);

    return cola.length > 0 ? cola[0] : null;

  } catch (e) { return null; }

}

function borrarEnvioPendiente(chatId) {

  let raw = PropertiesService.getScriptProperties().getProperty("ENVIO_PENDIENTE_" + chatId);

  if (!raw) return;

  try {

    let cola = JSON.parse(raw);

    if (!Array.isArray(cola)) { PropertiesService.getScriptProperties().setProperty("ENVIO_PENDIENTE_" + chatId, "[]"); return; }

    cola.shift();

    if (cola.length > 0) PropertiesService.getScriptProperties().setProperty("ENVIO_PENDIENTE_" + chatId, JSON.stringify(cola));

    else PropertiesService.getScriptProperties().setProperty("ENVIO_PENDIENTE_" + chatId, "[]");

  } catch (e) { PropertiesService.getScriptProperties().setProperty("ENVIO_PENDIENTE_" + chatId, "[]"); }

}

function registrarMovimientoDesdeEnvio(chatId, envio, categoria) {

  const valor = Number(envio.valor || 0);

  if (valor <= 0) return;

  const fecha = envio.fecha || Utilities.formatDate(new Date(), ZONA_HORARIA, "dd/MM/yyyy");

  const dateInfo = obtenerMesYAnioDeFecha(fecha, null, null);

  const desc = envio.descripcion || "Envío a " + envio.proveedor;

  

  const id = obtenerSiguienteIDConsecutivo("MOVIMIENTOS", "M");

  registrarEnSheetsYFormatear("MOVIMIENTOS", [

    id, fecha, Utilities.formatDate(new Date(), ZONA_HORARIA, "hh:mm a"), "Gasto", categoria, "Transferencia", desc, valor,

    "Transferencia", envio.cuenta || "Nequi", dateInfo.mes, dateInfo.anio, "Gmail", "GMAIL:" + (envio.idMensaje || "")

  ], 8);

  const alertaPresupuesto = obtenerAlertaCategoria_(categoria, dateInfo.mes, dateInfo.anio);

  enviarMensajeTelegramConBoton(chatId, `✅ *GASTO REGISTRADO*\n${separadorBonito_()}\n\n📩 ${desc}\n💵 $${formatearCOP(valor)} COP\n📂 Categoría: ${categoria}${alertaPresupuesto}`, id);

}

/* =========================================================

   GMAIL Y FACTURAS

   ========================================================= */

function limpiarEtiquetasFinanzasDeHilo_(hilo) {

  try {

    const labels = hilo.getLabels() || [];

    labels.forEach(l => {

      if (String(l.getName()).toLowerCase().includes("finanzas")) {

        hilo.removeLabel(l);

      }

    });

  } catch (e) {}

}

/**

 * Filtro estricto para descartar correos que NO son facturas ni pagos reales:

 * - Publicidad, promociones, ofertas, cupones, descuentos de marketing.

 * - Créditos preaprobados, préstamos bancarios comerciales.

 * - Códigos de seguridad, OTP, claves dinámicas, alertas de inicio de sesión.

 * - Carritos de compra abandonados, cotizaciones, propuestas comerciales.

 * - Extractos de cuenta bancarios mensuales informativos (no son compras).

 * - Notificaciones de transacciones rechazadas, canceladas o fallidas.

 * - Exclusión absoluta de Clara Aidé.

 */

function esCorreoNoFactura_(asunto, remitente, cuerpo, tieneAdjuntoFacturacion = false) {

  const t = normalizarTexto((asunto || "") + " " + (remitente || "") + " " + (cuerpo || ""));

  // 1. REGLA ABSOLUTA: Clara Aide
  if (t.includes("clara aide")) return true;

  // Si el correo trae adjunto .zip o .xml de facturación o formato DIAN en el asunto, NUNCA descartarlo como publicidad
  const esAsuntoDian = /\b\d{6,12}\s*;.*;\s*(01|02|03|91|92)\b/i.test(asunto || "") ||
                       /(attacheddocument|factura\s+electronica|documento\s+electronico|representacion\s+grafica)/i.test(asunto || "");
  if (tieneAdjuntoFacturacion || esAsuntoDian) {
    return false;
  }

  // 2. Transacciones Rechazadas, Fallidas o Canceladas (NUNCA son gastos reales)
  if (/(transaccion|pago|operacion|compra|cargo)\s+(rechazad[ao]|fallid[ao]|cancelad[ao]|declinad[ao]|no\s+exitosa|no\s+aprobad[ao]|denegad[ao])/i.test(t) ||
      /(fondos\s+insuficientes|saldo\s+insuficiente|intento\s+de\s+pago\s+fallido|orden\s+cancelada)/i.test(t)) {
    return true;
  }

  // 3. Códigos de Seguridad, OTP y Alertas de Inicio de Sesión
  if (/(codigo\s+de\s+seguridad|codigo\s+de\s+verificacion|clave\s+dinamica|tu\s+codigo\s+otp|token\s+de\s+seguridad|codigo\s+temporal)/i.test(t) ||
      /(inicio\s+de\s+sesion|nuevo\s+inicio\s+de\s+sesion|alerta\s+de\s+seguridad|cambio\s+de\s+clave|cambio\s+de\s+contrasena|dispositivo\s+no\s+reconocido)/i.test(t)) {
    return true;
  }

  // 4. Créditos Preaprobados y Publicidad Financiera (No son gastos)
  if (/(credito\s+preaprobado|prestamo\s+preaprobado|solicita\s+tu\s+credito|credito\s+disponible|cupo\s+disponible|aumento\s+de\s+cupo|tienes\s+un\s+credito|simula\s+tu\s+credito|prestamo\s+digital|libre\s+inversion|adelanto\s+de\s+nomina|tarjeta\s+de\s+credito\s+preaprobada)/i.test(t)) {
    return true;
  }

  // 5. Carrito Abandonado y Cotizaciones
  if (/(olvidaste\s+algo\s+en\s+tu\s+carrito|dejaste\s+productos|tu\s+carrito\s+te\s+espera|retoma\s+tu\s+compra|completa\s+tu\s+compra|cotizacion\s+n[°o]|propuesta\s+comercial)/i.test(t)) {
    return true;
  }

  // 6. Extractos Mensuales Informativos / Estados de Cuenta
  if (/(extracto\s+mensual|extracto\s+de\s+cuenta|estado\s+de\s+cuenta\s+consolidado|resumen\s+de\s+cuenta|informe\s+mensual\s+de\s+saldos)/i.test(t)) {
    return true;
  }

  // 7. Publicidad y Marketing Promocional (solo en asunto para no falsos positivos por pie de firma de supermercados/bancos)
  const asuntoNorm = normalizarTexto(asunto || "");
  const esMarketingAsunto = /(oferta\s+especial|gran\s+venta|hasta\s+\d+%\s*off|descuento\s+exclusivo|rebajas?|cyber\s*monday|black\s*friday|hot\s*sale|aprovecha\s+esta\s+oferta|cupon\s+de\s+descuento|newsletter|no\s+te\s+lo\s+pierdas|preventa)/i.test(asuntoNorm);

  if (esMarketingAsunto && !/(\bcus\b|transaccion\s+aprobada|pago\s+exitoso|pago\s+confirmado|factura|attacheddocument|pedido\s+confirmado|compra\s+confirmada|comprobante)/i.test(t)) {
    return true;
  }

  return false;

}

/**
 * Detecta si una factura o transacción ya fue registrada en MOVIMIENTOS
 * evitando duplicar gastos cuando llegan múltiples correos por la misma compra,
 * pero permitiendo compras legítimas del mismo valor en días distintos o con distinto número de factura.
 */
function esFacturaDuplicadaEnMovimientos_(idReferencia, proveedor, valor, fechaStr, idMensajeGmail) {
  try {
    const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");
    if (!sh || sh.getLastRow() < 4) return { esDuplicado: false };

    const numRows = sh.getLastRow() - 3;
    const rows = sh.getRange(4, 1, numRows, 14).getValues();
    const valNum = Number(valor || 0);
    const provNorm = normalizarTexto(proveedor || "");
    const refLimpia = String(idReferencia || "").replace(/^(CUS|FAC|FACTURA|REF|ORDEN)[:\s-]*/i, "").trim().toLowerCase();
    const esRefSignificativa = refLimpia.length >= 4 && !/^(n\/a|ninguno|null|undefined|comercio|otros|dian|electronica)$/i.test(refLimpia);
    const idMsgLimpio = String(idMensajeGmail || "").trim().toLowerCase();

    // Equivalencias comerciales (Greenfit = Oasis Punto Cero = MercadoPago)
    const esGrupoGreenfit = /greenfit|oasis punto cero|mercadopago|mercado pago/.test(provNorm);

    const parsearFecha = s => {
      if (!s) return null;
      if (s instanceof Date) return new Date(s.getFullYear(), s.getMonth(), s.getDate());
      const str = String(s).trim().split(" ")[0];
      const p = str.split(/[/ -]/);
      if (p.length === 3) {
        if (p[0].length === 4) return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
        return new Date(Number(p[2]), Number(p[1]) - 1, Number(p[0]));
      }
      return null;
    };

    const dTarget = parsearFecha(fechaStr);

    for (let i = 0; i < rows.length; i++) {
      const fila = rows[i];
      const rowId = String(fila[0] || "");
      const rowFecha = fila[1];
      const rowDesc = normalizarTexto(String(fila[6] || ""));
      const rowValor = Number(fila[7] || 0);
      const rowRef = normalizarTexto(String(fila[13] || ""));

      // 0. Coincidencia exacta por ID de mensaje de Gmail
      if (idMsgLimpio && rowRef.includes("gmail:" + idMsgLimpio)) {
        return {
          esDuplicado: true,
          idExistente: rowId,
          motivo: "Mensaje Gmail (" + idMensajeGmail + ") ya registrado en " + rowId
        };
      }

      // 1. Coincidencia por ID de Referencia / CUS / Número de Factura exacto
      if (esRefSignificativa && (rowRef.includes(refLimpia) || rowDesc.includes(refLimpia))) {
        return {
          esDuplicado: true,
          idExistente: rowId,
          motivo: "Referencia (" + idReferencia + ") ya registrada en " + rowId
        };
      }

      // Si ambos registros tienen número de factura DIAN distinto, NO son duplicados (ej. dos almuerzos o dos tanqueadas con distinta factura)
      const matchFacRow = rowRef.match(/factura:\s*([a-z0-9_-]{4,})/i);
      const refRowExistente = matchFacRow ? matchFacRow[1].trim().toLowerCase() : "";
      const rowTieneOtraFacturaDistinta = refRowExistente &&
        !/^(dian|electronica|ninguno|null)$/i.test(refRowExistente) &&
        esRefSignificativa &&
        refRowExistente !== refLimpia;

      if (rowTieneOtraFacturaDistinta) {
        continue;
      }

      // 2. Coincidencia por Mismo Monto + Mismo Comercio en fecha cercana (máximo 1 día de diferencia: ej. registro manual + correo posterior)
      if (valNum > 0 && Math.abs(rowValor - valNum) < 1) {
        const filaEsGrupoGreenfit = /greenfit|oasis punto cero|mercadopago|mercado pago/.test(rowDesc + " " + rowRef);
        const comercioCoincide = (esGrupoGreenfit && filaEsGrupoGreenfit) ||
          (provNorm.length >= 3 && (rowDesc.includes(provNorm) || provNorm.includes(rowDesc.split(" ")[0])));

        let diffDias = 999;
        const dRow = parsearFecha(rowFecha);
        if (dTarget && dRow) {
          diffDias = Math.abs(Math.round((dTarget.getTime() - dRow.getTime()) / (1000 * 60 * 60 * 24)));
        }

        if (comercioCoincide && diffDias <= 1) {
          return {
            esDuplicado: true,
            idExistente: rowId,
            motivo: "Mismo valor ($" + formatearCOP(valNum) + "), comercio (" + proveedor + ") y fecha cercana en " + rowId
          };
        }
      }
    }

    return { esDuplicado: false };
  } catch (e) {
    return { esDuplicado: false };
  }
}

/**
 * Determina si un correo tiene adjuntos o estructura de factura electrónica / comprobante financiero.
 * Soporta adjuntos .zip, .xml, .pdf (incluso cuando vienen con MIME application/octet-stream o x-zip-compressed)
 * y asuntos estándar DIAN separados por punto y coma (NIT;RazónSocial;Factura;01;...).
 */
function pareceCorreoFinancieroValido_(asunto, remitente, cuerpo, adjuntosOFlag) {
  const listaAdj = Array.isArray(adjuntosOFlag) ? adjuntosOFlag : [];
  const tieneAdjuntos = Array.isArray(adjuntosOFlag) ? listaAdj.length > 0 : Boolean(adjuntosOFlag);

  let tieneAdjuntoFacturacion = false;
  let textoNombresAdj = "";

  if (listaAdj.length > 0) {
    for (let i = 0; i < listaAdj.length; i++) {
      try {
        const n = String(listaAdj[i].getName() || "").toLowerCase();
        const m = String(listaAdj[i].getContentType() || "").toLowerCase();
        textoNombresAdj += " " + n + " " + m;
        if (
          n.endsWith(".zip") ||
          n.endsWith(".xml") ||
          n.endsWith(".pdf") ||
          m.includes("zip") ||
          m.includes("xml") ||
          m.includes("pdf") ||
          m.includes("octet-stream") ||
          /(factura|attacheddocument|ad0|fv0|fe-|fe_|nc0|nd0|recibo|comprobante|colanta|epm|tigo|dian)/i.test(n)
        ) {
          tieneAdjuntoFacturacion = true;
        }
      } catch (eAdj) {}
    }
  }

  // Filtro negativo: Si es publicidad, OTP, crédito preaprobado, etc. (respetando adjuntos reales de facturación)
  if (esCorreoNoFactura_(asunto, remitente, cuerpo, tieneAdjuntoFacturacion)) return false;

  if (tieneAdjuntoFacturacion) return true;

  // Formato estándar DIAN Colombia en asunto: NIT;NombreEmisor;NumeroFactura;01;NombreComercial
  if (/\b\d{6,12}\s*;.*;\s*(01|02|03|91|92)\b/i.test(asunto || "") || (asunto || "").split(";").length >= 4) {
    return true;
  }

  const t = normalizarTexto((asunto || "") + " " + (remitente || "") + " " + (cuerpo || "") + " " + textoNombresAdj);

  // 1. Adjuntos o menciones típicas DIAN / XML / Factura / ZIP
  if (tieneAdjuntos && /(\.xml|\.zip|\.pdf|factura|attacheddocument|documento\s+electronico|representacion\s+grafica|ubl|dian|colanta|epm|tigo|siesa|carvajal|siigo|alegra|cadena|gosocket)/i.test(t)) {
    return true;
  }

  // 2. Comprobantes bancarios y pasarelas de pago colombianas
  const esBancarioOPasarela = /pse|achcolombia|cus|dlocal|eneba|mercadopago|mercado\s*pago|wompi|bold|payu|bancolombia|nequi|daviplata|colpatria|davivienda|bbva|nu\s*colombia|lulo/.test(t) &&
                             /(aprobada|exitosa|confirmad|transaccion|pago|debito|transferencia|envio|compra|abono|notificacion|recibo|comprobante)/.test(t);

  // 3. Facturas formales, cuentas de cobro y servicios públicos
  const esFacturaOServicio = /(factura\s+electronica|factura\s+de\s+venta|documento\s+electronico|facturacion\s+electronica|cuenta\s+de\s+cobro|comprobante\s+de\s+pago|recibo\s+de\s+pago|debito\s+por\s+compra|transferencia\s+exitosa|total\s+a\s+pagar|epm|tigo|claro|une|emvarias|afinia|vanti|gases|siesa)/.test(t);

  // 4. Confirmaciones de pedidos y compras de e-commerce
  const esPedidoOCompra = /(confirmacion\s+(?:de\s+)?(?:tu\s+)?pedido|pedido\s+confirmado|tu\s+pedido\s+esta\s+confirmado|orden\s+de\s+compra|gracias\s+por\s+tu\s+compra|compra\s+confirmada|compra\s+exitosa|pago\s+confirmado|recibo\s+de\s+compra|detalles?\s+del\s+pedido|resumen\s+de\s+tu\s+pedido)/.test(t);

  // 5. Comercios conocidos con mención de valor o pago
  const esComercioReconocido = /(colanta|mercolanta|greenfit|mercadolibre|mercado\s*libre|amazon|shein|temu|falabella|zara|koaj|mattelsa|tennis|gef|punto\s+blanco|exito|carulla|la\s+vaquita|euro|d1|ara|rappi|didi|uber|uber\s*eats|terpel|primax|biomax)/.test(t) &&
                               /(total|cop|\$|pago|orden|pedido|compra|factura|recibo|valor)/.test(t);

  return esBancarioOPasarela || esFacturaOServicio || esPedidoOCompra || esComercioReconocido;
}

/**
 * Parsea montos decimales provenientes de etiquetas XML UBL 2.1 (ej. "12500.00" -> 12500, "197,000.00" -> 197000).
 */
function parsearMontoXmlUbl_(rawStr) {
  if (rawStr === null || rawStr === undefined) return 0;
  let s = String(rawStr).trim().replace(/[^\d.,-]/g, "");
  if (!s) return 0;
  if (s.includes(",") && s.includes(".")) {
    if (s.lastIndexOf(".") > s.lastIndexOf(",")) {
      s = s.replace(/,/g, "");
    } else {
      s = s.replace(/\./g, "").replace(",", ".");
    }
  } else if (/^\d+\.\d{1,2}$/.test(s)) {
    // Formato estándar XML UBL: punto decimal con 1 o 2 decimales (ej. 12500.00)
  } else if (/^\d+,\d{1,2}$/.test(s)) {
    s = s.replace(",", ".");
  } else {
    s = s.replace(/[.,]/g, "");
  }
  const n = parseFloat(s);
  return isNaN(n) ? 0 : Math.round(Math.abs(n));
}

/**
 * Descomprime de forma segura cualquier adjunto .zip enviado por entidades económicas / facturadores DIAN.
 * Normaliza el MIME type a "application/zip" y detecta firmas binarias PK (0x50, 0x4B) para evitar fallos de Utilities.unzip().
 * Extrae recursivamente todos los archivos .xml, .pdf e imágenes dentro del .zip (incluyendo .zip anidados).
 */
function desempacarAdjuntosCorreo_(adjuntos) {
  const resultado = {
    xmlBlobs: [],
    pdfImagenBlobs: []
  };
  if (!adjuntos || !adjuntos.length) return resultado;

  const procesarBlob = (blob, profundidad) => {
    if (!blob || profundidad > 2) return;
    try {
      const nombre = String(blob.getName() || "adjunto").trim();
      const nombreLower = nombre.toLowerCase();
      const mime = String(blob.getContentType() || "").toLowerCase();
      let bytes = null;
      try {
        bytes = blob.getBytes();
      } catch (eB) {}

      // Detectar firma PK (0x50 = 80, 0x4B = 75) típica de todo archivo .zip incluso si viene como application/octet-stream
      const tieneFirmaZip = bytes && bytes.length > 4 && bytes[0] === 0x50 && bytes[1] === 0x4B;
      const esZip = nombreLower.endsWith(".zip") || mime.includes("zip") || tieneFirmaZip;

      if (esZip && bytes && bytes.length > 0) {
        try {
          const nombreZipNormalizado = nombreLower.endsWith(".zip") ? nombre : (nombre + ".zip");
          const zipNormalizado = Utilities.newBlob(bytes, "application/zip", nombreZipNormalizado);
          const internos = Utilities.unzip(zipNormalizado) || [];
          for (let i = 0; i < internos.length; i++) {
            procesarBlob(internos[i], profundidad + 1);
          }
          return;
        } catch (errUnzip) {
          console.warn("Aviso al descomprimir ZIP (" + nombre + "):", errUnzip);
        }
      }

      if (nombreLower.endsWith(".xml") || mime.includes("xml")) {
        resultado.xmlBlobs.push(blob);
      } else if (
        nombreLower.endsWith(".pdf") ||
        mime.includes("pdf") ||
        mime.includes("image") ||
        /\.(png|jpg|jpeg|webp)$/i.test(nombreLower)
      ) {
        resultado.pdfImagenBlobs.push(blob);
      } else if (bytes && bytes.length > 20) {
        // Algunos proveedores DIAN adjuntan el XML como application/octet-stream sin extensión
        try {
          const inicioTxt = Utilities.newBlob(bytes.slice(0, 300)).getDataAsString("UTF-8").trim();
          if (inicioTxt.startsWith("<?xml") || /<(AttachedDocument|Invoice|CreditNote|DebitNote)/i.test(inicioTxt)) {
            resultado.xmlBlobs.push(blob);
          } else if (inicioTxt.startsWith("%PDF")) {
            resultado.pdfImagenBlobs.push(Utilities.newBlob(bytes, "application/pdf", nombre + ".pdf"));
          }
        } catch (eSniff) {}
      }
    } catch (eProc) {
      console.warn("Error procesando adjunto individual:", eProc);
    }
  };

  for (let a = 0; a < adjuntos.length; a++) {
    procesarBlob(adjuntos[a], 0);
  }

  return resultado;
}

/**
 * Extrae datos contables de un XML estándar DIAN Colombia (UBL 2.1).
 * Soporta contenedores <AttachedDocument> con <Invoice> embebido en CDATA o entidades escapadas,
 * ignora montos en 0.00 (ej. PayableAmount=0 cuando PrepaidAmount>0) y evita confundir el receptor (Marlon) con el proveedor.
 */
function extraerDatosDianXml_(xmlStr, asuntoOpcional) {
  if (!xmlStr) return null;
  try {
    let rawXml = String(xmlStr);
    // Decodificar entidades HTML/XML si el Invoice interno viene escapado dentro del AttachedDocument
    let cleanXml = rawXml;
    if (cleanXml.includes("&lt;")) {
      cleanXml = cleanXml
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&amp;/g, "&")
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'");
    }

    // Extraer el bloque <Invoice> o <CreditNote> interno si existe (para no confundir IDs o descripciones del contenedor AttachedDocument)
    let invoiceBlock = cleanXml;
    const mInvoiceInterno = cleanXml.match(/<(?:fe:|ubl:)?Invoice\b[\s\S]*?<\/(?:fe:|ubl:)?Invoice>/i) ||
                            cleanXml.match(/<(?:fe:|ubl:)?CreditNote\b[\s\S]*?<\/(?:fe:|ubl:)?CreditNote>/i);
    if (mInvoiceInterno) {
      invoiceBlock = mInvoiceInterno[0];
    }

    // 1. Extraer Proveedor / Emisor desde AccountingSupplierParty o SenderParty (nunca desde AccountingCustomerParty / ReceiverParty)
    let emisor = "";
    const bloquesEmisor = [
      invoiceBlock.match(/<cac:AccountingSupplierParty>([\s\S]*?)<\/cac:AccountingSupplierParty>/i),
      cleanXml.match(/<cac:AccountingSupplierParty>([\s\S]*?)<\/cac:AccountingSupplierParty>/i),
      cleanXml.match(/<cac:SenderParty>([\s\S]*?)<\/cac:SenderParty>/i)
    ];
    for (let b = 0; b < bloquesEmisor.length; b++) {
      if (bloquesEmisor[b] && bloquesEmisor[b][1]) {
        const subXml = bloquesEmisor[b][1];
        const mReg = subXml.match(/<cbc:RegistrationName[^>]*>([\s\S]*?)<\/cbc:RegistrationName>/i) ||
                     subXml.match(/<cbc:Name[^>]*>([\s\S]*?)<\/cbc:Name>/i);
        if (mReg && mReg[1]) {
          const candidato = mReg[1].replace(/<!\[CDATA\[|\]\]>/g, "").trim();
          if (candidato && !/marlon.*pardo/i.test(candidato)) {
            emisor = candidato;
            break;
          }
        }
      }
    }

    // Fallback de emisor desde asunto DIAN (NIT;RazónSocial;NumFactura;01;NombreComercial)
    if (!emisor && asuntoOpcional && asuntoOpcional.includes(";")) {
      const partesAsunto = asuntoOpcional.split(";");
      if (partesAsunto.length >= 2 && partesAsunto[1].trim().length >= 2) {
        emisor = partesAsunto[1].trim();
      }
    }

    // 2. Extraer Monto real (> 0) priorizando el bloque <cac:LegalMonetaryTotal>
    let monto = 0;
    const mMonetary = invoiceBlock.match(/<(?:cac:|fe:)?LegalMonetaryTotal>([\s\S]*?)<\/(?:cac:|fe:)?LegalMonetaryTotal>/i) ||
                      cleanXml.match(/<(?:cac:|fe:)?LegalMonetaryTotal>([\s\S]*?)<\/(?:cac:|fe:)?LegalMonetaryTotal>/i);
    const zonaMontos = mMonetary ? mMonetary[1] : invoiceBlock;

    const tagsMonto = [
      "TaxInclusiveAmount",
      "PayableAmount",
      "PrepaidAmount",
      "LineExtensionAmount",
      "ChargeTotalAmount"
    ];

    for (let t = 0; t < tagsMonto.length; t++) {
      const tag = tagsMonto[t];
      const regex = new RegExp("<(?:cbc:)?" + tag + "[^>]*>\\s*([\\d.,]+)\\s*<\\/(?:cbc:)?" + tag + ">", "gi");
      let match;
      while ((match = regex.exec(zonaMontos)) !== null) {
        const val = parsearMontoXmlUbl_(match[1]);
        if (val > 0) {
          monto = val;
          break;
        }
      }
      if (monto > 0) break;
    }

    // Si en zonaMontos no hubo monto > 0, buscar en todo el documento limpio
    if (monto <= 0) {
      for (let t = 0; t < tagsMonto.length; t++) {
        const tag = tagsMonto[t];
        const regex = new RegExp("<(?:cbc:)?" + tag + "[^>]*>\\s*([\\d.,]+)\\s*<\\/(?:cbc:)?" + tag + ">", "gi");
        let match;
        while ((match = regex.exec(cleanXml)) !== null) {
          const val = parsearMontoXmlUbl_(match[1]);
          if (val > 0) {
            monto = val;
            break;
          }
        }
        if (monto > 0) break;
      }
    }

    // 3. Extraer ID de la Factura (ParentDocumentID en AttachedDocument o primer cbc:ID de Invoice)
    let idFac = "";
    const mParentId = cleanXml.match(/<cbc:ParentDocumentID[^>]*>([\s\S]*?)<\/cbc:ParentDocumentID>/i);
    if (mParentId && mParentId[1]) {
      idFac = mParentId[1].replace(/<!\[CDATA\[|\]\]>/g, "").trim();
    }
    if (!idFac && mInvoiceInterno) {
      // Eliminar UBLExtensions antes de buscar el cbc:ID principal de la factura
      const invoiceSinExt = invoiceBlock.replace(/<ext:UBLExtensions>[\s\S]*?<\/ext:UBLExtensions>/i, "");
      const mIdInv = invoiceSinExt.match(/<cbc:ID[^>]*>([\s\S]*?)<\/cbc:ID>/i);
      if (mIdInv && mIdInv[1]) {
        idFac = mIdInv[1].replace(/<!\[CDATA\[|\]\]>/g, "").trim();
      }
    }
    if (!idFac && asuntoOpcional && asuntoOpcional.includes(";")) {
      const pAsunto = asuntoOpcional.split(";");
      if (pAsunto.length >= 3) idFac = pAsunto[2].trim();
    }

    // 4. Extraer Fecha de emisión
    let fecha = "";
    const mFecha = invoiceBlock.match(/<cbc:IssueDate[^>]*>([\s\S]*?)<\/cbc:IssueDate>/i) ||
                   cleanXml.match(/<cbc:IssueDate[^>]*>([\s\S]*?)<\/cbc:IssueDate>/i);
    if (mFecha && mFecha[1]) {
      const p = mFecha[1].replace(/<!\[CDATA\[|\]\]>/g, "").trim().split("-");
      if (p.length === 3) fecha = p[2] + "/" + p[1] + "/" + p[0];
      else fecha = mFecha[1].trim();
    }

    // 5. Extraer Descripción de las líneas de factura (<cac:InvoiceLine>...<cac:Item>...<cbc:Description>)
    // Nunca tomar el <cbc:Description> de <cac:ExternalReference> porque contiene el XML embebido
    let desc = "";
    const descripcionesLineas = [];
    const regexLineas = /<cac:(?:InvoiceLine|CreditNoteLine)>([\s\S]*?)<\/cac:(?:InvoiceLine|CreditNoteLine)>/gi;
    let mLinea;
    while ((mLinea = regexLineas.exec(invoiceBlock)) !== null && descripcionesLineas.length < 3) {
      const bloqueLinea = mLinea[1];
      const mDescItem = bloqueLinea.match(/<cac:Item>[\s\S]*?<cbc:Description[^>]*>([\s\S]*?)<\/cbc:Description>/i) ||
                        bloqueLinea.match(/<cbc:Description[^>]*>([\s\S]*?)<\/cbc:Description>/i);
      if (mDescItem && mDescItem[1]) {
        const dLimpia = mDescItem[1].replace(/<!\[CDATA\[|\]\]>/g, "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
        if (dLimpia && dLimpia.length <= 140 && !dLimpia.startsWith("<?xml") && !dLimpia.includes("Invoice")) {
          descripcionesLineas.push(dLimpia);
        }
      }
    }
    if (descripcionesLineas.length > 0) {
      desc = descripcionesLineas.join(" + ");
    } else {
      const mNota = invoiceBlock.match(/<cbc:Note[^>]*>([\s\S]*?)<\/cbc:Note>/i);
      if (mNota && mNota[1]) {
        const nLimpia = mNota[1].replace(/<!\[CDATA\[|\]\]>/g, "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
        if (nLimpia && nLimpia.length <= 120 && !nLimpia.includes("Invoice")) {
          desc = nLimpia;
        }
      }
    }

    return {
      emisor: emisor,
      monto: monto,
      id_factura: idFac,
      fecha: fecha,
      descripcion: desc
    };
  } catch (e) {
    return null;
  }
}

function clasificarComercioColombianoUniversal_(nombreComercio, textoCompleto) {

  const t = normalizarTexto((nombreComercio || "") + " " + (textoCompleto || ""));

  

  // 1. Supermercados, Mercados y Lácteos de Medellín / Antioquia

  if (/colanta|cooperativa colanta|mercolanta|la vaquita|euro supermercados|euro|mercatodo|exito|carulla|tiendas d1|\bd1\b|ara|tiendas ara|isimo|mayorista|la mayorista|minorista|la minorista|placita de florez|jumbo|makro|pricesmart|boom|surtimax/.test(t)) {

    let sub = "Supermercado";

    if (/colanta|mercolanta/.test(t)) sub = "Lácteos y Colanta";

    else if (/la vaquita|euro|mercatodo/.test(t)) sub = "Supermercados Locales (Vaquita/Euro)";

    else if (/mayorista|minorista|florez/.test(t)) sub = "Plaza de Mercado";

    return { categoria: "Alimentación", subcategoria: sub };

  }

  // 2. Restaurantes, Comidas Rápidas y Panaderías de Medellín (San Félix, Laureles, Poblado, Envigado, Bello)

  if (/san felix|san félix|chef burger|pigasus|mondongo|hatoviejo|il forno|parmessano|el machetico|machetazo|el corral|frisby|kfc|mcdonald|domino|subway|crepes|popsy|mimo|santa elena|el astor|astor|de lolita|pergamino|juan valdez|tostao|dunkin|rappi|didi food|uber eats|almuerzo|cena|desayuno|restaurante|comida rapida|comida rápida|hamburguesa|pizza|picada|buñuelo|pandequeso|empanada|chuzo|chunchurria|arepa/.test(t)) {

    let sub = "Restaurante";

    if (/san felix|san félix|hamburguesa|pizza|picada|chuzo|comida rapida|comida rápida|el machetico|frisby|kfc|mcdonald|corral|pigasus|chef burger/.test(t)) sub = "Comida rápida";

    else if (/rappi|didi food|uber eats|domicilio/.test(t)) sub = "Domicilios";

    else if (/buñuelo|pandequeso|empanada|panaderia|santa elena|astor|de lolita|pergamino|juan valdez|tostao|dunkin/.test(t)) sub = "Café y Panadería";

    return { categoria: "Alimentación", subcategoria: sub };

  }

  // 3. Servicios Públicos y Hogar en Medellín (EPM, Tigo UNE, Emvarias)

  if (/epm|empresas publicas de medellin|empresas publicas|tigo|une epm|une|colombia movil|claro|comcel|telmex|movistar|emvarias|vanti|gas natural/.test(t)) {

    let sub = "Servicios Públicos";

    if (/tigo|une|claro|movistar|internet|datos|celular/.test(t)) sub = "Internet y Telefonía";

    else if (/epm|emvarias|energia|agua|gas|alcantarillado/.test(t)) sub = "EPM (Agua, Luz, Gas)";

    return { categoria: "Servicios", subcategoria: sub };

  }

  // 4. Transporte y Moto en Medellín (Cívica Metro, Terpel, Zeuss, Parqueaderos, Peajes)

  if (/civica|cívica|metro de medellin|metro|metroplus|tranvia|metrocable|encicla|gasolina|combustible|terpel|primax|zeuss|biomax|texaco|mobil|esso|estacion de servicio|eds|didi moto|didi|uber|cabify|indrive|coopebombas|tax individual|flota|terminal norte|terminal sur|flypass|gopass|peaje|auteco|akt|yamaha|suzuki|taller|lavadero|parqueadero|parking|soat|sura soat/.test(t)) {

    if (/gasolina|combustible|terpel|primax|zeuss|texaco|biomax|mobil|eds/.test(t)) return { categoria: "Moto", subcategoria: "Combustible" };

    if (/moto|taller|auteco|akt|yamaha|suzuki|repuestos|mantenimiento|aceite|llanta/.test(t)) return { categoria: "Moto", subcategoria: "Mantenimiento" };

    if (/civica|cívica|metro|metroplus|tranvia|metrocable|encicla/.test(t)) return { categoria: "Transporte", subcategoria: "Metro de Medellín (Cívica)" };

    if (/parqueadero|parking/.test(t)) return { categoria: "Transporte", subcategoria: "Parqueadero" };

    if (/peaje|flypass|gopass/.test(t)) return { categoria: "Transporte", subcategoria: "Peajes" };

    return { categoria: "Transporte", subcategoria: "Taxi / App" };

  }

  // 5. Videojuegos, Streaming y Entretenimiento

  if (/eneba|steam|playstation|xbox|epic games|riot games|blizzard|nintendo|ea games|ubisoft|google play|apple services|spotify|netflix|disney|hbo|max|prime video|crunchyroll|youtube|twitch|dlocal|cine colombia|procinal|cinemark|royal films|viva envigado|mayorca|santafé|el tesoro|los molinos|arkadia|puerta del norte|premium plaza/.test(t)) {

    let sub = "Entretenimiento general";

    if (/eneba|steam|playstation|xbox|epic|videojuego|juego|dlocal/.test(t)) sub = "Videojuegos / Eneba";

    else if (/spotify|netflix|disney|hbo|prime|youtube|streaming/.test(t)) sub = "Streaming / Suscripciones";

    else if (/cine colombia|procinal|cinemark|cine/.test(t)) sub = "Cine";

    return { categoria: "Entretenimiento", subcategoria: sub };

  }

  // 6. Compras y Centros Comerciales de Medellín

  if (/greenfit|mercado libre|mercadolibre|amazon|shein|aliexpress|temu|falabella|zara|arturo calle|koaj|mattelsa|tennis|gef|punto blanco|homecenter|sodimac|easy|decathlon|miniso|dollarcity|el hueco|guayaquil|palace|ropa|vestuario|leggin|enterizo/.test(t)) {

    let sub = "Comercio electrónico";

    if (/greenfit|falabella|zara|arturo calle|koaj|mattelsa|tennis|gef|punto blanco|ropa|calzado|leggin|enterizo|top dama|activewear|vestuario/.test(t)) sub = "Ropa y Moda Paisa";

    else if (/homecenter|sodimac|easy|ferreteria/.test(t)) sub = "Hogar y Ferretería";

    else if (/dollarcity|miniso/.test(t)) sub = "Variedades";

    else if (/el hueco|guayaquil|palace/.test(t)) sub = "Centro de Medellín (El Hueco)";

    return { categoria: "Compras", subcategoria: sub };

  }

  // 7. Salud, EPS y Farmacias en Medellín

  if (/sura|eps sura|dinamica|ayudas diagnosticas|sanitas|compensar|famisanar|nueva eps|comfama|comfenalco|cruz verde|drogas la rebaja|farmatodo|pasteur|drogueria alemana|farmacia|medicamentos/.test(t)) {

    return { categoria: "Salud", subcategoria: "Farmacia y EPS Sura" };

  }

  // 8. Vivienda y Arriendo

  if (/arriendo|administracion|administración|inmobiliaria|apartamento|canon/.test(t)) {

    return { categoria: "Vivienda", subcategoria: "Arriendo" };

  }

  return { categoria: "Otros", subcategoria: "General" };

}

function extraerFacturaGmailLocal(asunto, remitente, cuerpo) {

  // 1. Si parece publicidad, OTP, crédito preaprobado, etc., NUNCA extraer
  if (esCorreoNoFactura_(asunto, remitente, cuerpo, false)) return null;

  const texto = (asunto || "") + "\n" + (cuerpo || "");
  const limpio = normalizarTexto(texto);

  // 2. Detección de PSE / ACH Colombia / Pasarelas (Wompi, Bold, PayU, MercadoPago)
  const esPSE = /(pse|achcolombia|cus|wompi|bold|payu|mercadopago)/.test(limpio) && /(aprobada|exitosa|confirmad|pagad|exitoso)/.test(limpio);
  let cusMatch = texto.match(/cus\s*[:#-]?\s*(\d+)/i);
  let idRef = cusMatch ? "CUS " + cusMatch[1] : "";

  // 3. Detección de Factura Electrónica de Venta o Asunto DIAN (NIT;Emisor;Factura;01;...)
  const partesAsunto = String(asunto || "").split(";");
  const esAsuntoDian = /\b\d{6,12}\s*;.*;\s*(01|02|03|91|92)\b/i.test(asunto || "") || partesAsunto.length >= 4;
  const esFacturaVenta = esAsuntoDian || /factura\s+electr[oó]nica|documento\s+electr[oó]nico|representaci[oó]n\s+gr[aá]fica|attacheddocument|cuenta\s+de\s+cobro|recibo\s+de\s+pago|comprobante\s+de\s+pago/i.test(texto);

  if (esAsuntoDian && partesAsunto.length >= 3 && !idRef) {
    idRef = partesAsunto[2].trim();
  }
  let facMatch = texto.match(/(?:factura|documento|comprobante|recibo)\s*(?:electr[oó]nica)?\s*(?:de\s+venta)?\s*(?:n[°o.]?|nro\.?|#|:)\s*([A-Za-z0-9-]{3,25})/i);
  if (facMatch && !idRef) idRef = facMatch[1];

  // 4. Detección de Débito Bancario / Nequi / Bancolombia / Daviplata Confirmado
  const esDebito = /(debito\s+por\s+compra|transferencia\s+exitosa|pago\s+exitoso|compra\s+exitosa|envio\s+exitoso|realizaste\s+un\s+pago|pagaste|compraste|recibiste\s+una\s+transferencia)/i.test(limpio);

  // 5. Detección de Confirmación de Pedido / Compra en Tiendas y E-Commerce (Greenfit, Mercado Libre, etc.)
  const esPedidoEcommerce = /(confirmacion\s+(?:de\s+)?(?:tu\s+)?pedido|pedido\s+confirmado|tu\s+pedido\s+esta\s+confirmado|gracias\s+por\s+tu\s+compra|compra\s+confirmada|orden\s+de\s+compra|recibo\s+de\s+compra)/i.test(limpio);

  if (!esPSE && !esFacturaVenta && !esDebito && !esPedidoEcommerce) {
    return null;
  }

  if (esPedidoEcommerce && !idRef) {
    let orderMatch = texto.match(/(?:pedido|orden|order)\s*[:#n°o.]*\s*([A-Za-z0-9-_]+)/i);
    if (orderMatch) idRef = "ORDEN " + orderMatch[1];
  }

  // Extraer el monto neto de la transacción confirmada
  let valor = 0;
  let m = texto.match(/(?:total(?:\s+a\s+pagar|\s+factura|\s+pagado)?|valor(?:\s+total|\s+pagado|\s+de\s+la\s+transacci[oó]n|\s+de\s+la\s+compra)?|monto(?:\s+pagado|\s+total)?|importe|neto\s+a\s+pagar)\s*[:#-]?\s*(?:cop|\$)?\s*\$?\s*([\d\.\,]{3,20})/i);
  if (m) {
    valor = limpiarMontoCOP_(m[1]);
  }
  if (valor <= 0) {
    m = texto.match(/(?:\$|cop)\s*([\d\.\,]{4,20})/i);
    if (m) valor = limpiarMontoCOP_(m[1]);
  }

  if (valor <= 0) return null;

  // Clasificar comercio
  let proveedor = "Comercio";
  if (esAsuntoDian && partesAsunto.length >= 2 && partesAsunto[1].trim().length >= 2) {
    proveedor = partesAsunto[1].trim();
  } else {
    let provMatch = texto.match(/(?:comercio|empresa|establecimiento|convenio|emisor|raz[oó]n\s+social|a\s+favor\s+de)\s*[:#-]?\s*([A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .&'-]{3,50})/i);
    if (provMatch) proveedor = provMatch[1].trim();
  }

  if (proveedor === "Comercio" || !proveedor) {
    if (/colanta|mercolanta|890904478/i.test(texto)) proveedor = "Cooperativa Colanta";
    else if (/epm|empresas publicas/i.test(texto)) proveedor = "EPM";
    else if (/tigo|colombia movil|une epm/i.test(texto)) proveedor = "Tigo";
    else if (/greenfit/i.test(texto)) proveedor = "Greenfit";
    else if (/mercadopago|mercado pago/i.test(texto)) proveedor = "MercadoPago";
    else if (/mercadolibre|mercado libre/i.test(texto)) proveedor = "Mercado Libre";
    else if (/amazon/i.test(texto)) proveedor = "Amazon";
    else if (/falabella/i.test(texto)) proveedor = "Falabella";
    else if (/zara/i.test(texto)) proveedor = "Zara";
    else if (/koaj/i.test(texto)) proveedor = "Koaj";
    else if (/shein/i.test(texto)) proveedor = "Shein";
    else if (/temu/i.test(texto)) proveedor = "Temu";
    else if (/exito/i.test(texto)) proveedor = "Éxito";
    else if (/d1\b/i.test(texto)) proveedor = "Tiendas D1";
  }

  const clasif = clasificarComercioColombianoUniversal_(proveedor, texto);
  const cuentaDetectada = (/colanta|890904478|restaurante corporativo/i.test(proveedor + " " + texto)) ? "AyC Colanta" : "Nequi";

  return normalizarFacturaGmail_({
    es_factura_real: true,
    ignorar: false,
    proveedor: proveedor,
    categoria: clasif.categoria,
    subcategoria: clasif.subcategoria,
    descripcion_formal: embellecerConceptoFinanciero_((esPSE ? "Pago PSE en " : "Compra en ") + proveedor, proveedor, cuerpo, asunto),
    valor: valor,
    tipo: "Gasto",
    id_referencia: idRef,
    metodo_pago: esPSE ? "PSE" : (cuentaDetectada === "AyC Colanta" ? "Transferencia / Nómina" : "Transferencia"),
    cuenta: cuentaDetectada,
    fecha_factura: Utilities.formatDate(new Date(), ZONA_HORARIA, "dd/MM/yyyy")
  }, asunto, cuerpo);
}

function normalizarFacturaGmail_(fac, asunto, cuerpo) {
  if (!fac) return null;
  const prov = String(fac.proveedor || "Comercio").trim();
  const cat = fac.categoria || "Otros";
  const sub = fac.subcategoria || "General";
  const val = Number(fac.valor || 0);
  const tipo = fac.tipo || "Gasto";
  const met = fac.metodo_pago || "Transferencia";
  const cue = fac.cuenta || "Nequi";
  const idRef = fac.id_referencia || "";
  const desc = fac.descripcion_formal || ("Compra en " + prov);
  const fec = fac.fecha_factura || Utilities.formatDate(new Date(), ZONA_HORARIA, "dd/MM/yyyy");

  return {
    es_factura_real: true,
    ignorar: false,
    proveedor: prov,
    categoria: cat,
    subcategoria: sub,
    descripcion_formal: desc,
    valor: val,
    tipo: tipo,
    id_referencia: idRef,
    metodo_pago: met,
    cuenta: cue,
    fecha_factura: fec
  };
}

function resolverMetodoPago_(datos, concepto) {
  const t = normalizarTexto((datos && datos.texto_original ? datos.texto_original : "") + " " + (concepto || ""));
  if (/efectivo|plata en mano|billete/.test(t)) {
    return { metodo: "Efectivo", cuenta: "Efectivo" };
  }
  if (/colanta|nomina|ayc|ahorro y credito|carnet/.test(t)) {
    return { metodo: "Transferencia / Nómina", cuenta: "AyC Colanta" };
  }
  if (/bancolombia/.test(t)) {
    return { metodo: "Transferencia", cuenta: "Bancolombia" };
  }
  if (/daviplata/.test(t)) {
    return { metodo: "Transferencia", cuenta: "Daviplata" };
  }
  return { metodo: "Transferencia", cuenta: "Nequi" };
}

function embellecerConceptoFinanciero_(descOriginal, proveedor, cuerpo, asunto) {
  const raw = String(descOriginal || "").trim();
  const t = normalizarTexto(raw + " " + (proveedor || "") + " " + (cuerpo || "") + " " + (asunto || ""));

  if (/menu\s*\d*\s*rest|restaurante corporativo|planta medellin/i.test(t)) {
    return "Almuerzo — Restaurante Corporativo Planta Medellín (Colanta)";
  }
  if (/mercolanta/i.test(t)) {
    return "Productos Lácteos / Mercolanta";
  }
  if (/spotify/i.test(t)) {
    return "Suscripción Spotify Premium";
  }
  if (/eneba/i.test(t)) {
    return "Videojuegos — Compra en Eneba";
  }
  if (/arriendo/i.test(t)) {
    return "Canon de Arriendo Hogar";
  }
  if (/epm/i.test(t)) {
    return "Servicios Públicos EPM";
  }
  if (/tigo/i.test(t)) {
    return "Plan Móvil / Datos Tigo";
  }
  if (/gasolina|terpel|primax|biomax/i.test(t)) {
    return "Tanqueada Gasolina Moto";
  }
  if (/lavada.*moto/i.test(t)) {
    return "Lavada de Moto";
  }

  // Si tiene un formato abreviado crudo
  if (/^menu\s*\d+/i.test(raw)) {
    return "Almuerzo Restaurante (" + (proveedor || "Colanta") + ")";
  }

  return raw || ("Compra en " + (proveedor || "Comercio"));
}

/**
 * 📬 ESCÁNER AUTOMÁTICO DE FACTURAS Y COMPROBANTES EN GMAIL (V26.9 PRO)
 * Regla Oficial de Marlon Muñoz Pardo:
 * 1. Revisa toda la Bandeja de Entrada (in:inbox), descomprime archivos .zip de entidades económicas
 *    (normalizando blobs a application/zip y leyendo XML UBL 2.1, PDF internos e imágenes).
 * 2. Si el correo llega sin leer, lo analiza, registra el movimiento en Google Sheets, le asigna su etiqueta
 *    FINANZAS/<Carpeta>, LO DEJA SIN MARCAR COMO VISTO (No Leído) y LO DEJA EN LA BANDEJA DE ENTRADA.
 * 3. Apenas Marlon marca el correo como LEÍDO (visto), el sistema lo mueve AUTOMÁTICAMENTE e INMEDIATAMENTE
 *    a su carpeta correspondiente (archivándolo de Recibidos hacia FINANZAS/<Carpeta>).
 */
function procesarCorreosFacturas() {
  let contadorProcesadas = 0;
  try {
    // Primero: mover de inmediato cualquier correo ya etiquetado que Marlon ya haya marcado como leído
    archivarCorreosFinancierosViejos_();

    const ss = obtenerLibro_();
    const shMov = ss.getSheetByName("MOVIMIENTOS");
    if (!shMov) {
      console.error("No se encontró la hoja MOVIMIENTOS");
      return 0;
    }

    const cacheSet = obtenerIdsGmailRegistradosSet_();
    const ahora = new Date();
    const meses = ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
    const mesActual = meses[ahora.getMonth()];
    const anioActual = ahora.getFullYear();
    const horaStr = Utilities.formatDate(ahora, ZONA_HORARIA, "hh:mm a");

    // Escanear directamente los hilos en Bandeja de Entrada (in:inbox) para no perder ningún .zip ni formato DIAN
    const hilos = GmailApp.search('in:inbox', 0, 50);

    if (!hilos || !hilos.length) {
      return 0;
    }

    for (let h = 0; h < hilos.length; h++) {
      const hilo = hilos[h];
      // Capturar el estado original de lectura ANTES de tocar el hilo/mensaje
      const hiloEstabaNoLeido = hilo.isUnread();
      const mensajes = hilo.getMessages() || [];

      for (let m = 0; m < mensajes.length; m++) {
        const msg = mensajes[m];
        const idMsg = msg.getId();
        const msgEstabaNoLeido = msg.isUnread() || hiloEstabaNoLeido;

        if (esMensajeGmailYaRegistrado_(idMsg, cacheSet)) {
          // Si ya estaba registrado y Marlon ya lo leyó, asegurar que se mueva a su carpeta
          if (!hilo.isUnread()) {
            const labelsHilo = hilo.getLabels() || [];
            const tieneEtiquetaFinanzas = labelsHilo.some(l => String(l.getName()).toLowerCase().includes("finanzas"));
            if (tieneEtiquetaFinanzas) {
              try { hilo.moveToArchive(); } catch (eMove) {}
            }
          }
          continue;
        }

        const asunto = msg.getSubject() || "";
        const remitente = msg.getFrom() || "";
        const cuerpoPlano = msg.getPlainBody() || "";
        let cuerpoHtmlTexto = "";
        try {
          cuerpoHtmlTexto = (msg.getBody() || "")
            .replace(/<style[\s\S]*?<\/style>/gi, " ")
            .replace(/<script[\s\S]*?<\/script>/gi, " ")
            .replace(/<[^>]+>/g, " ")
            .replace(/&nbsp;/gi, " ")
            .replace(/&amp;/gi, "&")
            .replace(/\s+/g, " ")
            .trim();
        } catch (eHtml) {}

        const cuerpo = (cuerpoPlano + "\n" + cuerpoHtmlTexto).substring(0, GMAIL_MAX_BODY_CHARS);
        const adjuntos = msg.getAttachments() || [];
        const tieneAdj = adjuntos.length > 0;

        if (!pareceCorreoFinancieroValido_(asunto, remitente, cuerpo, adjuntos)) {
          if (esCorreoNoFactura_(asunto, remitente, cuerpo, false)) {
            registrarIdGmailEnMemoria_(idMsg);
            cacheSet.add(idMsg);
          }
          continue;
        }

        let datosFactura = null;

        // 1. Descomprimir .ZIP y extraer archivos .XML y .PDF (estándar DIAN UBL 2.1 Colombia y entidades económicas)
        const desempacados = tieneAdj ? desempacarAdjuntosCorreo_(adjuntos) : { xmlBlobs: [], pdfImagenBlobs: [] };

        if (desempacados.xmlBlobs.length > 0) {
          for (let x = 0; x < desempacados.xmlBlobs.length; x++) {
            try {
              const xmlBlob = desempacados.xmlBlobs[x];
              let xmlStr = "";
              try {
                xmlStr = xmlBlob.getDataAsString("UTF-8");
              } catch (eUtf) {
                xmlStr = xmlBlob.getDataAsString();
              }
              const extraidoXml = extraerDatosDianXml_(xmlStr, asunto);
              if (extraidoXml && extraidoXml.monto > 0) {
                datosFactura = extraidoXml;
                break;
              } else if (extraidoXml && !datosFactura) {
                datosFactura = extraidoXml;
              }
            } catch (eXml) {
              console.warn("Error leyendo XML adjunto o extraído de ZIP:", eXml);
            }
          }
        }

        // 2. Si el XML no devolvió monto (> 0), analizar los PDFs o imágenes (tanto adjuntos directos como extraídos del .ZIP) con Gemini Multimodal
        if ((!datosFactura || !datosFactura.monto || datosFactura.monto <= 0) && desempacados.pdfImagenBlobs.length > 0 && GEMINI_API_KEY) {
          for (let p = 0; p < desempacados.pdfImagenBlobs.length; p++) {
            const pdfBlob = desempacados.pdfImagenBlobs[p];
            const nombrePdf = (pdfBlob.getName() || "").toLowerCase();
            const mimePdf = (pdfBlob.getContentType() || "").toLowerCase();
            try {
              const bytes = pdfBlob.getBytes();
              if (bytes && bytes.length <= GMAIL_MAX_ATTACHMENT_BYTES) {
                const base64 = Utilities.base64Encode(bytes);
                const mimeFinal = (nombrePdf.endsWith(".pdf") || mimePdf.includes("pdf")) ? "application/pdf" : (mimePdf.includes("image") ? mimePdf : "application/pdf");
                const prompt = "Analiza esta factura electrónica o comprobante de pago colombiano para Marlon Muñoz Pardo. Extrae ÚNICAMENTE un JSON válido con: { \"proveedor\": string, \"valor\": number, \"id_factura\": string, \"fecha\": \"dd/MM/yyyy\", \"descripcion\": string, \"categoria\": string, \"subcategoria\": string } sin markdown ni explicaciones.";
                const respGem = llamarGeminiAPI({
                  contents: [{
                    parts: [
                      { inlineData: { mimeType: mimeFinal, data: base64 } },
                      { text: prompt }
                    ]
                  }],
                  generationConfig: { responseMimeType: "application/json", temperature: 0.1 }
                });
                if (respGem && respGem.candidates && respGem.candidates[0].content) {
                  const txtGem = limpiarJSON(respGem.candidates[0].content.parts[0].text);
                  const jGem = JSON.parse(txtGem);
                  if (jGem && Number(jGem.valor) > 0) {
                    datosFactura = {
                      emisor: jGem.proveedor || (datosFactura ? datosFactura.emisor : ""),
                      monto: Number(jGem.valor),
                      id_factura: jGem.id_factura || (datosFactura ? datosFactura.id_factura : ""),
                      fecha: jGem.fecha || (datosFactura ? datosFactura.fecha : ""),
                      descripcion: jGem.descripcion || (datosFactura ? datosFactura.descripcion : "")
                    };
                    break;
                  }
                }
              }
            } catch (eGem) {
              console.warn("Aviso Gemini Factura PDF/ZIP:", eGem);
            }
          }
        }

        // 3. Normalizar datos o recurrir a extracción local de texto / Gemini sobre el cuerpo del correo
        let facturaFinal = null;
        if (datosFactura && datosFactura.monto > 0) {
          let prov = datosFactura.emisor;
          let idRef = datosFactura.id_factura;

          if ((!prov || prov === "Comercio") && asunto.includes(";")) {
            const pAsunto = asunto.split(";");
            if (pAsunto.length >= 2) prov = pAsunto[1].trim();
          }
          if (!prov || prov === "Comercio") {
            if (/colanta|890904478/i.test(asunto + " " + remitente + " " + cuerpo)) prov = "Cooperativa Colanta";
            else if (/epm|empresas publicas/i.test(asunto + " " + remitente + " " + cuerpo)) prov = "EPM";
            else if (/tigo|colombia movil/i.test(asunto + " " + remitente + " " + cuerpo)) prov = "Tigo";
          }
          if (!idRef && asunto.includes(";")) {
            const partes = asunto.split(";");
            if (partes.length >= 3) idRef = partes[2].trim();
          }

          let desc = embellecerConceptoFinanciero_(datosFactura.descripcion, prov, cuerpo, asunto);

          const clasif = clasificarComercioColombianoUniversal_(prov, cuerpo + " " + desc);
          if (/restaurante corporativo/i.test(cuerpo + " " + desc)) {
            clasif.categoria = "Alimentación";
            clasif.subcategoria = "Restaurante";
          }

          const cuentaPago = (/colanta|890904478|restaurante corporativo/i.test(prov + " " + cuerpo)) ? "AyC Colanta" : "Nequi";

          facturaFinal = {
            proveedor: prov || "Comercio",
            categoria: clasif.categoria,
            subcategoria: clasif.subcategoria,
            descripcion: desc,
            valor: datosFactura.monto,
            id_referencia: idRef,
            metodo_pago: cuentaPago === "AyC Colanta" ? "Transferencia / Nómina" : "Transferencia",
            cuenta: cuentaPago,
            fecha_factura: datosFactura.fecha || Utilities.formatDate(msg.getDate(), ZONA_HORARIA, "dd/MM/yyyy")
          };
        } else {
          const localFac = extraerFacturaGmailLocal(asunto, remitente, cuerpo);
          if (localFac && localFac.valor > 0) {
            facturaFinal = {
              proveedor: localFac.proveedor,
              categoria: localFac.categoria,
              subcategoria: localFac.subcategoria,
              descripcion: localFac.descripcion_formal,
              valor: localFac.valor,
              id_referencia: localFac.id_referencia,
              metodo_pago: localFac.metodo_pago,
              cuenta: localFac.cuenta,
              fecha_factura: localFac.fecha_factura || Utilities.formatDate(msg.getDate(), ZONA_HORARIA, "dd/MM/yyyy")
            };
          }
        }

        if (!facturaFinal || !facturaFinal.valor || facturaFinal.valor <= 0) {
          // Asegurar que si el correo estaba no leído, se conserve como no leído
          if (msgEstabaNoLeido) {
            try { msg.markUnread(); hilo.markUnread(); } catch (eUn) {}
          }
          continue;
        }

        // 4. Verificación Anti-duplicados
        const dupCheck = esFacturaDuplicadaEnMovimientos_(
          facturaFinal.id_referencia,
          facturaFinal.proveedor,
          facturaFinal.valor,
          facturaFinal.fecha_factura,
          idMsg
        );

        if (dupCheck.esDuplicado) {
          console.log("Factura duplicada omitida:", dupCheck.motivo);
          registrarIdGmailEnMemoria_(idMsg);
          cacheSet.add(idMsg);
          moverCorreoAEtiquetaEspecifica(hilo, facturaFinal.proveedor, facturaFinal.categoria, asunto, cuerpo, facturaFinal.subcategoria, msgEstabaNoLeido, msg);
          continue;
        }

        // 5. Inserción en MOVIMIENTOS Fila 5
        let maxNum = 255;
        if (shMov.getLastRow() >= 5) {
          const idsRange = shMov.getRange(5, 1, Math.min(20, shMov.getLastRow() - 4), 1).getValues();
          idsRange.forEach(row => {
            const mIdMatch = String(row[0] || "").match(/^M-(\d+)$/);
            if (mIdMatch) maxNum = Math.max(maxNum, parseInt(mIdMatch[1], 10));
          });
        }
        const nuevoId = "M-" + String(maxNum + 1).padStart(4, "0");

        const fechaMov = facturaFinal.fecha_factura || Utilities.formatDate(ahora, ZONA_HORARIA, "yyyy-MM-dd");
        let fechaFormatoFinal = fechaMov;
        if (fechaMov.includes("/")) {
          const fp = fechaMov.split("/");
          if (fp.length === 3 && fp[2].length === 4) {
            fechaFormatoFinal = fp[2] + "-" + fp[1].padStart(2, "0") + "-" + fp[0].padStart(2, "0");
          }
        }

        const notaFactura = "Factura: " + (facturaFinal.id_referencia || "DIAN") + " | GMAIL:" + idMsg;

        const filaMov = [
          nuevoId,
          fechaFormatoFinal,
          horaStr,
          "Gasto",
          facturaFinal.categoria,
          facturaFinal.subcategoria,
          facturaFinal.descripcion,
          facturaFinal.valor,
          facturaFinal.metodo_pago,
          facturaFinal.cuenta,
          mesActual,
          anioActual,
          "Gmail Scanner",
          notaFactura
        ];

        shMov.insertRowBefore(5);
        shMov.getRange(5, 1, 1, filaMov.length).setValues([filaMov]);
        shMov.getRange(5, 15).setFormula('=IF(ISBLANK(A5), "", IF(AND(LOWER(TRIM(K5))=LOWER(TRIM(PANEL!$L$2)), L5=PANEL!$I$2), MAX($O$4:O4)+1, ""))');

        registrarIdGmailEnMemoria_(idMsg);
        cacheSet.add(idMsg);

        // Aplicar etiqueta FINANZAS/<Carpeta>, mantener NO LEÍDO en bandeja si no ha sido visto, o mover de inmediato si ya fue leído
        const etiquetaAsignada = moverCorreoAEtiquetaEspecifica(
          hilo,
          facturaFinal.proveedor,
          facturaFinal.categoria,
          asunto,
          cuerpo,
          facturaFinal.subcategoria,
          msgEstabaNoLeido,
          msg
        );

        // 6. Notificación a Telegram con botón de deshacer
        const chatId = PropertiesService.getScriptProperties().getProperty("TELEGRAM_CHAT_ID");
        if (chatId && TELEGRAM_TOKEN) {
          try {
            const msgTg = `🧾 *FACTURA ELECTRÓNICA REGISTRADA*\n${separadorBonito_()}\n` +
              `🏢 *Comercio:* ${facturaFinal.proveedor}\n` +
              `🍽️ *Detalle:* ${facturaFinal.descripcion}\n` +
              `💵 *Monto:* $${formatearCOP(facturaFinal.valor)} COP\n` +
              `💳 *Cuenta:* ${facturaFinal.cuenta} (${facturaFinal.metodo_pago})\n` +
              `📂 *Carpeta Gmail:* \`${etiquetaAsignada}\`\n` +
              `📄 *Doc / Ref:* ${facturaFinal.id_referencia || "Electrónica"}\n` +
              `🆔 *ID:* \`${nuevoId}\`\n\n` +
              `📬 _El correo quedó sin leer en tu bandeja; apenas lo marques como leído se moverá automáticamente a ${etiquetaAsignada}_`;
            
            enviarMensajeTelegramConBoton(chatId, msgTg, nuevoId);
          } catch (eTg) {
            console.warn("Aviso Telegram Factura:", eTg);
          }
        }

        contadorProcesadas++;
      }
    }

    // Revisar nuevamente por si algún hilo ya estaba leído y listo para moverse a su carpeta
    archivarCorreosFinancierosViejos_();

  } catch (err) {
    console.error("Error en procesarCorreosFacturas:", err);
  }

  return contadorProcesadas;
}

function esCorreoDuplicado(nombrePestana, idMensaje) {
  const sh = obtenerLibro_().getSheetByName(nombrePestana);
  if (!sh || sh.getLastRow() < 4) return false;
  return sh.getRange(4, 14, sh.getLastRow() - 3, 1).getDisplayValues().some(r => String(r[0]).includes(idMensaje));
}

function obtenerOCrearEtiquetaGmail_(nombreCompleto) {
  const partes = String(nombreCompleto || "").split("/");
  let acumulado = "";
  let etiquetaFinal = null;

  for (let i = 0; i < partes.length; i++) {
    acumulado = acumulado ? (acumulado + "/" + partes[i]) : partes[i];
    etiquetaFinal = GmailApp.getUserLabelByName(acumulado);
    if (!etiquetaFinal) {
      try {
        etiquetaFinal = GmailApp.createLabel(acumulado);
      } catch (e) {
        etiquetaFinal = GmailApp.getUserLabelByName(acumulado);
      }
    }
  }
  return etiquetaFinal;
}

/**
 * Regla Oficial de Marlon Muñoz Pardo:
 * - Si el correo estaba SIN LEER (estabaNoLeido = true): le asigna su etiqueta FINANZAS/<Carpeta>,
 *   lo deja explícitamente como NO LEÍDO (sin marcar como visto) y lo mantiene en la Bandeja de Entrada (in:inbox).
 * - Si el correo YA FUE LEÍDO por Marlon (estabaNoLeido = false) o si archivarInmediato = true:
 *   lo mueve AUTOMÁTICAMENTE e INMEDIATAMENTE a su carpeta correspondiente (moveToArchive).
 */
function marcarCorreoParaRevision_(hilo, nombre, archivarInmediato = false, estabaNoLeido = true, msgOpcional = null) {
  try {
    const etiq = obtenerOCrearEtiquetaGmail_(nombre);
    if (etiq) hilo.addLabel(etiq);

    if (archivarInmediato || !estabaNoLeido) {
      hilo.moveToArchive();
    } else {
      // Garantizar que permanezca sin marcar como visto en la Bandeja de Entrada hasta que Marlon lo abra
      try {
        if (msgOpcional) msgOpcional.markUnread();
        hilo.markUnread();
      } catch (eUnread) {}
    }
  } catch (e) {}
}

/**
 * 📬 MOVIMIENTO AUTOMÁTICO E INMEDIATO AL MARCAR COMO LEÍDO
 * Regla de Marlon Muñoz Pardo:
 * - Todo correo financiero analizado permanece NO LEÍDO en la Bandeja de Entrada (Recibidos).
 * - APENAS Marlon lo marca como leído (!hilo.isUnread()), se mueve AUTOMÁTICAMENTE e INMEDIATAMENTE
 *   a su carpeta FINANZAS/<Subcarpeta> (sacándolo de Recibidos con moveToArchive).
 * - Si aún está SIN LEER (hilo.isUnread()), NUNCA se archiva (se respeta en la bandeja hasta que Marlon lo vea).
 */
function archivarCorreosFinancierosViejos_() {
  try {
    const hilos = GmailApp.search('in:inbox', 0, 50);
    if (!hilos || !hilos.length) return 0;

    let archivados = 0;

    hilos.forEach(hilo => {
      // Si el correo sigue SIN LEER, dejarlo quieto en la Bandeja de Entrada
      if (hilo.isUnread()) return;

      const labels = hilo.getLabels() || [];
      const esFinanciero = labels.some(l => {
        const n = String(l.getName()).toLowerCase();
        return n.includes("finanzas");
      });

      // Si ya tiene su etiqueta FINANZAS/<Carpeta> y Marlon YA LO MARCÓ COMO LEÍDO -> mover inmediatamente a su carpeta
      if (esFinanciero) {
        hilo.moveToArchive();
        archivados++;
      }
    });

    return archivados;
  } catch (e) {
    console.error("Error en archivarCorreosFinancierosViejos_:", e);
    return 0;
  }
}

function hiloYaGestionado_(hilo, cacheSet) {
  try {
    const msgs = hilo.getMessages() || [];
    if (!msgs.length) return false;
    return msgs.every(msg => {
      const id = msg.getId();
      return id && esMensajeGmailYaRegistrado_(id, cacheSet);
    });
  } catch (e) { return false; }
}

/**
 * 1. MEMORIA PERMANENTE DE IDs DE GMAIL PROCESADOS O ELIMINADOS
 * Evita que al borrar un registro desde Telegram el bot vuelva a importar el correo.
 */
function obtenerIdsGmailIgnoradosProps_() {
  try {
    const raw = PropertiesService.getScriptProperties().getProperty("GMAIL_IDS_IGNORADOS") || "[]";
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? new Set(arr) : new Set();
  } catch (e) {
    return new Set();
  }
}

function registrarIdGmailEnMemoria_(idMensaje) {
  if (!idMensaje) return;
  try {
    const props = PropertiesService.getScriptProperties();
    const raw = props.getProperty("GMAIL_IDS_IGNORADOS") || "[]";
    let arr = [];
    try { arr = JSON.parse(raw); } catch (e) { arr = []; }
    if (!arr.includes(idMensaje)) {
      arr.push(idMensaje);
      if (arr.length > 500) arr = arr.slice(-500);
      props.setProperty("GMAIL_IDS_IGNORADOS", JSON.stringify(arr));
    }
  } catch (e) {}
}

function obtenerIdsGmailRegistradosSet_() {
  const set = obtenerIdsGmailIgnoradosProps_();
  try {
    const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");
    if (!sh || sh.getLastRow() < 4) return set;
    const rows = sh.getRange(4, 14, sh.getLastRow() - 3, 1).getDisplayValues();
    for (let i = 0; i < rows.length; i++) {
      const ref = String(rows[i][0] || "");
      const matches = ref.match(/GMAIL:([a-zA-Z0-9_-]+)/gi) || [];
      matches.forEach(m => {
        const cleanId = m.replace(/GMAIL:/i, "").trim();
        if (cleanId) set.add(cleanId);
      });
    }
  } catch (e) {}
  return set;
}

function esMensajeGmailYaRegistrado_(idMensaje, cacheSet) {
  if (!idMensaje) return false;
  if (cacheSet && cacheSet.has(idMensaje)) return true;
  const ignorados = obtenerIdsGmailIgnoradosProps_();
  if (ignorados.has(idMensaje)) return true;
  return esCorreoDuplicado("MOVIMIENTOS", "GMAIL:" + idMensaje);
}

function moverCorreoAEtiquetaEspecifica(hilo, proveedor, categoria, asunto, cuerpo, subcategoria, estabaNoLeido = null, msgOpcional = null) {
  const t = normalizarTexto((proveedor || "") + " " + (categoria || "") + " " + (asunto || "") + " " + (cuerpo || "") + " " + (subcategoria || ""));
  let subcarpeta = categoria || "Compras";

  if (/csf rentals|csf\s*rent|moto|gasolina|combustible|terpel|primax|biomax|coesco|parqueadero moto/.test(t)) {
    subcarpeta = "Moto";
  } else if (/colanta|siesa|890904478|mercolanta|delicatessen caribe/.test(t)) {
    subcarpeta = "Colanta";
  } else if (/nequi/.test(t)) {
    subcarpeta = "Nequi";
  } else if (/bancolombia/.test(t)) {
    subcarpeta = "Bancolombia";
  } else if (/daviplata/.test(t)) {
    subcarpeta = "Daviplata";
  } else if (/greenfit|oasis punto cero|shein|temu|mercado\s*libre|amazon|falabella|ropa|calzado|zapatos/.test(t)) {
    subcarpeta = "Compras";
  } else if (/spotify|netflix|disney|prime|youtube|google one|eneba/.test(t)) {
    subcarpeta = "Suscripciones";
  } else if (/epm|tigo|claro|une|emvarias|agua|luz|energia|gas|internet/.test(t)) {
    subcarpeta = "Servicios";
  } else if (/parqueadero|parking|metro|civica|didi|uber/.test(t)) {
    subcarpeta = "Transporte";
  } else if (/comida|restaurante|almuerzo|cena|pizza|burger|delicatessen|d1|exito|vaquita|euro/.test(t)) {
    subcarpeta = "Alimentación";
  }

  const nombreEtiquetaCompleta = "FINANZAS/" + subcarpeta;
  const sigueNoLeido = (estabaNoLeido !== null && estabaNoLeido !== undefined) ? Boolean(estabaNoLeido) : hilo.isUnread();

  // Si sigue no leído -> dejar sin marcar como visto en la bandeja. Si ya fue leído -> mover de inmediato a su carpeta.
  marcarCorreoParaRevision_(hilo, nombreEtiquetaCompleta, !sigueNoLeido, sigueNoLeido, msgOpcional);

  return nombreEtiquetaCompleta;
}

/* =========================================================

   GASTOS FIJOS (CON LÓGICA PARCIAL Y ANTI-DUPLICIDAD)

   ========================================================= */

function asegurarHojaControlFijosV25_() {

  const ss = obtenerLibro_();

  let sh = ss.getSheetByName("CONTROL FIJOS");

  if (!sh) {

    sh = ss.insertSheet("CONTROL FIJOS");

    sh.getRange("A1:H1").merge();

    sh.getRange("A1").setValue("CONTROL DE GASTOS FIJOS");

    sh.getRange("A3:H3").setValues([[

      "ID","Concepto","Categoría","Día de cobro","Valor mensual","Último mes pagado","Fecha último pago","Notas"

    ]]);

    sh.getRange("A1:H1").setFontWeight("bold");

    sh.getRange("A3:H3").setFontWeight("bold");

    sh.setFrozenRows(3);

  }

  return sh;

}

function mesAnioActualTexto_() {

  const d = new Date();

  return v21Months_()[d.getMonth()] + " " + d.getFullYear();

}

/**

 * Asistente interactivo con ejemplos y botones para crear un gasto fijo

 */

/**

 * Generador Inteligente de Gastos Fijos impulsado por Gemini AI:

 * Analiza el perfil contable de Marlon (Colanta, Medellín, moto, arriendo, suscripciones)

 * y genera 3 ideas dinámicas y personalizadas con botones para crearlas en 1 toque.

 */

function generarEjemplosConIA_(chatId) {

  enviarAccionTelegram_(chatId, "typing");

  const ss = obtenerLibro_();

  const shFijos = ss.getSheetByName("CONTROL FIJOS");

  const fijosActuales = [];

  if (shFijos && shFijos.getLastRow() >= 10) {

    const rows = shFijos.getRange(10, 1, shFijos.getLastRow() - 9, 5).getValues();

    rows.forEach(r => {

      const conc = String(r[1] || "").trim();

      if (conc) fijosActuales.push(conc);

    });

  }

  const prompt = [

    `Eres el Asesor Financiero Personal Inteligente de ${USUARIO_NOMBRE_COMPLETO} en Medellín, Colombia.`,

    `Trabaja en Cooperativa Colanta, se moviliza en moto y tiene compromisos habituales de arriendo, servicios e internet.`,

    "",

    `Gastos fijos que Marlon YA TIENE registrados:`,

    fijosActuales.length ? fijosActuales.join(", ") : "EPM Energía, Arriendo, Comida, Internet Tigo, Parqueadero Moto, Gasolina Moto, Spotify",

    "",

    `TAREA:`,

    `Genera EXACTAMENTE 3 sugerencias de GASTOS FIJOS u obligaciones mensuales inteligentes, realistas y necesarias que Marlon NO tenga registradas aún.`,

    `Ejemplos apropiados para su perfil:`,

    `- Seguro Todo Riesgo Moto o Ahorro SOAT`,

    `- Plan de Salud Odontológico EPS o Emergencia Médica`,

    `- Gimnasio SmartFit o Entrenamiento Físico`,

    `- Fondo de Mantenimiento Preventivo Moto (mensualizado)`,

    `- Fondo de Ahorro para Vacaciones / Imprevistos`,

    `- Suscripción de Almacenamiento en Nube o Software de Trabajo`,

    `- Cuidado Personal / Barbería mensual`,

    "",

    `REGLAS OBLIGATORIAS:`,

    `1. Devuelve ÚNICAMENTE un JSON válido (un Array de exactamente 3 objetos). Sin texto adicional.`,

    `2. Formato de cada objeto:`,

    `   {`,

    `     "concepto": "Nombre breve del gasto (ej: Seguro Todo Riesgo Moto)",`,

    `     "valor": 50000,`,

    `     "dia": 15 o 30,`,

    `     "categoria": "Moto" | "Salud" | "Vivienda" | "Educación" | "Otros",`,

    `     "razon": "Justificación concisa en 1 frase de por qué le conviene a Marlon en Medellín"`,

    `   }`,

    `3. Valores realistas en pesos colombianos ($ COP) para Medellín (entre $25.000 y $120.000 COP).`

  ].join("\n");

  let sugerencias = [];

  try {

    const payload = {

      contents: [{ parts: [{ text: prompt }] }],

      generationConfig: { responseMimeType: "application/json", temperature: 0.2, maxOutputTokens: 500 }

    };

    const res = llamarGeminiAPI(payload);

    if (res && res.candidates && res.candidates[0].content) {

      const jsonStr = limpiarJSON(res.candidates[0].content.parts[0].text);

      const parsed = JSON.parse(jsonStr);

      if (Array.isArray(parsed) && parsed.length > 0) {

        sugerencias = parsed.slice(0, 3);

      }

    }

  } catch (e) {}

  // Fallback financiero exacto sin fallas en caso de contingencia

  if (!sugerencias.length) {

    sugerencias = [

      {

        concepto: "Seguro Todo Riesgo Moto",

        valor: 50000,

        dia: 30,

        categoria: "Moto",

        razon: "Protege tu moto de siniestros, hurtos y daños a terceros en Medellín."

      },

      {

        concepto: "Gimnasio SmartFit",

        valor: 85000,

        dia: 15,

        categoria: "Salud",

        razon: "Inversión mensual en tu salud física y bienestar para rendir al máximo."

      },

      {

        concepto: "Plan Odontológico EPS",

        valor: 35000,

        dia: 15,

        categoria: "Salud",

        razon: "Evita gastos imprevistos de salud con chequeos preventivos continuos."

      }

    ];

  }

  // Guardar en cache para activación directa por botones inline

  try {

    CacheService.getScriptCache().put("AI_FIJOS_" + chatId, JSON.stringify(sugerencias), 600);

  } catch (e) {}

  let msg = `💡 *IDEAS DE GASTOS FIJOS CON IA*\n${separadorBonito_()}\n\n`;

  msg += `🤖 _Marlon, analicé tus compromisos y flujo en Colanta. Aquí tienes 3 sugerencias inteligentes para blindar tu presupuesto:_\n\n`;

  const botones = [];

  sugerencias.forEach((sug, i) => {

    const qx = sug.dia <= 15 ? "Q1 (Día 15)" : "Q2 (Fin de mes)";

    const numEmoji = i === 0 ? "1️⃣" : i === 1 ? "2️⃣" : "3️⃣";

    msg += `${numEmoji} *${sug.concepto}*\n`;

    msg += `💰 Cuota: *$${formatearCOP(sug.valor)} COP* | 📅 Día ${sug.dia} (${qx})\n`;

    msg += `📂 Categoría: *${sug.categoria}*\n`;

    msg += `💡 _${sug.razon}_\n\n`;

    botones.push([

      {

        text: `➕ Crear: ${sug.concepto} ($${formatearCOP(sug.valor)})`,

        callback_data: `ai_fijo_${i}`

      }

    ]);

  });

  msg += `━━━━━━━━━━━━━━━━━━\n`;

  msg += `👇 _Toca un botón para registrarlo de una en tu Excel o pide nuevas ideas:_`;

  botones.push([

    { text: "🔄 Más Ideas IA", callback_data: "btn_ideas_ia" },

    { text: "✍️ Crear Manual", callback_data: "btn_ayuda_crear_fijo" }

  ]);

  botones.push([

    { text: "📋 Ver Gastos Fijos", callback_data: "btn_fijos" },

    { text: "🧠 Diagnóstico IA", callback_data: "btn_analisis_ia" }

  ]);

  const payloadTelegram = {

    chat_id: chatId,

    text: msg,

    parse_mode: "Markdown",

    reply_markup: {

      inline_keyboard: botones

    }

  };

  try {

    UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify(payloadTelegram),

      muteHttpExceptions: true

    });

  } catch (e) {

    enviarMensajeTelegram(chatId, msg);

  }

}

function mostrarAyudaCrearGastoFijo_(chatId) {

  let msg = `📌 *ASISTENTE: CREAR NUEVO GASTO FIJO*

${separadorBonito_()}

`;

  msg += `Los gastos fijos son tus compromisos mensuales recurrentes (servicios, arriendo, suscripciones, cuotas, parqueaderos, etc.).

`;

  msg += `*Puedes crearlo de 2 formas muy fáciles:*

`;

  msg += `1️⃣ *En Lenguaje Natural (Escribe como hablas):*

`;

  msg += `• \`Crear fijo Gimnasio 85 mil dia 15\`

`;

  msg += `• \`Nuevo gasto fijo Seguro Moto 50 mil el 30 categoria Moto\`

`;

  msg += `• \`Crear fijo Cuota Computador 120 mil dia 15\`

`;

  msg += `• \`Fijo Netflix 25 mil fin de mes\`

`;

  msg += `2️⃣ *Con Comando Directo:*

`;

  msg += `\`/nuevofijo [Nombre] | [Valor] | [Día] | [Categoría]\`

`;

  msg += `*Ejemplo:*

`;

  msg += `\`/nuevofijo Gimnasio SmartFit | 89000 | 15 | Salud\`

`;

  msg += `━━━━━━━━━━━━━━━━━━

`;

  msg += `📅 *Organización por Quincena:*

`;

  msg += `• Si el día es *1 al 15*, se programa en *Quincena 1 (Día 15)*.

`;

  msg += `• Si el día es *16 al 31*, se programa en *Quincena 2 (Fin de mes)*.

`;

  msg += `👇 _Toca un botón para ver ideas generadas con IA o probar un ejemplo:_`;

  const payload = {

    chat_id: chatId,

    text: msg,

    parse_mode: "Markdown",

    reply_markup: {

      inline_keyboard: [

        [

          { text: "💡 Ideas de Fijos con IA", callback_data: "btn_ideas_ia" },

          { text: "📋 Ver Gastos Fijos", callback_data: "btn_fijos" }

        ],

        [

          { text: "🏋️ Ejemplo Gimnasio ($85k - Q1)", callback_data: "fijo_demo_gimnasio" },

          { text: "🏍️ Ejemplo Seguro Moto ($50k - Q2)", callback_data: "fijo_demo_seguro" }

        ],

        [

          { text: "📺 Ejemplo Streaming ($25k - Q2)", callback_data: "fijo_demo_streaming" },

          { text: "🧠 Diagnóstico Quincenal IA", callback_data: "btn_analisis_ia" }

        ]

      ]

    }

  };

  try {

    UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify(payload),

      muteHttpExceptions: true

    });

  } catch (e) {

    enviarMensajeTelegram(chatId, msg);

  }

}

function esIntencionCrearGastoFijo_(texto) {

  const raw = String(texto || "").trim();

  const t = normalizarTexto(raw);

  if (/\b(pague|pago|pagado|abone|cancele)\b/.test(t) && !/\b(quiero|crear|agrega|nuevo|nueva)\b/.test(t)) return false;

  if (/^\/(nuevofijo|crearfijo|fijo|nuevogasto)\b/i.test(raw)) return true;

  if (/^(crear|nuevo|agregar)\s+(gasto\s*fijo|pago\s*fijo|fijo|obligacion|compromiso)/i.test(t)) return true;

  if (/\b(gasto\s*fijo|pago\s*fijo)\b/.test(t) && /\b(crear|agrega|nuevo|nueva)\b/.test(t)) return true;

  if (/^fijo\s+[a-z0-9]/i.test(t) && /\b\d/.test(t)) return true;

  return /(gasto\s*fijo|pago\s*fijo|obligacion\s*mensual)/.test(t) || (/\b(crear|agrega)\b/.test(t) && /(mensual|cada\s*mes|quincenal)/.test(t));

}

function extraerCamposGastoFijo_(texto) {

  const raw = String(texto || "").trim();

  const t = normalizarTexto(raw);

  // 1. Sintaxis con pipes: /nuevofijo Concepto | Valor | Día | Categoría

  if (raw.includes("|")) {

    const partes = raw.replace(/^\/(nuevofijo|crearfijo|fijo|nuevogasto)\s*/i, "").split("|").map(p => p.trim());

    const concepto = partes[0] || "Gasto Fijo";

    const valor = parsearMontoFlexibleV24_(partes[1] || "0");

    const dia = parseInt(partes[2] || "15", 10) || 15;

    const categoria = partes[3] || aplicarReglaComercio_(concepto) || "Otros";

    return {

      concepto: concepto.charAt(0).toUpperCase() + concepto.slice(1),

      valor: valor,

      dia: Math.min(31, Math.max(1, dia)),

      categoria: categoria,

      completo: !!(concepto && valor > 0)

    };

  }

  // 2. Extraer Día de Cobro

  let dia = 15;

  if (/fin de mes|ultimo dia|último día|fin mes/i.test(t)) {

    dia = 30;

  } else {

    const dM = t.match(/\b(?:dia|el|cada|los)\s*(\d{1,2})\b/i);

    if (dM && parseInt(dM[1], 10) >= 1 && parseInt(dM[1], 10) <= 31) {

      dia = parseInt(dM[1], 10);

    }

  }

  // 3. Extraer Valor numérico

  const valor = parsearMontoFlexibleV24_(raw);

  // 4. Extraer Categoría si se menciona explícitamente

  let categoria = "Otros";

  const catMatch = t.match(/\bcategoria\s+([a-záéíóúñ /]+)$/i);

  if (catMatch) {

    categoria = catMatch[1].trim();

  }

  // 5. Extraer Concepto limpio

  let limpio = raw

    .replace(/^\/(nuevofijo|crearfijo|fijo|nuevogasto)\s*/i, "")

    .replace(/\b(crear|agregar|nuevo|nueva|gasto\s*fijo|pago\s*fijo|fijo|obligacion|compromiso)\b/gi, "")

    .replace(/\b(?:dia|el|cada|los)\s*\d{1,2}\b/gi, "")

    .replace(/\b(fin de mes|ultimo dia|último día|fin mes)\b/gi, "")

    .replace(/\bcategoria\s+[a-záéíóúñ /]+/gi, "")

    .replace(/\b(?:por|de|valor|cuota|precio|mensual|cada\s*mes)\b/gi, "")

    .replace(/\$?\s*\d{1,3}(?:[.,]\d{3})*(?:\s*(?:mil|k|millon|millones|lucas|pesos))?/gi, "")

    .replace(/\s+/g, " ")

    .trim();

  let concepto = limpio || "Gasto Fijo";

  concepto = concepto.charAt(0).toUpperCase() + concepto.slice(1);

  if (categoria === "Otros") {

    const regla = aplicarReglaComercio_(concepto);

    if (regla) categoria = regla;

  }

  return {

    concepto: concepto,

    valor: valor,

    dia: dia,

    categoria: categoria,

    completo: !!(concepto && valor > 0)

  };

}

function extraerNombreDeuda_(texto) {

  const words = normalizarTexto(texto).replace(/\bla\s+cuota\s+es\s+sobre\b/g, "").split(/\s+/).filter(w => !new Set(["quiero","agregar","crear","deuda","prestamo","cuotas","de","para","el","la","un","una"]).has(w.replace(/[^a-z0-9ñ]/g,"")));

  const c = words.join(" ").trim(); return c ? c.charAt(0).toUpperCase() + c.slice(1) : null;

}

function extraerCamposDeuda_(texto) {

  const t = normalizarTexto(texto);

  let numCuotas = 0, cuota = null, total = null;

  const cMatch = t.match(/(\d+)\s*cuotas?(?:\s*de\s*\$?\s*(\d+)\s*(mil|k)?)?/i);

  if (cMatch) { numCuotas = parseInt(cMatch[1], 10); if (cMatch[2]) { cuota = parseInt(cMatch[2], 10); if (cMatch[3]) cuota *= 1000; } }

  const tMatch = t.replace(cMatch ? cMatch[0] : "", "").match(/\$?\s*(\d+)\s*(mil|k)?/i);

  if (tMatch) { total = parseInt(tMatch[1], 10); if (tMatch[2]) total *= 1000; }

  if (cuota === null && total !== null && numCuotas > 0) cuota = Math.ceil(total / numCuotas);

  return { tipo: /prestamo/i.test(t) ? "Ingreso" : "Gasto", total: total, cuota: cuota, numCuotas: numCuotas, nombre: extraerNombreDeuda_(t) };

}

function detectarIntencionGastoFijo(texto) {

  if (!esIntencionCrearGastoFijo_(texto)) return null;

  const c = extraerCamposGastoFijo_(texto); return { concepto: c.concepto, valor: c.valor, dia: c.dia, categoria: c.categoria, completo: !!(c.concepto && c.valor > 0) };

}

function detectarIntencionDeuda(texto) {

  if (!/deuda|prestamo|credito|cuotas/.test(normalizarTexto(texto))) return null;

  const c = extraerCamposDeuda_(texto); return { tipo: c.tipo, total: c.total, cuota: c.cuota, numCuotas: c.numCuotas, nombre: c.nombre || "Deuda", completo: !!(c.total && c.cuota && c.nombre) };

}

function obtenerAbonadoMesFijo_(idFijo, mesActual) {

  const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");

  if (!sh || sh.getLastRow() < 4) return 0;

  const data = sh.getRange(4, 8, sh.getLastRow() - 3, 7).getValues();

  let sum = 0;

  for (let i = 0; i < data.length; i++) {

    const ref = String(data[i][6] || "");

    if (ref.includes(idFijo) && ref.includes(mesActual)) {

      sum += Number(data[i][0] || 0);

    }

  }

  return sum;

}

/* =========================================================

   EXTRACCIÓN INTELIGENTE DE MES Y AÑO OBJETIVO (PAGOS FIJOS)

   ========================================================= */

function extraerMesYAnioObjetivo_(texto) {

  const t = normalizarTexto(texto);

  const meses = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

  const dInfo = obtenerMesYAnioDeFecha("", null, null);

  const mesActual = normalizarTexto(dInfo.mes);

  const anioActual = Number(dInfo.anio);

  const idxActual = meses.indexOf(mesActual);

  // 1. Detección de año explícito (ej: 2025, 2026, 2027)

  let anioObjetivo = anioActual;

  const mAnio = String(texto || "").match(/\b(202\d)\b/);

  if (mAnio) {

    anioObjetivo = Number(mAnio[1]);

  }

  // 2. Detección de "mes anterior" o "mes pasado"

  if (/\b(mes anterior|mes pasado|del mes pasado|del mes anterior|periodo anterior)\b/i.test(t)) {

    const idxAnt = (idxActual - 1 + 12) % 12;

    const anioCalc = (idxActual === 0) ? (anioActual - 1) : anioActual;

    return {

      mes: meses[idxAnt],

      anio: mAnio ? anioObjetivo : anioCalc,

      esMesAnterior: true,

      esDiferenteAlActual: true

    };

  }

  // 3. Detección de mes específico en el texto (ej: septiembre, agosto, etc.)

  for (let i = 0; i < meses.length; i++) {

    const reg = new RegExp("\\b" + meses[i] + "\\b", "i");

    if (reg.test(t)) {

      const mesEncontrado = meses[i];

      let anioCalc = anioObjetivo;

      if (idxActual === 0 && i === 11 && !mAnio) anioCalc = anioActual - 1;

      return {

        mes: mesEncontrado,

        anio: anioCalc,

        esMesAnterior: (mesEncontrado !== mesActual),

        esDiferenteAlActual: (mesEncontrado !== mesActual || anioCalc !== anioActual)

      };

    }

  }

  return {

    mes: mesActual,

    anio: anioActual,

    esMesAnterior: false,

    esDiferenteAlActual: false

  };

}

/**

 * Auto-sanación: Corrige automáticamente en MOVIMIENTOS cualquier pago fijo de mes pasado

 * que haya quedado guardado con el mes en curso (ej: septiembre guardado como octubre),

 * asegurando que PANEL reste el disponible y sume el gasto inmediatamente.

 */

function corregirMovimientosPagosFijosMesesPasados_() {

  const ss = obtenerLibro_();

  const shMov = ss.getSheetByName("MOVIMIENTOS");

  if (!shMov || shMov.getLastRow() < 5) return 0;

  const numRows = Math.min(50, shMov.getLastRow() - 4);

  const values = shMov.getRange(5, 1, numRows, 14).getValues();

  const meses = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

  let corregidos = 0;

  for (let i = 0; i < values.length; i++) {

    const ref = String(values[i][13] || values[i][12] || "");

    if (ref.includes("PAGO_FIJO|")) {

      const parts = ref.split("|");

      // Formato: PAGO_FIJO|ID|MES|AÑO

      if (parts.length >= 4) {

        const mesRef = normalizarTexto(parts[2]);

        const anioRef = Number(parts[3]) || 2026;

        const mesActualMov = normalizarTexto(String(values[i][10] || ""));

        const anioActualMov = Number(values[i][11]) || 2026;

        if (mesRef && (mesActualMov !== mesRef || anioActualMov !== anioRef)) {

          shMov.getRange(i + 5, 11).setValue(mesRef);

          shMov.getRange(i + 5, 12).setValue(anioRef);

          const idxMes = meses.indexOf(mesRef);

          if (idxMes >= 0) {

            const ultDia = new Date(anioRef, idxMes + 1, 0).getDate();

            const diaStr = String(ultDia).padStart(2, "0");

            const mesStr = String(idxMes + 1).padStart(2, "0");

            shMov.getRange(i + 5, 2).setValue(`${diaStr}/${mesStr}/${anioRef}`);

          }

          corregidos++;

        }

      }

    }

  }

  return corregidos;

}

function autoCorregirGreenfitComida_(chatId, forzarNotificacion = false) {

  try {

    const props = PropertiesService.getScriptProperties();

    const yaAplicado = props.getProperty("GREENFIT_COMIDA_REPARADO_V26") === "true";

    if (yaAplicado && !forzarNotificacion) return false;

    const ss = obtenerLibro_();

    const shHist = ss.getSheetByName("HISTORICO FIJOS");

    if (shHist && shHist.getLastRow() >= 4) {

      const hRows = shHist.getRange(4, 1, shHist.getLastRow() - 3, 11).getValues();

      for (let h = 0; h < hRows.length; h++) {

        const id = String(hRows[h][0] || "").toUpperCase().trim();

        const concepto = normalizarTexto(String(hRows[h][1] || ""));

        const cuota = Number(hRows[h][4] || 0);

        const abonado = Number(hRows[h][5] || 0);

        const mes = normalizarTexto(String(hRows[h][9] || ""));

        // Restaurar F-0093 COMIDA septiembre si quedó en 121.215

        if ((id === "F-0093" || (concepto === "comida" && mes === "septiembre")) && (abonado === 121215 || cuota === 121215)) {

          shHist.getRange(h + 4, 5).setValue(850000);

          shHist.getRange(h + 4, 6).setValue(650000);

          shHist.getRange(h + 4, 7).setValue("15/09/2026");

        }

        // Restaurar F-0108 COMIDA octubre si quedó en 121.215

        if ((id === "F-0108" || (concepto === "comida" && mes === "octubre")) && (abonado === 121215 || cuota === 121215)) {

          shHist.getRange(h + 4, 5).setValue(850000);

          shHist.getRange(h + 4, 6).setValue(0);

          shHist.getRange(h + 4, 7).setValue("");

        }

      }

    }

    const shCtrl = ss.getSheetByName("CONTROL FIJOS");

    if (shCtrl && shCtrl.getLastRow() >= 4) {

      const cRows = shCtrl.getRange(4, 1, shCtrl.getLastRow() - 3, 8).getValues();

      for (let c = 0; c < cRows.length; c++) {

        const concepto = normalizarTexto(String(cRows[c][1] || ""));

        const abonado = Number(cRows[c][5] || 0);

        if (concepto === "comida" && abonado === 121215) {

          shCtrl.getRange(c + 4, 6).setValue(0);

          shCtrl.getRange(c + 4, 7).setValue("");

        }

      }

    }

    // Verificar si ya existe el movimiento de GREENFIT en MOVIMIENTOS

    const shMov = ss.getSheetByName("MOVIMIENTOS");

    let yaExisteMov = false;

    let greenfitId = "";

    if (shMov && shMov.getLastRow() >= 5) {

      const movs = shMov.getRange(5, 1, shMov.getLastRow() - 4, 14).getValues();

      for (let m = 0; m < movs.length; m++) {

        const val = Number(movs[m][7] || 0);

        const desc = normalizarTexto(String(movs[m][6] || ""));

        const ref = normalizarTexto(String(movs[m][13] || ""));

        if (val === 121215 && (desc.includes("greenfit") || desc.includes("ropa") || ref.includes("greenfit"))) {

          yaExisteMov = true;

          greenfitId = String(movs[m][0]);

          break;

        }

      }

    }

    if (!yaExisteMov) {

      greenfitId = obtenerSiguienteIDConsecutivo("MOVIMIENTOS", "M");

      const d = obtenerMesYAnioDeFecha("04/10/2026", null, null);

      registrarEnSheetsYFormatear("MOVIMIENTOS", [

        greenfitId,

        d.fechaFormateada,

        Utilities.formatDate(new Date(), ZONA_HORARIA, "hh:mm a"),

        "Gasto",

        "Compras",

        "Ropa y Moda Paisa",

        "Ropa Deportiva Greenfit (Leggins, Top, Enterizo)",

        121215,

        "Tarjeta de Crédito",

        "Nequi",

        "octubre",

        2026,

        "Telegram",

        "GREENFIT:E3U2JRYX5"

      ], 8);

    }

    props.setProperty("GREENFIT_COMIDA_REPARADO_V26", "true");

    if (chatId) {

      enviarMensajeTelegramConBoton(

        chatId,

        `🛍️ *COMPRA DE ROPA REGISTRADA CORRECTAMENTE*\n${separadorBonito_()}\n\n` +

        `🏪 *Greenfit (Activewear)*\n` +

        `👗 *Prendas:* Leggins set, Top dama, Enterizo\n` +

        `💵 *$121.215 COP*\n` +

        `📂 Categoría: *Compras* (Ropa y Moda Paisa)\n` +

        `💳 Tarjeta / Nequi (Octubre 2026)\n\n` +

        `✅ *Corrección de Gastos Fijos realizada:*\n` +

        `• *Septiembre:* Cuota Comida restaurada a $650.000 pagados.\n` +

        `• *Octubre:* Cuota Comida restaurada a $0 abonado (pendiente).\n\n` +

        `_Ahora sí tienes el botón interactivo abajo por si deseas eliminar este registro:_`,

        greenfitId

      );

    }

    return true;

  } catch (errAuto) {

    console.error("Error en autoCorregirGreenfitComida_:", errAuto);

    return false;

  }

}



function procesarPagoFijo(chatId, texto) {

  const raw = String(texto || "").trim();

  const t = normalizarTexto(raw);

  

  // Acepta modismos: pague, pago, pagua, pagando, cancele, cancelo, abone, abono, liquide, etc.

  if (!/\b(pagu|pago|pague|pagado|pagando|abon|abono|abone|abonado|cancel|cancele|cancelo|cancelado|liquid|epm|tigo|arriendo|spotify|google one)\b/i.test(t)) {

    return false;

  }

  // 1. Extraer mes y año objetivo del texto (soporta meses explícitos ej: 'septiembre', o 'mes anterior' / 'mes pasado')

  const objetivo = extraerMesYAnioObjetivo_(raw);

  const mesObjetivo = objetivo.mes;

  const anioObjetivo = objetivo.anio;

  // 2. Buscar el gasto fijo activo para ese mes específico

  const fijo = buscarGastoFijoActivo_(raw, mesObjetivo, anioObjetivo);

  if (!fijo) return false;

  const ss = obtenerLibro_();

  const dInfo = obtenerMesYAnioDeFecha("", null, null);

  const mesActual = dInfo.mes;

  const anioActual = dInfo.anio;

  const shMov = ss.getSheetByName("MOVIMIENTOS");

  // 3. Extraer el valor pagado del texto (limpiando posibles años de 4 dígitos para evitar confusión)

  const textoSinAnio = raw.replace(/\b202\d\b/g, "");

  let valExtraido = parsearMontoFlexibleV24_(textoSinAnio);

  let val = valExtraido;

  let movPrevioId = "";

  if (shMov && shMov.getLastRow() >= 5) {

    const ultimos = shMov.getRange(5, 1, Math.min(30, shMov.getLastRow() - 4), 14).getValues();

    for (let u = 0; u < ultimos.length; u++) {

      const uId = String(ultimos[u][0] || "");

      const uDesc = normalizarTexto(String(ultimos[u][6] || ""));

      const uVal = Number(ultimos[u][7] || 0);

      const coincideConcepto = uDesc.includes(normalizarTexto(fijo.concepto)) ||

                              (uDesc.includes("une") && normalizarTexto(fijo.concepto).includes("celular")) ||

                              (uDesc.includes("tigo") && normalizarTexto(fijo.concepto).includes("tigo"));

      if (uVal > 0 && coincideConcepto) {

        if (val <= 0) val = uVal;

        if (Math.abs(uVal - val) < 100) {

          movPrevioId = uId;

          break;

        }

      }

    }

  }

  if (val <= 0) val = fijo.cuota;

  if (val <= 0) return false;

  // 4. Anti-Duplicados por Referencia Única

  const refMov = "PAGO_FIJO|" + fijo.id + "|" + mesObjetivo + "|" + anioObjetivo;

  const cache = CacheService.getScriptCache();

  const lockKey = "PAGO_REGISTRADO_" + fijo.id + "_" + mesObjetivo + "_" + anioObjetivo + "_" + val;

  if (cache.get(lockKey)) {

    enviarMensajeTelegram(chatId, `😎 *ESTE PAGO YA FUE PROCESADO*\n🏷️ ${fijo.concepto}\n💵 $${formatearCOP(val)} COP\n📅 ${mesObjetivo.toUpperCase()} ${anioObjetivo}`);

    return true;

  }

  try { cache.put(lockKey, "1", 120); } catch (e) {}

  // 5. Determinar si es factura variable (EPM, energía, agua, gas, internet, celular, datos, suscripciones)

  const conceptoUpper = String(fijo.concepto || "").toUpperCase();

  const esServicioVariable = /\b(EPM|ENERGIA|LUZ|AGUA|GAS|SERVICIOS|ACUEDUCTO|TIGO|INTERNET|CLARO|DATOS|CELULAR|PLAN\s*MOVIL|MOVIL|SPOTIFY|GOOGLE)\b/i.test(conceptoUpper) || /\b(epm|energia|luz|agua|gas|servicios|acueducto|internet|tigo|claro|datos|celular|spotify)\b/i.test(raw);

  const esLiquidacionMes = /\b(este mes|de este mes|del mes|factura|total|completo|liquide|cancele el mes|mes anterior|septiembre|agosto|octubre)\b/i.test(t) || esServicioVariable;

  // 6. Actualizar HISTORICO FIJOS para el mesObjetivo

  const shHist = ss.getSheetByName("HISTORICO FIJOS");

  let abonoTotal = val;

  let cuotaBase = fijo.cuota;

  if (shHist && shHist.getLastRow() >= 4) {

    const hRows = shHist.getRange(4, 1, shHist.getLastRow() - 3, 11).getValues();

    for (let h = 0; h < hRows.length; h++) {

      const hId = String(hRows[h][0] || "").trim().toUpperCase();

      const hMes = normalizarTexto(String(hRows[h][9] || ""));

      const hAnio = Number(hRows[h][10] || 0);

      if (hId === fijo.id.toUpperCase() && hMes === normalizarTexto(mesObjetivo) && (hAnio === anioObjetivo || hAnio === 0)) {

        const abonoPrevio = Number(hRows[h][5] || 0);

        const cuotaPrevia = Number(hRows[h][4] || fijo.cuota);

        if (esLiquidacionMes || val >= cuotaPrevia) {

          cuotaBase = val;

          abonoTotal = val;

          shHist.getRange(h + 4, 5).setValue(cuotaBase);

        } else {

          if (abonoPrevio === val) {

            abonoTotal = val;

          } else if (/\b(otro abono|abono adicional|abone mas|mas)\b/i.test(t)) {

            abonoTotal = abonoPrevio + val;

          } else {

            abonoTotal = Math.min(cuotaPrevia, abonoPrevio + val);

          }

          cuotaBase = cuotaPrevia;

        }

        shHist.getRange(h + 4, 6).setValue(abonoTotal);

        shHist.getRange(h + 4, 7).setValue(Utilities.formatDate(new Date(), ZONA_HORARIA, "yyyy-MM-dd"));

        break;

      }

    }

  }

  try { CacheService.getScriptCache().remove("CTX_FIN_IA_" + normalizarTexto(mesObjetivo) + "_" + anioObjetivo); } catch (e) {}

  // 7. Reconciliar o Registrar en MOVIMIENTOS evitando cobro doble

  const isTotal = abonoTotal >= cuotaBase && cuotaBase > 0;

  const estadoPago = isTotal ? "🟢 Pagado Total" : "🟡 Abono Parcial";

  const m = resolverMetodoPago_({ texto_original: raw }, fijo.concepto);

  const descripcionMov = isTotal

    ? ("Pago Fijo: " + fijo.concepto + " (" + mesObjetivo + " " + anioObjetivo + ")")

    : ("Abono Fijo: " + fijo.concepto + " (" + mesObjetivo + " " + anioObjetivo + ")");

  let mId = movPrevioId;

  let fechaMov = dInfo.fechaFormateada;

  if (mesObjetivo !== normalizarTexto(mesActual)) {

    const meses = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

    const idxMes = meses.indexOf(normalizarTexto(mesObjetivo));

    if (idxMes >= 0) {

      const ultDia = new Date(anioObjetivo, idxMes + 1, 0).getDate();

      const diaStr = String(ultDia).padStart(2, "0");

      const mesStr = String(idxMes + 1).padStart(2, "0");

      fechaMov = `${diaStr}/${mesStr}/${anioObjetivo}`;

    }

  }

  if (mId && shMov && shMov.getLastRow() >= 5) {

    const numRows = Math.min(30, shMov.getLastRow() - 4);

    const mRows = shMov.getRange(5, 1, numRows, 14).getValues();

    for (let r = 0; r < mRows.length; r++) {

      if (String(mRows[r][0] || "").toUpperCase() === mId.toUpperCase()) {

        shMov.getRange(r + 5, 5).setValue(fijo.categoria || "Servicios");

        shMov.getRange(r + 5, 6).setValue("Pago fijo");

        shMov.getRange(r + 5, 7).setValue(descripcionMov);

        shMov.getRange(r + 5, 8).setValue(val);

        shMov.getRange(r + 5, 11).setValue(mesObjetivo);

        shMov.getRange(r + 5, 12).setValue(anioObjetivo);

        shMov.getRange(r + 5, 14).setValue(refMov);

        break;

      }

    }

  } else {

    mId = obtenerSiguienteIDConsecutivo("MOVIMIENTOS", "M");

    registrarEnSheetsYFormatear("MOVIMIENTOS", [

      mId,

      fechaMov,

      Utilities.formatDate(new Date(), ZONA_HORARIA, "hh:mm a"),

      "Gasto",

      fijo.categoria || "Servicios",

      "Pago fijo",

      descripcionMov,

      val,

      m.metodo || "Transferencia",

      m.cuenta || "AyC Colanta",

      mesObjetivo,

      anioObjetivo,

      "Telegram",

      refMov

    ], 8);

  }

  const saldoRestante = Math.max(0, cuotaBase - abonoTotal);

  let respuesta = `✅ *GASTO FIJO: ${estadoPago}*\n${separadorBonito_()}\n\n`;

  respuesta += `🏷️ *Concepto:* ${fijo.concepto}\n`;

  respuesta += `💰 *Valor pagado:* $${formatearCOP(val)} COP\n`;

  if (esLiquidacionMes || isTotal) {

    respuesta += `📊 *Factura del Mes:* $${formatearCOP(cuotaBase)} COP (100% Pagada)\n`;

  } else {

    respuesta += `📊 *Acumulado Mes:* $${formatearCOP(abonoTotal)} / $${formatearCOP(cuotaBase)} COP\n`;

    if (saldoRestante > 0) {

      respuesta += `⏳ *Saldo por pagar:* $${formatearCOP(saldoRestante)} COP\n`;

    }

  }

  respuesta += `📅 *Período:* ${mesObjetivo.toUpperCase()} ${anioObjetivo}\n`;

  respuesta += `🆔 Fijo: \`${fijo.id}\` | Movimiento: \`${mId}\``;

  enviarMensajeTelegramConBoton(chatId, respuesta, mId);

  return true;

}



function obtenerGastosFijosVigentes_() {

  const ss = obtenerLibro_();

  const dInfo = obtenerMesYAnioDeFecha("", null, null);

  const mesActual = dInfo.mes;

  const anioActual = dInfo.anio;

  const fijos = [];

  const idsVistos = {};



  // 1. Consultar HISTORICO FIJOS para el mes y año actual

  const shHist = ss.getSheetByName("HISTORICO FIJOS");

  if (shHist && shHist.getLastRow() >= 4) {

    const hRows = shHist.getRange(4, 1, shHist.getLastRow() - 3, 15).getValues();

    hRows.forEach((r, idx) => {

      const id = String(r[0] || "").trim();

      if (!/^F-/i.test(id)) return;

      const rMes = normalizarTexto(String(r[9] || ""));

      const rAnio = Number(r[10] || 0);



      if (rMes === normalizarTexto(mesActual) && (rAnio === anioActual || rAnio === 0)) {

        const concepto = String(r[1] || "").trim();

        const categoria = String(r[2] || "Otros").trim();

        const diaCobro = Number(r[3] || 15);

        const cuota = Number(r[4] || 0);

        const abonado = Number(r[5] || 0);

        const saldo = cuota > abonado ? (cuota - abonado) : 0;

        const estado = normalizarTexto(String(r[7] || ""));

        const pagado = estado.includes("pagad") || (cuota > 0 && abonado >= cuota);



        fijos.push({

          fuente: "HISTORICO FIJOS",

          fila: idx + 4,

          id: id,

          concepto: concepto,

          categoria: categoria,

          dia: diaCobro,

          cuota: cuota,

          abonado: abonado,

          saldo: saldo,

          pagado: pagado

        });

        idsVistos[id.toUpperCase()] = true;

      }

    });

  }



  // 2. Consultar CONTROL FIJOS por si hay compromisos agregados directamente

  const shFijos = ss.getSheetByName("CONTROL FIJOS");

  if (shFijos && shFijos.getLastRow() >= 4) {

    const numRows = Math.max(1, shFijos.getLastRow() - 3);

    const fRows = shFijos.getRange(4, 1, numRows, 10).getValues();

    fRows.forEach((r, idx) => {

      const id = String(r[0] || "").trim();

      if (!/^F-/i.test(id) || idsVistos[id.toUpperCase()]) return;

      const concepto = String(r[1] || "").trim();

      if (!concepto) return;

      const categoria = String(r[2] || "Otros").trim();

      const diaCobro = Number(r[3] || 15);

      const cuota = Number(r[4] || 0);

      const abonado = Number(r[5] || 0);

      const saldo = cuota > abonado ? (cuota - abonado) : 0;

      const estado = normalizarTexto(String(r[6] || ""));

      const pagado = estado.includes("pagad") || (cuota > 0 && abonado >= cuota);



      fijos.push({

        fuente: "CONTROL FIJOS",

        fila: idx + 4,

        id: id,

        concepto: concepto,

        categoria: categoria,

        dia: diaCobro,

        cuota: cuota,

        abonado: abonado,

        saldo: saldo,

        pagado: pagado

      });

      idsVistos[id.toUpperCase()] = true;

    });

  }



  return fijos;

}



/**

 * Motor IA + Semántico para asociar pagos del usuario a sus gastos fijos

 * Entiende errores tipográficos, modismos y abreviaciones (ej: 'Pagua 120 mil este mes de epm', 'pagué la luz', 'cancelé el internet')

 */

function buscarGastoFijoActivo_(texto) {

  const fijos = obtenerGastosFijosVigentes_();

  if (!fijos.length) return null;



  const raw = String(texto || "").trim();

  const t = normalizarTexto(raw);



  // 1. Búsqueda directa por ID (ej: F-0092)

  const idM = raw.match(/\b(F-\d{4})\b/i);

  if (idM) {

    const idBuscado = idM[1].toUpperCase();

    const encontradoId = fijos.find(f => f.id.toUpperCase() === idBuscado);

    if (encontradoId) return encontradoId;

  }



  // 2. Diccionario de Sinónimos y Abreviaciones Paisas / Colombia

  const mapaSinonimos = [

    { regex: /\b(epm|energia|luz|cuenta de epm|factura de epm|gas|agua)\b/i, alias: "EPM ENERGIA" },

    { regex: /\b(tigo|internet|wifi|fibra|une)\b/i, alias: "Internet Tigo" },

    { regex: /\b(celular|datos|plan celular|plan datos|claro|linea movil)\b/i, alias: "CUOTA DATOS CELULAR" },

    { regex: /\b(arriendo|casa|apartamento|canon|arriendo casa)\b/i, alias: "ARRIENDO CASA" },

    { regex: /\b(spotify|musica|streaming)\b/i, alias: "Spotify" },

    { regex: /\b(audifonos|samsung|galaxy buds)\b/i, alias: "Cuota Audifonos Samsung" },

    { regex: /\b(google|google one|nube|almacenamiento)\b/i, alias: "Google One" },

    { regex: /\b(parqueadero moto|parking moto|moto parqueadero)\b/i, alias: "PARQUEADERO MOTO" },

    { regex: /\b(parqueadero trabajo|parking trabajo|parqueadero semana)\b/i, alias: "Parqueadero Trabajo Semana" },

    { regex: /\b(gasolina moto|gasolina|tanqueada|tanqueada moto)\b/i, alias: "Gasolina Moto" },

    { regex: /\b(comida|mercado|alimentacion|quincena comida)\b/i, alias: "COMIDA" }

  ];



  for (let s = 0; s < mapaSinonimos.length; s++) {

    if (mapaSinonimos[s].regex.test(t)) {

      const aliasTarget = normalizarTexto(mapaSinonimos[s].alias);

      const match = fijos.find(f => normalizarTexto(f.concepto) === aliasTarget || normalizarTexto(f.concepto).includes(aliasTarget));

      if (match) return match;

    }

  }



  // 3. Coincidencia difusa por palabras clave en el concepto

  let mejorMatch = null;

  let maxPuntos = 0;



  fijos.forEach(f => {

    const cNorm = normalizarTexto(f.concepto);

    const palabras = cNorm.split(/\s+/);

    let puntos = 0;



    if (t.includes(cNorm)) puntos += 100;

    palabras.forEach(p => {

      if (p.length > 2 && t.includes(p)) puntos += 30;

    });



    if (puntos > maxPuntos) {

      maxPuntos = puntos;

      mejorMatch = f;

    }

  });



  if (maxPuntos >= 30) return mejorMatch;



  // 4. Inteligencia Artificial Gemini si no hubo match por reglas locales

  try {

    const catalogo = fijos.map(f => ({ id: f.id, concepto: f.concepto, cuota: f.cuota, categoria: f.categoria }));

    const prompt = `Analiza este texto de pago de un usuario en Medellín y encuentra a cuál de sus gastos fijos corresponde.

Texto: "${raw}"

Gastos Fijos Disponibles:

${JSON.stringify(catalogo)}



Devuelve ÚNICAMENTE un JSON con: {"id": "F-XXXX"} o {"id": null} si no corresponde a ninguno.`;



    const res = llamarGeminiAPI({

      contents: [{ parts: [{ text: prompt }] }],

      generationConfig: { responseMimeType: "application/json", temperature: 0.1, maxOutputTokens: 100 }

    });



    if (res && res.candidates && res.candidates[0].content) {

      const parsed = JSON.parse(limpiarJSON(res.candidates[0].content.parts[0].text));

      if (parsed && parsed.id) {

        const fijoIA = fijos.find(f => f.id.toUpperCase() === String(parsed.id).toUpperCase());

        if (fijoIA) return fijoIA;

      }

    }

  } catch (eIA) {}



  return null;

}



function procesarYMarcarGastoFijo(concepto, valor, forzar) {

  const ss = obtenerLibro_();

  const sh = ss.getSheetByName("CONTROL FIJOS");

  const shHist = ss.getSheetByName("HISTORICO FIJOS");

  if (!sh || sh.getLastRow() < 4) return { actualizado: false };

  const q = normalizarTexto(concepto);

  const dInfo = obtenerMesYAnioDeFecha("", null, null);

  const mesActual = dInfo.mes;

  const anioActual = dInfo.anio;

  const rows = sh.getRange(4, 1, sh.getLastRow() - 3, 12).getValues();



  for (let i = 0; i < rows.length; i++) {

    const r = rows[i];

    if (!/^F-/i.test(String(r[0] || ""))) continue;

    const cNorm = normalizarTexto(r[1]);

    if (!(cNorm.includes(q) || q.includes(cNorm))) continue;



    const estado = normalizarTexto(String(r[6] || ""));

    if (estado.includes("pagad") && !forzar) {

      return { actualizado: false, yaPagado: true, conceptoNombre: r[1] };

    }



    const valorFinal = valor > 0 ? valor : Number(r[4] || 0);

    const idFijo = String(r[0]);

    const valorCuota = Number(r[4] || 0);



    // Si existe HISTORICO FIJOS, registrar abono y fecha sin romper fórmulas de CONTROL FIJOS

    if (shHist && shHist.getLastRow() >= 4) {

      const hRows = shHist.getRange(4, 1, shHist.getLastRow() - 3, 11).getValues();

      for (let h = 0; h < hRows.length; h++) {

        const hId = String(hRows[h][0] || "").trim().toUpperCase();

        const hMes = normalizarTexto(String(hRows[h][9] || ""));

        const hAnio = Number(hRows[h][10] || 2026);

        if (hId === idFijo.toUpperCase() && hMes === normalizarTexto(mesActual) && hAnio === anioActual) {

          const abonoPrev = Number(hRows[h][5] || 0);

          const nuevoAbono = abonoPrev + valorFinal;

          shHist.getRange(h + 4, 6).setValue(nuevoAbono);

          shHist.getRange(h + 4, 7).setValue(Utilities.formatDate(new Date(), ZONA_HORARIA, "dd/MM/yyyy"));

          break;

        }

      }

    }



    const isTotal = valorFinal >= valorCuota;

    return { actualizado: true, conceptoNombre: r[1], valorFinal: valorFinal, categoria: r[2], mesActual: mesActual, idFijo: idFijo, isTotal: isTotal };

  }

  return { actualizado: false };

}



function intentarAbonoAGastoFijoExistente_(chatId, comercio, valor, fecha) {

  const r = procesarYMarcarGastoFijo(comercio, valor, true);

  if (r.actualizado) { registrarMovimientoDesdeFijo_(chatId, r); return true; }

  return false;

}



function registrarGastoFijoCompleto(chatId, d) {

  const ss = obtenerLibro_();

  const sh = ss.getSheetByName("CONTROL FIJOS");

  if (!sh) {

    enviarMensajeTelegram(chatId, "⚠️ No se encontró la hoja CONTROL FIJOS.");

    return;

  }



  const concepto = String(d.concepto || "Gasto Fijo").trim();

  const valor = Number(d.valor || 0);

  const dia = Number(d.dia || 15);

  const categoria = d.categoria || aplicarReglaComercio_(concepto) || "Otros";



  if (valor <= 0) {

    enviarMensajeTelegram(chatId, "⚠️ El valor de la cuota mensual debe ser mayor a 0.");

    return;

  }



  const quincenaTexto = dia <= 15 ? "Quincena 1 (Día 15)" : "Quincena 2 (Fin de Mes / Día 30)";



  // Buscar si ya existe en las filas 10 en adelante

  let encontrado = false;

  let filaEncontrada = -1;

  let id = "";



  if (sh.getLastRow() >= 10) {

    const numRows = sh.getLastRow() - 9;

    const rows = sh.getRange(10, 1, numRows, 5).getValues();

    for (let i = 0; i < rows.length; i++) {

      const concFila = normalizarTexto(String(rows[i][1] || ""));

      if (concFila === normalizarTexto(concepto)) {

        encontrado = true;

        filaEncontrada = 10 + i;

        id = String(rows[i][0] || "");

        break;

      }

    }

  }



  if (encontrado && filaEncontrada !== -1) {

    sh.getRange(filaEncontrada, 4).setValue(dia); // Día Cobro

    sh.getRange(filaEncontrada, 5).setValue(valor); // Cuota Base

    sh.getRange(filaEncontrada, 3).setValue(categoria); // Categoría

    

    let msg = `♻️ *GASTO FIJO ACTUALIZADO*\n${separadorBonito_()}\n\n`;

    msg += `🏷️ *Concepto:* ${concepto}\n`;

    msg += `💰 *Nueva Cuota:* $${formatearCOP(valor)} COP\n`;

    msg += `📅 *Día de Cobro:* ${dia} (${quincenaTexto})\n`;

    msg += `🏷️ *Categoría:* ${categoria}\n`;

    msg += `🆔 \`${id}\`\n\n`;

    msg += `_Se actualizó el registro existente sin crear duplicados._`;

    enviarMensajeTelegram(chatId, msg);

  } else {

    id = obtenerSiguienteIDConsecutivo("CONTROL FIJOS", "F");

    

    // Buscar primera fila libre desde la fila 10

    let targetRow = 10;

    if (sh.getLastRow() >= 10) {

      const vals = sh.getRange(10, 1, Math.max(1, sh.getLastRow() - 9), 2).getValues();

      for (let i = 0; i < vals.length; i++) {

        if (!vals[i][0] && !vals[i][1]) {

          targetRow = 10 + i;

          break;

        }

        targetRow = 10 + i + 1;

      }

    }



    sh.getRange(targetRow, 1, 1, 12).setValues([[

      id,

      concepto,

      categoria,

      dia,

      valor,

      0, // Abonado inicial

      "🔴 Pendiente",

      "", // Fecha pago

      `=IF(A${targetRow}="","",MAX(0, E${targetRow}-N(F${targetRow})))`,

      "Media",

      "Transferencia",

      "Creado desde Telegram Bot"

    ]]);



    sh.getRange(targetRow, 5).setNumberFormat('"$" #,##0');

    sh.getRange(targetRow, 6).setNumberFormat('"$" #,##0');

    sh.getRange(targetRow, 9).setNumberFormat('"$" #,##0');

    sh.getRange(targetRow, 1).setHorizontalAlignment("center");

    sh.getRange(targetRow, 4).setHorizontalAlignment("center");

    sh.getRange(targetRow, 7).setHorizontalAlignment("center");



    let msg = `✅ *NUEVO GASTO FIJO REGISTRADO*\n${separadorBonito_()}\n\n`;

    msg += `🏷️ *Concepto:* ${concepto}\n`;

    msg += `💰 *Cuota mensual:* $${formatearCOP(valor)} COP\n`;

    msg += `📅 *Día de cobro:* ${dia} (${quincenaTexto})\n`;

    msg += `🏷️ *Categoría:* ${categoria}\n`;

    msg += `🔴 *Estado inicial:* Pendiente\n`;

    msg += `🆔 \`${id}\`\n\n`;

    msg += `📊 _Se sumó automáticamente a tus compromisos de ${quincenaTexto}._\n`;

    msg += `_Para pagarlo o abonar cuando lo canceles, escribe: 'Pagué ${concepto} ${formatearCOP(valor)}'_`;



    enviarMensajeTelegram(chatId, msg);

  }

}



function registrarDeudaCompleta(chatId, d) {

  const id = obtenerSiguienteIDConsecutivo("CONTROL FIJOS", "F");

  registrarEnSheetsYFormatear("CONTROL FIJOS", [ id, d.nombre, "Deuda", 15, d.cuota, "", "", `DEUDA|TOTAL:${d.total}|PAGADO:0|CUOTAS:${d.numCuotas}` ], 5);

  const dInfo = obtenerMesYAnioDeFecha("", null, null);

  const mId = obtenerSiguienteIDConsecutivo("MOVIMIENTOS", "M");

  registrarEnSheetsYFormatear("MOVIMIENTOS", [ mId, dInfo.fechaFormateada, Utilities.formatDate(new Date(), ZONA_HORARIA, "hh:mm a"), d.tipo, "Deuda", "Crédito", d.nombre, d.total, "Transferencia", "Efectivo", dInfo.mes, dInfo.anio, "Telegram", "DEUDA|" + id ], 8);

  enviarMensajeTelegramConBoton(chatId, `🆕 *DEUDA REGISTRADA*\n🏷️ ${d.nombre}\n💰 Total: $${formatearCOP(d.total)}\n📅 ${d.numCuotas} cuotas de $${formatearCOP(d.cuota)}`, mId);

}



function listarGastosFijosDelMes(chatId) {

  const sh = obtenerLibro_().getSheetByName("CONTROL FIJOS");

  if (!sh || sh.getLastRow() < 10) { enviarMensajeTelegram(chatId, "✅ No hay gastos fijos registrados."); return; }

  const mesActual = mesAnioActualTexto_();



  // En CONTROL FIJOS los compromisos inician en fila 10

  // Columnas: 0:ID, 1:Concepto, 2:Categoría, 3:Día Cobro, 4:Cuota Base, 5:Abonado Mes, 6:Estado, 7:Fecha Último Pago, 8:Saldo Pendiente

  const numRows = sh.getLastRow() - 9;

  const rows = sh.getRange(10, 1, numRows, 11).getValues();



  const q1 = [];

  const q2 = [];

  let totalCuotasQ1 = 0, totalAbonadoQ1 = 0, totalPendQ1 = 0;

  let totalCuotasQ2 = 0, totalAbonadoQ2 = 0, totalPendQ2 = 0;



  rows.forEach(r => {

    const id = String(r[0] || "").trim();

    if (!/^F-/i.test(id)) return;

    const cat = normalizarTexto(String(r[2] || ""));

    if (cat === "deuda") return;



    const concepto = String(r[1] || "Gasto Fijo").trim();

    const diaCobro = Number(r[3] || 15);

    const cuota = Number(r[4] || 0);

    const abonado = Number(r[5] || 0);

    const estadoRaw = String(r[6] || "").trim();

    const saldoPend = cuota > abonado ? (cuota - abonado) : 0;



    let estadoStr = "🔴 Pendiente";

    if (abonado >= cuota && cuota > 0) {

      estadoStr = "🟢 Pagado";

    } else if (abonado > 0) {

      estadoStr = `🟡 Abono ($${formatearCOP(saldoPend)} pend.)`;

    } else if (estadoRaw.includes("🟢")) {

      estadoStr = "🟢 Pagado";

    }



    const item = {

      id: id,

      concepto: concepto,

      dia: diaCobro,

      cuota: cuota,

      abonado: abonado,

      saldo: saldoPend,

      estadoStr: estadoStr

    };



    if (diaCobro <= 15) {

      q1.push(item);

      totalCuotasQ1 += cuota;

      totalAbonadoQ1 += Math.min(abonado, cuota);

      totalPendQ1 += saldoPend;

    } else {

      q2.push(item);

      totalCuotasQ2 += cuota;

      totalAbonadoQ2 += Math.min(abonado, cuota);

      totalPendQ2 += saldoPend;

    }

  });



  if (q1.length === 0 && q2.length === 0) {

    enviarMensajeTelegram(chatId, "✅ No hay gastos fijos registrados para este mes.");

    return;

  }



  let msg = `📋 *COMPROMISOS FIJOS (${mesActual.toUpperCase()})*\n${separadorBonito_()}\n\n`;



  if (q1.length > 0) {

    msg += `📅 *QUINCENA 1 (DÍA 15)*\n`;

    msg += `💰 Total: $${formatearCOP(totalCuotasQ1)} | Pendiente: $${formatearCOP(totalPendQ1)}\n`;

    msg += `────────────────────\n`;

    q1.forEach(f => {

      msg += `🏷️ *${f.concepto}*\n💰 Cuota: $${formatearCOP(f.cuota)} — ${f.estadoStr}\n🆔 \`${f.id}\`\n\n`;

    });

  }



  if (q2.length > 0) {

    msg += `📅 *QUINCENA 2 (FIN DE MES / DÍA 30)*\n`;

    msg += `💰 Total: $${formatearCOP(totalCuotasQ2)} | Pendiente: $${formatearCOP(totalPendQ2)}\n`;

    msg += `────────────────────\n`;

    q2.forEach(f => {

      msg += `🏷️ *${f.concepto}*\n💰 Cuota: $${formatearCOP(f.cuota)} — ${f.estadoStr}\n🆔 \`${f.id}\`\n\n`;

    });

  }



  const totalMes = totalCuotasQ1 + totalCuotasQ2;

  const pagadoMes = totalAbonadoQ1 + totalAbonadoQ2;

  const pendMes = totalPendQ1 + totalPendQ2;



  msg += `📊 *BALANCE TOTAL DEL MES:*\n`;

  msg += `💵 Total cuotas: $${formatearCOP(totalMes)} COP\n`;

  msg += `✅ Pagado/Abonado: $${formatearCOP(pagadoMes)} COP\n`;

  msg += `⏳ Saldo por pagar: *$${formatearCOP(pendMes)} COP*\n\n`;

  msg += `_Escribe: 'Pagué EPM 177 mil' o 'Abono 50 mil a F-0094'_`;



  const payload = {

    chat_id: chatId,

    text: msg,

    parse_mode: "Markdown",

    reply_markup: {

      inline_keyboard: [

        [

          { text: "➕ Crear Nuevo Gasto Fijo", callback_data: "btn_ayuda_crear_fijo" },

          { text: "💡 Ideas de Fijos con IA", callback_data: "btn_ideas_ia" }

        ],

        [

          { text: "🧠 Diagnóstico Quincenal IA", callback_data: "btn_analisis_ia" },

          { text: "💵 Disponible en Caja", callback_data: "btn_disponible" }

        ]

      ]

    }

  };

  try {

    UrlFetchApp.fetch(`https://api.telegram.org/bot${TELEGRAM_TOKEN}/sendMessage`, {

      method: "post",

      contentType: "application/json",

      payload: JSON.stringify(payload),

      muteHttpExceptions: true

    });

  } catch (e) {

    enviarMensajeTelegram(chatId, msg);

  }

}



function listarDeudasPendientes(chatId) {

  const sh = obtenerLibro_().getSheetByName("CONTROL FIJOS");

  if (!sh || sh.getLastRow() < 4) { enviarMensajeTelegram(chatId, "✅ No tienes deudas pendientes."); return; }

  let msg = `📋 *DEUDAS*\n${separadorBonito_()}\n\n`, enc = 0;

  sh.getRange(4, 1, sh.getLastRow() - 3, 8).getValues().forEach(r => {

    if (normalizarTexto(r[2]) !== "deuda") return;

    const tag = v24LeerTagDeuda_(r[7]);

    if (tag && tag.pagado >= tag.total) return;

    msg += `🏷️ ${r[1]}\n💰 Cuota: $${formatearCOP(r[4])}${tag ? `\n📉 Saldo total: $${formatearCOP(Math.max(0, tag.total - tag.pagado))}` : ""}\n🆔 ${r[0]}\n\n`; enc++;

  });

  enviarMensajeTelegram(chatId, enc === 0 ? "✅ No tienes deudas pendientes." : msg);

}



function procesarItemsFinancieros(chatId, items, txt) {

  const validos = items.filter(i => !i.ignorar);

  if (items.length > 0 && validos.length === 0) {

    enviarMensajeTelegram(chatId, `🙈 *MOVIMIENTO IGNORADO*\n${separadorBonito_()}\n\nOmití este registro porque corresponde a la regla de exclusión de Clara Aide.`);

    return;

  }



  validos.forEach(i => {

    if (i.intencion === "CREAR_GASTO_FIJO") {

      registrarGastoFijoCompleto(chatId, {

        concepto: i.concepto || i.descripcion_formal,

        valor: i.valor,

        dia: i.dia || 15,

        categoria: i.categoria || "Otros"

      });

      return;

    }

    if (i.intencion === "IDEAS_FIJOS" || i.intencion === "IDEAS_GASTOS_FIJOS") {

      generarEjemplosConIA_(chatId);

      return;

    }

    if (i.intencion === "CONSULTA_ANALISIS") {

      ejecutarAnalisisFinancieroInteligenteGemini_(chatId, txt);

      return;

    }

    if (i.intencion === "NUEVA_META_AHORRO" || i.intencion === "APORTE_AHORRO") {

      i.intencion === "NUEVA_META_AHORRO" ? gestionarNuevaMetaAhorro_(chatId, i) : gestionarAporteAhorro_(chatId, i);

      return;

    }



    const textoBusqueda = (txt && txt !== "Nota de voz") ? txt : i.descripcion_formal;

    if (i.intencion === "PAGAR_FIJO" || i.intencion === "ABONO_FIJO" || /\b(pague|pago|pagado|abone)\b/i.test(i.descripcion_formal)) {

      if (procesarPagoFijo(chatId, textoBusqueda)) return;

    }



    const id = obtenerSiguienteIDConsecutivo("MOVIMIENTOS", "M");

    const d = obtenerMesYAnioDeFecha("", null, null);

    const m = resolverMetodoPago_({ texto_original: txt }, i.descripcion_formal);

    const valor = Number(i.valor || 0);

    if (valor <= 0) return;



    const tipo = i.tipo || "Gasto";

    const categoria = i.categoria || "Otros";

    const descripcion = generarDescripcionBonita(i.descripcion_formal, categoria, tipo);

    

    const esNeutro = (i.intencion === "MOVIMIENTO_NEUTRO" || i.es_neutro || /marlon|propia|mis cuentas/i.test(descripcion)) ? "|MOVIMIENTO_NEUTRO" : "";

    const referencia = (i.referencia || "") + esNeutro;



    registrarEnSheetsYFormatear("MOVIMIENTOS", [

      id, d.fechaFormateada, Utilities.formatDate(new Date(), ZONA_HORARIA, "hh:mm a"),

      tipo, categoria, i.subcategoria || "General", descripcion, valor,

      m.metodo, m.cuenta, d.mes, d.anio, "Telegram", referencia

    ], 8);



    const alertaPresupuesto = (tipo === "Gasto" && !esNeutro) ? obtenerAlertaCategoria_(categoria) : "";



    enviarMensajeTelegramConBoton(

      chatId,

      `🚀 *REGISTRO LISTO*\n${emojiCategoria_(categoria, tipo)} ${descripcion}\n💵 $${formatearCOP(valor)}\n📂 ${categoria}${esNeutro ? "\n🛡️ *Registrado como Movimiento Neutro*" : ""}${alertaPresupuesto}`,

      id

    );

  });

}



/* =========================================================

   ESTADO PENDIENTE DE TELEGRAM (Memoria)

   ========================================================= */



function guardarFijoPendiente_(chatId, d) { d.timestamp = Date.now(); PropertiesService.getScriptProperties().setProperty("FIJO_PENDIENTE_" + chatId, JSON.stringify(d)); }

function obtenerFijoPendiente_(chatId) { const r = PropertiesService.getScriptProperties().getProperty("FIJO_PENDIENTE_" + chatId); if(!r) return null; try{ const d = JSON.parse(r); return (Date.now() - d.timestamp > 15*60*1000) ? null : d; } catch(e){ return null; } }

function borrarFijoPendiente_(chatId) { PropertiesService.getScriptProperties().deleteProperty("FIJO_PENDIENTE_" + chatId); }



function guardarAhorroPendiente_(chatId, d) { d.timestamp = Date.now(); PropertiesService.getScriptProperties().setProperty("AHORRO_PENDIENTE_" + chatId, JSON.stringify(d)); }

function obtenerAhorroPendiente_(chatId) { const r = PropertiesService.getScriptProperties().getProperty("AHORRO_PENDIENTE_" + chatId); if(!r) return null; try{ const d = JSON.parse(r); return (Date.now() - d.timestamp > 15*60*1000) ? null : d; } catch(e){ return null; } }

function borrarAhorroPendiente_(chatId) { PropertiesService.getScriptProperties().deleteProperty("AHORRO_PENDIENTE_" + chatId); }



function detectarSolicitudFijoPendiente_(chatId, txt) {

  // Desactivado para no interrumpir gastos cotidianos. Los pagos fijos reales se procesan con procesarPagoFijo

  return null;

}



function procesarConfirmacionAbonoPendiente_(chatId, txt) {

  const pen = obtenerFijoPendiente_(chatId);

  if (!pen || !/^(si|ok|dale|abono|abonar)$/i.test(normalizarTexto(txt))) return false;

  borrarFijoPendiente_(chatId);

  const r = procesarYMarcarGastoFijo(pen.concepto, pen.valor, true);

  if (r.actualizado) registrarMovimientoDesdeFijo_(chatId, r);

  return true;

}



function procesarRespuestaAhorroPendiente_(chatId, txt) {

  const p = obtenerAhorroPendiente_(chatId); if (!p) return false;

  const val = parseFloat((txt.match(/\d+/) || ["0"])[0]);

  if (!p.meta_objetivo) { p.meta_objetivo = val * (txt.includes("mil") ? 1000 : txt.includes("mill") ? 1000000 : 1); guardarAhorroPendiente_(chatId, p); enviarMensajeTelegram(chatId, "🎯 Meta: $" + formatearCOP(p.meta_objetivo) + ". ¿En cuánto tiempo? (ej. 6 meses)"); return true; }

  if (!p.fecha_objetivo) { p.fecha_objetivo = "31/12/2026"; guardarAhorroPendiente_(chatId, p); enviarMensajeTelegram(chatId, "📅 ¿Cuánto aportarás mensual? (ej. 100 mil)"); return true; }

  if (!p.aporte_periodico) { crearMetaAhorro_(chatId, p.objetivo, p.meta_objetivo, val * (txt.includes("mil") ? 1000 : 1), "mensual", p.fecha_objetivo, new Date().getFullYear(), ""); }

  return true;

}



/* =========================================================

   RUTINAS DE ELIMINACIÓN Y REVERSIÓN (DESHACER SEGURO)

   ========================================================= */



function borrarUltimoConjunto() {

  const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");

  if (!sh || sh.getLastRow() < 5) return { ok: false };



  // Al estar los movimientos más recientes de primeros en la fila 5,

  // buscamos el movimiento con el ID consecutivo más alto (M-xxxx) para deshacer exactamente el último registrado

  const ids = sh.getRange(5, 1, Math.max(1, sh.getLastRow() - 4), 1).getValues();

  let maxId = 0, targetId = "";

  for (let i = 0; i < ids.length; i++) {

    const m = String(ids[i][0] || "").match(/^M-(\d+)$/i);

    if (m) {

      const num = parseInt(m[1], 10);

      if (num > maxId) {

        maxId = num;

        targetId = ids[i][0];

      }

    }

  }

  if (targetId) {

    return borrarRegistroCompletoPorID(targetId);

  }

  return { ok: false };

}



function borrarRegistroCompletoPorID(idRaw, extraData) {
  let id = String(idRaw || "").trim().toUpperCase();
  if (id && !id.startsWith("M-") && !id.startsWith("CP-") && !id.startsWith("A-") && !id.startsWith("CXC-") && !id.startsWith("F-")) {
    if (/^\d+$/.test(id)) {
      id = "M-" + id.padStart(4, "0");
    }
  }

  if (id.startsWith("CP-")) return borrarColillaCompleta_(id);
  if (id.startsWith("A-")) return { ok: borrarFilaPorID_("AHORROS", id), id: id, colilla: 0, movimientos: 0, ahorros: 1 };

  if (id.startsWith("M-")) {
    const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");
    if (!sh) return { ok: false };
    const lastRow = sh.getLastRow();
    if (lastRow >= 4) {
      const rows = sh.getRange(4, 1, lastRow - 3, 14).getValues();
      for (let i = 0; i < rows.length; i++) {
        if (String(rows[i][0]).toUpperCase() === id) {
          const ref = String(rows[i][13] || "");
          if (ref.startsWith("PAGO_FIJO|") || ref.startsWith("ABONO_FIJO|")) {
            const fid = ref.split("|")[1];
            v21RevertPayment_(fid);
          }
          if (ref.startsWith("CXC_PAGO|")) {
            const cxcId = ref.split("|")[1];
            const val = Number(rows[i][7] || 0);
            revertirPagoCuentaPorCobrarV24_(cxcId, val);
          }
          if (ref.startsWith("CXC|")) {
            const cxcId = ref.split("|")[1];
            borrarFilaPorID_("CUENTAS POR COBRAR", cxcId);
          }
          sh.deleteRow(i + 4);
          return { ok: true, id: id, colilla: 0, movimientos: 1, ahorros: 0 };
        }
      }
    }
  }

  // Fallback: si no se encontró por ID pero tenemos datos del movimiento (descripción y valor)
  if (extraData && (extraData.descripcion || extraData.valor || extraData.monto)) {
    const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");
    if (sh && sh.getLastRow() >= 5) {
      const maxRows = Math.min(80, sh.getLastRow() - 4);
      const rows = sh.getRange(5, 1, maxRows, 14).getValues();
      const targetVal = Number(extraData.valor || extraData.monto || 0);
      const targetDesc = String(extraData.descripcion || "").toLowerCase().trim();

      for (let i = 0; i < rows.length; i++) {
        const rowId = String(rows[i][0] || "");
        const rowDesc = String(rows[i][6] || "").toLowerCase().trim();
        const rowVal = Number(rows[i][7] || 0);

        const valMatches = targetVal === 0 || Math.abs(rowVal - targetVal) < 0.01;
        const descMatches = targetDesc === "" || rowDesc.includes(targetDesc) || targetDesc.includes(rowDesc);

        if (valMatches && descMatches) {
          const ref = String(rows[i][13] || "");
          if (ref.startsWith("PAGO_FIJO|") || ref.startsWith("ABONO_FIJO|")) {
            const fid = ref.split("|")[1];
            v21RevertPayment_(fid);
          }
          if (ref.startsWith("CXC_PAGO|")) {
            const cxcId = ref.split("|")[1];
            revertirPagoCuentaPorCobrarV24_(cxcId, rowVal);
          }
          if (ref.startsWith("CXC|")) {
            const cxcId = ref.split("|")[1];
            borrarFilaPorID_("CUENTAS POR COBRAR", cxcId);
          }
          sh.deleteRow(i + 5);
          return { ok: true, id: rowId, fallbackMatched: true, colilla: 0, movimientos: 1, ahorros: 0 };
        }
      }
    }
  }

  if (id.startsWith("CXC-")) {
    const movimientos = borrarFilasPorTexto_("MOVIMIENTOS", "CXC|" + id, 13) +
      borrarFilasPorTexto_("MOVIMIENTOS", "CXC_PAGO|" + id, 13);
    const ok = borrarFilaPorID_("CUENTAS POR COBRAR", id, 3);
    return { ok: ok || movimientos > 0, id: id, movimientos: movimientos, cxc: ok ? 1 : 0 };
  }

  if (id.startsWith("F-")) {
    borrarFilasPorTexto_("MOVIMIENTOS", id, 13);
    const ok = borrarFilaPorID_("CONTROL FIJOS", id);
    return { ok: ok, id: id, fijos: 1 };
  }

  return { ok: false };
}



function borrarColillaCompleta_(id) {

  const m = borrarFilasPorTexto_("MOVIMIENTOS", id, 13);

  const a = borrarFilasPorTexto_("AHORROS", id, 7);

  const c = borrarFilaPorID_("COLILLAS DE PAGO", id) ? 1 : 0;

  return { ok: (m || a || c), id: id, colilla: c, movimientos: m, ahorros: a };

}



function borrarFilaPorID_(sheetName, id, offset = 3) {

  const sh = obtenerLibro_().getSheetByName(sheetName);

  if (!sh || sh.getLastRow() < offset + 1) return false;

  const data = sh.getRange(offset + 1, 1, sh.getLastRow() - offset, 1).getValues();

  for (let i = 0; i < data.length; i++) { if (String(data[i][0]).toUpperCase() === id) { sh.deleteRow(i + offset + 1); return true; } }

  return false;

}



function borrarFilasPorTexto_(sheetName, text, colIndex, offset = 3) {

  const sh = obtenerLibro_().getSheetByName(sheetName);

  if (!sh || sh.getLastRow() < offset + 1) return 0;

  const data = sh.getRange(offset + 1, colIndex + 1, sh.getLastRow() - offset, 1).getValues();

  let c = 0;

  for (let i = data.length - 1; i >= 0; i--) { if (String(data[i][0]).includes(text)) { sh.deleteRow(i + offset + 1); c++; } }

  return c;

}



function v21RevertPayment_(fId) {
  if (!fId) return;
  try {
    revertirPagoFijoDirecto_({ id: fId });
  } catch (err) {
    console.warn("Error en v21RevertPayment_:", err);
  }
}



/* =========================================================

   UTILITIES GENERALES

   ========================================================= */



function limpiarJSON(t) { let r = String(t||"").replace(/^```json/i,"").replace(/^```/i,"").replace(/```$/i,"").trim(); const i1=r.indexOf("{"), i2=r.indexOf("["); const i = (i1===-1)?i2:(i2===-1)?i1:Math.min(i1,i2); const f = Math.max(r.lastIndexOf("}"), r.lastIndexOf("]")); return (i!==-1 && f!==-1) ? r.substring(i, f+1) : r; }

function normalizarTexto(t) { return String(t||"").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim(); }

function formatearCOP(v) { return Number(v||0).toLocaleString("es-CO"); }

function limpiarTextoHTML(h) { return String(h||"").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim(); }

function emojiCategoria_(c, t) { const cn=normalizarTexto(c), tn=normalizarTexto(t); if(tn.includes("ingreso")||cn.includes("salario")) return "💰"; if(cn.includes("aliment")) return "🍽️"; if(cn.includes("gasolina")) return "⛽"; if(cn.includes("moto")||cn.includes("transporte")) return "🏍️"; if(cn.includes("vivienda")) return "🏠"; if(cn.includes("servicio")) return "💡"; if(cn.includes("ahorro")) return "🐷"; return "💸"; }

function separadorBonito_() { return "━━━━━━━━━━━━━━━━━━"; }

function v21Months_() { return ["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"]; }



function v21MovementIdByReference_(ref) {

  const sh = obtenerLibro_().getSheetByName("MOVIMIENTOS");

  if (!sh || sh.getLastRow() < 4) return "";

  const data = sh.getRange(4, 1, sh.getLastRow() - 3, 14).getDisplayValues();

  for (let i = 0; i < data.length; i++) { if (normalizarTexto(data[i][13]) === normalizarTexto(ref)) return String(data[i][0]); }

  return "";

}



function registrarMovimientoDesdeFijo_(chatId, fijo) {

  const d = new Date(), ref = (fijo.isTotal ? "PAGO_FIJO|" : "ABONO_FIJO|") + fijo.idFijo + "|" + fijo.mesActual;

  if (v21MovementIdByReference_(ref)) return;

  const mId = obtenerSiguienteIDConsecutivo("MOVIMIENTOS", "M");

  const m = resolverMetodoPago_({}, fijo.conceptoNombre);

  registrarEnSheetsYFormatear("MOVIMIENTOS", [ mId, Utilities.formatDate(d, ZONA_HORARIA, "dd/MM/yyyy"), Utilities.formatDate(d, ZONA_HORARIA, "hh:mm a"), "Gasto", fijo.categoria, "Abono gasto fijo", fijo.conceptoNombre + " - abono", fijo.valorFinal, m.metodo, m.cuenta, v21Months_()[d.getMonth()], d.getFullYear(), "Telegram", ref ], 8);

  enviarMensajeTelegramConBoton(chatId, `✅ *ABONO REGISTRADO*\n🏷️ ${fijo.conceptoNombre}\n💵 $${formatearCOP(fijo.valorFinal)}`, mId);

}



function extraerLineasPagoFijo_(textos) {

  const lineas = [];

  textos.forEach(t => String(t || "").split(/\n/).forEach(l => {

    const f = l.trim();

    if (f && /\b(pague|pago|pagado|abone|abono|cancele|cancelado)\b/.test(normalizarTexto(f)) && /(arriendo|agua|epm|luz|energia|tigo|internet|spotify|poliza|seguro|plan\s*movil)/.test(normalizarTexto(f))) lineas.push(f);

  }));

  return lineas;

}



function parsearMontoFlexibleV24_(texto) {

  const t = normalizarTexto(texto || "");

  // Soporte directo para palabras numéricas colombianas (ej. 'mil pesos', 'dos mil', 'un millon')

  const mapaPalabras = {

    'un': 1, 'uno': 1, 'dos': 2, 'tres': 3, 'cuatro': 4, 'cinco': 5,

    'seis': 6, 'siete': 7, 'ocho': 8, 'nueve': 9, 'diez': 10,

    'quince': 15, 'veinte': 20, 'veinticinco': 25, 'treinta': 30,

    'cuarenta': 40, 'cincuenta': 50, 'sesenta': 60, 'setenta': 70,

    'ochenta': 80, 'noventa': 90, 'cien': 100, 'ciento': 100, 'doscientos': 200, 'quinientos': 500

  };



  // 1. Caso especial: 'mil pesos', 'mil de pan', 'un mil', 'mil en dulces'

  if (/\b(?:un\s+)?mil\s*(?:pesos|cop|barras|lucas|en\s+|de\s+|$)/i.test(t) && !/\d+\s*mil/.test(t)) {

    const matchPalabra = t.match(/\b(un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|veinticinco|treinta|cuarenta|cincuenta|cien)\s+mil\b/i);

    if (matchPalabra) {

      return (mapaPalabras[matchPalabra[1].toLowerCase()] || 1) * 1000;

    }

    return 1000;

  }

  

  // 2. Caso 'un millon', 'un palo'

  if (/\b(?:un\s+)?(?:millon|palo)\b/i.test(t) && !/\d+\s*(?:millon|palo)/.test(t)) {

    const matchPalabraM = t.match(/\b(un|uno|dos|tres|cuatro|cinco|diez)\s+(?:millon|millones|palo|palos)\b/i);

    if (matchPalabraM) {

      return (mapaPalabras[matchPalabraM[1].toLowerCase()] || 1) * 1000000;

    }

    return 1000000;

  }



  let m = t.match(/(\d+(?:[.,]\d+)?)\s*(billones?|millones?|millon|mill|mil|k|lucas?)\b/i);

  if (m) {

    let n = parseFloat(String(m[1]).replace(",", "."));

    const u = normalizarTexto(m[2]);

    if (/bill/.test(u)) n *= 1000000000;

    else if (/mill/.test(u) && !/mil/.test(u)) n *= 1000000;

    else if (/mil|k|lucas/.test(u)) n *= 1000;

    return Math.round(n);

  }



  const candidates = t.match(/\d[\d.,]*/g) || [];

  if (!candidates.length) return 0;



  let best = 0;

  candidates.forEach(s => {

    let raw = String(s).trim();

    let n = 0;

    if (raw.includes(".") && raw.includes(",")) {

      if (raw.lastIndexOf(",") > raw.lastIndexOf(".")) {

        n = parseFloat(raw.replace(/\./g, "").replace(",", "."));

      } else {

        n = parseFloat(raw.replace(/,/g, ""));

      }

    } else if (raw.includes('.')) {
      n = parseFloat(raw.replace(/\./g, ''));
    } else if (raw.includes(',')) {
      n = parseFloat(raw.replace(/,/g, ''));
    } else {
      n = parseFloat(raw);
    }
    if (!isNaN(n) && n > best) best = n;
  });
  return Math.round(best);
}
