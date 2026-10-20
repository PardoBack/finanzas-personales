# Nota para Antigravity IDE: replicar el rediseño nativo en la web

La app Android nativa rediseñada en `android/app` es la referencia visual para una siguiente pasada del dashboard web. Replica ese lenguaje visual en `index.html` y `css/styles.css`, y sincroniza los recursos equivalentes en `app/src/main/assets/` para que el WebView histórico también quede alineado.

**En Android Studio, abre la carpeta `android/` para ejecutar la app nativa Compose.** El módulo `app/` de la raíz es un proyecto WebView anterior y no representa la interfaz Android rediseñada.

El rediseño Compose ya reorganizó el dashboard, dio más espacio a las acciones rápidas y simplificó la barra inferior a Inicio, Movs, Fijos, Más y un botón central para crear un movimiento. Presupuestos, Análisis, Metas e IA siguen accesibles desde Más. Usa ese comportamiento como referencia; no diseñes una navegación web que esconda esas secciones.

## Dirección visual acordada

- Interfaz financiera sobria, limpia y fácil de leer; evitar neones, brillos animados, bordes multicolor y exceso de glassmorphism.
- Fondo azul casi negro, superficies azul pizarra con bordes discretos, verde esmeralda como acento principal y violeta reservado para acciones de IA.
- Jerarquía de dashboard: saldo y disponible primero, acciones principales después, resumen de ingresos y gastos en tarjetas equilibradas, alertas y movimientos al final.
- En móvil, mantener acciones de tamaño cómodo, textos cortos, buen espacio entre controles y navegación inferior sencilla. Las secciones secundarias deben seguir siendo accesibles desde “Más”.
- El botón central para crear un movimiento debe destacar con un solo acento; los demás botones conservan el mismo alto, radio, alineación, feedback y estados de foco.
- Mantener contraste legible, áreas táctiles de al menos 44 px, soporte para zoom del navegador, safe areas y `prefers-reduced-motion`.

## Límites para preservar el funcionamiento

- No cambiar IDs del DOM, atributos `data-*`, nombres de eventos, contratos de API, Apps Script, Google Sheets, Gmail, Telegram, almacenamiento local ni flujos de sincronización.
- Conservar todos los destinos y acciones existentes; redistribuirlos en la navegación adaptable si hace falta, sin ocultar funciones.
- Comparar las pantallas web de escritorio y móvil con el dashboard nativo Compose antes de cerrar el trabajo.

## Contexto de pagos automáticos Nequi / Google Wallet

El usuario paga con su tarjeta débito Nequi Visa agregada a Google Wallet, incluso acercando el celular a un datáfono (NFC), y espera que el movimiento quede registrado con comercio, importe, fecha, cuenta y categoría. La app Android ya tiene un `NotificationListenerService` y un parser para intentar hacerlo desde notificaciones financieras.

- La web debe mostrar con claridad los movimientos que Android haya sincronizado, incluyendo su origen automático, comercio, medio de pago y estado de sincronización cuando esos campos existan.
- No diseñes un interruptor que prometa que el navegador puede leer pagos NFC, notificaciones de Nequi/Wallet o transacciones de otras apps. Esa captura pertenece a Android y requiere autorización explícita del usuario.
- Android solo puede detectar automáticamente los pagos para los que una app bancaria o Wallet publique una notificación útil. Sin una notificación compatible, se ofrece el registro manual del movimiento.
- El diseño de la web debe contemplar movimientos automáticos, por confirmar, confirmados y sincronizados sin inventar datos ni cambiar APIs/columnas sin revisar el contrato de Apps Script y Sheets.
- No mostrar notificaciones crudas, códigos OTP, números de tarjeta ni credenciales; limitar la pantalla a los campos necesarios del movimiento.
- La interfaz web puede presentar y filtrar los movimientos automáticos ya sincronizados, pero no debe afirmar que el navegador detecta por sí mismo el toque NFC ni las notificaciones de otras aplicaciones. En el registro, distinguir el origen Android automático y permitir corregir comercio/categoría si esos campos fueron sincronizados.

## Archivos de referencia

- Dashboard Android: `android/app/src/main/java/com/example/appfinanzas/ui/screens/DashboardScreen.kt`
- Navegación Android: `android/app/src/main/java/com/example/appfinanzas/ui/FinanceApp.kt`
- Componentes visuales Android: `android/app/src/main/java/com/example/appfinanzas/ui/components/Glassmorphism.kt`
- Tokens de tema Android: `android/app/src/main/java/com/example/appfinanzas/ui/theme/Color.kt`
- Dashboard web: `index.html`, `css/styles.css`

--- CONTEXTO DEL USUARIO Y DE LAS CAPTURAS (LEER ANTES DE EDITAR) ---
El usuario quiere una interfaz de finanzas personales que se sienta profesional, clara y cómoda. En las capturas reales de un Samsung se ven problemas concretos: la tira de meses queda cortada, tarjetas y textos se desbordan a la derecha, el encabezado se cruza con la barra de estado, los filtros no caben y los movimientos/gastos fijos aparecen como tablas de escritorio ilegibles en móvil. No basta con cambiar la paleta: hay que arreglar el layout y conservar las acciones reales.

