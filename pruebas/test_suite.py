# -*- coding: utf-8 -*-
"""
Suite de Pruebas Automatizadas - Finanzas Personales PWA & Bot
Verifica la integridad de archivos, la base de datos Excel y el motor de procesamiento financiero.
"""

import os
import sys
import unittest
import re
from pathlib import Path

# Asegurar codificación UTF-8 en terminal Windows
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent

class TestArchivosProyecto(unittest.TestCase):
    """Verifica que todos los archivos clave del proyecto existan y no estén vacíos."""

    def test_archivos_frontend_criticos(self):
        archivos = [
            "index.html",
            "manifest.json",
            "sw.js",
            "css/styles.css",
            "js/app.js",
            "js/api.js",
            "js/nlp_parser.js",
            "assets/icon-192.png",
            "assets/icon-512.png",
            "assets/icon.svg",
        ]
        for rel_path in archivos:
            full_path = PROJECT_ROOT / rel_path
            self.assertTrue(full_path.exists(), f"Falta archivo crítico: {rel_path}")
            self.assertGreater(full_path.stat().st_size, 0, f"El archivo {rel_path} está vacío")

    def test_archivos_backend_y_herramientas(self):
        archivos = [
            "Panel_Finanzas.xlsx",
            "google_apps_script/Codigo.gs",
            "python_tools/financial_analyzer.py",
            "python_tools/export_to_excel.py",
        ]
        for rel_path in archivos:
            full_path = PROJECT_ROOT / rel_path
            self.assertTrue(full_path.exists(), f"Falta archivo: {rel_path}")
            self.assertGreater(full_path.stat().st_size, 0, f"El archivo {rel_path} está vacío")


class TestIntegridadExcel(unittest.TestCase):
    """Valida la estructura y coherencia del archivo Panel_Finanzas.xlsx."""

    def setUp(self):
        import openpyxl
        self.wb_path = PROJECT_ROOT / "Panel_Finanzas.xlsx"
        self.wb = openpyxl.load_workbook(self.wb_path, data_only=False)

    def test_hojas_requeridas(self):
        hojas_esperadas = ["PANEL", "MOVIMIENTOS", "HISTORICO FIJOS", "CONTROL FIJOS", "METAS AHORRO"]
        for hoja in hojas_esperadas:
            self.assertIn(hoja, self.wb.sheetnames, f"Falta la hoja requerida: {hoja}")

    def test_columnas_movimientos(self):
        ws = self.wb["MOVIMIENTOS"]
        # Fila 4 son los encabezados
        headers = [ws.cell(row=4, column=col).value for col in range(1, 16)]
        esperados_subset = ["FECHA", "DESCRIPCIÓN", "CATEGORÍA", "VALOR ($ COP)"]
        headers_str = " ".join([str(h).upper() for h in headers if h])
        for col_name in esperados_subset:
            self.assertIn(col_name, headers_str, f"Falta la columna {col_name} en MOVIMIENTOS")

    def test_sin_errores_ref(self):
        ws_mov = self.wb["MOVIMIENTOS"]
        # Validar que no haya fórmulas con #REF!
        for row in range(4, min(ws_mov.max_row + 1, 50)):
            for col in range(1, 16):
                val = str(ws_mov.cell(row=row, column=col).value or "")
                self.assertNotIn("#REF!", val, f"Error #REF! detectado en MOVIMIENTOS Celda fila {row}, col {col}")


class TestMotorNLP(unittest.TestCase):
    """Prueba la lógica de extracción de montos y categorías con jerga colombiana."""

    def parsear_monto_test(self, texto):
        t = texto.lower().strip()
        # "25 mil", "50k", "100 lucas"
        m_mil = re.search(r'(\d+(?:[.,]\d+)?)\s*(?:mil|k|lucas?)\b', t)
        if m_mil:
            val = float(m_mil.group(1).replace(",", "."))
            return int(round(val * 1000))

        # "1 millon", "2.5 millones", "1 palo"
        m_millon = re.search(r'(\d+(?:[.,]\d+)?)\s*(?:millones?|mill[oó]n|palos?)\b', t)
        if m_millon:
            val = float(m_millon.group(1).replace(",", "."))
            return int(round(val * 1000000))

        # "$ 25.000" o "25000" o "85000"
        m_num = re.search(r'\$?\s*(\d{1,3}(?:[.]\d{3})+|\d{3,})', t)
        if m_num:
            limpio = m_num.group(1).replace(".", "")
            return int(limpio)
        return 0

    def test_jerga_colombiana_montos(self):
        casos = [
            ("Almuerzo corrientazo 25 mil", 25000),
            ("Gaste 50 lucas en gasolina", 50000),
            ("Pagué 1 lucas de propina", 1000),
            ("Abono de 1.5 millones arriendo", 1500000),
            ("2 palos sueldo", 2000000),
            ("Salchipapa 15k con tarjeta", 15000),
            ("Compré víveres por $120.000", 120000),
            ("Mercado 85000 nequi", 85000),
        ]
        for frase, esperado in casos:
            resultado = self.parsear_monto_test(frase)
            self.assertEqual(resultado, esperado, f"Falló al parsear '{frase}': obtenido {resultado}, esperado {esperado}")


if __name__ == "__main__":
    print("==================================================")
    print("   PRUEBAS DEL SISTEMA FINANCIERO Y PWA")
    print("==================================================")
    unittest.main(verbosity=2)
