# -*- coding: utf-8 -*-
"""
Probador Interactivo de NLP Financiero - Slang Colombiano
Permite probar en consola cómo el sistema entiende frases cotidianas en menos de 5ms.
"""

import sys
import re
import json

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

def parsear_transaccion_colombia(texto):
    t = texto.lower().strip()
    
    # 1. Tipo
    es_ingreso = bool(re.search(r'\b(sueldo|salario|pago|ingreso|honorarios|me pagaron|ganancia|recib[ií]|abono a favor)\b', t))
    tipo = "Ingreso" if es_ingreso else "Gasto"

    # 2. Monto con slang colombiano
    monto = 0
    m_palos = re.search(r'(\d+(?:[.,]\d+)?)\s*(?:millones?|mill[oó]n|palos?)\b', t)
    m_mil = re.search(r'(\d+(?:[.,]\d+)?)\s*(?:mil|k|lucas?)\b', t)
    m_num = re.search(r'\$?\s*(\d{1,3}(?:[.]\d{3})+|\d{3,})', t)

    if m_palos:
        monto = int(float(m_palos.group(1).replace(',', '.')) * 1000000)
    elif m_mil:
        monto = int(float(m_mil.group(1).replace(',', '.')) * 1000)
    elif m_num:
        limpio = m_num.group(1).replace(".", "")
        monto = int(limpio)

    # 3. Categoría
    categoria = "Otros Gastos" if tipo == "Gasto" else "Otros Ingresos"
    cat_map = {
        "Alimentación": [r'almuerzo', r'comida', r'cena', r'desayuno', r'mercado', r'hamburguesa', r'pizza', r'salchipapa', r'restaurante', r'panaderia', r'café'],
        "Transporte": [r'gasolina', r'uber', r'didi', r'taxi', r'bus', r'transmilenio', r'metro', r'peaje', r'tanquear', r'moto', r'carro'],
        "Servicios Públicos": [r'epm', r'luz', r'agua', r'gas', r'internet', r'claro', r'tigo', r'movistar', r'recibo'],
        "Salud y Bienestar": [r'gym', r'gimnasio', r'drogueria', r'farmacia', r'medico', r'cita', r'eps', r'suplementos', r'proteina'],
        "Entretenimiento": [r'cine', r'cerveza', r'rumba', r'fiesta', r'netflix', r'spotify', r'juego', r'salida'],
        "Educación": [r'curso', r'libro', r'universidad', r'semestre', r'platzi', r'udemy'],
        "Sueldo y Salarios": [r'sueldo', r'quincena', r'nomina', r'salario']
    }
    for cat, patterns in cat_map.items():
        if any(re.search(pat, t) for pat in patterns):
            categoria = cat
            break

    # 4. Método de pago
    metodo = "Efectivo"
    if re.search(r'\b(nequi)\b', t): metodo = "Nequi"
    elif re.search(r'\b(daviplata)\b', t): metodo = "Daviplata"
    elif re.search(r'\b(tarjeta\s*credito|tc|cr[eé]dito)\b', t): metodo = "Tarjeta Crédito"
    elif re.search(r'\b(bancolombia)\b', t): metodo = "Bancolombia"
    elif re.search(r'\b(debito|débito)\b', t): metodo = "Tarjeta Débito"

    return {
        "frase_original": texto,
        "tipo": tipo,
        "monto_cop": f"${monto:,.0f}".replace(',', '.'),
        "monto_numero": monto,
        "categoria": categoria,
        "metodo_pago": metodo
    }

def main():
    print("=" * 60)
    print("   MOTOR NLP FINANCIERO - MODO PRUEBAS COLOMBIA")
    print("=" * 60)
    print("Ejemplos que puedes probar:")
    print(" - 'Almuerzo 25 mil'")
    print(" - 'Pagué 50 lucas de gasolina con nequi'")
    print(" - '1.5 millones de sueldo quincena en bancolombia'")
    print(" - 'Salchipapa 22k con tarjeta crédito'")
    print("-" * 60)

    if len(sys.argv) > 1:
        texto = " ".join(sys.argv[1:])
        res = parsear_transaccion_colombia(texto)
        print(json.dumps(res, indent=4, ensure_ascii=False))
        return

    # Modo interactivo
    while True:
        try:
            linea = input("\nEscribe un gasto o ingreso (o 'salir'): ")
            if not linea or linea.strip().lower() in ['salir', 'exit', 'q']:
                break
            res = parsear_transaccion_colombia(linea)
            print("-" * 40)
            print(f"Tipo:       {res['tipo']}")
            print(f"Monto:      {res['monto_cop']} COP")
            print(f"Categoría:  {res['categoria']}")
            print(f"Método:     {res['metodo_pago']}")
            print("-" * 40)
        except (KeyboardInterrupt, EOFError):
            break

if __name__ == "__main__":
    main()
