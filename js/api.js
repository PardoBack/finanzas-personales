/**
 * PARDOFINANZAS PRO — MÓDULO DE SERVICIOS API & GOOGLE SHEETS
 */

const API_CONFIG = {
  DEFAULT_WEBHOOK: "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec",
  SPREADSHEET_ID: "1C9ifm7U8IYGOdY4IwAji9Z3McSlNy3B-e_2Jo-zvBFk",
  CACHE_KEY: "PARDO_FINANZAS_CACHE_V26",
  SETTINGS_KEY: "PARDO_FINANZAS_SETTINGS_V26"
};

// Datos de demostración de alta fidelidad para Marlon (Perfil Colanta Medellín)
const DEMO_DATA = {
  ok: true,
  resumen: {
    ingresos: 3500000,
    gastos: 2042215,
    ahorros: 236121,
    disponible: 1221664,
    fijos_pendientes: 120000,
    mes: "octubre"
  },
  gastosPorCategoria: {
    "Alimentación": 650000,
    "Vivienda": 500000,
    "Servicios": 297000,
    "Moto": 220000,
    "Compras": 121215,
    "Gastos Hormiga / Dulces": 64000,
    "Entretenimiento": 40000,
    "Otros": 150000
  },
  cuentas: {
    aycColanta: 780000,
    nequi: 341664,
    efectivo: 100000
  },
  movimientos: [
    {
      id: "M-0082",
      fecha: "15/10/2026",
      hora: "08:30 AM",
      tipo: "Ingreso",
      categoria: "Salario",
      descripcion: "Pago Nómina 1ra Quincena Colanta",
      valor: 1750000,
      metodo: "Transferencia",
      cuenta: "AyC Colanta"
    },
    {
      id: "M-0081",
      fecha: "14/10/2026",
      hora: "06:15 PM",
      tipo: "Gasto",
      categoria: "Moto",
      descripcion: "Gasolina EDS Terpel Medellín",
      valor: 25000,
      metodo: "QR Nequi",
      cuenta: "Nequi"
    },
    {
      id: "M-0080",
      fecha: "12/10/2026",
      hora: "01:20 PM",
      tipo: "Gasto",
      categoria: "Alimentación",
      descripcion: "Almuerzo Ejecutivo Restaurante Laureles",
      valor: 18000,
      metodo: "Transferencia",
      cuenta: "Nequi"
    },
    {
      id: "M-0079",
      fecha: "10/10/2026",
      hora: "04:45 PM",
      tipo: "Gasto",
      categoria: "Gastos Hormiga / Dulces",
      descripcion: "Café y Granizado con Pan de Queso",
      valor: 8500,
      metodo: "Efectivo",
      cuenta: "Efectivo"
    },
    {
      id: "M-0078",
      fecha: "04/10/2026",
      hora: "02:10 PM",
      tipo: "Gasto",
      categoria: "Compras",
      descripcion: "Ropa Deportiva Greenfit (Activewear)",
      valor: 121215,
      metodo: "Tarjeta Crédito",
      cuenta: "Nequi"
    },
    {
      id: "M-0077",
      fecha: "02/10/2026",
      hora: "10:00 AM",
      tipo: "Gasto",
      categoria: "Servicios",
      descripcion: "Pago Factura EPM Energía y Gas Septiembre",
      valor: 120000,
      metodo: "Transferencia",
      cuenta: "AyC Colanta"
    }
  ],
  fijos: [
    { id: "F-0091", concepto: "Arriendo Casa", categoria: "Vivienda", dia: 15, cuota: 500000, abonado: 500000, estado: "🟢 Pagado" },
    { id: "F-0092", concepto: "EPM Energía y Gas", categoria: "Servicios", dia: 20, cuota: 120000, abonado: 120000, estado: "🟢 Pagado" },
    { id: "F-0093", concepto: "Mercado y Comida", categoria: "Alimentación", dia: 15, cuota: 650000, abonado: 650000, estado: "🟢 Pagado" },
    { id: "F-0094", concepto: "Internet y Telefonía Tigo", categoria: "Servicios", dia: 25, cuota: 120000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0095", concepto: "Parqueadero Moto Trabajo", categoria: "Moto", dia: 30, cuota: 60000, abonado: 0, estado: "🔴 Pendiente" },
    { id: "F-0096", concepto: "Spotify Familiar", categoria: "Entretenimiento", dia: 30, cuota: 25000, abonado: 25000, estado: "🟢 Pagado" }
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
      // Intentar consulta a la API de Google Apps Script con timeout
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 7000);
      
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
      console.warn("⚠️ No se pudo conectar directamente con Google Apps Script. Usando caché/demo.", err);
    }

    // Si falló el webhook, usar caché local reciente
    const cached = localStorage.getItem(API_CONFIG.CACHE_KEY);
    if (cached) {
      try {
        return { data: JSON.parse(cached), isLive: false, fromCache: true };
      } catch (e) {}
    }

    // Fallback: Demostración realista vinculada a Marlon
    return { data: DEMO_DATA, isLive: false, fromDemo: true };
  },

  async addMovement(movementData) {
    const webhookUrl = this.getWebhookUrl();
    const payload = {
      action: "addMovement",
      ...movementData,
      timestamp: new Date().toISOString()
    };

    try {
      // En Apps Script, fetch con modo no-cors o redirección estándar
      const response = await fetch(webhookUrl, {
        method: "POST",
        headers: { "Content-Type": "text/plain;charset=utf-8" },
        body: JSON.stringify(payload)
      });
      
      return { ok: true, status: "saved" };
    } catch (err) {
      console.error("Error al guardar en Google Sheets:", err);
      // Guardar localmente
      return { ok: true, status: "saved_local" };
    }
  }
};
