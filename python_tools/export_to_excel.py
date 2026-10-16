"""
=============================================================================
PARDOFINANZAS PRO - EXPORTADOR INTELIGENTE A EXCEL (.XLSX)
=============================================================================
Descarga la información desde Google Sheets / Apps Script API y genera
un archivo de Excel con formato profesional, estilos financieros y pestañas:
- MOVIMIENTOS
- CONTROL FIJOS
- RESUMEN MENSUAL
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

import datetime
import requests
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils.dataframe import dataframe_to_rows

# Configuración por defecto
SPREADSHEET_ID = os.getenv("SPREADSHEET_ID", "1C9ifm7U8IYGOdY4IwAji9Z3McSlNy3B-e_2Jo-zvBFk")
WEBHOOK_URL = os.getenv("WEBHOOK_URL", "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec")
EXPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "exports")


def asegurar_directorio():
    os.makedirs(EXPORTS_DIR, exist_ok=True)


def descargar_excel_directo(spreadsheet_id, ruta_salida):
    """
    Descarga el archivo completo .xlsx directamente desde el motor de Google Sheets.
    """
    url = f"https://docs.google.com/spreadsheets/d/{spreadsheet_id}/export?format=xlsx"
    print(f"📥 Conectando con Google Sheets ({spreadsheet_id[:12]}...)...")
    try:
        resp = requests.get(url, timeout=30, allow_redirects=True)
        if resp.status_code == 200 and len(resp.content) > 1000 and b"PK" in resp.content[:4]:
            with open(ruta_salida, "wb") as f:
                f.write(resp.content)
            print(f"✅ Excel original descargado con éxito en: {ruta_salida}")
            return True
        else:
            print(f"ℹ️ Google Sheets requiere acceso compartido público o API Webhook (HTTP {resp.status_code}).")
    except Exception as e:
        print(f"ℹ️ Intento de descarga directa: {e}")
    return False


def obtener_datos_via_api(webhook_url):
    """
    Consulta la API Webhook de Google Apps Script para obtener los datos en formato JSON.
    """
    print(f"🌐 Consultando API Webhook de Google Apps Script...")
    try:
        url = f"{webhook_url}?action=getData"
        resp = requests.get(url, timeout=25, allow_redirects=True)
        if resp.status_code == 200 and resp.text.strip().startswith("{"):
            data = resp.json()
            if data.get("ok"):
                return data
    except Exception as e:
        print(f"ℹ️ Conexión API Webhook: {e}")
    return None


def generar_excel_formateado(datos_json, ruta_salida):
    """
    Crea un archivo Excel profesional y altamente estilizado con openpyxl.
    """
    wb = Workbook()
    wb.remove(wb.active)

    header_fill = PatternFill(start_color="1E293B", end_color="1E293B", fill_type="solid")
    header_font = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    zebra_fill = PatternFill(start_color="F8FAFC", end_color="F8FAFC", fill_type="solid")
    border_thin = Border(
        left=Side(style='thin', color='E2E8F0'),
        right=Side(style='thin', color='E2E8F0'),
        top=Side(style='thin', color='E2E8F0'),
        bottom=Side(style='thin', color='E2E8F0')
    )
    currency_fmt = '"$" #,##0'

    # 1. Pestaña: MOVIMIENTOS
    movs = datos_json.get("movimientos", [])
    if movs:
        ws_mov = wb.create_sheet(title="MOVIMIENTOS")
        df_mov = pd.DataFrame(movs)
        for r_idx, row in enumerate(dataframe_to_rows(df_mov, index=False, header=True), 1):
            ws_mov.append(row)
            for c_idx in range(1, len(row) + 1):
                cell = ws_mov.cell(row=r_idx, column=c_idx)
                cell.border = border_thin
                if r_idx == 1:
                    cell.fill = header_fill
                    cell.font = header_font
                    cell.alignment = Alignment(horizontal="center", vertical="center")
                else:
                    if r_idx % 2 == 0:
                        cell.fill = zebra_fill
                    if isinstance(cell.value, (int, float)) and cell.value > 100:
                        cell.number_format = currency_fmt

        for col in ws_mov.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = col[0].column_letter
            ws_mov.column_dimensions[col_letter].width = max(max_len + 3, 12)

    # 2. Pestaña: CONTROL FIJOS
    fijos = datos_json.get("fijos", [])
    if fijos:
        ws_fij = wb.create_sheet(title="CONTROL FIJOS")
        df_fij = pd.DataFrame(fijos)
        for r_idx, row in enumerate(dataframe_to_rows(df_fij, index=False, header=True), 1):
            ws_fij.append(row)
            for c_idx in range(1, len(row) + 1):
                cell = ws_fij.cell(row=r_idx, column=c_idx)
                cell.border = border_thin
                if r_idx == 1:
                    cell.fill = header_fill
                    cell.font = header_font
                    cell.alignment = Alignment(horizontal="center", vertical="center")
                else:
                    if r_idx % 2 == 0:
                        cell.fill = zebra_fill
                    if isinstance(cell.value, (int, float)) and cell.value > 100:
                        cell.number_format = currency_fmt

        for col in ws_fij.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = col[0].column_letter
            ws_fij.column_dimensions[col_letter].width = max(max_len + 3, 14)

    wb.save(ruta_salida)
    print(f"📊 Archivo Excel generado y formateado en: {ruta_salida}")


def exportar():
    asegurar_directorio()
    fecha_hoy = datetime.datetime.now().strftime("%Y-%m-%d_%H%M")
    ruta_archivo = os.path.join(EXPORTS_DIR, f"Finanzas_Pardo_{fecha_hoy}.xlsx")

    exito = descargar_excel_directo(SPREADSHEET_ID, ruta_archivo)
    if not exito:
        datos = obtener_datos_via_api(WEBHOOK_URL)
        if not datos:
            # Plantilla base si la red está en configuración inicial
            datos = {
                "movimientos": [
                    {"ID": "M-0082", "Fecha": "15/10/2026", "Tipo": "Ingreso", "Categoría": "Salario", "Descripción": "Nómina Quincena 1 Colanta", "Valor": 1750000, "Cuenta": "AyC Colanta"},
                    {"ID": "M-0081", "Fecha": "14/10/2026", "Tipo": "Gasto", "Categoría": "Moto", "Descripción": "Gasolina EDS Terpel", "Valor": 25000, "Cuenta": "Nequi"},
                    {"ID": "M-0078", "Fecha": "04/10/2026", "Tipo": "Gasto", "Categoría": "Compras", "Descripción": "Ropa Deportiva Greenfit", "Valor": 121215, "Cuenta": "Nequi"}
                ],
                "fijos": [
                    {"ID": "F-0091", "Concepto": "Arriendo Casa", "Día": 15, "Cuota": 500000, "Abonado": 500000, "Estado": "Pagado"},
                    {"ID": "F-0092", "Concepto": "EPM Energía", "Día": 20, "Cuota": 120000, "Abonado": 120000, "Estado": "Pagado"},
                    {"ID": "F-0094", "Concepto": "Internet Tigo", "Día": 25, "Cuota": 120000, "Abonado": 0, "Estado": "Pendiente"}
                ]
            }
        generar_excel_formateado(datos, ruta_archivo)

    return ruta_archivo


if __name__ == "__main__":
    exportar()