Lee esta nota completa y luego inspecciona el HTML/CSS/JS actual. Implementa cambios concretos en el sitio y sus recursos de producción. No respondas solo con un prompt genérico ni asumas que otro asistente completó la tarea. El Android nativo es Compose; esta nota es para Antigravity IDE y para la versión web.

OBJETIVOS VISUALES Y DE RESPONSIVIDAD
- Haz que el dashboard y cada vista se adapten al viewport real del teléfono sin overflow horizontal de la página. Las tiras que sí son desplazables deben desplazarse dentro de su propio contenedor y mostrar claramente que hay más opciones.
- En ancho móvil, reemplaza tablas densas de movimientos y gastos fijos por filas/tarjetas apiladas legibles o una presentación responsive equivalente. Mantén búsqueda, filtros, acciones y todos los campos importantes accesibles; no reduzcas la tabla completa hasta hacerla ilegible.
- Mantén encabezados bajo el área segura del dispositivo. Revisa `viewport-fit`, `env(safe-area-inset-*)`, posición de navegación inferior y comportamiento con zoom; evita padding duplicado.
- Reorganiza tarjetas de métricas y resúmenes con columnas `minmax(0, 1fr)`, tamaños flexibles, wrapping y jerarquía clara. Los nombres, labels y valores COP largos no deben empujar ni recortar la vista.
- Haz que botones y chips sean coherentes, cómodos de tocar, con texto legible y foco/selección visibles. Mantén navegación y jerarquía visual consistentes con la app nativa.
- Usa una identidad sobria: fondo azul profundo, superficies pizarra, bordes discretos, verde esmeralda como acento, rojo para gastos/alertas y violeta reservado para IA. Elimina exceso de neón/glow si interfiere con lectura.
- Incluye estados sin datos, carga, error y desconexión que expliquen qué puede hacer el usuario; no finjas sincronización activa ni datos inexistentes.

CONEXIÓN CON ANDROID Y MOVIMIENTOS AUTOMÁTICOS
El usuario quiere que un pago con la tarjeta Nequi Visa en Google Wallet (incluido el pago acercando el celular a un datáfono) aparezca como movimiento, con valor, comercio, fecha, medio de pago y categoría cuando esos datos estén disponibles.

La detección pertenece a la app Android nativa: una app web no puede leer por sí sola pagos NFC, el elemento seguro, datáfonos ni notificaciones de otras apps. Android puede recibir notificaciones financieras compatibles solo después de que el usuario conceda el permiso de acceso a notificaciones; Nequi/Wallet también deben publicar el aviso. Puede que alguna compra no produzca aviso útil, así que no prometas cobertura completa.

La web debe presentar con claridad movimientos que Android haya sincronizado: origen automático/manual, comercio, importe, fecha, cuenta/medio y estados de revisión/sincronización solo cuando los datos existan en el contrato actual. Permite buscar y filtrar registros automáticos; si las columnas actuales no tienen un dato o estado, no inventes valores ni cambies unilateralmente Apps Script/Sheets. Marca movimientos dudosos como pendientes de confirmar si el modelo actual ya lo permite; coordina cualquier cambio de contrato con el proyecto Android.

Privacidad: nunca renderizar texto crudo de notificaciones, OTP, número de tarjeta, tokens ni credenciales. No diseñar un botón “activar NFC automático” en la web que sugiera una capacidad del navegador. Incluye una explicación breve que dirija al usuario a configurar permisos en Android y un fallback de registro manual.

RESTRICCIONES PARA PRESERVAR INTEGRACIONES
- Conserva IDs del DOM, atributos `data-*`, nombres de eventos y callbacks que consuma `js/app.js`.
- No cambies contratos de API, Apps Script, Sheets, Gmail, Telegram ni almacenamiento local sin rastrear todas las dependencias y mantener compatibilidad.
- Si cambias markup de tablas a tarjetas móviles, conserva los datos y acciones de cada registro; usa CSS responsive y estructura semántica compatible con el render dinámico existente.
- La carpeta `app/src/main/assets/` pertenece al cliente Android WebView antiguo. Sincroniza allí solo los recursos web que realmente forman parte del build de ese módulo y evita dejar el sitio web y los assets en versiones distintas.
- Android Studio debe abrir `android/` para la app nativa Compose. No presentes el módulo WebView raíz como si fuera la app nativa.

FORMA DE TRABAJO Y ENTREGA
1. Lee la nota, inspecciona el estado real de `index.html`, `css/styles.css`, JS de render y recursos publicados antes de editar.
2. Corrige primero overflow y distribución móvil en Inicio, Movimientos y Gastos Fijos, que son las pantallas de las capturas; después revisa escritorio para no dañarlo.
3. Prueba en un viewport móvil cercano a 393–430 px y en escritorio. Comprueba que no haya scroll horizontal del documento, que sí se pueda desplazar dentro de selectores horizontales y que los registros completos/acciones sigan disponibles.
4. No elimines integraciones para simplificar el diseño. Resume cambios, verificaciones y datos/casos que no pudiste confirmar.
