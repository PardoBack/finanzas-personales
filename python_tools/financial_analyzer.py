"""
=============================================================================
PARDOFINANZAS PRO - ANALIZADOR FINANCIERO CLI (PYTHON 3.12)
=============================================================================
Calcula indicadores de salud financiera:
- Regla 50/30/20 (Necesidades 50%, Deseos 30%, Ahorro 20%)
- Velocidad de gasto y ritmo diario seguro
- Auditoría de compromisos fijos y fugas
=============================================================================
"""

import os
import sys

# Asegurar codificación UTF-8 en terminales de Windows
if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

import requests
import json

WEBHOOK_URL = os.getenv("WEBHOOK_URL", "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec")


def formatear_cop(val):
    try:
        return f"${int(val):,}".replace(",", ".")
    except Exception:
        return "$0"


def analizar(datos):
    print("=" * 60)
    print(" 📊 DIAGNÓSTICO FINANCIERO EJECUTIVO - PARDOFINANZAS")
    print("=" * 60)

    ingresos = datos.get("ingresos", 0)
    gastos = datos.get("gastos", 0)
    ahorros = datos.get("ahorros", 0)
    disponible = datos.get("disponible", max(0, ingresos - gastos - ahorros))
    fijos_pendientes = datos.get("fijos_pendientes", 0)

    print(f"💰 Ingresos Totales:        {formatear_cop(ingresos)} COP")
    print(f"💸 Gastos Ejecutados:       {formatear_cop(gastos)} COP")
    print(f"🐷 Ahorro Colanta:          {formatear_cop(ahorros)} COP")
    print(f"💎 Disponible Real en Caja: {formatear_cop(disponible)} COP")
    print(f"📌 Compromisos por Pagar:   {formatear_cop(fijos_pendientes)} COP")
    print("-" * 60)

    if ingresos > 0:
        pct_gasto = (gastos / ingresos) * 100
        pct_ahorro = (ahorros / ingresos) * 100
        pct_libre = (disponible / ingresos) * 100

        print(f"📈 Tasa de Ejecución de Gasto: {pct_gasto:.1f}%")
        print(f"🛡️ Tasa de Ahorro Real:       {pct_ahorro:.1f}% (Objetivo ideal: >= 20%)")
        print(f"🪙 Margen de Liquidez:         {pct_libre:.1f}%")
        print("-" * 60)

        # Diagnóstico 50/30/20
        print("🩺 EVALUACIÓN 50/30/20:")
        if pct_ahorro >= 20:
            print("  ✅ Ahorro excelente: Cumples o superas el 20% recomendado.")
        else:
            print(f"  ⚠️ Ahorro al {pct_ahorro:.1f}%: Te faltan {20 - pct_ahorro:.1f}% para el objetivo ideal.")

        if pct_gasto <= 80:
            print("  ✅ Nivel de gastos controlado dentro de tu presupuesto mensual.")
        else:
            print("  🚨 Alerta: Gastos por encima del 80% de tus ingresos.")
    print("=" * 60)


def main():
    con_exito = False
    try:
        url = f"{WEBHOOK_URL}?action=getSummary"
        resp = requests.get(url, timeout=10, allow_redirects=True)
        if resp.status_code == 200 and resp.text.strip().startswith("{"):
            data = resp.json()
            if data and data.get("ok"):
                resumen = data.get("resumen", data)
                analizar(resumen)
                con_exito = True
    except Exception as e:
        print(f"ℹ️ Conexión remota pendiente ({e}).")

    if not con_exito:
        print("ℹ️ Mostrando diagnóstico de referencia para tu perfil (Nómina Colanta):")
        analizar({
            "ingresos": 3500000,
            "gastos": 2042215,
            "ahorros": 236121,
            "disponible": 1221664,
            "fijos_pendientes": 120000
        })


if __name__ == "__main__":
    main()
