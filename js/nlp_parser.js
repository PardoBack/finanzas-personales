/**
 * PARDOFINANZAS PRO — MOTOR DE PROCESAMIENTO DE LENGUAJE NATURAL (NLP LOCAL)
 * Responde y procesa en < 0.05 segundos directamente en el navegador.
 */

const FinancialNLP = {
  // Diccionario de categorías y comercios comunes en Medellín
  categoriesMap: [
    { regex: /\b(gasolina|tanqueada|tanqueo|combustible|eds|terpel|primax|zeuss|biomax|texaco)\b/i, category: "Moto", subcategory: "Combustible" },
    { regex: /\b(taller|repuesto|mecanico|cambio de aceite|llanta|frenos|soat)\b/i, category: "Moto", subcategory: "Mantenimiento" },
    { regex: /\b(parqueadero moto|parking moto|parqueadero de moto)\b/i, category: "Moto", subcategory: "Parqueadero" },
    { regex: /\b(almuerzo|cena|desayuno|comida|restaurante|burger|pizza|corrientazo|frisby|kfc|mcdonalds|mondongo)\b/i, category: "Alimentación", subcategory: "Restaurante" },
    { regex: /\b(mercado|supermercado|d1|exito|carulla|euro|vaquita|mercatodo|viveres|plaza)\b/i, category: "Alimentación", subcategory: "Mercado" },
    { regex: /\b(epm|energia|luz|agua|gas|alcantarillado)\b/i, category: "Servicios", subcategory: "EPM (Agua, Luz, Gas)" },
    { regex: /\b(tigo|internet|wifi|fibra|claro|datos|linea celular|plan celular|movistar)\b/i, category: "Servicios", subcategory: "Internet y Telefonía" },
    { regex: /\b(arriendo|alquiler|canon|administracion|apartamento|casa)\b/i, category: "Vivienda", subcategory: "Arriendo" },
    { regex: /\b(dulce|helado|paleta|chocolatina|granizado|mecato|empanada|chucheria|snack|golosina|tinto|cafecito|pan de queso|buñuelo)\b/i, category: "Gastos Hormiga / Dulces", subcategory: "Antojo / Mecato" },
    { regex: /\b(ropa|tenis|zapatos|greenfit|camisa|pantalon|falabella|zara|koaj|mattelsa)\b/i, category: "Compras", subcategory: "Ropa y Calzado" },
    { regex: /\b(spotify|netflix|disney|prime|youtube|streaming|cine|cinecolombia)\b/i, category: "Entretenimiento", subcategory: "Suscripciones" },
    { regex: /\b(farmacia|drogueria|medicamento|medico|eps|sura|pastillas)\b/i, category: "Salud", subcategory: "Medicamentos y Salud" },
    { regex: /\b(sueldo|nomina|quincena|pago colanta|honorarios|prima|bono|consignacion sueldo)\b/i, category: "Salario", subcategory: "Nómina Colanta" }
  ],

  // Extrae montos en pesos colombianos con soporte de modismos: "25 mil", "50 lucas", "1.5 millones", "1 palo"
  parseAmount(text) {
    const t = text.toLowerCase();
    
    // "25 mil", "50k", "100 lucas"
    const matchMil = t.match(/(\d+(?:[.,]\d+)?)\s*(?:mil|k|lucas?)\b/i);
    if (matchMil) {
      const val = parseFloat(matchMil[1].replace(",", "."));
      return Math.round(val * 1000);
    }

    // "1 millon", "2.5 millones", "1 palo"
    const matchMillon = t.match(/(\d+(?:[.,]\d+)?)\s*(?:millones?|millon|palos?)\b/i);
    if (matchMillon) {
      const val = parseFloat(matchMillon[1].replace(",", "."));
      return Math.round(val * 1000000);
    }

    // "un millon" o "un palo"
    if (/\b(?:un\s+)?(?:millon|palo)\b/i.test(t)) {
      return 1000000;
    }

    // "mil pesos", "un mil"
    if (/\b(?:un\s+)?mil\s*(?:pesos|cop)?\b/i.test(t) && !/\d+\s*mil/.test(t)) {
      return 1000;
    }

    // "$ 25.000" o "25000" o "8500"
    const matchDirecto = t.match(/\$?\s*(\d{1,3}(?:[.]\d{3})+|\d{3,})/);
    if (matchDirecto) {
      const limpio = matchDirecto[1].replace(/\./g, "");
      const num = parseInt(limpio, 10) || 0;
      if (num !== 2026) return num;
    }

    return 0;
  },

  // Detecta la cuenta bancaria (Nequi, AyC Colanta, Efectivo, Bancolombia)
  parseAccount(text) {
    const t = text.toLowerCase();
    if (/\b(colanta|ayc|cooperativa|cuenta nomina|n[oó]mina)\b/i.test(t)) return "AyC Colanta";
    if (/\b(efectivo|cash|plata en mano|en mano|monedas|billetes)\b/i.test(t)) return "Efectivo";
    if (/\b(bancolombia)\b/i.test(t)) return "Bancolombia";
    return "Nequi"; // Default en Colombia
  },

  // Catálogo maestro de gastos fijos y compromisos de Marlon para reconciliación automática
  fixedObligationsCatalog: [
    { id: "F-0107", regex: /\b(epm|energ[ií]a|luz|agua|gas|factura epm|cuenta epm|servicios epm)\b/i, concepto: "EPM ENERGIA", cuota: 177000, variable: true, categoria: "Servicios", subcategoria: "EPM (Agua, Luz, Gas)" },
    { id: "F-0108", regex: /\b(comida|mercado|alimentaci[oó]n|mercado quincena)\b/i, concepto: "COMIDA", cuota: 850000, variable: false, categoria: "Alimentación", subcategoria: "Mercado" },
    { id: "F-0109", regex: /\b(tigo|internet|wifi|fibra|une)\b/i, concepto: "Internet Tigo", cuota: 120000, variable: true, categoria: "Servicios", subcategoria: "Internet" },
    { id: "F-0110", regex: /\b(parqueadero trabajo|parking trabajo|parqueadero semana)\b/i, concepto: "Parqueadero Trabajo Semana", cuota: 40000, variable: false, categoria: "Transporte", subcategoria: "Parqueadero" },
    { id: "F-0111", regex: /\b(aud[ií]fonos|samsung|galaxy buds)\b/i, concepto: "Cuota Audifonos Samsung", cuota: 50000, variable: false, categoria: "Otros", subcategoria: "Cuota" },
    { id: "F-0112", regex: /\b(parqueadero moto|parking moto)\b/i, concepto: "PARQUEADERO MOTO", cuota: 60000, variable: false, categoria: "Moto", subcategoria: "Parqueadero" },
    { id: "F-0113", regex: /\b(gasolina moto|gasolina|tanqueada)\b/i, concepto: "Gasolina Moto", cuota: 35000, variable: true, categoria: "Moto", subcategoria: "Gasolina" },
    { id: "F-0114", regex: /\b(spotify|m[uú]sica)\b/i, concepto: "Spotify", cuota: 10100, variable: false, categoria: "Entretenimiento", subcategoria: "Suscripciones" },
    { id: "F-0115", regex: /\b(celular|datos|plan celular|plan datos|claro|l[ií]nea m[oó]vil|cuota datos)\b/i, concepto: "CUOTA DATOS CELULAR", cuota: 43900, variable: false, categoria: "Servicios", subcategoria: "Telefonía" },
    { id: "F-0116", regex: /\b(arriendo|casa|apartamento|canon|arriendo casa)\b/i, concepto: "ARRIENDO CASA", cuota: 900000, variable: false, categoria: "Vivienda", subcategoria: "Arriendo" },
    { id: "F-0117", regex: /\b(google|google one|nube|almacenamiento)\b/i, concepto: "Google One", cuota: 20000, variable: false, categoria: "Servicios", subcategoria: "Suscripciones" },
    { id: "F-0118", regex: /\b(gasolina moto q2|gasolina quincena 2)\b/i, concepto: "Gasolina Moto", cuota: 25000, variable: true, categoria: "Moto", subcategoria: "Gasolina" },
    { id: "F-0119", regex: /\b(parqueadero trabajo q2)\b/i, concepto: "Parqueadero Trabajo Semana", cuota: 40000, variable: false, categoria: "Transporte", subcategoria: "Parqueadero" },
    { id: "F-0120", regex: /\b(comida q2|mercado fin de mes)\b/i, concepto: "COMIDA", cuota: 200000, variable: false, categoria: "Alimentación", subcategoria: "Mercado" },
    { id: "F-0121", regex: /\b(parqueadero moto q2)\b/i, concepto: "PARQUEADERO MOTO", cuota: 40000, variable: false, categoria: "Moto", subcategoria: "Parqueadero" }
  ],

  matchFixedObligation(text) {
    const t = String(text || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    for (const item of this.fixedObligationsCatalog) {
      if (item.regex.test(t) || item.regex.test(text)) {
        return item;
      }
    }
    return null;
  },

  // Detecta la intención principal del usuario
  parseIntent(text) {
    const t = text.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

    // 1. Borrado / Anulación / Deshacer
    if (/\b(borrar|eliminar|deshacer|quitar|anular|\/borrar|\/deshacer)\b/i.test(t)) {
      const matchId = t.match(/\b(m-\d{3,4}|cp-\d{3,4}|a-\d{3,4})\b/i);
      return {
        type: "DELETE_TRANSACTION",
        targetId: matchId ? matchId[1].toUpperCase() : null
      };
    }

    // 2. Registro de movimiento (Prioridad si hay un monto numérico y no es una pregunta explícita)
    const isExplicitQuestion = /[¿?]|\b(cuanto|cuales|como va|mostrar|consultar|ver\s+mis|ver\s+gastos)\b/i.test(t);
    const amount = this.parseAmount(text);

    if (amount > 0 && !isExplicitQuestion) {
      const isIngreso = /\b(recibi|recibo|ingreso|me pagaron|gane|cobre|nomina|sueldo|abono a mi favor)\b/i.test(t);
      const fixedMatch = !isIngreso ? this.matchFixedObligation(text) : null;
      const isFijo = !!fixedMatch;

      let category = isIngreso ? "Salario" : (fixedMatch ? fixedMatch.categoria : "Otros");
      let subcategory = isIngreso ? "Ingreso Extra" : (fixedMatch ? "Pago fijo" : "General");

      if (!fixedMatch && !isIngreso) {
        for (const item of this.categoriesMap) {
          if (item.regex.test(t) || item.regex.test(text)) {
            category = item.category;
            subcategory = item.subcategory;
            break;
          }
        }
      }

      // Limpiar descripción ejecutiva
      let desc = text
        .replace(/\b(pagu[eé]|pago|abon[eé]|cancel[eé]|compr[eé]|me gast[eé]|gast[eé]|recib[ií]|ingreso|este mes|hoy|ayer)\b/gi, "")
        .replace(/\b(con|por|de|en|el|la|los|las|un|una|pesos|cop|nequi|colanta|ayc|efectivo|bancolombia|tarjeta)\b/gi, "")
        .replace(/\$?\s*\d+(?:[.,]\d+)?\s*(?:mil|k|lucas?|mill[oó]n|millones|palos?)?\b/gi, "")
        .replace(/\s+/g, " ")
        .trim();

      if (fixedMatch) {
        desc = `Pago Fijo: ${fixedMatch.concepto}`;
      } else if (!desc || desc.length < 2) {
        desc = subcategory !== "General" ? subcategory : category;
      } else {
        desc = desc.charAt(0).toUpperCase() + desc.slice(1);
      }

      return {
        type: "RECORD_TRANSACTION",
        transactionType: isIngreso ? "Ingreso" : "Gasto",
        amount: amount,
        category: category,
        subcategory: subcategory,
        account: this.parseAccount(text),
        description: desc,
        isFixed: isFijo,
        idFijo: fixedMatch ? fixedMatch.id : null,
        conceptoFijo: fixedMatch ? fixedMatch.concepto : null,
        isVariableInvoice: fixedMatch ? fixedMatch.variable : false,
        cuotaBaseFijo: fixedMatch ? fixedMatch.cuota : 0
      };
    }

    // 3. Consultas Inteligentes
    if (/\b(disponible|saldo|plata|cuanto me queda|cuanto tengo|cuanto hay|caja|liquidez)\b/i.test(t)) {
      return { type: "QUERY_AVAILABLE" };
    }
    if (/\b(fugas|hormiga|mecato|antojos|micropagos|dulces)\b/i.test(t)) {
      return { type: "QUERY_FUGAS" };
    }
    if (/\b(fijos|obligaciones|compromisos|debo|cuotas|pendientes)\b/i.test(t)) {
      return { type: "QUERY_FIXED" };
    }
    if (/\b(ahorro|ahorros|navideno|ordinario|fondo|aportes)\b/i.test(t)) {
      return { type: "QUERY_SAVINGS" };
    }
    if (/\b(resumen|balance|mes|estado|reporte|consolidado|anual)\b/i.test(t)) {
      return { type: "QUERY_SUMMARY" };
    }
    if (/\b(hola|buenos dias|buenas tardes|buenas noches|hey|que tal|saludos|ayuda)\b/i.test(t)) {
      return { type: "GREETING" };
    }

    return { type: "UNKNOWN" };
  }
};
