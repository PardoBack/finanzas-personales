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
import datetime
import requests
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils.dataframe import dataframe_to_rows

# Configuración por defecto (puede sobreescribirse con variables de entorno)
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
    resp = requests.get(url, timeout=30)
    
    if resp.status_code == 200 and len(resp.content) > 1000:
        with open(ruta_salida, "wb") as f:
            f.write(resp.content)
        print(f"✅ Excel original descargado con éxito en: {ruta_salida}")
        return True
    else:
        print(f"⚠️ No se pudo descargar directamente (HTTP {resp.status_code}). Intentando por API Webhook...")
        return False


def obtener_datos_via_api(webhook_url):
    """
    Consulta la API Webhook de Google Apps Script para obtener los datos en formato JSON.
    """
    print(f"🌐 Consultando API Webhook de Google Apps Script...")
    try:
        url = f"{webhook_url}?action=getData"
        resp = requests.get(url, timeout=25)
        if resp.status_code == 200:
            data = resp.json()
            if data.get("ok"):
                return data
    except Exception as e:
        print(f"❌ Error al consultar Webhook: {e}")
    return None


def generar_excel_formateado(datos_json, ruta_salida):
    """
    Crea un archivo Excel profesional y altamente estilizado con openpyxl.
    """
    wb = Workbook()
    # Eliminar hoja por defecto
    wb.remove(wb.active)

    # Estilos profesionales
    header_fill = PatternFill(start_color="1E293B", end_color="1E293B", fill_type="solid")
    header_font = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    zebra_fill = PatternFill(start_color="F8FAFC", end_color="F8FAFC", fill_type="solid")
    border_thin = Border(
        left=Side(style='thin', color='E2E8F0'),
        right=Side(style='thin', color='E2E8F0'),
        top=Side(style='thin', color='E2E8F0'),
        bottom=Side(style='thin', color='E2E8F0')
    )
    font_bold = Font(name="Calibri", size=11, bold=True)
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
                    # Formato moneda para columnas con valores numéricos
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

    # Intentar descarga directa
    exito = descargar_excel_directo(SPREADSHEET_ID, ruta_archivo)
    if not exito:
        datos = obtener_datos_via_api(WEBHOOK_URL)
        if datos:
            generar_excel_formateado(datos, ruta_archivo)
        else:
            print("❌ No se pudo completar la exportación. Revisa la conexión de red o permisos del Sheet.")
            return None

    return ruta_archivo


if __name__ == "__main__":
    exportar()
