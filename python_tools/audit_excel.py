import sys
if hasattr(sys.stdout, "reconfigure"): sys.stdout.reconfigure(encoding="utf-8")
import openpyxl

wb = openpyxl.load_workbook('Panel_Finanzas.xlsx', data_only=True)
print("=== AUDITORÍA DE ERRORES EN Panel_Finanzas.xlsx ===")
errores = []

for sheetname in wb.sheetnames:
    ws = wb[sheetname]
    for r in range(1, ws.max_row + 1):
        for c in range(1, ws.max_column + 1):
            val = ws.cell(r, c).value
            if isinstance(val, str) and (val.startswith("#REF") or val.startswith("#VAL") or val.startswith("#NAME") or val.startswith("#N/A") or val.startswith("#DIV")):
                errores.append((sheetname, ws.cell(r, c).coordinate, val))

if errores:
    print(f"⚠️ Se encontraron {len(errores)} celdas con errores:")
    for sheet, coord, val in errores[:20]:
        print(f"  [{sheet}] {coord}: {val}")
else:
    print("✅ No hay celdas con errores de fórmula (#REF!, #VALUE!, #DIV/0!) en ninguna hoja.")
