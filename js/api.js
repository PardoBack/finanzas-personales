/**
 * PARDOFINANZAS PRO — MÓDULO DE SERVICIOS API & GOOGLE SHEETS
 * Sincronizado 1:1 con la estructura y datos de Panel_Finanzas.xlsx
 */

const API_CONFIG = {
  DEFAULT_WEBHOOK: "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec",
  SPREADSHEET_ID: "1C9ifm7U8IYGOdY4IwAji9Z3McSlNy3B-e_2Jo-zvBFk",
  CACHE_KEY: "PARDO_FINANZAS_CACHE_V26",
  SETTINGS_KEY: "PARDO_FINANZAS_SETTINGS_V26"
};

// Datos sincronizados 1:1 con Panel_Finanzas.xlsx (Octubre 2026 - Marlon Muñoz Pardo)
const DEMO_DATA = {
  ok: true,
  resumen: {
    ingresos: 0,
    gastos: 207123,
    ahorros: 0,
    disponible: 3390857,
    fijos_pendientes: 2557000,
    mes: "octubre",
    anio: 2026
  },
  gastosPorCategoria: {
    "Transporte y Moto": 116000,
    "Gastos Hormiga / Dulces": 67000,
    "Ocio y Entretenimiento": 14023,
    "Suscripciones": 10100
  },
  cuentas: {
    aycColanta: 3319057,
    nequi: 0,
    efectivo: 71800,
    total: 3390857
  },
  ahorrosColanta: {
    ordinarioAcumulado: 594000,
    navidenoAcumulado: 2160000,
    aportesAcumulado: 995083,
    otrosAcumulado: 182216,
    totalAcumulado: 3931299
  },
  movimientos: [
    {
      id: "M-0255",
      fecha: "15/10/2026",
      hora: "05:58 PM",
      tipo: "Gasto",
      categoria: "Entretenimiento",
      subcategoria: "Pago fijo",
      descripcion: "Spotify - Factura octubre 2026",
      valor: 10100,
      metodo: "Tarjeta Nequi",
      cuenta: "Nequi"
    },
    {
      id: "M-0254",
      fecha: "14/10/2026",
      hora: "07:15 PM",
      tipo: "Gasto",
      categoria: "Gastos Hormiga / Dulces",
      subcategoria: "General",
      descripcion: "Antojo y mecato tienda",
      valor: 35000,
      metodo: "Efectivo",
      cuenta: "Efectivo"
    },
    {
      id: "M-0253",
      fecha: "12/10/2026",
      hora: "08:30 AM",
      tipo: "Gasto",
      categoria: "Transporte y Moto",
      subcategoria: "Combustible",
      descripcion: "Gasolina EDS Terpel Medellín",
      valor: 35000,
      metodo: "Transferencia",
      cuenta: "AyC Colanta"
    },
    {
      id: "M-0252",
      fecha: "08/10/2026",
      hora: "04:10 PM",
      tipo: "Gasto",
      categoria: "Gastos Hormiga / Dulces",
      subcategoria: "General",
      descripcion: "Café, granizado y pan de queso",
      valor: 32000,
      metodo: "Efectivo",
      cuenta: "Efectivo"
    },
    {
      id: "M-0251",
      fecha: "02/10/2026",
      hora: "10:00 AM",
      tipo: "Gasto",
      categoria: "Servicios",
      subcategoria: "Pago fijo",
      descripcion: "Cuota Datos Celular Tigo",
      valor: 43900,
      metodo: "Efectivo",
      cuenta: "Efectivo"
    }
  ],
  fijos: [
    { id: "F-0107", concepto: "EPM ENERGIA", categoria: "Servicios", dia: 15, cuota: 177000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0108", concepto: "COMIDA", categoria: "Alimentación", dia: 15, cuota: 850000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0109", concepto: "Internet Tigo", categoria: "Servicios", dia: 15, cuota: 120000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0110", concepto: "Parqueadero Trabajo Semana", categoria: "Transporte", dia: 15, cuota: 40000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0111", concepto: "Cuota Audifonos Samsung", categoria: "Otros", dia: 15, cuota: 50000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0112", concepto: "PARQUEADERO MOTO", categoria: "Moto", dia: 15, cuota: 60000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0113", concepto: "Gasolina Moto", categoria: "Moto", dia: 15, cuota: 35000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0114", concepto: "Spotify", categoria: "Entretenimiento", dia: 15, cuota: 10100, abonado: 10100, estado: "🟢 Pagado" },
    { id: "F-0115", concepto: "CUOTA DATOS CELULAR", categoria: "Servicios", dia: 15, cuota: 43900, abonado: 43900, estado: "🟢 Pagado" },
    { id: "F-0116", concepto: "ARRIENDO CASA", categoria: "Vivienda", dia: 30, cuota: 900000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0117", concepto: "Google One", categoria: "Servicios", dia: 30, cuota: 20000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0118", concepto: "Gasolina Moto Fin de Mes", categoria: "Moto", dia: 30, cuota: 25000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0119", concepto: "Parqueadero Trabajo Fin de Mes", categoria: "Transporte", dia: 30, cuota: 40000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0120", concepto: "COMIDA Fin de Mes", categoria: "Alimentación", dia: 30, cuota: 200000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0121", concepto: "PARQUEADERO MOTO Fin de Mes", categoria: "Moto", dia: 30, cuota: 40000, abonado: 0, estado: "🔴 Pendiente" }
  ]
};

const FinanceAPI = {
  getWebhookUrl() {
    const saved = localStorage.getItem(API_CONFIG.SETTINGS_KEY);
    if (saved) {
      try {
        const s = JSON.parse(saved);
        if (s.webhookUrl && s.webhookUrl.trim().length > 10) return s.webhookUrl.trim();
      } catch (e) {}
    }
    return API_CONFIG.DEFAULT_WEBHOOK;
  },

  isDemoModeAllowed() {
    const saved = localStorage.getItem(API_CONFIG.SETTINGS_KEY);
    if (saved) {
      try {
        return JSON.parse(saved).demoMode !== false;
      } catch (e) {}
    }
    return true;
  },

  async fetchFinancialData() {
    const webhookUrl = this.getWebhookUrl();
    
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 6000);
      
      const endpoint = `${webhookUrl}?action=getData&t=${Date.now()}`;
      const response = await fetch(endpoint, {
        method: "GET",
        signal: controller.signal,
        headers: { "Accept": "application/json" }
      });
      clearTimeout(timeoutId);

      if (response.ok) {
        const data = await response.json();
        if (data && data.ok) {
          localStorage.setItem(API_CONFIG.CACHE_KEY, JSON.stringify(data));
          return { data: data, isLive: true };
        }
      }
    } catch (err) {
      console.warn("⚠️ Conectando en modo seguro local (Google Sheets protegido).", err);
    }

    const cached = localStorage.getItem(API_CONFIG.CACHE_KEY);
    if (cached) {
      try {
        return { data: JSON.parse(cached), isLive: false, fromCache: true };
      } catch (e) {}
    }

    return { data: DEMO_DATA, isLive: false, fromDemo: true };
  },

  async addMovement(movementData) {
    const webhookUrl = this.getWebhookUrl();
    const payload = {
      action: "addMovement",
      ...movementData,
      timestamp: new Date().toISOString()
    };

    // Actualizar cache local en localStorage para persistencia instantánea
    try {
      const cached = localStorage.getItem(API_CONFIG.CACHE_KEY);
      if (cached) {
        const parsed = JSON.parse(cached);
        if (parsed && parsed.movimientos) {
          parsed.movimientos.unshift({
            id: movementData.id || "M-" + Math.floor(1000 + Math.random() * 9000),
            fecha: new Date().toLocaleDateString("es-CO"),
            hora: new Date().toLocaleTimeString("es-CO", { hour: '2-digit', minute: '2-digit' }),
            ...movementData
          });
          localStorage.setItem(API_CONFIG.CACHE_KEY, JSON.stringify(parsed));
        }
      }
    } catch (e) {}

    try {
      // Envío a Google Apps Script Web App
      fetch(webhookUrl, {
        method: "POST",
        mode: "no-cors",
        headers: { "Content-Type": "text/plain;charset=utf-8" },
        body: JSON.stringify(payload)
      }).catch(e => console.warn("Background sync info:", e));
      
      return { ok: true, status: "saved", timestamp: new Date().toLocaleTimeString("es-CO") };
    } catch (err) {
      return { ok: true, status: "saved_local" };
    }
  },

  async processPaystub(fileData) {
    const webhookUrl = this.getWebhookUrl();
    const payload = {
      action: "procesarColilla",
      fileBase64: fileData.base64,
      fileName: fileData.fileName || "Colilla_Nomina.pdf",
      mimeType: fileData.mimeType || "application/pdf"
    };

    try {
      // Enviar solicitud POST a Apps Script
      const resp = await fetch(webhookUrl, {
        method: "POST",
        headers: { "Content-Type": "text/plain;charset=utf-8" },
        body: JSON.stringify(payload)
      });

      if (resp.ok) {
        const json = await resp.json();
        return json;
      }
      return { ok: false, error: "Error de servidor Apps Script: HTTP " + resp.status };
    } catch (err) {
      // En caso de CORS redirect, intentar envío no-cors y devolver confirmación optimista
      try {
        fetch(webhookUrl, {
          method: "POST",
          mode: "no-cors",
          headers: { "Content-Type": "text/plain;charset=utf-8" },
          body: JSON.stringify(payload)
        });
        return { ok: true, status: "sent_background", mensaje: "Colilla enviada a procesamiento en Google Apps Script" };
      } catch (e) {
        return { ok: false, error: err.message };
      }
    }
  },

  backendState: {
    tested: false,
    isV26: false,
    versionStr: ""
  },

  async testConnection() {
    const webhookUrl = this.getWebhookUrl();
    try {
      const resp = await fetch(`${webhookUrl}?action=ping&t=${Date.now()}`, {
        method: "GET",
        headers: { "Accept": "application/json" }
      });
      const text = await resp.text();
      this.backendState.tested = true;
      try {
        const json = JSON.parse(text);
        const isV26 = Boolean(json.version && json.version.includes("V26"));
        this.backendState.isV26 = isV26;
        this.backendState.versionStr = (json.version || "V26.9 PRO AI");
        return { ok: true, isModernV26: isV26, data: json, raw: text };
      } catch (e) {
        // Si devolvió texto plano antiguo como "Bot de finanzas V25.3 PRO activo y sincronizado"
        const isOld = text.includes("V25.3") || text.includes("V25");
        this.backendState.isV26 = false;
        this.backendState.versionStr = isOld ? "V25.3 PRO" : "Desconocida";
        return { ok: true, isOldVersion: isOld, raw: text };
      }
    } catch (err) {
      this.backendState.tested = true;
      this.backendState.isV26 = false;
      return { ok: false, error: err.message };
    }
  }
};
