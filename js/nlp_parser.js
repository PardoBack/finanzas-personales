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

    // "$ 25.000" o "25000"
    const matchDirecto = t.match(/\$?\s*(\d{1,3}(?:[.]\d{3})+|\d{4,})/);
    if (matchDirecto) {
      const limpio = matchDirecto[1].replace(/\./g, "");
      return parseInt(limpio, 10) || 0;
    }

    return 0;
  },

  // Detecta la cuenta bancaria (Nequi, AyC Colanta, Efectivo)
  parseAccount(text) {
    const t = text.toLowerCase();
    if (/\b(colanta|ayc|cooperativa|banco|cuenta nomina)\b/i.test(t)) return "AyC Colanta";
    if (/\b(efectivo|cash|plata en mano|en mano|monedas|billetes)\b/i.test(t)) return "Efectivo";
    return "Nequi"; // Default en Colombia
  },

  // Detecta la intención principal del usuario
  parseIntent(text) {
    const t = text.toLowerCase();

    // Consultas
    if (/\b(disponible|saldo|plata|cuanto me queda|cuanto tengo|cuanto hay|caja)\b/i.test(t)) {
      return { type: "QUERY_AVAILABLE" };
    }
    if (/\b(fugas|hormiga|mecato|antojos|micropagos)\b/i.test(t)) {
      return { type: "QUERY_FUGAS" };
    }
    if (/\b(fijos|obligaciones|compromisos|debo|cuotas)\b/i.test(t)) {
      return { type: "QUERY_FIXED" };
    }
    if (/\b(ahorro|ahorros|navideño|ordinario|fondo)\b/i.test(t)) {
      return { type: "QUERY_SAVINGS" };
    }
    if (/\b(resumen|balance|mes|estado)\b/i.test(t)) {
      return { type: "QUERY_SUMMARY" };
    }
    if (/\b(hola|buenos dias|buenas tardes|buenas noches|hey|que tal)\b/i.test(t)) {
      return { type: "GREETING" };
    }

    // Registro de movimiento
    const amount = this.parseAmount(text);
    if (amount > 0) {
      const isIngreso = /\b(recibi|recibo|ingreso|me pagaron|gane|cobre|nomina|sueldo)\b/i.test(t);
      const isFijo = /\b(epm|tigo|arriendo|spotify|cuota|fijo)\b/i.test(t);

      let category = "Otros";
      let subcategory = "General";

      for (const item of this.categoriesMap) {
        if (item.regex.test(t)) {
          category = item.category;
          subcategory = item.subcategory;
          break;
        }
      }

      // Limpiar descripción bonita
      let desc = text
        .replace(/\b(pague|pago|abone|cancele|compre|me gaste|gaste|recibi|ingreso)\b/gi, "")
        .replace(/\b(con|por|de|en|el|la|los|las|un|una|pesos|cop|nequi|colanta|efectivo)\b/gi, "")
        .replace(/\$?\s*\d+(?:[.,]\d+)?\s*(?:mil|k|lucas|millon|millones|palos)?/gi, "")
        .replace(/\s+/g, " ")
        .trim();

      if (!desc || desc.length < 3) {
        desc = category;
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

    return { type: "UNKNOWN" };
  }
};
