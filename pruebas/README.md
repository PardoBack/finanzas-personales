# 🧪 Carpeta de Pruebas - Finanzas Personales PWA & Bot

Esta carpeta contiene utilidades, scripts de verificación y launchers para probar todo el ecosistema de finanzas desde cualquier terminal de comandos (`CMD` o `PowerShell`) o haciendo doble clic.

---

## 🚀 Cómo Activar las Pruebas desde CMD

### Opción 1: Menú Interactivo (Recomendado)
Abre un `CMD` en Windows y ejecuta:
```cmd
cd "c:\Users\Marlon\OneDrive\Finanzas Personales Gym\pruebas"
iniciar_pruebas.bat
```
*(O simplemente haz doble clic sobre el archivo `iniciar_pruebas.bat` en el Explorador de Archivos)*.

Verás un menú con las siguientes opciones:
1. **Iniciar Servidor Local PWA**: Abre automáticamente `http://localhost:3000` en tu navegador con la vista de app móvil y dashboard.
2. **Ejecutar Suite de Pruebas Unitarias**: Valida que los archivos frontend, fórmulas de Excel y la integración no tengan errores.
3. **Probar Motor NLP con Slang Colombiano**: Te permite escribir frases como *"30 lucas almuerzo"*, *"50 mil gasolina"*, *"1.5 millones arriendo"* y ver la extracción en menos de 5ms.
4. **Analizador 50/30/20**: Diagnóstico financiero de tus ahorros y gastos.
5. **Generar Exportación Excel**: Crea un archivo formateado en `exports/`.
6. **Ver Estado de Git**: Muestra si hay cambios pendientes o sincronizados.

---

### Opción 2: Iniciar solo el Servidor Web
```cmd
cd "c:\Users\Marlon\OneDrive\Finanzas Personales Gym\pruebas"
servidor_local.bat
```

---

### Opción 3: Ejecutar la Suite de Pruebas Directamente
```cmd
python test_suite.py
```

---

### Opción 4: Probar una Frase en NLP desde Línea de Comandos
```cmd
python probar_nlp.py "Gaste 45 lucas en hamburguesa con tarjeta credito"
```
Salida en consola:
```json
{
    "frase_original": "Gaste 45 lucas en hamburguesa con tarjeta credito",
    "tipo": "Gasto",
    "monto_cop": "$45.000",
    "monto_numero": 45000,
    "categoria": "Alimentación",
    "metodo_pago": "Tarjeta Crédito"
}
```
