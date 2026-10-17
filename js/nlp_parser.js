/**
 * PARDOFINANZAS PRO — MOTOR DE PROCESAMIENTO DE LENGUAJE NATURAL (NLP LOCAL)
 * Motor inteligente ultrarrápido ejecutado directamente en el navegador.
 * Compatible con modismos colombianos y variaciones flexibles de lenguaje.
 */

const FinancialNLP = {
  // Diccionario de categorías y comercios comunes en Medellín
  categoriesMap: [
    { regex: /\b(gasolina|tanqueada|tanqueo|combustible|eds|terpel|primax|zeuss|biomax|texaco)\b/i, category: "Moto", subcategory: "Combustible" },
    { regex: /\b(taller|repuesto|mecanico|cambio de aceite|llanta|frenos|soat)\b/i, category: "Moto", subcategory: "Mantenimiento" },
    { regex: /\b(parqueadero moto|parking moto|parqueadero de moto)\b/i, category: "Moto", subcategory: "Parqueadero" },
    { regex: /\b(almuerzo|cena|desayuno|comida|restaurante|burger|pizza|corrientazo|frisby|kfc|mcdonalds|mondongo)\b/i, category: "Alimentación", subcategory: "Restaurante" },
    { regex: /\b(mercado|supermercado|d1|exito|carulla|euro|vaquita|mercatodo|viveres|plaza)\b/i, category: "Alimentación", subcategory: "Mercado" },
    { regex: /\b(epm|energia|luz|agua|gas|alcantarillado|servicios\s*publicos|servicios)\b/i, category: "Servicios", subcategory: "EPM (Agua, Luz, Gas)" },
    { regex: /\b(tigo|internet|wifi|fibra|claro|datos|linea celular|plan celular|movistar)\b/i, category: "Servicios", subcategory: "Internet y Telefonía" },
    { regex: /\b(arriendo|alquiler|canon|administracion|apartamento|casa)\b/i, category: "Vivienda", subcategory: "Arriendo" },
    { regex: /\b(dulce|helado|paleta|chocolatina|granizado|mecato|empanada|chucheria|snack|golosina|tinto|cafecito|pan de queso|buñuelo)\b/i, category: "Gastos Hormiga / Dulces", subcategory: "Antojo / Mecato" },
    { regex: /\b(ropa|tenis|zapatos|greenfit|camisa|pantalon|falabella|zara|koaj|mattelsa)\b/i, category: "Compras", subcategory: "Ropa y Calzado" },
    { regex: /\b(spotify|netflix|disney|prime|youtube|streaming|cine|cinecolombia)\b/i, category: "Entretenimiento", subcategory: "Suscripciones" },
    { regex: /\b(farmacia|drogueria|medicamento|medico|eps|sura|pastillas)\b/i, category: "Salud", subcategory: "Medicamentos y Salud" },
    { regex: /\b(sueldo|nomina|quincena|pago colanta|honorarios|prima|bono|consignacion sueldo)\b/i, category: "Salario", subcategory: "Nómina Colanta" }
  ],

  // Extrae montos en pesos colombianos con soporte de modismos: "25 mil", "50 lucas", "1.5 millones", "1 palo", "197k"
  parseAmount(text) {
    const t = String(text || "").toLowerCase().trim();
    
    // "25 mil", "50k", "100 lucas", "197mil"
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

    // "$ 25.000", "197.000" o "25000" o "197000" o "8500"
    const matchDirecto = t.match(/\$?\s*(\d{1,3}(?:[.]\d{3})+|\d{4,})/);
    if (matchDirecto) {
      const limpio = matchDirecto[1].replace(/\./g, "");
      const num = parseInt(limpio, 10) || 0;
      if (num !== 2026) return num;
    }

    // Soporte para abreviaciones coloquiales: "epm 197", "arriendo 900", "tigo 120"
    // Si un número de 2 a 3 dígitos aparece junto a nombres de fijos, son miles (ej: 197 -> $197.000 COP)
    const matchShorthand = t.match(/\b(\d{2,3})\b/);
    if (matchShorthand) {
      const rawNum = parseInt(matchShorthand[1], 10);
      if (rawNum >= 10 && rawNum <= 999 && rawNum !== 15 && rawNum !== 30 && rawNum !== 31 && rawNum !== 28) {
        if (/\b(epm|energia|luz|agua|gas|tigo|internet|wifi|arriendo|alquiler|comida|mercado|gasolina|moto|parqueadero|audifonos|samsung|google|datos|celular)\b/i.test(t)) {
          return rawNum * 1000;
        }
      }
    }

    return 0;
  },

  // Detecta la cuenta bancaria (Nequi, AyC Colanta, Efectivo, Bancolombia)
  parseAccount(text) {
    const t = String(text || "").toLowerCase();
    if (/\b(colanta|ayc|cooperativa|cuenta nomina|n[oó]mina)\b/i.test(t)) return "AyC Colanta";
    if (/\b(efectivo|cash|plata en mano|en mano|monedas|billetes)\b/i.test(t)) return "Efectivo";
    if (/\b(bancolombia)\b/i.test(t)) return "Bancolombia";
    return "Nequi"; // Default en Colombia
  },

  // Catálogo maestro de gastos fijos y compromisos de Marlon para reconciliación automática ultra-inteligente
  fixedObligationsCatalog: [
    {
      id: "F-0107",
      regex: /\b(epm|energ[ií]a|luz|gas|alcantarillado|servicios\s*p[uú]blicos|servicios|factura\s*epm|cuenta\s*epm|factura\s*servicios|factura\s*de\s*(?:la\s*)?luz|cuenta\s*de\s*(?:la\s*)?luz|recibo\s*epm|recibo\s*de\s*(?:la\s*)?luz)\b/i,
      concepto: "EPM ENERGIA",
      cuota: 177000,
      variable: true,
      categoria: "Servicios",
      subcategoria: "EPM (Agua, Luz, Gas)"
    },
    {
      id: "F-0108",
      regex: /\b(comida(?:\s*q1)?|mercado(?:\s*q1)?|alimentaci[oó]n|mercado\s*quincena|mercado\s*del?\s*15|comida\s*del?\s*15|v[ií]veres|mercado\s*grande|mercado\s*colanta)\b/i,
      concepto: "COMIDA",
      cuota: 650000,
      variable: false,
      categoria: "Alimentación",
      subcategoria: "Mercado"
    },
    {
      id: "F-0109",
      regex: /\b(tigo|internet|wifi|fibra|une|factura\s*tigo|cuenta\s*tigo|plan\s*tigo|recibo\s*tigo|internet\s*de\s*tigo|internet\s*casa|wifi\s*casa)\b/i,
      concepto: "Internet Tigo",
      cuota: 120000,
      variable: true,
      categoria: "Servicios",
      subcategoria: "Internet"
    },
    {
      id: "F-0110",
      regex: /\b(parqueadero\s*(?:del?\s*)?trabajo|parking\s*(?:del?\s*)?trabajo|parqueadero\s*semana|parqueadero\s*laboral|parqueadero\s*oficina)\b/i,
      concepto: "Parqueadero Trabajo Semana",
      cuota: 40000,
      variable: false,
      categoria: "Transporte",
      subcategoria: "Parqueadero"
    },
    {
      id: "F-0111",
      regex: /\b(aud[ií]fonos|samsung|galaxy\s*buds|cuota\s*aud[ií]fonos|auriculares|audifonos\s*samsung|cuota\s*samsung)\b/i,
      concepto: "Cuota Audifonos Samsung",
      cuota: 50000,
      variable: false,
      categoria: "Otros",
      subcategoria: "Cuota"
    },
    {
      id: "F-0112",
      regex: /\b(parqueadero\s*(?:de\s*la\s*)?moto|parking\s*(?:de\s*la\s*)?moto|mensualidad\s*moto|parqueo\s*moto)\b/i,
      concepto: "PARQUEADERO MOTO",
      cuota: 60000,
      variable: false,
      categoria: "Moto",
      subcategoria: "Parqueadero"
    },
    {
      id: "F-0113",
      regex: /\b(gasolina\s*(?:de\s*la\s*)?moto|gasolina|tanquead[ao]|tanqui[eé]|tanqueo|combustible(?:\s*moto)?|tanquear|eds|terpel\s*moto)\b/i,
      concepto: "Gasolina Moto",
      cuota: 35000,
      variable: true,
      categoria: "Moto",
      subcategoria: "Gasolina"
    },
    {
      id: "F-0114",
      regex: /\b(spotify|m[uú]sica|streaming\s*m[uú]sica|cuenta\s*spotify|plan\s*spotify|suscripcion\s*spotify)\b/i,
      concepto: "Spotify",
      cuota: 10100,
      variable: false,
      categoria: "Entretenimiento",
      subcategoria: "Suscripciones"
    },
    {
      id: "F-0115",
      regex: /\b(celular|datos(?:\s*celular)?|plan\s*(?:de\s*)?datos|plan\s*(?:de\s*)?celular|claro|cuota\s*datos|recarga\s*datos|l[ií]nea\s*m[oó]vil|paquete\s*datos|datos\s*claro|factura\s*celular)\b/i,
      concepto: "CUOTA DATOS CELULAR",
      cuota: 43900,
      variable: false,
      categoria: "Servicios",
      subcategoria: "Telefonía"
    },
    {
      id: "F-0116",
      regex: /\b(arriendo|casa|apartamento|canon(?:\s*de\s*arrendamiento)?|arriendo\s*casa|alquiler|renta|pago\s*arriendo|casa\s*arriendo|arriendo\s*de\s*la\s*casa)\b/i,
      concepto: "ARRIENDO CASA",
      cuota: 900000,
      variable: false,
      categoria: "Vivienda",
      subcategoria: "Arriendo"
    },
    {
      id: "F-0117",
      regex: /\b(google|google\s*one|nube(?:\s*google)?|almacenamiento\s*google|drive|google\s*drive|suscripcion\s*google)\b/i,
      concepto: "Google One",
      cuota: 20000,
      variable: false,
      categoria: "Servicios",
      subcategoria: "Suscripciones"
    },
    {
      id: "F-0118",
      regex: /\b(gasolina\s*moto\s*q2|gasolina\s*quincena\s*2|gasolina\s*fin\s*de\s*mes|segunda\s*tanqueada\s*moto)\b/i,
      concepto: "Gasolina Moto",
      cuota: 25000,
      variable: true,
      categoria: "Moto",
      subcategoria: "Gasolina"
    },
    {
      id: "F-0119",
      regex: /\b(parqueadero\s*trabajo\s*q2|parqueadero\s*fin\s*de\s*mes|parqueadero\s*trabajo\s*quincena\s*2)\b/i,
      concepto: "Parqueadero Trabajo Semana",
      cuota: 40000,
      variable: false,
      categoria: "Transporte",
      subcategoria: "Parqueadero"
    },
    {
      id: "F-0120",
      regex: /\b(comida\s*q2|mercado\s*fin\s*de\s*mes|comida\s*quincena\s*2|mercado\s*quincena\s*2|mercado\s*30|comida\s*30)\b/i,
      concepto: "COMIDA",
      cuota: 200000,
      variable: false,
      categoria: "Alimentación",
      subcategoria: "Mercado"
    },
    {
      id: "F-0121",
      regex: /\b(parqueadero\s*moto\s*q2|parqueadero\s*moto\s*fin\s*de\s*mes|parqueadero\s*moto\s*quincena\s*2)\b/i,
      concepto: "PARQUEADERO MOTO",
      cuota: 40000,
      variable: false,
      categoria: "Moto",
      subcategoria: "Parqueadero"
    },
    {
      id: "F-0122",
      regex: /\b(agua|factura\s*del?\s*agua|recibo\s*del?\s*agua|cuenta\s*del?\s*agua|acueducto)\b/i,
      concepto: "Agua",
      cuota: 60000,
      variable: true,
      categoria: "Servicios",
      subcategoria: "EPM (Agua, Luz, Gas)"
    }
  ],

  // Elimina muletillas de cortesía y frases conversacionales
  cleanCourtesyAndFiller(text) {
    return String(text || "")
      .replace(/\b(por\s*favor|porfa(?:vor)?|por\s*fa|h[aá]game\s+el\s+favor|hace(?:me)?\s+el\s+favor|favor|muchas\s+gracias|gracias|te\s+agradezco|buen\s+d[ií]a|buenas\s+tardes|hola|hey|oye|mira)\b/gi, " ")
      .replace(/\b(de\s+todos\s+los\s+meses|todos\s+los\s+meses|cada\s+mes|de\s+cada\s+mes|al\s+mes|mensual(?:es|mente)?|quincenal(?:es|mente)?|por\s+quincena|cada\s+quincena|todas\s+las\s+quincenas|en\s+cada\s+mes|siempre)\b/gi, " ")
      .replace(/\s+/g, " ")
      .trim();
  },

  // Limpia y normaliza el nombre de un concepto o servicio sin ruido conversacional
  cleanConcept(text, fallback = "Compromiso Fijo") {
    let t = String(text || "").trim();
    if (!t) return fallback;

    // 1. Remover cortesías
    t = t.replace(/\b(por\s*favor|porfa(?:vor)?|por\s*fa|favor|h[aá]game\s+el\s+favor|hace(?:me)?\s+el\s+favor|muchas\s+gracias|gracias|te\s+agradezco|buen\s+d[ií]a|buenas\s+tardes|hola|hey|oye|mira)\b/gi, " ");

    // 2. Remover periodicidades y recurrencias
    t = t.replace(/\b(de\s+todos\s+los\s+meses|todos\s+los\s+meses|cada\s+mes|de\s+cada\s+mes|al\s+mes|mensual(?:es|mente)?|quincenal(?:es|mente)?|por\s+quincena|cada\s+quincena|todas\s+las\s+quincenas|en\s+cada\s+mes|siempre)\b/gi, " ");

    // 3. Remover verbos y comandos de creación
    t = t.replace(/\b(pon(?:me)?|coloca|registra|inscribe|a[ñn]ad[ae]|a[ñn]adir|agreg[ae]|agregar|crear?|poner|meter|anota|anotar|nuevo|nueva|quiero\s+poner|quiero\s+crear|necesito\s+poner|puedes\s+poner)\b/gi, " ");

    // 4. Remover tipos de entidad
    t = t.replace(/\b(un\s+|una\s+)?(gasto\s*fijo|compromiso\s*fijo|obligaci[oó]n\s*fija|obligaci[oó]n|pago\s*fijo|fijo|fija|fijos|cuota|deuda|credito|pr[eé]stamo)\b/gi, " ");

    // 5. Remover fechas y días
    t = t.replace(/\b(?:los|el|dia|dias|fecha)\s*([1-9]|[12]\d|3[01])\b/gi, " ");

    // 6. Remover montos de dinero
    t = t.replace(/\$?\s*\d+(?:[.,]\d+)?\s*(?:mil|k|lucas?|mill[oó]n|millones|palos?|pesos|cop)?\b/gi, " ");

    // 7. Remover conectores y frases puente
    t = t.replace(/\b(que\s+se\s+llame|llamado|llamada|con\s+el\s+nombre|nombre|como)\b/gi, " ");
    t = t.replace(/\b(de|del|por|para|con|en|que|sea|el|la|los|las|un|una|al|a)\b/gi, " ");
    t = t.replace(/\s+/g, " ").trim();

    // 8. Diccionario canónico de servicios del hogar comunes
    const tLower = t.toLowerCase();
    if (/\b(agua|acueducto|alcantarillado)\b/i.test(tLower)) return "Agua";
    if (/\b(luz|energia|epm)\b/i.test(tLower)) return "EPM Energía";
    if (/\b(gas|gas\s*domiciliario)\b/i.test(tLower)) return "Gas Domiciliario";
    if (/\b(tigo|internet|wifi|fibra)\b/i.test(tLower)) return "Internet Tigo";
    if (/\b(celular|datos|linea\s*movil|plan\s*celular)\b/i.test(tLower)) return "Plan Datos Celular";
    if (/\b(arriendo|casa|apartamento)\b/i.test(tLower)) return "Arriendo Casa";
    if (/\b(gimnasio|gym|smart\s*fit)\b/i.test(tLower)) return "Gimnasio";
    if (/\b(comida|mercado|alimentacion)\b/i.test(tLower)) return "COMIDA";
    if (/\b(gasolina|combustible)\b/i.test(tLower)) return "Gasolina Moto";
    if (/\b(parqueadero\s*moto)\b/i.test(tLower)) return "Parqueadero Moto";
    if (/\b(parqueadero\s*trabajo)\b/i.test(tLower)) return "Parqueadero Trabajo";
    if (/\b(spotify)\b/i.test(tLower)) return "Spotify";
    if (/\b(netflix)\b/i.test(tLower)) return "Netflix";
    if (/\b(google|google\s*one)\b/i.test(tLower)) return "Google One";

    if (t.length >= 2) {
      return t.split(" ")
        .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
        .join(" ");
    }
    return fallback;
  },

  // Búsqueda inteligente de coincidencias en el catálogo de fijos
  matchFixedObligation(text) {
    const raw = String(text || "").trim();
    const clean = raw.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    
    for (const item of this.fixedObligationsCatalog) {
      if (item.regex.test(clean) || item.regex.test(raw)) {
        return item;
      }
    }
    return null;
  },

  // Detecta la intención principal del usuario con máxima flexibilidad
  parseIntent(text) {
    const raw = String(text || "").trim();
    const t = raw.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

    // 1. Borrado / Anulación / Deshacer
    if (/\b(borrar|eliminar|deshacer|quitar|anular|\/borrar|\/deshacer)\b/i.test(t)) {
      const matchId = t.match(/\b(m-\d{3,4}|cp-\d{3,4}|a-\d{3,4})\b/i);
      return {
        type: "DELETE_TRANSACTION",
        targetId: matchId ? matchId[1].toUpperCase() : null
      };
    }

    // 2. Creación / Adición de un nuevo Gasto Fijo o Compromiso
    const isCreateFixed = /\b(pon(?:me)?|coloca|registra|inscribe|a[ñn]ad[ae]|a[ñn]adir|agreg[ae]|agregar|crear?|poner|meter)\s+(?:un\s+|una\s+)?(?:nuevo\s+)?(?:gasto\s*fijo|compromiso(?:\s*fijo)?|obligaci[oó]n|pago\s*fijo|fijo)\b/i.test(t) ||
                          /\b(nuevo\s+gasto\s*fijo|nueva\s+obligaci[oó]n|nuevo\s+compromiso)\b/i.test(t);

    if (isCreateFixed) {
      let dia = 15;
      const matchDia = t.match(/\b(?:los|el|dia|dias|fecha)\s*([1-9]|[12]\d|3[01])\b/i);
      if (matchDia) dia = parseInt(matchDia[1], 10);

      let concepto = "";
      const matchNamed = raw.match(/(?:que\s+se\s+llame|llamado|nombre|como)\s+([a-zA-Z0-9\s]+?)(?:\s+(?:los|el|dia|por|de\s+\d|\$)|$)/i);
      if (matchNamed) {
        concepto = this.cleanConcept(matchNamed[1], "Nuevo Compromiso Fijo");
      } else {
        concepto = this.cleanConcept(raw, "Nuevo Compromiso Fijo");
      }

      let categoria = "Servicios";
      if (/agua|luz|energia|gas|servicios?|epm|tigo|internet|wifi|claro/i.test(concepto)) categoria = "Servicios";
      else if (/arriendo|casa|apartamento|canon|administracion/i.test(concepto)) categoria = "Vivienda";
      else if (/comida|mercado|almuerzo|cena/i.test(concepto)) categoria = "Alimentación";
      else if (/moto|gasolina|parqueadero|soat/i.test(concepto)) categoria = "Moto";
      else if (/spotify|netflix|disney|youtube|musica/i.test(concepto)) categoria = "Entretenimiento";
      else if (/gimnasio|gym|fitness|smart\s*fit/i.test(concepto)) categoria = "Salud";

      const amount = this.parseAmount(raw);

      return {
        type: "CREATE_FIXED_OBLIGATION",
        concepto: concepto,
        dia: dia,
        categoria: categoria,
        cuota: amount || 0
      };
    }

    // 2.1 Préstamos, Deudas y Créditos a Cuotas (Addi, Sistecrédito, Tarjetas, Bancos)
    // Soporta: "saqué una deuda en Addi a 6 cuotas y es de 600.000, es sobre un teléfono", "saqué un préstamo por addi...", etc.
    const isLoanOrInstallment = /\b(saqu[eé]|tengo|hice|ped[ií]|adquir[ií]|financ[ieé]|meti|prestamo|credito|deuda)\b.*\b(addi|sistecredito|bancolombia|tarjeta|banco|cuotas?|prestamo|credito|deuda)\b/i.test(t) ||
                                /\b(addi|sistecredito)\b.*\b(cuota|cuotas|meses|total|prestamo|credito|deuda|saque|compre|sobre)\b/i.test(t) ||
                                /\b(deuda|prestamo|credito)\s+(?:en|por|con)\s+(addi|sistecredito|banco|bancolombia)\b/i.test(t);

    if (isLoanOrInstallment) {
      let entidad = "Addi";
      if (/sistecredito/i.test(t)) entidad = "Sistecrédito";
      else if (/bancolombia/i.test(t)) entidad = "Bancolombia";
      else if (/tarjeta/i.test(t)) entidad = "Tarjeta de Crédito";

      let numCuotas = 3;
      const matchCuotas = t.match(/\b(?:a|en|de)?\s*([1-9]|[1-4]\d)\s*(?:cuotas?|meses?|pagos?)\b/i);
      if (matchCuotas) {
        numCuotas = parseInt(matchCuotas[1], 10);
      }

      let totalMonto = this.parseAmount(raw);

      const matchPorCuota = t.match(/(?:cuotas?\s+de|valor\s+cuota\s*(?:es\s*)?de?)\s*(\d+(?:[.,]\d+)?\s*(?:mil|k|lucas?|mill[oó]n)?)/i);
      let cuotaMensual = 0;
      if (matchPorCuota) {
        const valCuota = this.parseAmount(matchPorCuota[1]);
        if (valCuota > 0) {
          cuotaMensual = valCuota;
          if (totalMonto === 0 || totalMonto === cuotaMensual) {
            totalMonto = cuotaMensual * numCuotas;
          }
        }
      }

      if (cuotaMensual === 0 && totalMonto > 0 && numCuotas > 0) {
        cuotaMensual = Math.round(totalMonto / numCuotas);
      }

      // Extraer día de pago (15, 17, 22, 30, etc.)
      let dia = 15;
      const matchDia = t.match(/\b(?:los|el|dia|dias|fecha|para\s+el|pagar\s+el|corte\s+el)\s*([1-9]|[12]\d|3[01])\b/i);
      if (matchDia) dia = parseInt(matchDia[1], 10);

      // Extraer producto o motivo (ej: "teléfono", "celular", "moto", "cama", "ropa")
      let concepto = `Crédito ${entidad}`;
      const matchPara = raw.match(/(?:para|sobre|comprar?|compr[eé]|es\s+un|es\s+una)\s+(?:un|una|el|la|este|esta)?\s*([a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s]{3,25}?)(?:\s*[,.]|\s+(?:en|por|a|con|total|\$)\s+(?:addi|sistecredito|\d)|$)/i);
      if (matchPara) {
        let prod = this.cleanCourtesyAndFiller(matchPara[1].trim());
        prod = prod.replace(/\b(a|al|de|del|para|por|con|en|un|una|el|la|los|las)\b/gi, " ").replace(/\s+/g, " ").trim();
        if (prod.length >= 2 && !/^(prestamo|credito|deuda|cuotas?|cuota)$/i.test(prod)) {
          const prodCap = prod.charAt(0).toUpperCase() + prod.slice(1);
          concepto = `${prodCap} en ${entidad}`;
        }
      }

      return {
        type: "CREATE_INSTALLMENT_LOAN",
        entidad: entidad,
        concepto: concepto,
        totalMonto: totalMonto,
        numCuotas: numCuotas,
        cuotaMensual: cuotaMensual,
        dia: dia,
        categoria: "Compras / Créditos"
      };
    }

    // 2.2 Consultas de Asesoría Financiera Inteligente con IA (CFO Asesor)
    // Ej: "quiero sacar esta moto, quiero sacar una cama, ¿qué me recomiendas? ¿cómo ves mis finanzas?"
    const isFinancialAdvice = /\b(recomiend[ao]s?|como\s+ves|qu[eé]\s+(?:me\s+)?aconsejas?|qu[eé]\s+opinas?|consejo|puedo\s+comprar|puedo\s+sacar|quiero\s+sacar|quiero\s+comprar|me\s+alcanza|capacidad\s+de\s+endeudamiento|como\s+van\s+mis\s+finanzas|analiz[ao]|eval[uú]a)\b/i.test(t);

    if (isFinancialAdvice) {
      let item = "esta compra / inversión";
      const matchItem = raw.match(/(?:sacar|comprar|adquirir|financiar|meterme\s+en)\s+(?:esta|este|un|una)?\s*([a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\s]{3,25}?)(?:\s*[,?¿.]|\s+(?:que|como|cuanto|\$)|$)/i);
      if (matchItem && matchItem[1].trim()) {
        item = this.cleanCourtesyAndFiller(matchItem[1].trim());
      }

      const amount = this.parseAmount(raw);

      return {
        type: "CFO_ADVICE_CONSULTATION",
        item: item,
        montoEstimado: amount || 0,
        rawQuery: raw
      };
    }

    // 3. Comprobación de verbos de acción de pago en Colombia
    const isPaymentAction = /\b(pagu[eé]|pago|pagad[ao]|se\s*pago|ya\s*pagu[eé]|abon[eé]|abono|cancel[eé]|cancelad[ao]|transfer[ií]|met[ií]|le\s*met[ií]|d[ií]|liquid[eé]|descont[eé]|sald[eé]|saldad[ao]|consign[eé]|qued[oó]\s*pag[ao])\b/i.test(t);
    const isExplicitQuestion = /[¿?]|\b(cuanto|cuales|como va|mostrar|consultar|ver\s+mis|ver\s+gastos|historial)\b/i.test(t);

    let amount = this.parseAmount(raw);
    const fixedMatch = this.matchFixedObligation(raw);

    // Si el usuario dijo explícitamente que PAGÓ un compromiso fijo ("ya pague el arriendo", "pague epm", "se pago la luz") SIN decir el monto:
    // El sistema asume la cuota base del compromiso fijo automáticamente
    let usedDefaultAmount = false;
    if (amount === 0 && fixedMatch && isPaymentAction && !isExplicitQuestion) {
      amount = fixedMatch.cuota;
      usedDefaultAmount = true;
    }

    // 3. Registro de movimiento si hay monto (explícito o inferido de compromiso fijo)
    if (amount > 0 && !isExplicitQuestion) {
      const isIngreso = /\b(recibi|recibo|ingreso|me pagaron|gane|cobre|nomina|sueldo|abono a mi favor)\b/i.test(t) && !fixedMatch;
      const isFijo = !!fixedMatch && !isIngreso;

      let category = isIngreso ? "Salario" : (fixedMatch ? fixedMatch.categoria : "Otros");
      let subcategory = isIngreso ? "Ingreso Extra" : (fixedMatch ? "Pago fijo" : "General");

      if (!fixedMatch && !isIngreso) {
        for (const item of this.categoriesMap) {
          if (item.regex.test(t) || item.regex.test(raw)) {
            category = item.category;
            subcategory = item.subcategory;
            break;
          }
        }
      }

      // Limpiar descripción ejecutiva sin cortesías ni muletillas conversacionales
      let desc = this.cleanCourtesyAndFiller(raw)
        .replace(/(?:^|\s)(pague|pagué|pago|pagado|pagada|se pago|se pagó|ya pague|ya pagué|abone|aboné|abono|cancele|cancelé|compre|compré|me gaste|me gasté|gaste|gasté|recibi|recibí|ingreso|este mes|hoy|ayer)(?=\s|$)/gi, " ")
        .replace(/\$?\s*\d+(?:[.,]\d+)?\s*(?:millones?|millón|palos?|mil(?:es)?|k|lucas?|pesos|cop)?(?=\s|$)/gi, " ")
        .replace(/\$?\s*\d+(?:[.,]\d+)?/g, " ")
        .replace(/(?:^|\s)(con|por|de|del|en|el|la|los|las|un|una|al|a|nequi|colanta|ayc|efectivo|bancolombia|tarjeta)(?=\s|$)/gi, " ")
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
        account: this.parseAccount(raw),
        description: desc,
        isFixed: isFijo,
        idFijo: fixedMatch ? fixedMatch.id : null,
        conceptoFijo: fixedMatch ? fixedMatch.concepto : null,
        isVariableInvoice: fixedMatch ? fixedMatch.variable : false,
        cuotaBaseFijo: fixedMatch ? fixedMatch.cuota : 0,
        usedDefaultAmount: usedDefaultAmount
      };
    }

    // 4. Consultas Inteligentes
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

// Exportar globalmente para navegador y módulos Node.js
if (typeof window !== "undefined") {
  window.FinancialNLP = FinancialNLP;
}
if (typeof module !== "undefined" && module.exports) {
  module.exports = FinancialNLP;
}
