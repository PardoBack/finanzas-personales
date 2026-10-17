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
      const isFijo = /\b(epm|tigo|arriendo|spotify|cuota|fijo|google one)\b/i.test(t);

      let category = isIngreso ? "Salario" : "Otros";
      let subcategory = isIngreso ? "Ingreso Extra" : "General";

      for (const item of this.categoriesMap) {
        if (item.regex.test(t) || item.regex.test(text)) {
          category = item.category;
          subcategory = item.subcategory;
          break;
        }
      }

      // Limpiar descripción ejecutiva
      let desc = text
        .replace(/\b(pagu[eé]|pago|abon[eé]|cancel[eé]|compr[eé]|me gast[eé]|gast[eé]|recib[ií]|ingreso|este mes|hoy|ayer)\b/gi, "")
        .replace(/\b(con|por|de|en|el|la|los|las|un|una|pesos|cop|nequi|colanta|ayc|efectivo|bancolombia|tarjeta)\b/gi, "")
        .replace(/\$?\s*\d+(?:[.,]\d+)?\s*(?:mil|k|lucas?|mill[oó]n|millones|palos?)?\b/gi, "")
        .replace(/\s+/g, " ")
        .trim();

      if (!desc || desc.length < 2) {
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
        isFixed: isFijo
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
