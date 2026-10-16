# ☁️ Guía de Implementación en Google Apps Script

Este archivo explica cómo activar la API Web en tu proyecto de Google Apps Script para que el Dashboard Web pueda leer y guardar datos en tu Google Sheet en tiempo real.

---

### Paso 1: Abrir el editor de Apps Script
1. Entra a tu Google Sheet de finanzas.
2. En el menú superior, haz clic en **Extensiones** > **Apps Script**.

### Paso 2: Actualizar el código
1. Copia todo el contenido de [`Codigo.gs`](Codigo.gs).
2. Pégalo en tu editor de Google Apps Script reemplazando el contenido existente.
3. Guarda con `Ctrl + S`.

### Paso 3: Crear la Implementación Web App
1. Arriba a la derecha, haz clic en el botón azul **Implementar** > **Nueva implementación**.
2. Haz clic en el ícono de engranaje ⚙️ junto a "Seleccionar tipo" y elige **Aplicación web**.
3. Configura exactamente estos campos:
   - **Descripción**: `API Dashboard V26`
   - **Ejecutar como**: `Yo (tu correo de Google)`
   - **Quién tiene acceso**: `Cualquier persona` (Esto permite que el dashboard web pueda consultar los datos sin pedir login de Google).
4. Haz clic en **Implementar**.
5. Google te mostrará una **URL de la aplicación web** que termina en `/exec`. Copia esa URL.

### Paso 4: Vincular con tu Dashboard
1. Abre tu Dashboard Web (`index.html`).
2. Haz clic en el ícono de engranaje ⚙️ (arriba a la derecha).
3. Pega la URL `/exec` en el campo correspondiente y haz clic en **Guardar Cambios**.
4. ¡Listo! Tu dashboard ya leerá y registrará movimientos directamente en tu Google Sheet.
