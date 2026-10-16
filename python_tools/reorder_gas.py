# -*- coding: utf-8 -*-
import sys
from pathlib import Path

def main():
    root = Path(__file__).resolve().parent.parent
    gas_path = root / "google_apps_script" / "Codigo.gs"

    with open(gas_path, "r", encoding="utf-8") as f:
        text = f.read()

    idx_api_header = text.find("/* =========================================================\n   API WEB (doGet & doPost)")
    idx_after_api = text.find("/* =========================================================\n\n   ACTIVADORES GLOBALES", idx_api_header)
    if idx_after_api == -1:
        idx_after_api = text.find("/* =========================================================", idx_api_header + 500)

    api_block = text[idx_api_header:idx_after_api].strip()

    pre_api = text[:idx_api_header].strip()
    post_api = text[idx_after_api:].strip()

    # Eliminar viejo bloque V25.6 y constantes duplicadas de pre_api
    idx_v25 = pre_api.find("/************************************************************")
    if idx_v25 != -1:
        idx_obtener = pre_api.find("function obtenerLibro_()", idx_v25)
        if idx_obtener != -1:
            pre_clean = pre_api[:idx_v25].strip() + "\n\n" + pre_api[idx_obtener:].strip()
        else:
            pre_clean = pre_api
    else:
        pre_clean = pre_api

    header = """/************************************************************
 * MIS FINANZAS & PARDOFINANZAS DASHBOARD WEB
 * VERSIÓN V26.9 PRO AI — ARCHIVO OFICIAL DEFINITIVO UNIFICADO
 * 
 * Contiene el 100% de la automatización para Google Apps Script:
 * 1. API Web en tiempo real para Dashboard (doGet & doPost)
 * 2. Inserción directa en fila 5 de MOVIMIENTOS y fórmula Columna O
 * 3. Opción de Borrar / Anular registros con 1 clic (deleteMovement)
 * 4. Auditoría de Colillas de Nómina Colanta con Gemini Multimodal (PDF/Foto)
 * 5. Bot de Telegram oficial (comandos, callbacks, notas de voz)
 * 6. Escáner de Facturas Gmail (procesarCorreosFacturas)
 ************************************************************/

/* =========================================================
   CONFIGURACIÓN Y ENTORNO
   ========================================================= */
const TELEGRAM_TOKEN = String(PropertiesService.getScriptProperties().getProperty("TELEGRAM_TOKEN") || "").trim();
const SPREADSHEET_ID = String(PropertiesService.getScriptProperties().getProperty("SPREADSHEET_ID") || "1C9ifm7U8IYGOdY4IwAji9Z3McSlNy3B-e_2Jo-zvBFk").trim();
const WEBHOOK_URL = String(PropertiesService.getScriptProperties().getProperty("WEBHOOK_URL") || "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec").trim();
const GEMINI_API_KEY = String(PropertiesService.getScriptProperties().getProperty("GEMINI_API_KEY") || "").trim();
const GMAIL_FACTURAS_REMITENTE = String(PropertiesService.getScriptProperties().getProperty("GMAIL_FACTURAS_REMITENTE") || "marlonmunozpardo5@gmail.com").trim();
const USUARIO_NOMBRE_COMPLETO = String(PropertiesService.getScriptProperties().getProperty("USUARIO_NOMBRE_COMPLETO") || "MARLON MUNOZ PARDO").trim();
const ZONA_HORARIA = "America/Bogota";
const GMAIL_MAX_BODY_CHARS = 30000;
const GMAIL_MAX_ATTACHMENT_BYTES = 6 * 1024 * 1024;
"""

    full_code = header + "\n\n" + api_block + "\n\n/* =========================================================\n   FUNCIONES AUXILIARES DEL BOT Y PROCESAMIENTO\n   ========================================================= */\n\n" + pre_clean + "\n\n" + post_api

    with open(gas_path, "w", encoding="utf-8") as f:
        f.write(full_code)

    print(f"Reordered successfully! Total bytes: {len(full_code)}")

if __name__ == "__main__":
    main()
