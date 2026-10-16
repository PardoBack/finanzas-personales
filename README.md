# 📊 PardoFinanzas PRO — Dashboard Financiero & Google Sheets

> Sistema integral de control financiero personal, automatización de nómina Colanta y gestión presupuestaria con **Dashboard Web**, **Google Sheets** y herramientas en **Python**.

---

## 🌟 Características Principales

- 💳 **Dashboard Web Ultra-Moderno**: Interfaz con tema oscuro (Dark Mode), Glassmorphism, diseño responsivo y métricas en vivo.
- 📊 **Gráficos Dinámicos con Chart.js**: Distribución de gastos por categoría y balanza quincenal (Q1 vs Q2).
- 📌 **Monitor de Compromisos Fijos**: Semáforo de obligaciones mensuales (Pagado, Abono, Pendiente).
- 🐜 **Radar de Gastos Hormiga**: Seguimiento de micropagos y fugas de capital.
- 📥 **Exportación a Excel**: Descarga directa de archivos `.xlsx` formateados con fórmulas y estilos bancarios.
- 🐍 **Módulo Python 3.12**: Scripts de auditoría, regla 50/30/20 y exportador automatizado con `pandas` y `openpyxl`.
- ☁️ **100% Gratuito**: Alojamiento sin costo en **GitHub Pages** o **Vercel**, con base de datos en **Google Sheets**.

---

## 📁 Estructura del Proyecto

```text
├── index.html                   # Dashboard Web Principal
├── css/
│   └── styles.css               # Sistema de diseño, Glassmorphism y animaciones
├── js/
│   ├── api.js                   # Conexión con Google Apps Script y caché local
│   └── app.js                   # Controlador de gráficos, tablas, modales y eventos
├── google_apps_script/
│   ├── Codigo.gs                # Backend API REST (doGet/doPost) y Bot Telegram
│   └── README_APPS_SCRIPT.md    # Guía de implementación en Google Apps Script
├── python_tools/
│   ├── export_to_excel.py       # Descargador y formateador profesional a Excel (.xlsx)
│   ├── financial_analyzer.py    # Auditoría financiera CLI y diagnóstico 50/30/20
│   └── requirements.txt         # Dependencias Python (requests, pandas, openpyxl)
├── exports/                     # Carpeta de salida para reportes locales de Excel
├── .gitignore                   # Archivos ignorados por Git
└── README.md                    # Documentación del proyecto
```

---

## 🚀 Despliegue 100% Gratuito en GitHub Pages

Para publicar tu Dashboard en internet y acceder desde tu celular o computadora:

1. **Crea un repositorio en GitHub**:
   - Ingresa a [github.com/new](https://github.com/new) y nómbralo `finanzas-personales` (o el nombre que prefieras).
   - Déjalo en modo **Público** o **Privado**.

2. **Sube tus archivos**:
   ```bash
   git add .
   git commit -m "feat: initial release PardoFinanzas PRO Dashboard"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
   git push -u origin main
   ```

3. **Activar GitHub Pages**:
   - En tu repositorio de GitHub, ve a **Settings** (Configuración) > pestaña **Pages** (lado izquierdo).
   - En **Source**, selecciona `Deploy from a branch`.
   - En **Branch**, selecciona `main` y la carpeta `/ (root)`.
   - Haz clic en **Save**. En 1 minuto tendrás tu enlace HTTPS gratuito (ej: `https://tu-usuario.github.io/finanzas-personales/`).

---

## ⚡ Despliegue Alternativo en Vercel (1 Clic)

1. Ingresa a [vercel.com](https://vercel.com) e inicia sesión con tu cuenta de GitHub.
2. Haz clic en **Add New Project** y selecciona tu repositorio.
3. Haz clic en **Deploy**. ¡Listo en 10 segundos!

---

## 🐍 Uso de Herramientas Python

### 1. Instalación de dependencias:
```bash
pip install -r python_tools/requirements.txt
```

### 2. Exportar datos a Excel (.xlsx):
Genera una hoja de cálculo local estilizada en la carpeta `exports/`:
```bash
python python_tools/export_to_excel.py
```

### 3. Diagnóstico de Salud Financiera (50/30/20):
```bash
python python_tools/financial_analyzer.py
```

---

## ⚙️ Conexión con Google Sheets

1. Abre tu hoja de cálculo en [Google Sheets](https://docs.google.com/spreadsheets).
2. Ve a **Extensiones** > **Apps Script**.
3. Pega el código de [`google_apps_script/Codigo.gs`](google_apps_script/Codigo.gs).
4. Haz clic en **Implementar** > **Nueva implementación**:
   - Tipo: **Aplicación web**.
   - Ejecutar como: **Yo**.
   - Quién tiene acceso: **Cualquier persona**.
5. Copia la URL terminada en `/exec` y pégala en el icono de configuración ⚙️ del Dashboard Web.

---

Desarrollado para la gestión financiera personal de **Marlon Muñoz Pardo** en Medellín, Colombia.
