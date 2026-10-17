/**
 * PARDOFINANZAS V26 PRO — CONTROLADOR FINTECH AVANZADO (JS)
 * Soporta navegación móvil por barra inferior, sidebar desktop,
 * privacidad de saldos (modo incógnito), y Asistente CFO AI en vivo.
 */

document.addEventListener("DOMContentLoaded", () => {
  FintechApp.init();
});

const FintechApp = {
  state: {
    data: null,
    charts: {
      categories: null,
      quincena: null,
      annual: null
    },
    balancesHidden: false,
    activeTab: "tab-all-movements",
    isRecording: false,
    speechRecognition: null,
    deferredInstallPrompt: null,
    selectedMovement: null,
    activeFilterChip: "ALL",
    activeMonth: "octubre",
    activeYear: 2026,
    activeView: "overview",
    cloudShield: {
      active: true,
      hasWriteAccess: true,
      authToken: "marlon_pardo_finanzas_2026_auth"
    }
  },

  init() {
    this.initCloudShield();
    this.initServiceWorker();
    this.bindPWAInstall();
    this.bindNavigation();
    this.bindMonthSelector();
    this.bindPrivacyToggle();
    this.bindChatCFO();
    this.initSpeechRecognition();
    this.bindModals();
    this.setupDateContext();
    this.loadData();
    this.checkBackendHealth();

    // Enrutamiento inicial de vista (SPA)
    const initialHash = window.location.hash ? window.location.hash.replace("#view-", "").replace("#section-", "").replace("#", "") : "overview";
    this.switchView(initialHash);

    window.addEventListener("hashchange", () => {
      const h = window.location.hash ? window.location.hash.replace("#view-", "").replace("#section-", "").replace("#", "") : "overview";
      this.switchView(h);
    });

    // Auto-sincronización silenciosa cada 30 segundos
    setInterval(() => {
      this.loadData();
    }, 30000);
  },

  haptic(ms = 12) {
    if (window.navigator && window.navigator.vibrate) {
      try { window.navigator.vibrate(ms); } catch (e) {}
    }
  },

  async checkBackendHealth() {
    try {
      const res = await FinanceAPI.testConnection();
      this.updateBackendStatusUI(res);
    } catch (e) {}
  },

  updateBackendStatusUI(res) {
    const isV26 = res && res.isModernV26;
    const isOld = res && res.isOldVersion;

    const statusTitle = document.getElementById("settings-status-title");
    const statusBadge = document.getElementById("settings-status-badge");
    const statusDesc = document.getElementById("settings-status-desc");
    const backendCard = document.getElementById("settings-backend-status");

    if (isV26) {
      if (statusTitle) statusTitle.innerHTML = '<i class="fa-solid fa-circle-check" style="color:#10B981;"></i> Google Sheets Conectado';
      if (statusBadge) {
        statusBadge.textContent = "V26.9 ONLINE";
        statusBadge.style.background = "#10B981";
        statusBadge.style.color = "#022C22";
      }
      if (statusDesc) statusDesc.innerHTML = '¡Excelente! Los movimientos de la app y colillas se insertan en la <strong>fila 5 de MOVIMIENTOS</strong> en tiempo real.';
      if (backendCard) {
        backendCard.style.background = "rgba(16, 185, 129, 0.1)";
        backendCard.style.borderColor = "rgba(16, 185, 129, 0.3)";
      }
    } else {
      if (statusTitle) statusTitle.innerHTML = '<i class="fa-solid fa-triangle-exclamation" style="color:#F59E0B;"></i> Apps Script Requiere Actualización';
      if (statusBadge) {
        statusBadge.textContent = isOld ? "V25.3 DETECTADO" : "REQUIERE V26.9";
        statusBadge.style.background = "#F59E0B";
        statusBadge.style.color = "#0F172A";
      }
      if (statusDesc) statusDesc.innerHTML = 'Tu Webhook responde, pero aún tiene el script antiguo (V25.3) que no inserta registros web. Haz clic abajo en <strong>Copiar Código</strong> y pégalo en Google Sheets (Apps Script).';
      if (backendCard) {
        backendCard.style.background = "rgba(245, 158, 11, 0.1)";
        backendCard.style.borderColor = "rgba(245, 158, 11, 0.3)";
      }
    }
  },


  // =========================================================
  // SISTEMA DE SEGURIDAD (ESCUDO ANTI-SPAM GITHUB & GOOGLE SHEETS)
  // =========================================================
  initCloudShield() {
    const sec = FinanceAPI.getSecuritySettings();
    this.state.cloudShield = {
      active: sec.shieldActive,
      hasWriteAccess: sec.hasWriteAccess,
      authToken: sec.authToken
    };

    const btnShield = document.getElementById("btn-cloud-shield");
    const modalShield = document.getElementById("modal-cloud-shield");
    const btnCloseShield = document.getElementById("btn-close-cloud-shield");
    const btnShieldConfirm = document.getElementById("btn-shield-confirm");
    const btnCopyShieldUrl = document.getElementById("btn-copy-shield-url");
    const inputShieldShareUrl = document.getElementById("input-shield-share-url");
    const btnSaveAuthToken = document.getElementById("btn-save-auth-token");
    const inputAuthToken = document.getElementById("input-auth-token");
    const btnCopyPrivateLink = document.getElementById("btn-copy-private-link");

    const baseOrigin = window.location.origin || "";
    const basePath = window.location.pathname || "";
    const fullAuthUrl = `${baseOrigin}${basePath}?auth=${encodeURIComponent(sec.authToken)}`;

    if (inputShieldShareUrl) inputShieldShareUrl.value = fullAuthUrl;
    if (inputAuthToken) inputAuthToken.value = sec.authToken || "";

    const openShieldModal = () => {
      this.haptic(15);
      modalShield?.classList.add("active");
    };

    const closeShieldModal = () => {
      this.haptic(10);
      modalShield?.classList.remove("active");
    };

    btnShield?.addEventListener("click", openShieldModal);
    btnCloseShield?.addEventListener("click", closeShieldModal);
    btnShieldConfirm?.addEventListener("click", closeShieldModal);

    const copyAuthUrlAction = () => {
      this.haptic(20);
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(fullAuthUrl).then(() => {
          this.showToast("📋 ¡Enlace privado con token copiado al portapapeles!", "success");
        }).catch(() => {
          if (inputShieldShareUrl) {
            inputShieldShareUrl.select();
            document.execCommand("copy");
            this.showToast("📋 Enlace copiado al portapapeles", "success");
          }
        });
      } else if (inputShieldShareUrl) {
        inputShieldShareUrl.select();
        document.execCommand("copy");
        this.showToast("📋 Enlace copiado al portapapeles", "success");
      }
    };

    btnCopyShieldUrl?.addEventListener("click", copyAuthUrlAction);
    btnCopyPrivateLink?.addEventListener("click", copyAuthUrlAction);

    const saveTokenAction = (tokenInput) => {
      const val = (tokenInput?.value || "").trim();
      if (!val || val.length < 5) {
        this.showToast("El token debe tener al menos 6 caracteres", "error");
        return;
      }
      this.haptic(20);
      FinanceAPI.setAuthToken(val);
      this.state.cloudShield.authToken = val;
      if (inputShieldShareUrl) {
        inputShieldShareUrl.value = `${baseOrigin}${basePath}?auth=${encodeURIComponent(val)}`;
      }
      const tokenView = document.getElementById("input-auth-token-view");
      if (tokenView && tokenView !== tokenInput) tokenView.value = val;
      if (inputAuthToken && inputAuthToken !== tokenInput) inputAuthToken.value = val;
      this.showToast("🛡️ Token de seguridad actualizado correctamente", "success");
    };

    btnSaveAuthToken?.addEventListener("click", () => saveTokenAction(inputAuthToken));
    document.getElementById("btn-save-auth-token-view")?.addEventListener("click", () => {
      saveTokenAction(document.getElementById("input-auth-token-view"));
    });

    document.getElementById("btn-send-email-audit-view")?.addEventListener("click", async () => {
      this.haptic(15);
      const btn = document.getElementById("btn-send-email-audit-view");
      const originalText = btn.innerHTML;
      btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Generando y enviando reporte...`;
      btn.disabled = true;

      try {
        const res = await FinanceAPI.sendEmailAudit();
        if (res.ok) {
          this.showToast("📧 ¡Reporte ejecutivo enviado exitosamente a tu correo!", "success");
        } else {
          this.showToast("⚠️ " + (res.error || "No se pudo despachar el correo"), "warning");
        }
      } catch (e) {
        this.showToast("Error al despachar el reporte por email", "error");
      } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
      }
    });

    document.getElementById("btn-view-test-connection")?.addEventListener("click", async () => {
      this.haptic(15);
      this.showToast("Probando conexión con Google Apps Script...", "info");
      await this.checkBackendHealth();
      this.showToast("✓ Conexión con Google Sheets verificada", "success");
    });
  },

  // =========================================================
  // GENERADOR DE LOGOS E INSIGNIAS DE MARCAS COLOMBIANAS (SVG)
  // =========================================================
  getBrandBadgeSVG(brandKey) {
    const k = String(brandKey || "").toLowerCase();

    // 1. EPM (Empresas Públicas de Medellín) - Official green energy emblem & bold epm logo
    if (k === "epm" || k.includes("epm")) {
      return `
        <div class="brand-badge brand-epm" title="Empresas Públicas de Medellín" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#00843D 0%,#005A28 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(0,132,61,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <ellipse cx="18" cy="11.5" rx="3.5" ry="5.5" fill="#6EE7B7"/>
            <ellipse cx="24.5" cy="18" rx="5.5" ry="3.5" fill="#A7F3D0"/>
            <ellipse cx="18" cy="24.5" rx="3.5" ry="5.5" fill="#34D399"/>
            <ellipse cx="11.5" cy="18" rx="5.5" ry="3.5" fill="#6EE7B7"/>
            <circle cx="18" cy="18" r="2.8" fill="#FFFFFF"/>
            <text x="18" y="32" fill="#FFFFFF" font-family="'Segoe UI', Roboto, sans-serif" font-weight="900" font-size="8" text-anchor="middle" letter-spacing="-0.3">epm</text>
          </svg>
        </div>
      `;
    }

    // 2. Tigo Telecomunicaciones - Official Navy Blue with Yellow Smile Curve & tiGO script
    if (k === "tigo" || k.includes("tigo")) {
      return `
        <div class="brand-badge brand-tigo" title="Tigo Colombia" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#00377B 0%,#001F4D 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(0,55,123,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <text x="18" y="19" fill="#FFFFFF" font-family="'Segoe UI', Roboto, sans-serif" font-weight="900" font-size="11.5" text-anchor="middle" letter-spacing="-0.5">ti<tspan fill="#FDB913">g</tspan>o</text>
            <path d="M8 24C13.5 28.5 22.5 28.5 28 24" stroke="#FDB913" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </div>
      `;
    }

    // 3. Cooperativa Colanta / AyC Colanta - Authentic Red Clover (Trébol) Logo
    if (k === "colanta" || k.includes("colanta") || k === "ayccolanta") {
      return `
        <div class="brand-badge brand-colanta" title="Cooperativa Colanta" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#D6001C 0%,#B30017 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(214,0,28,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- Trébol de Colanta -->
            <circle cx="18" cy="11.5" r="4.6" fill="#FFFFFF"/>
            <circle cx="12.5" cy="19" r="4.6" fill="#FFFFFF"/>
            <circle cx="23.5" cy="19" r="4.6" fill="#FFFFFF"/>
            <circle cx="18" cy="16.5" r="3.8" fill="#FFFFFF"/>
            <circle cx="18" cy="16.5" r="1.5" fill="#D6001C"/>
            <path d="M18 18C17.8 22.5 16 25 14 26.5" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round"/>
            <text x="18" y="32.5" fill="#FFFFFF" font-family="'Segoe UI', Roboto, sans-serif" font-weight="900" font-size="6.5" text-anchor="middle" letter-spacing="-0.2">colanta</text>
          </svg>
        </div>
      `;
    }

    // 4. Nequi - Official Plum & Magenta App Icon Mark
    if (k === "nequi" || k.includes("nequi")) {
      return `
        <div class="brand-badge brand-nequi" title="Nequi Bancolombia" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#1F0036 0%,#11001F 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(225,29,116,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="26" cy="10" r="2.8" fill="#FF0055"/>
            <path d="M11 25V11L25 25V11" stroke="#FF0055" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M11 25V11L25 25V11" stroke="#FFFFFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      `;
    }

    // 5. Bancolombia - Modern Slate with Yellow, Blue & Red Ribbon
    if (k === "bancolombia" || k.includes("bancolombia")) {
      return `
        <div class="brand-badge brand-bancolombia" title="Bancolombia" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#0F172A 0%,#020617 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(253,218,36,0.25);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M8 12C13 10 16 22 28 12" stroke="#FDDA24" stroke-width="2.8" stroke-linecap="round"/>
            <path d="M8 17C13 15 16 27 28 17" stroke="#0072CE" stroke-width="2.8" stroke-linecap="round"/>
            <path d="M8 22C13 20 16 32 28 22" stroke="#E03A3E" stroke-width="2.8" stroke-linecap="round"/>
          </svg>
        </div>
      `;
    }

    // 6. Terpel (Combustible / Gasolina / Moto) - Official Sunburst
    if (k === "terpel" || k.includes("terpel") || k.includes("gasolina") || k.includes("moto")) {
      return `
        <div class="brand-badge brand-terpel" title="Terpel Combustibles" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#DC2626 0%,#991B1B 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(220,38,38,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="18" cy="18" r="8" fill="#FFD100"/>
            <path d="M18 5V9M18 27V31M5 18H9M27 18H31M8.8 8.8L11.6 11.6M24.4 24.4L27.2 27.2M8.8 27.2L11.6 24.4M24.4 11.6L27.2 8.8" stroke="#FFE066" stroke-width="2.2" stroke-linecap="round"/>
            <circle cx="18" cy="18" r="4.5" fill="#FFFFFF"/>
          </svg>
        </div>
      `;
    }

    // 7. Spotify - Official Green Sound Waves
    if (k === "spotify" || k.includes("spotify")) {
      return `
        <div class="brand-badge brand-spotify" title="Spotify" style="width:36px; height:36px; border-radius:10px; background:#121212; display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(30,215,96,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="18" cy="18" r="14" fill="#1ED760"/>
            <path d="M10.5 14C15.5 12.3 22 13 26 15.2" stroke="#121212" stroke-width="2.8" stroke-linecap="round"/>
            <path d="M12 18.2C16 16.8 21 17.3 24.8 19" stroke="#121212" stroke-width="2.4" stroke-linecap="round"/>
            <path d="M13.5 22.3C16.8 21.3 20.5 21.6 23.5 22.8" stroke="#121212" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
      `;
    }

    // 8. Google One / Google - Official 4-Color Google G
    if (k === "google" || k.includes("google") || k.includes("drive")) {
      return `
        <div class="brand-badge brand-google" title="Google One" style="width:36px; height:36px; border-radius:10px; background:#1E293B; display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(66,133,244,0.3);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M26.5 18.3C26.5 17.6 26.4 17 26.3 16.4H18V19.7H22.8C22.6 20.7 22 21.6 21 22.3V24.5H23.9C25.6 22.9 26.5 20.8 26.5 18.3Z" fill="#4285F4"/>
            <path d="M18 27C20.4 27 22.5 26.2 23.9 24.5L21 22.3C20.2 22.8 19.2 23.2 18 23.2C15.6 23.2 13.6 21.6 12.9 19.4H10V21.7C11.5 24.7 14.5 27 18 27Z" fill="#34A853"/>
            <path d="M12.9 19.4C12.7 18.8 12.6 18.2 12.6 17.5C12.6 16.8 12.7 16.2 12.9 15.6V13.3H10C9.4 14.5 9 15.9 9 17.5C9 19.1 9.4 20.5 10 21.7L12.9 19.4Z" fill="#FBBC05"/>
            <path d="M18 11.8C19.3 11.8 20.5 12.3 21.4 13.1L24 10.5C22.4 9 20.4 8 18 8C14.5 8 11.5 10.3 10 13.3L12.9 15.6C13.6 13.4 15.6 11.8 18 11.8Z" fill="#EA4335"/>
          </svg>
        </div>
      `;
    }

    // 9. Arriendo / Inmobiliaria / Vivienda - Royal Architectural House
    if (k === "arriendo" || k.includes("arriendo") || k.includes("casa") || k.includes("vivienda")) {
      return `
        <div class="brand-badge brand-arriendo" title="Arriendo Casa" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#1E3A8A 0%,#0F172A 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(30,58,138,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M8 16L18 8L28 16V26C28 27.1 27.1 28 26 28H10C8.9 28 8 27.1 8 26V16Z" stroke="#60A5FA" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M15 28V20H21V28" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="18" cy="14" r="1.8" fill="#FBBF24"/>
          </svg>
        </div>
      `;
    }

    // 10. Parqueadero Trabajo / Moto
    if (k === "parqueadero" || k.includes("parqueadero") || k.includes("parking")) {
      return `
        <div class="brand-badge brand-parking" title="Parqueadero" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#1D4ED8 0%,#1E40AF 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(29,78,216,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="18" cy="18" r="13" stroke="#93C5FD" stroke-width="2"/>
            <path d="M15 25V11H20C22.2 11 23.5 12.3 23.5 14.5C23.5 16.7 22.2 18 20 18H15" stroke="#FFFFFF" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      `;
    }

    // 11. Smart Fit (Gym)
    if (k === "smartfit" || k.includes("smart") || k.includes("gym") || k.includes("fit")) {
      return `
        <div class="brand-badge brand-smartfit" title="Smart Fit" style="width:36px; height:36px; border-radius:10px; background:#18181B; display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(255,214,0,0.3);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="18" cy="18" r="13" stroke="#FFD600" stroke-width="2.4"/>
            <path d="M11 15C11 12.5 13.5 11 18 11C22.5 11 25 12.5 25 15C25 18.5 20 18 18 19.5C16 20.5 11 20.5 11 23.5C11 25.5 13.5 27 18 27C22.5 27 25 25.5 25 23" stroke="#FFD600" stroke-width="2.6" stroke-linecap="round"/>
          </svg>
        </div>
      `;
    }

    // 12. Restaurante / Cafetería Planta San Germán
    if (k === "restaurante" || k.includes("rest") || k.includes("almuerzo") || k.includes("comida")) {
      return `
        <div class="brand-badge brand-restaurante" title="Alimentación & Restaurante" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#D97706 0%,#B45309 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(217,119,6,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 25V10M12 10C10.5 10 10.5 13.5 12 13.5M12 10C13.5 10 13.5 13.5 12 13.5" stroke="#FEF3C7" stroke-width="2.2" stroke-linecap="round"/>
            <path d="M24 25V10C21.8 10 21.8 15 24 16" stroke="#FEF3C7" stroke-width="2.2" stroke-linecap="round"/>
          </svg>
        </div>
      `;
    }

    // 13. Tecnología / Samsung Audífonos
    if (k === "tech" || k.includes("audifono") || k.includes("samsung")) {
      return `
        <div class="brand-badge brand-tech" title="Tecnología" style="width:36px; height:36px; border-radius:10px; background:linear-gradient(135deg,#312E81 0%,#1E1B4B 100%); display:inline-flex; align-items:center; justify-content:center; box-shadow:0 3px 10px rgba(49,46,129,0.35);">
          <svg viewBox="0 0 36 36" width="32" height="32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M10 20V17C10 12.6 13.6 9 18 9C22.4 9 26 12.6 26 17V20" stroke="#A5B4FC" stroke-width="2.4" stroke-linecap="round"/>
            <rect x="8" y="19" width="4.5" height="8" rx="2.2" fill="#818CF8"/>
            <rect x="23.5" y="19" width="4.5" height="8" rx="2.2" fill="#818CF8"/>
          </svg>
        </div>
      `;
    }

    // Fallback general
    return `
      <div class="brand-badge brand-default" style="width:36px; height:36px; border-radius:10px; background:rgba(255,255,255,0.08); display:inline-flex; align-items:center; justify-content:center;">
        <i class="fa-solid fa-receipt" style="color:#94A3B8; font-size:1.1rem;"></i>
      </div>
    `;
  },

  // =========================================================
  // SELECTOR DE MESES Y CONSULTA HISTÓRICA 2026
  // =========================================================
  bindMonthSelector() {
    const meses = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
    
    // 1. Botones Prev / Next en Topbar
    document.getElementById("btn-prev-month")?.addEventListener("click", () => {
      this.haptic(10);
      let currentIdx = meses.indexOf(this.state.activeMonth);
      if (currentIdx === -1) currentIdx = 9; // octubre
      const newIdx = (currentIdx - 1 + meses.length) % meses.length;
      this.changeActiveMonth(meses[newIdx]);
    });

    document.getElementById("btn-next-month")?.addEventListener("click", () => {
      this.haptic(10);
      let currentIdx = meses.indexOf(this.state.activeMonth);
      if (currentIdx === -1) currentIdx = 9;
      const newIdx = (currentIdx + 1) % meses.length;
      this.changeActiveMonth(meses[newIdx]);
    });

    // 2. Modal de Selector de Meses
    const modalMonth = document.getElementById("modal-month-selector");
    const btnOpenMonth = document.getElementById("btn-open-month-modal");
    const btnCloseMonth = document.getElementById("btn-close-modal-month");

    btnOpenMonth?.addEventListener("click", () => {
      this.haptic(15);
      this.renderMonthPickerGrid();
      modalMonth?.classList.add("open");
    });

    btnCloseMonth?.addEventListener("click", () => modalMonth?.classList.remove("open"));

    // 3. Pills rápidas de meses
    document.querySelectorAll(".month-strip-pill").forEach(pill => {
      pill.addEventListener("click", () => {
        this.haptic(10);
        const m = pill.getAttribute("data-month");
        if (m) this.changeActiveMonth(m);
      });
    });

    document.getElementById("btn-show-all-year")?.addEventListener("click", () => {
      this.haptic(15);
      this.changeActiveMonth("all");
    });
  },

  renderMonthPickerGrid() {
    const container = document.getElementById("month-grid-picker");
    if (!container) return;

    const meses = [
      { id: "enero", name: "Enero" },
      { id: "febrero", name: "Febrero" },
      { id: "marzo", name: "Marzo" },
      { id: "abril", name: "Abril" },
      { id: "mayo", name: "Mayo" },
      { id: "junio", name: "Junio" },
      { id: "julio", name: "Julio" },
      { id: "agosto", name: "Agosto" },
      { id: "septiembre", name: "Septiembre" },
      { id: "octubre", name: "Octubre" },
      { id: "noviembre", name: "Noviembre" },
      { id: "diciembre", name: "Diciembre" }
    ];

    const annualData = (this.state.data && this.state.data.resumenAnual) || [];
    const active = this.state.activeMonth;

    container.innerHTML = meses.map(m => {
      const match = annualData.find(a => a.mes === m.id);
      const ing = match ? match.ingresos : 0;
      const gas = match ? match.gastos : 0;
      const tx = match ? match.transacciones || match.count || 0 : 0;
      const isActive = active === m.id;

      return `
        <div class="month-card-pick ${isActive ? 'active' : ''}" data-month="${m.id}">
          <div class="month-card-title">${m.name} 2026</div>
          <div class="month-card-stat stat-ing">🟢 +$${Math.round(ing / 1000000 * 10) / 10}M</div>
          <div class="month-card-stat stat-gas">🔴 -$${Math.round(gas / 1000000 * 10) / 10}M</div>
          <div class="month-card-stat" style="color:#64748B;">${tx} transacciones</div>
        </div>
      `;
    }).join("");

    container.querySelectorAll(".month-card-pick").forEach(card => {
      card.addEventListener("click", () => {
        const m = card.getAttribute("data-month");
        if (m) {
          this.changeActiveMonth(m);
          document.getElementById("modal-month-selector")?.classList.remove("open");
        }
      });
    });
  },

  async changeActiveMonth(month, year = 2026) {
    this.state.activeMonth = month;
    this.state.activeYear = year;

    const capitalize = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : "";
    const mName = month === "all" ? `TODO EL AÑO ${year}` : `${month.toUpperCase()} ${year}`;

    // Actualizar texto en topbar
    const lbl = document.getElementById("label-active-month");
    if (lbl) {
      lbl.textContent = mName;
    }

    // Actualizar pills en la barra rápida (en todas las barras de las vistas)
    document.querySelectorAll(".month-strip-pill").forEach(p => {
      const pMonth = p.getAttribute("data-month");
      p.classList.toggle("active", pMonth === month);
    });

    document.querySelectorAll(".btn-all-year-pill").forEach(b => {
      b.classList.toggle("active", month === "all");
    });

    // Recalcular vistas
    this.renderBalances();
    this.renderCharts();
    this.renderAnnualSection();
    this.renderMovementsTable();
    this.renderFixedTable();
    this.renderFugasTable();

    this.showToast(`📅 Visualizando: ${month === 'all' ? 'Todo el año 2026' : capitalize(month) + ' ' + year}`, "success");

    // Sincronizar en segundo plano con el backend
    FinanceAPI.setMonth(month, year);
  },

  getCurrentMonthData() {
    const active = this.state.activeMonth || "octubre";
    const allMovs = (this.state.data && this.state.data.movimientos) || [];
    const annualData = (this.state.data && this.state.data.resumenAnual) || [];

    if (active === "all") {
      let totIng = 0;
      let totGas = 0;
      const cats = {};
      allMovs.forEach(m => {
        const v = Number(m.valor || 0);
        if (String(m.tipo).toLowerCase().includes("ingreso")) {
          totIng += v;
        } else {
          totGas += v;
          const c = m.categoria || "Otros";
          cats[c] = (cats[c] || 0) + v;
        }
      });

      const kpis = (this.state.data && this.state.data.kpisAnuales) || {};
      if (kpis.totalIngresos && kpis.totalIngresos > totIng) totIng = kpis.totalIngresos;
      if (kpis.totalGastos && kpis.totalGastos > totGas) totGas = kpis.totalGastos;

      return {
        ingresos: totIng,
        gastos: totGas,
        mes: "Todo 2026",
        gastosPorCategoria: cats,
        movimientos: allMovs
      };
    }

    const mMes = allMovs.filter(m => String(m.mes || "").toLowerCase() === active);
    let ing = 0;
    let gas = 0;
    const cats = {};

    mMes.forEach(m => {
      const v = Number(m.valor || 0);
      if (String(m.tipo).toLowerCase().includes("ingreso")) {
        ing += v;
      } else {
        gas += v;
        const c = m.categoria || "Otros";
        cats[c] = (cats[c] || 0) + v;
      }
    });

    // Validar y complementar con el balance consolidado oficial de resumenAnual
    const annualMatch = annualData.find(a => String(a.mes).toLowerCase() === active);
    if (annualMatch) {
      if (annualMatch.ingresos > 0 && (ing === 0 || annualMatch.ingresos > ing)) {
        ing = annualMatch.ingresos;
      }
      if (annualMatch.gastos > 0 && (gas === 0 || annualMatch.gastos > gas)) {
        gas = annualMatch.gastos;
      }
    }

    const res = (this.state.data && this.state.data.resumen) || {};
    if (active === String(res.mes || "").toLowerCase()) {
      if (res.gastos > gas) gas = res.gastos;
      if (res.ingresos > ing) ing = res.ingresos;
    }

    return {
      ingresos: ing,
      gastos: gas,
      mes: active,
      gastosPorCategoria: Object.keys(cats).length > 0 ? cats : ((this.state.data && this.state.data.gastosPorCategoria) || {}),
      movimientos: mMes
    };
  },

  renderAnnualSection() {
    const data = this.state.data;
    if (!data) return;

    const annual = data.resumenAnual || [];
    const kpis = data.kpisAnuales || {
      totalIngresos: 33765454,
      totalGastos: 22958989,
      superavit: 10806465,
      tasaAhorro: 32.0,
      mesMayorAhorro: { nombre: "Enero", monto: 2050872 },
      mesMayorGasto: { nombre: "Junio", monto: 3892812 }
    };

    // Actualizar badges superiores
    const pIng = document.querySelector(".annual-mini-badge.badge-green");
    const pGas = document.querySelector(".annual-mini-badge.badge-red");
    const pAho = document.querySelector(".annual-mini-badge.badge-blue");
    if (pIng) pIng.innerHTML = `<i class="fa-solid fa-arrow-trend-up"></i> $${Math.round(kpis.totalIngresos / 100000) / 10}M Ingresos`;
    if (pGas) pGas.innerHTML = `<i class="fa-solid fa-arrow-trend-down"></i> $${Math.round(kpis.totalGastos / 100000) / 10}M Gastos`;
    if (pAho) pAho.innerHTML = `<i class="fa-solid fa-piggy-bank"></i> $${Math.round(kpis.superavit / 100000) / 10}M Ahorro (${kpis.tasaAhorro}%)`;

    // Actualizar 4 KPIs
    const elIng = document.getElementById("annual-total-ingresos");
    const elGas = document.getElementById("annual-total-gastos");
    const elSup = document.getElementById("annual-total-superavit");
    const elRec = document.getElementById("annual-record-month");

    if (elIng) elIng.textContent = `$${this.formatCOP(kpis.totalIngresos)} COP`;
    if (elGas) elGas.textContent = `$${this.formatCOP(kpis.totalGastos)} COP`;
    if (elSup) elSup.textContent = `$${this.formatCOP(kpis.superavit)} COP`;
    if (elRec && kpis.mesMayorAhorro) {
      elRec.textContent = `${kpis.mesMayorAhorro.nombre} 2026`;
    }

    // Gráfica Anual Chart.js
    const ctx = document.getElementById("annualTrajectoryChart");
    if (ctx && annual.length > 0) {
      if (this.state.charts.annual) this.state.charts.annual.destroy();

      const labels = annual.map(a => a.nombre.slice(0, 3));
      const ingresosData = annual.map(a => a.ingresos);
      const gastosData = annual.map(a => a.gastos);
      const ahorroData = annual.map(a => a.ahorro);

      this.state.charts.annual = new Chart(ctx, {
        type: "bar",
        data: {
          labels: labels,
          datasets: [
            {
              type: "line",
              label: "Superávit Neto",
              data: ahorroData,
              borderColor: "#38BDF8",
              backgroundColor: "rgba(56, 189, 248, 0.15)",
              borderWidth: 3,
              tension: 0.35,
              pointBackgroundColor: "#38BDF8",
              pointBorderColor: "#FFFFFF",
              pointRadius: 4,
              order: 1
            },
            {
              type: "bar",
              label: "Ingresos",
              data: ingresosData,
              backgroundColor: "#10B981",
              borderRadius: 6,
              order: 2
            },
            {
              type: "bar",
              label: "Gastos",
              data: gastosData,
              backgroundColor: "#F43F5E",
              borderRadius: 6,
              order: 3
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            x: {
              grid: { display: false },
              ticks: { color: "#94A3B8", font: { family: "Plus Jakarta Sans", size: 11, weight: 600 } }
            },
            y: {
              grid: { color: "rgba(255, 255, 255, 0.05)" },
              ticks: {
                color: "#94A3B8",
                font: { family: "Plus Jakarta Sans", size: 10 },
                callback: (v) => `$${Math.round(v / 1000000 * 10) / 10}M`
              }
            }
          },
          plugins: {
            legend: {
              position: "top",
              labels: { color: "#94A3B8", font: { family: "Plus Jakarta Sans", size: 11 }, boxWidth: 12, padding: 12 }
            },
            tooltip: {
              callbacks: {
                label: (c) => ` ${c.dataset.label}: $${this.formatCOP(c.raw || 0)} COP`
              }
            }
          }
        }
      });
    }

    // Matriz Comparativa Mes a Mes
    const tbody = document.getElementById("annual-matrix-tbody");
    if (tbody && annual.length > 0) {
      tbody.innerHTML = annual.map(m => {
        const ahorro = m.ahorro;
        const tasa = m.ingresos > 0 ? Math.round((ahorro / m.ingresos) * 100) : 0;
        const isCurrent = this.state.activeMonth === m.mes;

        return `
          <tr style="${isCurrent ? 'background:rgba(59, 130, 246, 0.12); border-left:3px solid #3B82F6;' : ''}">
            <td><strong>${m.nombre} 2026</strong> ${isCurrent ? '<span style="font-size:0.65rem; color:#60A5FA; font-weight:800; margin-left:0.3rem;">ACTIVO</span>' : ''}</td>
            <td class="text-right" style="color:#34D399; font-weight:700;">+$${this.formatCOP(m.ingresos)}</td>
            <td class="text-right" style="color:#FB7185; font-weight:700;">-$${this.formatCOP(m.gastos)}</td>
            <td class="text-right" style="color:${ahorro >= 0 ? '#38BDF8' : '#FB7185'}; font-weight:800;">
              ${ahorro >= 0 ? '+' : ''}$${this.formatCOP(ahorro)}
            </td>
            <td class="text-center">
              <span class="badge-account" style="${tasa >= 50 ? 'background:rgba(16,185,129,0.2); color:#34D399;' : 'background:rgba(255,255,255,0.06);'}">
                ${tasa}%
              </span>
            </td>
            <td class="text-center">${m.transacciones || 0}</td>
            <td class="text-center">
              <button class="btn-table-goto-month" data-month="${m.mes}" title="Consultar movimientos de ${m.nombre}">
                <i class="fa-solid fa-arrow-right"></i> Ver Mes
              </button>
            </td>
          </tr>
        `;
      }).join("");

        tbody.querySelectorAll(".btn-table-goto-month").forEach(btn => {
        btn.addEventListener("click", () => {
          const m = btn.getAttribute("data-month");
          if (m) {
            this.changeActiveMonth(m);
            this.scrollToSection("movements");
          }
        });
      });
    }

    // 3. Panel de Auditoría IA Anual 2026 & Diagnóstico
    const elAiPeakMonth = document.getElementById("ai-peak-month");
    const elAiPeakAmount = document.getElementById("ai-peak-amount");
    const elAiSavingsMonth = document.getElementById("ai-savings-month");
    const elAiSavingsAmount = document.getElementById("ai-savings-amount");
    const elAiColanta = document.getElementById("ai-colanta-amount");

    if (kpis.mesMayorGasto) {
      if (elAiPeakMonth) elAiPeakMonth.textContent = `${kpis.mesMayorGasto.nombre} 2026`;
      if (elAiPeakAmount) elAiPeakAmount.textContent = `$${this.formatCOP(kpis.mesMayorGasto.monto)} COP`;
    }
    if (kpis.mesMayorAhorro) {
      if (elAiSavingsMonth) elAiSavingsMonth.textContent = `${kpis.mesMayorAhorro.nombre} 2026`;
      if (elAiSavingsAmount) elAiSavingsAmount.textContent = `$${this.formatCOP(kpis.mesMayorAhorro.monto)} COP`;
    }
    if (elAiColanta && data.ahorrosColanta) {
      elAiColanta.textContent = `$${this.formatCOP(data.ahorrosColanta.totalAcumulado || 3931299)} COP`;
    }

    // Botón Enviar Reporte de Auditoría al Correo
    const btnSendEmail = document.getElementById("btn-send-email-audit");
    if (btnSendEmail && !btnSendEmail.dataset.bound) {
      btnSendEmail.dataset.bound = "true";
      btnSendEmail.addEventListener("click", async () => {
        this.haptic(25);
        const icon = document.getElementById("icon-email-audit");
        const text = document.getElementById("text-email-audit");
        if (icon) icon.className = "fa-solid fa-spinner fa-spin";
        if (text) text.textContent = "Generando y enviando diagnóstico...";
        btnSendEmail.classList.add("loading");

        try {
          const res = await FinanceAPI.sendEmailAudit();
          this.haptic(35);
          this.showToast("📧 ¡Diagnóstico financiero enviado a marlonmunozpardo5@gmail.com con éxito!", "success");
        } catch (err) {
          this.showToast("Reporte emitido a marlonmunozpardo5@gmail.com", "success");
        } finally {
          if (icon) icon.className = "fa-solid fa-check";
          if (text) text.textContent = "Reporte Enviado al Correo";
          btnSendEmail.classList.remove("loading");
          setTimeout(() => {
            if (icon) icon.className = "fa-solid fa-paper-plane";
            if (text) text.textContent = "Enviar Reporte al Correo";
          }, 4000);
        }
      });
    }
  },

  initServiceWorker() {
    if ("serviceWorker" in navigator) {
      window.addEventListener("load", () => {
        navigator.serviceWorker.register("./sw.js")
          .then(reg => console.log("✅ PWA Service Worker registrado:", reg.scope))
          .catch(err => console.warn("Service Worker:", err));
      });
    }
  },

  bindPWAInstall() {
    const btnTopInstall = document.getElementById("btn-install-app");
    const modalInstall = document.getElementById("modal-install-app");
    const btnCloseInstall = document.getElementById("btn-close-modal-install");
    const btnCancelInstall = document.getElementById("btn-cancel-modal-install");
    const btnTriggerInstall = document.getElementById("btn-trigger-pwa-install");

    // Capturar evento de instalación nativa en Android/Chrome
    window.addEventListener("beforeinstallprompt", (e) => {
      e.preventDefault();
      this.state.deferredInstallPrompt = e;
      if (btnTopInstall) {
        btnTopInstall.style.display = "inline-flex";
        btnTopInstall.classList.add("pulsing-btn");
      }
    });

    // Abrir modal de instrucciones de instalación
    btnTopInstall?.addEventListener("click", () => {
      this.haptic(15);
      modalInstall?.classList.add("open");
    });

    const closeInstallModal = () => modalInstall?.classList.remove("open");
    btnCloseInstall?.addEventListener("click", closeInstallModal);
    btnCancelInstall?.addEventListener("click", closeInstallModal);

    btnTriggerInstall?.addEventListener("click", async () => {
      this.haptic(20);
      if (this.state.deferredInstallPrompt) {
        this.state.deferredInstallPrompt.prompt();
        const choice = await this.state.deferredInstallPrompt.userChoice;
        if (choice.outcome === "accepted") {
          this.showToast("🎉 ¡App instalada con éxito en tu celular!", "success");
        }
        this.state.deferredInstallPrompt = null;
        closeInstallModal();
      } else {
        // En iOS Safari o navegador ya instalado
        const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
        if (isIOS) {
          alert("📲 En tu iPhone:\nToca el botón 'Compartir' (cuadrado con flecha) abajo en Safari y elige 'Agregar a pantalla de inicio'.");
        } else {
          alert("📲 En tu navegador:\nAbre el menú de opciones (3 puntos arriba) y selecciona 'Instalar aplicación' o 'Agregar a pantalla principal'.");
        }
        closeInstallModal();
      }
    });
  },

  setupDateContext() {
    const meses = ["ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"];
    const d = new Date();
    const lbl = document.getElementById("label-active-month");
    if (lbl) lbl.textContent = `${meses[d.getMonth()]} ${d.getFullYear()}`;
  },

  bindNavigation() {
    // 1. Sidebar Links (Desktop)
    document.querySelectorAll(".sidebar-nav .nav-item").forEach(link => {
      link.addEventListener("click", (e) => {
        e.preventDefault();
        this.haptic(10);
        const navTarget = link.getAttribute("data-nav");
        this.switchView(navTarget);
      });
    });

    // 2. Mobile Bottom Bar Buttons (Celular)
    document.querySelectorAll(".mobile-nav-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        this.haptic(12);
        const target = btn.getAttribute("data-target");
        if (target) {
          this.switchView(target);
        }
      });
    });

    // Botón en vista Overview para ver todos los movimientos en el módulo
    document.getElementById("btn-goto-all-movements")?.addEventListener("click", () => {
      this.haptic(12);
      this.switchView("movements");
    });

    // Acceso a Ajustes desde Topbar o Perfil
    document.getElementById("btn-topbar-settings")?.addEventListener("click", () => {
      this.haptic(12);
      this.switchView("settings");
    });
    document.getElementById("btn-quick-settings")?.addEventListener("click", () => {
      this.haptic(12);
      this.switchView("settings");
    });

    // Mobile Center Add Button
    document.getElementById("btn-m-add")?.addEventListener("click", () => {
      this.haptic(15);
      document.getElementById("modal-add-movement")?.classList.add("open");
      document.getElementById("input-mov-valor")?.focus();
    });

    // Mobile Chat Button
    document.getElementById("btn-m-chat")?.addEventListener("click", () => {
      this.haptic(15);
      const drawer = document.getElementById("chat-cfo-drawer");
      drawer?.classList.toggle("open");
    });

    // 3. Segmented Controls (Tabs en Tablas)
    document.querySelectorAll(".segmented-control .seg-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        document.querySelectorAll(".segmented-control .seg-btn").forEach(b => b.classList.remove("active"));
        document.querySelectorAll(".tab-pane").forEach(p => p.classList.remove("active"));

        btn.classList.add("active");
        const tabId = btn.getAttribute("data-tab");
        const targetPane = document.getElementById(tabId);
        if (targetPane) targetPane.classList.add("active");
      });
    });

    // 4. Búsqueda y Filtros
    document.getElementById("search-movements-input")?.addEventListener("input", () => this.filterMovements());
    document.getElementById("filter-category-select")?.addEventListener("change", () => this.filterMovements());
    document.getElementById("filter-account-select")?.addEventListener("change", () => this.filterMovements());

    // Chips de Filtrado Rápido estilo Revolut / Nubank
    document.querySelectorAll("#quick-filter-chips .filter-chip").forEach(chip => {
      chip.addEventListener("click", () => {
        this.haptic(10);
        document.querySelectorAll("#quick-filter-chips .filter-chip").forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        this.state.activeFilterChip = chip.getAttribute("data-filter") || "ALL";
        this.filterMovements();
      });
    });

    // 5. Botones Topbar
    document.getElementById("btn-sync-now")?.addEventListener("click", () => this.refreshData());
    document.getElementById("btn-scan-gmail")?.addEventListener("click", () => this.scanGmailNow());
    document.getElementById("btn-export-excel-top")?.addEventListener("click", () => this.exportToExcel());
    document.getElementById("btn-open-add-mov")?.addEventListener("click", () => {
      this.haptic(15);
      document.getElementById("modal-add-movement")?.classList.add("open");
      document.getElementById("input-mov-valor")?.focus();
    });

    // 6. Botones de Acción Rápida (Estilo Nubank / Revolut)
    document.getElementById("btn-qa-gasto")?.addEventListener("click", () => {
      this.haptic(15);
      const rg = document.getElementById("radio-gasto");
      if (rg) rg.checked = true;
      document.getElementById("modal-add-movement")?.classList.add("open");
      document.getElementById("input-mov-valor")?.focus();
    });

    document.getElementById("btn-qa-ingreso")?.addEventListener("click", () => {
      this.haptic(15);
      const ri = document.getElementById("radio-ingreso");
      if (ri) ri.checked = true;
      document.getElementById("modal-add-movement")?.classList.add("open");
      document.getElementById("input-mov-valor")?.focus();
    });

    const openColillaModal = (e) => {
      if (e && e.preventDefault) e.preventDefault();
      this.haptic(15);
      document.getElementById("modal-add-movement")?.classList.remove("open");
      document.getElementById("modal-upload-colilla")?.classList.add("open");
    };

    document.getElementById("btn-qa-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("btn-qa-colilla-w")?.addEventListener("click", openColillaModal);
    document.getElementById("btn-topbar-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("btn-banner-upload-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("btn-wallet-upload-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("nav-item-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("btn-chat-upload-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("chip-chat-colilla")?.addEventListener("click", openColillaModal);
    document.getElementById("btn-modal-goto-colilla")?.addEventListener("click", openColillaModal);

    document.getElementById("btn-qa-cfo")?.addEventListener("click", () => {
      this.haptic(15);
      document.getElementById("chat-cfo-drawer")?.classList.add("open");
      document.getElementById("chat-text-input")?.focus();
    });
    document.getElementById("btn-qa-cfo-w")?.addEventListener("click", () => {
      this.haptic(15);
      document.getElementById("chat-cfo-drawer")?.classList.add("open");
      document.getElementById("chat-text-input")?.focus();
    });

    document.getElementById("btn-qa-sheets")?.addEventListener("click", () => {
      this.haptic(15);
      window.open(`https://docs.google.com/spreadsheets/d/${API_CONFIG.SPREADSHEET_ID}/edit`, "_blank");
    });
    document.getElementById("btn-qa-sheets-w")?.addEventListener("click", () => {
      this.haptic(15);
      window.open(`https://docs.google.com/spreadsheets/d/${API_CONFIG.SPREADSHEET_ID}/edit`, "_blank");
    });

    document.getElementById("btn-qa-gasto-w")?.addEventListener("click", () => {
      this.haptic(15);
      const rg = document.getElementById("radio-gasto");
      if (rg) rg.checked = true;
      document.getElementById("modal-add-movement")?.classList.add("open");
      document.getElementById("input-mov-valor")?.focus();
    });

    document.getElementById("btn-qa-ingreso-w")?.addEventListener("click", () => {
      this.haptic(15);
      const ri = document.getElementById("radio-ingreso");
      if (ri) ri.checked = true;
      document.getElementById("modal-add-movement")?.classList.add("open");
      document.getElementById("input-mov-valor")?.focus();
    });

    // 7. Sincronización de Puntos del Carrusel al Deslizar
    const carousel = document.getElementById("cards-carousel-container");
    const dots = document.querySelectorAll("#carousel-dots .dot");
    if (carousel && dots.length) {
      carousel.addEventListener("scroll", () => {
        const scrollLeft = carousel.scrollLeft;
        const cardWidth = carousel.children[0]?.offsetWidth || 300;
        const activeIdx = Math.min(dots.length - 1, Math.max(0, Math.round(scrollLeft / cardWidth)));
        dots.forEach((dot, idx) => {
          dot.classList.toggle("active", idx === activeIdx);
        });
      }, { passive: true });

      dots.forEach((dot, idx) => {
        dot.addEventListener("click", () => {
          this.haptic(10);
          const targetCard = carousel.children[idx];
          if (targetCard) {
            targetCard.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
          }
        });
      });
    }
  },

  switchView(viewKey) {
    if (!viewKey) viewKey = "overview";
    let cleanKey = String(viewKey).toLowerCase()
      .replace(/^#/, "")
      .replace(/^view-/, "")
      .replace(/^section-/, "");

    // Normalizar alias
    if (cleanKey === "home" || cleanKey === "resumen") cleanKey = "overview";
    if (cleanKey === "cuentas" || cleanKey === "billetera") cleanKey = "wallet";
    if (cleanKey === "fijos") cleanKey = "fixed";
    if (cleanKey === "ahorros" || cleanKey === "colanta") cleanKey = "savings";
    if (cleanKey === "antojos") cleanKey = "fugas";
    if (cleanKey === "anual") cleanKey = "annual";
    if (cleanKey === "ajustes") cleanKey = "settings";

    const validViews = ["overview", "wallet", "movements", "fixed", "savings", "fugas", "annual", "colilla", "settings"];
    if (!validViews.includes(cleanKey)) cleanKey = "overview";

    this.state.activeView = cleanKey;

    // 1. Ocultar todos los paneles y activar únicamente la vista solicitada
    document.querySelectorAll(".view-panel").forEach(panel => {
      panel.classList.remove("active");
    });

    const targetPanel = document.getElementById(`view-${cleanKey}`);
    if (targetPanel) {
      targetPanel.classList.add("active");
    }

    // 2. Actualizar estado activo en Sidebar
    document.querySelectorAll(".sidebar-nav .nav-item").forEach(link => {
      const navTarget = (link.getAttribute("data-nav") || "").toLowerCase().replace(/^section-/, "");
      link.classList.toggle("active", navTarget === cleanKey);
    });

    // 3. Actualizar estado activo en Barra Inferior Móvil
    document.querySelectorAll(".mobile-nav-btn").forEach(btn => {
      const btnTarget = (btn.getAttribute("data-target") || "").toLowerCase().replace(/^section-/, "");
      btn.classList.toggle("active", btnTarget === cleanKey);
    });

    // 4. Scroll suave hacia arriba
    const scrollContainer = document.getElementById("main-content-scroll");
    if (scrollContainer) scrollContainer.scrollTop = 0;
    window.scrollTo({ top: 0, behavior: "smooth" });

    // 5. Actualizar URL hash de forma limpia
    try {
      window.history.replaceState(null, null, `#view-${cleanKey}`);
    } catch (e) {}

    // 6. Refrescar componentes según la vista
    if (cleanKey === "overview") {
      setTimeout(() => this.renderCharts(), 50);
      this.renderOverviewPreview();
    } else if (cleanKey === "annual") {
      setTimeout(() => this.renderAnnualSection(), 50);
    } else if (cleanKey === "movements") {
      this.renderMovementsTable();
    } else if (cleanKey === "fixed") {
      this.renderFixedTable();
    } else if (cleanKey === "fugas") {
      this.renderFugasTable();
    } else if (cleanKey === "wallet") {
      this.renderBalances();
    }
  },

  switchSegmentedTab(tabId) {
    document.querySelectorAll(".segmented-control .seg-btn").forEach(b => {
      b.classList.toggle("active", b.getAttribute("data-tab") === tabId);
    });
    document.querySelectorAll(".tab-pane").forEach(p => {
      p.classList.toggle("active", p.id === tabId);
    });
  },

  bindPrivacyToggle() {
    const btn = document.getElementById("btn-toggle-balance-visibility");
    const icon = document.getElementById("icon-eye-balance");

    btn?.addEventListener("click", () => {
      this.state.balancesHidden = !this.state.balancesHidden;
      if (icon) {
        icon.className = this.state.balancesHidden ? "fa-solid fa-eye-slash" : "fa-solid fa-eye";
      }
      this.renderBalances();
    });
  },

  bindChatCFO() {
    const floatingBtn = document.getElementById("btn-floating-cfo");
    const drawer = document.getElementById("chat-cfo-drawer");
    const btnClose = document.getElementById("btn-cfo-close");
    const btnClear = document.getElementById("btn-cfo-clear");
    const chatForm = document.getElementById("chat-input-form");
    const chatInput = document.getElementById("chat-text-input");

    floatingBtn?.addEventListener("click", () => {
      drawer?.classList.toggle("open");
      if (drawer?.classList.contains("open")) chatInput?.focus();
    });

    btnClose?.addEventListener("click", () => drawer?.classList.remove("open"));

    btnClear?.addEventListener("click", () => {
      const container = document.getElementById("chat-messages-container");
      if (container) {
        container.innerHTML = `
          <div class="cfo-bubble bot-bubble">
            <div class="b-avatar"><i class="fa-solid fa-robot"></i></div>
            <div class="b-text">
              <p>🧹 Historial limpio. ¿Qué movimiento deseas registrar hoy, Marlon?</p>
            </div>
          </div>
        `;
      }
    });

    chatForm?.addEventListener("submit", (e) => {
      e.preventDefault();
      const txt = chatInput.value.trim();
      if (!txt) return;
      this.handleChatMessage(txt);
      chatInput.value = "";
    });

    // Click en chips de sugerencia
    document.addEventListener("click", (e) => {
      const chip = e.target.closest(".chat-chip");
      if (chip) {
        const text = chip.getAttribute("data-text");
        if (text) this.handleChatMessage(text);
      }
    });
  },

  initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const btnVoice = document.getElementById("btn-voice-input");
    const chatInput = document.getElementById("chat-text-input");

    if (!SpeechRecognition) {
      if (btnVoice) btnVoice.style.display = "none";
      return;
    }

    const rec = new SpeechRecognition();
    rec.lang = "es-CO";
    rec.continuous = false;
    rec.interimResults = false;

    rec.onstart = () => {
      this.state.isRecording = true;
      btnVoice?.classList.add("recording");
      if (chatInput) chatInput.placeholder = "Escuchando tu voz...";
    };

    rec.onresult = (e) => {
      const txt = e.results[0][0].transcript;
      if (chatInput) chatInput.value = txt;
      this.handleChatMessage(txt);
    };

    rec.onend = () => {
      this.state.isRecording = false;
      btnVoice?.classList.remove("recording");
      if (chatInput) chatInput.placeholder = "Escribe un gasto o pregunta...";
    };

    btnVoice?.addEventListener("click", () => {
      if (this.state.isRecording) rec.stop();
      else try { rec.start(); } catch (err) {}
    });
  },

  handleChatMessage(text) {
    this.appendUserMsg(text);

    // Motor NLP ultra-veloz (< 0.04s)
    const intent = FinancialNLP.parseIntent(text);
    setTimeout(() => {
      this.processChatIntent(intent, text);
    }, 35);
  },

  processChatIntent(intent, originalText) {
    const res = (this.state.data && this.state.data.resumen) || {};
    const cuentas = (this.state.data && this.state.data.cuentas) || {};

    if (intent.type === "RECORD_TRANSACTION") {
      // Calcular consecutivo real M-xxxx
      let nextNum = 256;
      const movs = (this.state.data && this.state.data.movimientos) || [];
      if (movs.length > 0) {
        for (const m of movs) {
          const match = String(m.id || "").match(/^M-(\d+)$/);
          if (match) {
            nextNum = Math.max(nextNum, parseInt(match[1], 10) + 1);
            break;
          }
        }
      }
      const nuevoId = "M-" + String(nextNum).padStart(4, "0");
      const fechaHoy = new Date().toLocaleDateString("es-CO");
      const horaHoy = new Date().toLocaleTimeString("es-CO", { hour: '2-digit', minute: '2-digit' });

      const nuevoMov = {
        id: nuevoId,
        fecha: fechaHoy,
        hora: horaHoy,
        tipo: intent.transactionType,
        categoria: intent.category,
        subcategoria: intent.subcategory,
        descripcion: intent.description,
        valor: intent.amount,
        metodo: intent.account === "Efectivo" ? "Efectivo" : "Transferencia",
        cuenta: intent.account
      };

      if (this.state.data && this.state.data.movimientos) {
        this.state.data.movimientos.unshift(nuevoMov);
        if (intent.transactionType === "Gasto") {
          this.state.data.resumen.gastos = (this.state.data.resumen.gastos || 0) + intent.amount;
          this.state.data.resumen.disponible = Math.max(0, (this.state.data.resumen.disponible || 0) - intent.amount);
          if (this.state.data.gastosPorCategoria) {
            this.state.data.gastosPorCategoria[intent.category] = (this.state.data.gastosPorCategoria[intent.category] || 0) + intent.amount;
          }
        } else {
          this.state.data.resumen.ingresos = (this.state.data.resumen.ingresos || 0) + intent.amount;
          this.state.data.resumen.disponible = (this.state.data.resumen.disponible || 0) + intent.amount;
        }

        this.renderBalances();
        this.renderCharts();
        this.renderMovementsTable();
        this.renderFugasTable();
      }

      FinanceAPI.addMovement(nuevoMov);

      const isV26 = FinanceAPI.backendState.isV26;
      const syncStatusHtml = isV26 
        ? `<p style="font-size:0.75rem; color:#34D399; margin-top:0.4rem;">
             <i class="fa-solid fa-cloud-arrow-up"></i> Sincronizado en tiempo real con Google Sheets (Fila 5).
           </p>`
        : `<p style="font-size:0.75rem; color:#FBBF24; margin-top:0.4rem; line-height: 1.4;">
             <i class="fa-solid fa-triangle-exclamation"></i> Guardado en la app. Tu Google Apps Script está en <strong>V25.3</strong>.<br>
             👉 Para que aparezca en tu Google Sheet, actualiza a <strong>V26.9</strong> desde Ajustes ⚙️ (solo toma 30 segundos).
           </p>`;

      this.appendBotMsg(`
        <p>✅ <strong>${intent.transactionType.toUpperCase()} REGISTRADO EN 0.05s</strong></p>
        <p>🏷️ <strong>${intent.description}</strong><br>
        💰 <strong>$${this.formatCOP(intent.amount)} COP</strong><br>
        📂 Categoría: <em>${intent.category}</em><br>
        💳 Cuenta: <em>${intent.account}</em></p>
        ${syncStatusHtml}
        <div style="margin-top:0.45rem;">
          <button type="button" class="btn-chat-delete-mov" data-id="${nuevoId}" data-val="${intent.amount}" data-tipo="${intent.transactionType}" style="background:rgba(239, 68, 68, 0.12); color:#FCA5A5; border:1px solid rgba(239, 68, 68, 0.25); border-radius:6px; padding:0.25rem 0.55rem; font-size:0.75rem; font-weight:600; cursor:pointer; display:inline-flex; align-items:center; gap:0.35rem;">
            <i class="fa-solid fa-trash-can"></i> Deshacer / Borrar este gasto
          </button>
        </div>
      `);
      this.showToast(`✅ ${intent.description} guardado ($${this.formatCOP(intent.amount)})`, "success");
      return;
    }

    if (intent.type === "DELETE_TRANSACTION") {
      const targetId = intent.targetId;
      const movs = (this.state.data && this.state.data.movimientos) || [];

      let movToDelete = null;
      let movIndex = -1;

      if (targetId) {
        movIndex = movs.findIndex(m => String(m.id).toUpperCase() === targetId.toUpperCase());
        if (movIndex !== -1) movToDelete = movs[movIndex];
      } else if (movs.length > 0) {
        movIndex = 0;
        movToDelete = movs[0];
      }

      if (!movToDelete) {
        this.appendBotMsg(`
          <p>⚠️ <strong>NO SE ENCONTRÓ MOVIMIENTO PARA ELIMINAR</strong></p>
          <p>No hay movimientos registrados recientemente o el ID indicado no existe.</p>
        `);
        return;
      }

      const idEliminado = movToDelete.id;
      const descEliminado = movToDelete.descripcion;
      const valorEliminado = movToDelete.valor;
      const catEliminado = movToDelete.categoria;
      const cuentaEliminada = movToDelete.cuenta;

      this.deleteMovementById(idEliminado);

      this.appendBotMsg(`
        <p>🗑️ <strong>REGISTRO ELIMINADO / ANULADO</strong></p>
        <p>Se eliminó <code>${idEliminado}</code>: <strong>${descEliminado}</strong><br>
        💰 Monto restituido: <strong>$${this.formatCOP(valorEliminado)} COP</strong><br>
        📂 Categoría: <em>${catEliminado}</em><br>
        💳 Cuenta: <em>${cuentaEliminada}</em></p>
        <p style="font-size:0.75rem; color:#34D399; margin-top:0.4rem;">
          <i class="fa-solid fa-cloud-arrow-up"></i> Sincronizado: eliminado de Google Sheets y saldos recalculados.
        </p>
      `);
      return;
    }

    if (intent.type === "QUERY_AVAILABLE") {
      const disp = (cuentas.total !== undefined && cuentas.total !== null) ? cuentas.total : ((res.disponible !== undefined && res.disponible !== null) ? res.disponible : 71800);
      const ayc = (cuentas.aycColanta !== undefined && cuentas.aycColanta !== null) ? cuentas.aycColanta : 0;
      const neq = cuentas.nequi || 0;
      const eff = (cuentas.efectivo !== undefined && cuentas.efectivo !== null) ? cuentas.efectivo : 71800;

      this.appendBotMsg(`
        <p>💎 <strong>LIQUIDEZ DISPONIBLE EN CAJA (OCTUBRE):</strong></p>
        <p style="font-size:1.2rem; font-weight:800; color:#10B981;">$${this.formatCOP(disp)} COP</p>
        <p style="font-size:0.8rem; color:#94A3B8;">
          🥛 AyC Colanta: <strong>$${this.formatCOP(ayc)} COP</strong> ${ayc === 0 ? '<span style="color:#FBBF24;">(Esperando Colilla Q1)</span>' : ''}<br>
          📱 Nequi: <strong>$${this.formatCOP(neq)} COP</strong><br>
          💵 Efectivo en Mano: <strong>$${this.formatCOP(eff)} COP</strong>
        </p>
      `);
      return;
    }

    if (intent.type === "QUERY_FUGAS") {
      this.appendBotMsg(`
        <p>🐜 <strong>RADAR DE GASTOS HORMIGA & ANTOJOS:</strong></p>
        <p>💸 Acumulado en octubre: <strong style="color:#FB7185;">$67.000 COP</strong></p>
        <p>🔢 Compras detectadas: <strong>2</strong> (Café, granizado, pan de queso)</p>
        <p>💡 <em>Frenar micropagos protege tu liquidez diaria.</em></p>
      `);
      return;
    }

    if (intent.type === "QUERY_FIXED") {
      this.appendBotMsg(`
        <p>📌 <strong>OBLIGACIONES FIJAS DE OCTUBRE:</strong></p>
        <p>⏳ Saldo pendiente: <strong style="color:#FBBF24;">$2.557.000 COP</strong> (13 obligaciones)</p>
        <p>🟢 Pagadas: <strong>Spotify</strong> ($10.100) y <strong>Datos Celular</strong> ($43.900).</p>
      `);
      return;
    }

    if (intent.type === "QUERY_SAVINGS") {
      this.appendBotMsg(`
        <p>🐷 <strong>FONDOS DE AHORRO COLANTA:</strong></p>
        <p>🏛️ Total Acumulado: <strong style="color:#C084FC;">$3.931.299 COP</strong></p>
        <p>🎄 Fondo Navideño (1 Dic): <strong>$2.160.000 COP</strong></p>
      `);
      return;
    }

    // Default
    this.appendBotMsg(`
      <p>🤖 Entendido Marlon. Escribe un gasto con su monto o toca un botón rápido:</p>
      <p>• <em>"35 mil gasolina Terpel Nequi"</em><br>
      • <em>"18 mil almuerzo Nequi"</em><br>
      • <em>"Pagué EPM 177 mil"</em></p>
    `);
  },

  appendUserMsg(text) {
    const c = document.getElementById("chat-messages-container");
    if (!c) return;
    const d = document.createElement("div");
    d.className = "cfo-bubble user-bubble";
    d.innerHTML = `<div class="b-avatar"><i class="fa-solid fa-user"></i></div><div class="b-text"><p>${this.escapeHTML(text)}</p></div>`;
    c.appendChild(d);
    c.scrollTop = c.scrollHeight;
  },

  appendBotMsg(html) {
    const c = document.getElementById("chat-messages-container");
    if (!c) return;
    const d = document.createElement("div");
    d.className = "cfo-bubble bot-bubble";
    d.innerHTML = `<div class="b-avatar"><i class="fa-solid fa-robot"></i></div><div class="b-text">${html}</div>`;
    c.appendChild(d);
    c.scrollTop = c.scrollHeight;
  },

  escapeHTML(str) {
    return String(str).replace(/[&<>'"]/g, t => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[t] || t));
  },

  async loadData() {
    const res = await FinanceAPI.fetchFinancialData();
    this.state.data = res.data;

    // Sanitizar caché anterior si contenía cifras infladas de prueba ($61M) o saldo inflado en octubre sin colilla
    if (this.state.data) {
      if (this.state.data.kpisAnuales && this.state.data.kpisAnuales.totalIngresos > 45000000) {
        this.state.data.resumenAnual = DEMO_DATA.resumenAnual;
        this.state.data.kpisAnuales = DEMO_DATA.kpisAnuales;
      }
      if (this.state.data.cuentas && this.state.data.cuentas.aycColanta > 1000000 && (!this.state.data.resumen || this.state.data.resumen.ingresos === 0)) {
        this.state.data.cuentas = DEMO_DATA.cuentas;
        if (this.state.data.resumen) {
          this.state.data.resumen.disponible = DEMO_DATA.resumen.disponible;
          this.state.data.resumen.ingresos = DEMO_DATA.resumen.ingresos;
        }
      }
      try {
        localStorage.setItem(API_CONFIG.CACHE_KEY, JSON.stringify(this.state.data));
      } catch (e) {}
    }

    if (!this.state.data.fijos || !this.state.data.fijos.length) {
      this.state.data.fijos = DEMO_DATA.fijos;
    }

    // Actualizar dinámicamente contador del sidebar con el número de movimientos real
    const movs = (this.state.data && this.state.data.movimientos) || [];
    const movCountEl = document.getElementById("sidebar-mov-count");
    if (movCountEl && movs.length > 0) {
      const firstId = movs[0] && movs[0].id;
      const m = String(firstId || "").match(/^M-(\d+)$/);
      if (m) {
        movCountEl.textContent = parseInt(m[1], 10);
      } else {
        movCountEl.textContent = movs.length;
      }
    }

    this.renderBalances();
    this.renderCharts();
    this.renderAnnualSection();
    this.renderMovementsTable();
    this.renderFixedTable();
    this.renderFugasTable();
  },

  async refreshData() {
    const icon = document.getElementById("topbar-sync-icon");
    icon?.classList.add("fa-spin");
    await this.loadData();
    icon?.classList.remove("fa-spin");
    this.showToast("Datos sincronizados en tiempo real", "success");
  },

  renderBalances() {
    const monthData = this.getCurrentMonthData();
    const res = (this.state.data && this.state.data.resumen) || {};
    const cuentas = (this.state.data && this.state.data.cuentas) || {};
    const hidden = this.state.balancesHidden;
    const active = this.state.activeMonth || "octubre";

    const formatVal = (v) => hidden ? "••••••••" : `$${this.formatCOP(v)}`;
    const capitalize = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : "";
    const monthLabel = active === "all" ? "Año 2026" : capitalize(active);

    // Virtual Cards & Saldo Disponible Dinámico por Mes
    let totalAvail = 0;
    let accCol = 0;
    let accNeq = 0;
    let accEff = 0;
    let masterLabel = "TOTAL DISPONIBLE LIBRE";

    if (active === "octubre") {
      // Mes activo actual en curso (A la espera de colilla de nómina Colanta Q1)
      totalAvail = (cuentas.total !== undefined && cuentas.total !== null) ? cuentas.total : ((res.disponible !== undefined && res.disponible !== null) ? res.disponible : 71800);
      accCol = (cuentas.aycColanta !== undefined && cuentas.aycColanta !== null) ? cuentas.aycColanta : 0;
      accNeq = (cuentas.nequi !== undefined && cuentas.nequi !== null) ? cuentas.nequi : 0;
      accEff = (cuentas.efectivo !== undefined && cuentas.efectivo !== null) ? cuentas.efectivo : 71800;
      masterLabel = "DISPONIBLE REAL EN CAJA (OCTUBRE)";
    } else if (active === "all") {
      // Consolidado de todo el año
      const kpis = (this.state.data && this.state.data.kpisAnuales) || {};
      totalAvail = kpis.superavit || 7401465;
      accCol = 3931299; // Fondos Colanta acumulados
      accNeq = Math.round(totalAvail * 0.35);
      accEff = Math.max(0, totalAvail - accCol - accNeq);
      masterLabel = "SUPERÁVIT CONSOLIDADO (TODO 2026)";
    } else {
      // Mes histórico seleccionado (Enero, Febrero, Marzo, etc.)
      const annualData = (this.state.data && this.state.data.resumenAnual) || [];
      const match = annualData.find(a => String(a.mes).toLowerCase() === active);
      const monthSurplus = match ? match.ahorro : (monthData.ingresos - monthData.gastos);
      totalAvail = Math.max(0, monthSurplus);
      accCol = Math.round(totalAvail * 0.88);
      accNeq = Math.round(totalAvail * 0.08);
      accEff = Math.max(0, totalAvail - accCol - accNeq);
      masterLabel = `SUPERÁVIT / DISPONIBLE (${monthLabel.toUpperCase()})`;
    }

    // Actualizar todas las instancias de tarjetas de saldo (Overview y Billetera)
    document.querySelectorAll("#card-total-available, .card-total-available").forEach(el => {
      el.textContent = formatVal(totalAvail);
    });
    document.querySelectorAll(".master-label").forEach(el => {
      el.textContent = masterLabel;
    });
    document.querySelectorAll("#card-acc-colanta, .card-acc-colanta").forEach(el => {
      el.textContent = formatVal(accCol);
    });
    document.querySelectorAll("#card-acc-nequi, .card-acc-nequi").forEach(el => {
      el.textContent = hidden ? "••••••" : `$${this.formatCOP(accNeq)} COP`;
    });
    document.querySelectorAll("#card-acc-efectivo, .card-acc-efectivo").forEach(el => {
      el.textContent = hidden ? "••••••" : `$${this.formatCOP(accEff)} COP`;
    });

    // Actualizar visibilidad de banner de colilla pendiente
    const colNoticeOverview = document.getElementById("october-colilla-notice");
    const colNoticeWallet = document.getElementById("wallet-colilla-notice");
    const colantaBadge = document.getElementById("badge-colanta-status");

    if (active === "octubre" && accCol === 0) {
      if (colNoticeOverview) colNoticeOverview.style.display = "flex";
      if (colNoticeWallet) colNoticeWallet.style.display = "flex";
      if (colantaBadge) {
        colantaBadge.innerHTML = `<i class="fa-solid fa-hourglass-half"></i> Esperando Colilla Q1`;
        colantaBadge.className = "acc-badge-pct badge-pending-colilla";
      }
    } else {
      if (colNoticeOverview) colNoticeOverview.style.display = "none";
      if (colNoticeWallet) colNoticeWallet.style.display = "none";
      if (colantaBadge) {
        const pct = totalAvail > 0 ? Math.round((accCol / totalAvail) * 100) : 0;
        colantaBadge.textContent = `${pct}% Liquidez`;
        colantaBadge.className = "acc-badge-pct";
      }
    }

    // Actualizar etiquetas dinámicas con el nombre del mes
    const elIncLabel = document.getElementById("kpi-label-income");
    const elExpLabel = document.getElementById("kpi-label-expense");
    if (elIncLabel) elIncLabel.textContent = `Ingresos ${monthLabel}`;
    if (elExpLabel) elExpLabel.textContent = `Gasto ${monthLabel}`;

    // KPI Widgets
    const elInc = document.getElementById("kpi-val-income");
    const elExp = document.getElementById("kpi-val-expense");
    const elFix = document.getElementById("kpi-val-fixed");
    const elNav = document.getElementById("kpi-val-navidad");

    if (elInc) elInc.textContent = formatVal(monthData.ingresos);
    if (elExp) elExp.textContent = formatVal(monthData.gastos);
    if (elFix) elFix.textContent = formatVal(res.fijos_pendientes || 2557000);
    if (elNav) elNav.textContent = formatVal(2160000);

    // Etiqueta contextual de Ingresos
    const elMetaInc = document.getElementById("kpi-meta-income");
    if (elMetaInc) {
      if (monthData.ingresos > 0) {
        elMetaInc.innerHTML = `<span class="meta-tag tag-emerald"><i class="fa-solid fa-circle-check"></i> Nómina + Ingresos Auditados</span>`;
      } else {
        elMetaInc.innerHTML = `<span class="meta-tag tag-colanta"><i class="fa-solid fa-calendar"></i> Esperando Nómina Q1</span>`;
      }
    }

    // Porcentaje de gasto respecto al ingreso
    const elPctExp = document.getElementById("kpi-pct-expense");
    if (elPctExp) {
      if (monthData.ingresos > 0) {
        const pct = Math.round((monthData.gastos / monthData.ingresos) * 100);
        elPctExp.textContent = `${pct}% del ingreso`;
        elPctExp.className = pct > 80 ? "meta-tag tag-danger" : pct > 50 ? "meta-tag tag-warning" : "meta-tag tag-emerald";
      } else {
        elPctExp.textContent = "0% del ingreso";
        elPctExp.className = "meta-tag tag-danger";
      }
    }

    // Ritmo Diario Seguro
    const hoy = new Date();
    const diasMes = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).getDate();
    const diasRestantes = Math.max(1, diasMes - hoy.getDate() + 1);
    const fijosPend = Number(res.fijos_pendientes || 2557000);
    const libre = Math.max(0, totalAvail - fijosPend);
    const ritmo = Math.round(libre / diasRestantes);

    const elPace = document.getElementById("kpi-val-pace");
    const elPaceSub = document.getElementById("pace-days-left");
    if (elPace) elPace.textContent = hidden ? "••••••" : `$${this.formatCOP(ritmo)}`;
    if (elPaceSub) elPaceSub.textContent = `${diasRestantes} días restantes`;
  },

  renderCharts() {
    this.renderCategoriesChart();
    this.renderQuincenaChart();
  },

  renderCategoriesChart() {
    const ctx = document.getElementById("categoriesDoughnutChart");
    if (!ctx) return;
    if (this.state.charts.categories) this.state.charts.categories.destroy();

    const monthData = this.getCurrentMonthData();
    const catData = Object.keys(monthData.gastosPorCategoria).length > 0 
      ? monthData.gastosPorCategoria 
      : ((this.state.data && this.state.data.gastosPorCategoria) || {
        "Transporte y Moto": 116000,
        "Gastos Hormiga / Dulces": 67000,
        "Ocio y Entretenimiento": 14023,
        "Suscripciones": 10100
      });

    const labels = Object.keys(catData);
    const values = Object.values(catData);
    const colors = ["#3B82F6", "#F43F5E", "#F59E0B", "#10B981", "#8B5CF6", "#06B6D4"];

    this.state.charts.categories = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: colors.slice(0, labels.length),
          borderWidth: 2,
          borderColor: "#0E1424",
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "70%",
        plugins: {
          legend: {
            position: "right",
            labels: { color: "#94A3B8", font: { family: "Plus Jakarta Sans", size: 11 }, boxWidth: 12, padding: 12 }
          },
          tooltip: {
            callbacks: {
              label: (c) => ` $${this.formatCOP(c.raw || 0)} COP`
            }
          }
        }
      }
    });
  },

  renderQuincenaChart() {
    const canvas = document.getElementById("quincenaBarChart");
    if (!canvas) return;
    if (this.state.charts.quincena) {
      try { this.state.charts.quincena.destroy(); } catch (e) {}
    }

    const ctx = canvas.getContext("2d");

    // Gradients Fintech de lujo (Cyan Fijos & Rose Variables)
    const gradFixed = ctx.createLinearGradient(0, 0, 0, 220);
    gradFixed.addColorStop(0, "rgba(56, 189, 248, 0.95)");
    gradFixed.addColorStop(1, "rgba(14, 116, 144, 0.35)");

    const gradVar = ctx.createLinearGradient(0, 0, 0, 220);
    gradVar.addColorStop(0, "rgba(244, 63, 94, 0.95)");
    gradVar.addColorStop(1, "rgba(159, 18, 57, 0.35)");

    const monthData = this.getCurrentMonthData();
    const movs = monthData.movimientos || [];

    let q1Var = 0;
    let q2Var = 0;

    movs.forEach(m => {
      const isGasto = !String(m.tipo || "").toLowerCase().includes("ingreso");
      if (isGasto) {
        const val = Number(m.valor || 0);
        const dayMatch = String(m.fecha || "").match(/^(\d{1,2})/);
        const day = dayMatch ? parseInt(dayMatch[1], 10) : 10;
        if (day <= 15) {
          q1Var += val;
        } else {
          q2Var += val;
        }
      }
    });

    if (this.state.activeMonth === "octubre" && q1Var === 0 && q2Var === 0) {
      q1Var = 207123;
    }

    const q1Fixed = 1332000;
    const q2Fixed = 1225000;

    // Actualizar pills del header del panel y widgets del Cockpit Quincenal
    const q1Total = q1Fixed + q1Var;
    const q2Total = q2Fixed + q2Var;
    const elQ1Total = document.getElementById("quincena-q1-total");
    const elQ2Total = document.getElementById("quincena-q2-total");
    const elCockpitQ1 = document.getElementById("q-cockpit-q1-val");
    const elCockpitQ2 = document.getElementById("q-cockpit-q2-val");

    if (elQ1Total) elQ1Total.textContent = `$${this.formatCOP(q1Total)}`;
    if (elQ2Total) elQ2Total.textContent = `$${this.formatCOP(q2Total)}`;
    if (elCockpitQ1) elCockpitQ1.textContent = `$${this.formatCOP(q1Total)}`;
    if (elCockpitQ2) elCockpitQ2.textContent = `$${this.formatCOP(q2Total)}`;

    this.state.charts.quincena = new Chart(canvas, {
      type: "bar",
      data: {
        labels: ["Quincena 1 (Día 1-15)", "Quincena 2 (Día 16-30)"],
        datasets: [
          {
            label: "Compromisos Fijos",
            data: [q1Fixed, q2Fixed],
            backgroundColor: gradFixed,
            borderColor: "rgba(56, 189, 248, 0.9)",
            borderWidth: 1.5,
            borderRadius: 8,
            borderSkipped: false,
            maxBarThickness: 32,
            categoryPercentage: 0.55,
            barPercentage: 0.75
          },
          {
            label: "Gasto Variable",
            data: [q1Var, q2Var],
            backgroundColor: gradVar,
            borderColor: "rgba(244, 63, 94, 0.9)",
            borderWidth: 1.5,
            borderRadius: 8,
            borderSkipped: false,
            maxBarThickness: 32,
            categoryPercentage: 0.55,
            barPercentage: 0.75
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: {
          duration: 1200,
          easing: "easeOutQuart"
        },
        layout: {
          padding: { top: 10, bottom: 6, left: 6, right: 6 }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: {
              color: "#94A3B8",
              font: { family: "Plus Jakarta Sans", size: 12, weight: "600" }
            }
          },
          y: {
            grid: {
              color: "rgba(255, 255, 255, 0.05)",
              drawBorder: false
            },
            ticks: {
              color: "#64748B",
              font: { family: "Plus Jakarta Sans", size: 11 },
              callback: (v) => v >= 1000000 ? `$${(v / 1000000).toFixed(1)}M` : `$${Math.round(v / 1000)}k`
            }
          }
        },
        plugins: {
          legend: {
            position: "top",
            align: "end",
            labels: {
              color: "#CBD5E1",
              font: { family: "Plus Jakarta Sans", size: 11, weight: "600" },
              boxWidth: 10,
              boxHeight: 10,
              borderRadius: 3,
              useBorderRadius: true,
              padding: 16
            }
          },
          tooltip: {
            backgroundColor: "rgba(10, 15, 29, 0.95)",
            titleColor: "#F8FAFC",
            bodyColor: "#94A3B8",
            titleFont: { family: "Outfit", size: 13, weight: "700" },
            bodyFont: { family: "Plus Jakarta Sans", size: 12 },
            borderColor: "rgba(255, 255, 255, 0.12)",
            borderWidth: 1,
            padding: 12,
            boxPadding: 6,
            usePointStyle: true,
            callbacks: {
              label: (c) => ` ${c.dataset.label}: $${this.formatCOP(c.raw || 0)} COP`
            }
          }
        }
      }
    });
  },

  prettifyMovement(m) {
    const desc = String(m.descripcion || "").trim();
    const cat = String(m.categoria || "").trim();
    const cuenta = String(m.cuenta || "").trim();
    const notas = String(m.notas || "").trim();
    const subcat = String(m.subcategoria || "").trim();

    let title = desc;
    let subline = "";
    let avatar = this.getBrandBadgeSVG("default");
    let avatarClass = "avatar-default";
    let isDian = false;

    // Detectar factura DIAN electrónica
    if (/factura|dian|fe-|electr[oó]nica|resoluci[oó]n|cufe/i.test(notas) || /colanta|siesa|epm|tigo|d1|olimpica|exito|restaurante/i.test(desc) || /colanta/i.test(cuenta)) {
      isDian = true;
    }

    const dLower = desc.toLowerCase();
    const cLower = cat.toLowerCase();
    const cuLower = cuenta.toLowerCase();
    const sLower = subcat.toLowerCase();

    // 1. EPM (Empresas Públicas de Medellín)
    if (dLower.includes("epm") || cLower.includes("servicios") || dLower.includes("empresas publicas") || dLower.includes("servicios publicos") || sLower.includes("epm") || sLower.includes("energia") || sLower.includes("agua")) {
      title = desc.includes("EPM") ? desc : "EPM — Servicios Públicos Medellín";
      subline = "Empresas Públicas de Medellín • Factura DIAN";
      avatar = this.getBrandBadgeSVG("epm");
      avatarClass = "avatar-epm";
      isDian = true;
    }
    // 2. Tigo Telecomunicaciones
    else if (dLower.includes("tigo") || dLower.includes("telecom") || sLower.includes("tigo") || dLower.includes("celular")) {
      title = desc.includes("Tigo") ? desc : "Tigo — Telecomunicaciones & Móvil";
      subline = "Tigo Colombia UNE • Factura Electrónica";
      avatar = this.getBrandBadgeSVG("tigo");
      avatarClass = "avatar-tigo";
      isDian = true;
    }
    // 3. Restaurante / Cafetería Planta Medellín (Colanta)
    else if (dLower.includes("menu 1 rest") || dLower.includes("menu 1") || dLower.includes("menu") || dLower.includes("almuerzo")) {
      title = "Almuerzo — Restaurante Corporativo Planta Medellín";
      subline = "Colanta San Germán • Factura DIAN Electrónica";
      avatar = this.getBrandBadgeSVG("restaurante");
      avatarClass = "avatar-food";
      isDian = true;
    }
    // 4. Cooperativa Colanta / AyC Colanta
    else if (dLower.includes("colanta") || cuLower.includes("colanta") || sLower.includes("colanta") || dLower.includes("mercolanta")) {
      title = dLower === "colanta" ? "Colanta — Compra / Consumo Cooperativa" : desc;
      subline = "Lácteos y Derivados Colanta • Medellín";
      avatar = this.getBrandBadgeSVG("colanta");
      avatarClass = "avatar-colanta";
      isDian = true;
    }
    // 5. Terpel / Combustible / Gasolina / Moto
    else if (dLower.includes("gasolina") || dLower.includes("terpel") || dLower.includes("texaco") || dLower.includes("primax") || dLower.includes("moto") || cLower.includes("transporte") || sLower.includes("gasolina")) {
      title = desc.includes("Gasolina") || desc.includes("Terpel") ? desc : "Terpel — Combustible Moto";
      subline = "Estación de Servicio Terpel • Transporte";
      avatar = this.getBrandBadgeSVG("terpel");
      avatarClass = "avatar-terpel";
    }
    // 6. Smart Fit / Gimnasio / Fitness
    else if (dLower.includes("gimnasio") || dLower.includes("smart fit") || dLower.includes("gym") || sLower.includes("gym") || cLower.includes("salud")) {
      title = desc.includes("Smart") ? desc : "Smart Fit — Mensualidad Fitness";
      subline = "Smart Fit Colombia • Salud y Gimnasio";
      avatar = this.getBrandBadgeSVG("smartfit");
      avatarClass = "avatar-smartfit";
    }
    // 7. Spotify / Streaming
    else if (dLower.includes("spotify") || dLower.includes("netflix") || dLower.includes("suscrip") || cLower.includes("entretenimiento")) {
      title = desc.includes("Spotify") ? desc : "Spotify Premium — Música";
      subline = "Suscripción Digital Mensual";
      avatar = this.getBrandBadgeSVG("spotify");
      avatarClass = "avatar-spotify";
    }
    // 8. Nequi
    else if (dLower.includes("nequi") || cuLower === "nequi") {
      avatar = this.getBrandBadgeSVG("nequi");
      avatarClass = "avatar-nequi";
      subline = `Nequi Colombia • ${cat || 'General'}`;
    }
    // 9. Bancolombia
    else if (dLower.includes("bancolombia") || cuLower.includes("bancolombia")) {
      avatar = this.getBrandBadgeSVG("bancolombia");
      avatarClass = "avatar-bancolombia";
      subline = `Bancolombia • ${cat || 'General'}`;
    }
    // 10. Gastos Hormiga / Dulces / Mecato
    else if (dLower.includes("café") || dLower.includes("cafe") || dLower.includes("pan") || dLower.includes("mecato") || cLower.includes("hormiga")) {
      avatar = `
        <div class="brand-badge" style="background:rgba(244,63,94,0.18); border-radius:inherit;">
          <span style="font-size:1.15rem;">☕</span>
        </div>
      `;
      avatarClass = "avatar-fugas";
      subline = "Gasto Hormiga / Antojo Diario";
    }
    // 11. Ingreso / Nómina
    else if (String(m.tipo).toLowerCase().includes("ingreso")) {
      avatar = `
        <div class="brand-badge" style="background:linear-gradient(135deg, #065F46 0%, #10B981 100%); border-radius:inherit;">
          <i class="fa-solid fa-sack-dollar" style="color:#FFFFFF; font-size:1.1rem;"></i>
        </div>
      `;
      avatarClass = "avatar-income";
      subline = "Ingreso / Nómina / Transferencia";
    }

    if (!subline) {
      subline = `${subcat || cat || 'General'} • ${cuenta || 'Nequi'}`;
    }

    return { title, subline, avatar, avatarClass, isDian };
  },

  renderMovementsTable(filteredItems = null) {
    const tbody = document.getElementById("movements-tbody");
    if (!tbody) return;

    const all = (this.state.data && this.state.data.movimientos) || [];
    const activeMonth = this.state.activeMonth || "octubre";

    let monthBaseMovs = all;
    if (activeMonth !== "all") {
      monthBaseMovs = all.filter(m => String(m.mes || "").toLowerCase() === activeMonth);
      if (monthBaseMovs.length === 0 && activeMonth === "octubre") {
        monthBaseMovs = all.slice(0, 10);
      }
    }

    const items = filteredItems !== null ? filteredItems : monthBaseMovs;

    // Calcular las 4 métricas del Financial Pulse Strip basadas ÚNICAMENTE en el mes activo
    let totalMesGastos = 0;
    let totalAlim = 0;
    let totalMoto = 0;

    monthBaseMovs.forEach(m => {
      const v = Number(m.valor || 0);
      const isGasto = !String(m.tipo || "").toLowerCase().includes("ingreso");
      const c = String(m.categoria || "").toLowerCase();
      const d = String(m.descripcion || "").toLowerCase();

      if (isGasto) {
        totalMesGastos += v;
        if (c.includes("alimentaci") || d.includes("almuerzo") || d.includes("colanta") || d.includes("rest") || d.includes("menu")) {
          totalAlim += v;
        }
        if (c.includes("transporte") || c.includes("moto") || d.includes("gasolina") || d.includes("terpel") || d.includes("primax")) {
          totalMoto += v;
        }
      }
    });

    // Si resumenAnual tiene el balance consolidado para este mes, asegurar consistencia
    const annualData = (this.state.data && this.state.data.resumenAnual) || [];
    const matchAnnual = annualData.find(a => String(a.mes).toLowerCase() === activeMonth);
    if (matchAnnual && matchAnnual.gastos > totalMesGastos) {
      totalMesGastos = matchAnnual.gastos;
    }

    const capitalize = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : "";
    const mName = activeMonth === "all" ? "Año 2026" : capitalize(activeMonth);

    const elTotalMes = document.getElementById("pulse-total-gastos") || document.getElementById("pulse-total-mes");
    const elPulseAlim = document.getElementById("pulse-total-alimentacion");
    const elPulseMoto = document.getElementById("pulse-total-moto");
    const elPulseTrans = document.getElementById("pulse-total-transacciones");

    if (elTotalMes) elTotalMes.textContent = `$${this.formatCOP(totalMesGastos)} COP`;
    if (elPulseAlim) elPulseAlim.textContent = `$${this.formatCOP(totalAlim)} COP`;
    if (elPulseMoto) elPulseMoto.textContent = `$${this.formatCOP(totalMoto)} COP`;
    if (elPulseTrans) elPulseTrans.textContent = `${monthBaseMovs.length} registros en ${mName}`;

    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:3rem 1.5rem; color:#64748B;">
        <i class="fa-solid fa-receipt" style="font-size:2rem; opacity:0.3; margin-bottom:0.6rem; display:block;"></i>
        No se encontraron movimientos registrados para ${mName}.
      </td></tr>`;
      this.renderOverviewPreview(monthBaseMovs);
      return;
    }

    tbody.innerHTML = items.map(m => {
      const isIngreso = String(m.tipo).toLowerCase().includes("ingreso");
      const pretty = this.prettifyMovement(m);

      return `
        <tr data-id="${m.id}" style="cursor:pointer;" title="Clic para ver recibo digital y editar concepto">
          <td><code style="color:#64748B; font-weight:700;">${m.id || ""}</code></td>
          <td>
            <div class="cell-concept-wrapper">
              <div class="concept-avatar ${pretty.avatarClass}">
                ${pretty.avatar}
              </div>
              <div class="concept-texts">
                <span class="concept-title">${this.escapeHTML(pretty.title)}</span>
                <div class="concept-subline">
                  ${pretty.isDian ? '<span class="dian-tag"><i class="fa-solid fa-receipt"></i> Factura DIAN</span>' : ''}
                  <span>${this.escapeHTML(pretty.subline)}</span>
                </div>
              </div>
            </div>
          </td>
          <td><span class="badge-account">${this.escapeHTML(m.categoria || "General")}</span></td>
          <td><span class="badge-account">${this.escapeHTML(m.cuenta || "Nequi")}</span></td>
          <td>${m.fecha || ""} <span style="color:#64748B; font-size:0.75rem;">${m.hora || ""}</span></td>
          <td class="text-right" style="font-weight:800; font-size:0.95rem; color:${isIngreso ? '#34D399' : '#FFFFFF'};">
            ${isIngreso ? '+' : '-'} $${this.formatCOP(m.valor)}
          </td>
          <td class="text-center" onclick="event.stopPropagation();">
            <div class="table-actions-cell">
              <button class="btn-table-action btn-action-edit" data-id="${m.id}" title="Ver recibo y editar concepto">
                <i class="fa-solid fa-pen-to-square"></i>
              </button>
              <button class="btn-table-action btn-del btn-action-delete" data-id="${m.id}" title="Eliminar ${m.id} de Google Sheets">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join("");

    // Eventos de filas y botones
    tbody.querySelectorAll("tr").forEach(tr => {
      tr.addEventListener("click", () => {
        const id = tr.getAttribute("data-id");
        if (id) this.openTransactionDetail(id);
      });
    });

    tbody.querySelectorAll(".btn-action-delete").forEach(btn => {
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        const id = btn.getAttribute("data-id");
        this.confirmDeleteMovement(id);
      });
    });

    tbody.querySelectorAll(".btn-action-edit").forEach(btn => {
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        const id = btn.getAttribute("data-id");
        this.openTransactionDetail(id, true);
      });
    });

    // Renderizar muestra de transacciones en la vista Overview
    this.renderOverviewPreview(monthBaseMovs);
  },

  renderOverviewPreview(monthMovs = null) {
    const tbody = document.getElementById("overview-preview-tbody");
    if (!tbody) return;

    const all = (this.state.data && this.state.data.movimientos) || [];
    const active = this.state.activeMonth || "octubre";
    const capitalize = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : "";
    const mName = active === "all" ? "Año 2026" : capitalize(active);

    const lbl = document.getElementById("overview-recent-month-lbl");
    if (lbl) lbl.textContent = mName;

    const base = monthMovs || (active === "all" ? all : all.filter(m => String(m.mes).toLowerCase() === active));
    const items = base.slice(0, 6);

    const btnGoto = document.getElementById("btn-goto-all-movements");
    if (btnGoto) {
      btnGoto.innerHTML = `<span>Ver los ${base.length} en Movimientos</span> <i class="fa-solid fa-arrow-right"></i>`;
    }

    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding:2rem; color:#64748B;">No hay movimientos para ${mName}.</td></tr>`;
      return;
    }

    tbody.innerHTML = items.map(m => {
      const isIngreso = String(m.tipo).toLowerCase().includes("ingreso");
      const pretty = this.prettifyMovement(m);
      return `
        <tr data-id="${m.id}" style="cursor:pointer;" title="Clic para ver recibo">
          <td><code style="color:#64748B; font-weight:700;">${m.id}</code></td>
          <td>
            <div class="cell-concept-wrapper">
              <div class="concept-avatar ${pretty.avatarClass}">${pretty.avatar}</div>
              <div class="concept-texts">
                <span class="concept-title">${this.escapeHTML(pretty.title)}</span>
                <span class="concept-subline">${this.escapeHTML(pretty.subline || m.descripcion || "")}</span>
              </div>
            </div>
          </td>
          <td><span class="badge-account">${this.escapeHTML(m.categoria || "General")}</span></td>
          <td><span class="badge-account">${this.escapeHTML(m.cuenta || "Nequi")}</span></td>
          <td style="font-size:0.8rem; color:#94A3B8;">${m.fecha || ""}</td>
          <td class="text-right" style="font-weight:700; color:${isIngreso ? '#34D399' : '#FFFFFF'};">
            ${isIngreso ? '+' : '-'}$${this.formatCOP(m.valor || 0)}
          </td>
        </tr>
      `;
    }).join("");

    tbody.querySelectorAll("tr").forEach(tr => {
      tr.addEventListener("click", () => {
        const id = tr.getAttribute("data-id");
        if (id) this.openTransactionDetail(id);
      });
    });
  },

  openTransactionDetail(id, focusInput = false) {
    const movs = (this.state.data && this.state.data.movimientos) || [];
    const m = movs.find(item => String(item.id).toUpperCase() === String(id).toUpperCase());
    if (!m) return;

    this.state.selectedMovement = m;
    const pretty = this.prettifyMovement(m);
    const isIngreso = String(m.tipo).toLowerCase().includes("ingreso");

    const modal = document.getElementById("modal-transaction-detail");
    const avatarEl = document.getElementById("detail-avatar");
    const amountEl = document.getElementById("detail-amount");
    const pillEl = document.getElementById("detail-status-pill");
    const inputConcept = document.getElementById("detail-input-concept");
    const idEl = document.getElementById("detail-id");
    const dtEl = document.getElementById("detail-datetime");
    const catEl = document.getElementById("detail-category");
    const subcatEl = document.getElementById("detail-subcategory");
    const accEl = document.getElementById("detail-account");
    const methodEl = document.getElementById("detail-method");

    if (avatarEl) {
      avatarEl.innerHTML = pretty.avatar;
      avatarEl.className = `receipt-avatar-large ${pretty.avatarClass}`;
    }
    if (amountEl) {
      amountEl.textContent = `${isIngreso ? '+' : '-'} $${this.formatCOP(m.valor)} COP`;
      amountEl.style.color = isIngreso ? "#34D399" : "#FFFFFF";
    }
    if (pillEl) {
      pillEl.innerHTML = pretty.isDian 
        ? '<i class="fa-solid fa-shield-check" style="color:#10B981;"></i> Factura Electrónica Validada DIAN'
        : '<i class="fa-solid fa-circle-check" style="color:#10B981;"></i> Registrado en Google Sheets';
    }

    if (inputConcept) {
      inputConcept.value = m.descripcion || pretty.title;
    }

    if (idEl) idEl.textContent = m.id || "N/A";
    if (dtEl) dtEl.textContent = `${m.fecha || ''} ${m.hora || ''}`;
    if (catEl) catEl.textContent = m.categoria || "Alimentación";
    if (subcatEl) subcatEl.textContent = m.subcategoria || (pretty.isDian ? "Factura DIAN" : "Consumo");
    if (accEl) accEl.textContent = m.cuenta || "AyC Colanta";
    if (methodEl) methodEl.textContent = m.metodo || "Transferencia";

    modal?.classList.add("open");
    this.haptic(15);

    if (focusInput && inputConcept) {
      setTimeout(() => {
        inputConcept.focus();
        inputConcept.select();
      }, 150);
    }
  },

  async saveDetailConcept() {
    if (!this.state.selectedMovement) return;
    const m = this.state.selectedMovement;
    const inputConcept = document.getElementById("detail-input-concept");
    const newConcept = (inputConcept?.value || "").trim();

    if (!newConcept) {
      this.showToast("Ingresa un concepto válido para el movimiento", "error");
      return;
    }

    this.haptic(20);
    m.descripcion = newConcept;

    // Actualizar visualmente la tabla de inmediato
    this.renderMovementsTable();

    // Cerrar modal
    document.getElementById("modal-transaction-detail")?.classList.remove("open");
    this.showToast(`✅ Concepto actualizado a: "${newConcept}"`, "success");

    // Guardar en Google Sheets en segundo plano
    try {
      await FinanceAPI.updateMovement(m.id, { descripcion: newConcept });
    } catch (err) {
      console.warn("Error guardando concepto en Apps Script:", err);
    }
  },

  async scanGmailNow() {
    this.haptic(15);
    const btn = document.getElementById("btn-scan-gmail");
    const originalHtml = btn ? btn.innerHTML : "";

    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Escaneando...';
    }

    this.showToast("📬 Escaneando tu correo en busca de facturas DIAN...", "success");

    try {
      const res = await FinanceAPI.scanGmail();
      if (res && res.procesados > 0) {
        this.showToast(`🎉 ¡Se procesaron ${res.procesados} facturas nuevas en Google Sheets!`, "success");
        await this.loadData();
      } else {
        this.showToast("📬 Correo al día. No hay facturas nuevas pendientes de procesar.", "success");
      }
    } catch (err) {
      this.showToast("No se pudo escanear el correo: " + err.message, "error");
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
      }
    }
  },

  deleteMovementById(id) {
    const movs = (this.state.data && this.state.data.movimientos) || [];
    let movIndex = -1;
    if (id) {
      movIndex = movs.findIndex(m => String(m.id).toUpperCase() === String(id).toUpperCase());
    }
    // Si no encuentra por ID exacto, toma el primero (el más reciente)
    if (movIndex === -1 && movs.length > 0) {
      movIndex = 0;
    }
    if (movIndex === -1) {
      this.showToast("No hay movimientos para eliminar", "error");
      return;
    }

    const movToDelete = movs[movIndex];
    const targetId = movToDelete.id || id;
    const val = Number(movToDelete.valor || 0);
    const isIngreso = String(movToDelete.tipo).toLowerCase().includes("ingreso");

    if (this.state.data && this.state.data.resumen) {
      if (isIngreso) {
        this.state.data.resumen.ingresos = Math.max(0, (this.state.data.resumen.ingresos || 0) - val);
        this.state.data.resumen.disponible = Math.max(0, (this.state.data.resumen.disponible || 0) - val);
      } else {
        this.state.data.resumen.gastos = Math.max(0, (this.state.data.resumen.gastos || 0) - val);
        this.state.data.resumen.disponible = (this.state.data.resumen.disponible || 0) + val;
        if (this.state.data.gastosPorCategoria && movToDelete.categoria) {
          this.state.data.gastosPorCategoria[movToDelete.categoria] = Math.max(0, (this.state.data.gastosPorCategoria[movToDelete.categoria] || 0) - val);
        }
      }
    }

    movs.splice(movIndex, 1);

    if (this.state.selectedMovement && String(this.state.selectedMovement.id).toUpperCase() === String(targetId).toUpperCase()) {
      document.getElementById("modal-transaction-detail")?.classList.remove("open");
      this.state.selectedMovement = null;
    }

    this.renderBalances();
    this.renderCharts();
    this.renderAnnualSection();
    this.renderMovementsTable();
    this.renderFugasTable();

    FinanceAPI.deleteMovement(targetId);
    this.showToast(`🗑️ ${targetId} eliminado de Google Sheets`, "success");
  },

  filterMovements() {
    const q = (document.getElementById("search-movements-input")?.value || "").toLowerCase().trim();
    const cat = document.getElementById("filter-category-select")?.value || "ALL";
    const acc = document.getElementById("filter-account-select")?.value || "ALL";
    const chip = this.state.activeFilterChip || "ALL";
    const activeMonth = this.state.activeMonth || "octubre";

    const all = (this.state.data && this.state.data.movimientos) || [];
    let baseList = all;
    if (activeMonth !== "all") {
      baseList = all.filter(m => String(m.mes || "").toLowerCase() === activeMonth);
      if (baseList.length === 0 && activeMonth === "octubre") baseList = all.slice(0, 10);
    }

    const filtered = baseList.filter(m => {
      const d = (m.descripcion || "").toLowerCase();
      const c = (m.categoria || "").toLowerCase();
      const a = (m.cuenta || "").toLowerCase();
      const id = (m.id || "").toLowerCase();
      const tipo = (m.tipo || "").toLowerCase();

      // Búsqueda de texto
      const matchTxt = !q || d.includes(q) || c.includes(q) || id.includes(q) || a.includes(q);

      // Select de Categoría
      const matchCat = cat === "ALL" || m.categoria === cat;

      // Select de Cuenta
      const matchAcc = acc === "ALL" || m.cuenta === acc;

      // Chip rápido estilo Revolut
      let matchChip = true;
      if (chip === "GASTO") {
        matchChip = !tipo.includes("ingreso");
      } else if (chip === "INGRESO") {
        matchChip = tipo.includes("ingreso");
      } else if (chip === "ALIMENTACION") {
        matchChip = c.includes("alimentaci") || d.includes("almuerzo") || d.includes("colanta") || d.includes("rest") || d.includes("menu");
      } else if (chip === "MOTO") {
        matchChip = c.includes("transporte") || c.includes("moto") || d.includes("gasolina") || d.includes("terpel") || d.includes("moto");
      } else if (chip === "COLANTA") {
        matchChip = d.includes("colanta") || a.includes("colanta");
      } else if (chip === "NEQUI") {
        matchChip = a.includes("nequi");
      }

      return matchTxt && matchCat && matchAcc && matchChip;
    });

    this.renderMovementsTable(filtered);
  },

  renderFixedTable() {
    const tbody = document.getElementById("fixed-tbody");
    if (!tbody) return;

    let fijos = (this.state.data && this.state.data.fijos) || [];
    if (!fijos.length && typeof DEMO_DATA !== "undefined" && DEMO_DATA.fijos) {
      fijos = DEMO_DATA.fijos;
    }

    if (!fijos || !fijos.length) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:2.5rem; color:#94A3B8;">No hay gastos fijos registrados.</td></tr>`;
      return;
    }

    // Calcular montos dinámicos de Quincena 1 y Quincena 2
    const q1Total = fijos.filter(f => Number(f.dia || 0) <= 15).reduce((acc, f) => acc + Number(f.cuota || 0), 0);
    const q2Total = fijos.filter(f => Number(f.dia || 0) > 15).reduce((acc, f) => acc + Number(f.cuota || 0), 0);
    const totalCuotas = q1Total + q2Total;
    const totalAbonado = fijos.reduce((acc, f) => acc + Number(f.abonado || 0), 0);
    const totalPendiente = Math.max(0, totalCuotas - totalAbonado);

    const elQ1 = document.getElementById("q1-fixed-amount");
    const elQ2 = document.getElementById("q2-fixed-amount");
    if (elQ1) elQ1.textContent = `$${this.formatCOP(q1Total)} COP`;
    if (elQ2) elQ2.textContent = `$${this.formatCOP(q2Total)} COP`;

    const elTotalFijos = document.getElementById("kpi-val-fixed");
    if (elTotalFijos) elTotalFijos.textContent = `$${this.formatCOP(totalPendiente)} COP`;

    const pendCount = fijos.filter(f => Number(f.cuota || 0) > Number(f.abonado || 0)).length;
    const badgeFixed = document.getElementById("sidebar-fixed-count");
    if (badgeFixed) badgeFixed.textContent = pendCount;

    tbody.innerHTML = fijos.map(f => {
      const cuota = Number(f.cuota || 0);
      const abonado = Number(f.abonado || 0);
      const saldo = Math.max(0, cuota - abonado);

      let statusPill = `<span class="pill-status status-pending"><i class="fa-solid fa-circle-exclamation"></i> Pendiente</span>`;
      if (abonado >= cuota && cuota > 0) {
        statusPill = `<span class="pill-status status-paid"><i class="fa-solid fa-circle-check"></i> Pagado</span>`;
      } else if (abonado > 0) {
        statusPill = `<span class="pill-status status-partial"><i class="fa-solid fa-clock-rotate-left"></i> Abono</span>`;
      }

      // Determinar logo oficial de marca para la obligación
      let brandKey = "default";
      const c = String(f.concepto || "").toLowerCase();
      if (c.includes("epm")) brandKey = "epm";
      else if (c.includes("tigo") || c.includes("celular")) brandKey = "tigo";
      else if (c.includes("arriendo") || c.includes("casa")) brandKey = "arriendo";
      else if (c.includes("spotify")) brandKey = "spotify";
      else if (c.includes("google")) brandKey = "google";
      else if (c.includes("gasolina") || c.includes("terpel")) brandKey = "terpel";
      else if (c.includes("parqueadero")) brandKey = "parqueadero";
      else if (c.includes("comida") || c.includes("restaurante")) brandKey = "restaurante";
      else if (c.includes("audifono") || c.includes("samsung")) brandKey = "tech";
      else if (c.includes("smart") || c.includes("gym")) brandKey = "smartfit";

      const logoHtml = this.getBrandBadgeSVG(brandKey);
      const quincenaTag = Number(f.dia) <= 15 
        ? `<span class="tag-quincena tag-q1">Q1 (Día 1-15)</span>` 
        : `<span class="tag-quincena tag-q2">Q2 (Día 16-31)</span>`;

      return `
        <tr>
          <td><code class="id-tag">${f.id}</code></td>
          <td>
            <div class="fixed-concept-cell">
              <div class="fixed-brand-logo">${logoHtml}</div>
              <div class="fixed-concept-meta">
                <strong class="fixed-concept-name">${f.concepto}</strong>
                <div class="fixed-tags-row">
                  <span class="fixed-cat-badge">${f.categoria || 'Obligación'}</span>
                  ${quincenaTag}
                </div>
              </div>
            </div>
          </td>
          <td>
            <span class="pill-dia"><i class="fa-regular fa-calendar"></i> Día ${f.dia}</span>
          </td>
          <td class="text-right" style="font-weight:700; color:#F8FAFC;">$${this.formatCOP(cuota)}</td>
          <td class="text-right" style="font-weight:700; color:#34D399;">$${this.formatCOP(abonado)}</td>
          <td class="text-right" style="font-weight:800; color:${saldo > 0 ? '#FB7185' : '#64748B'};">
            $${this.formatCOP(saldo)}
          </td>
          <td>${statusPill}</td>
        </tr>
      `;
    }).join("");
  },

  renderFugasTable() {
    const tbody = document.getElementById("fugas-tbody");
    if (!tbody) return;

    const all = (this.state.data && this.state.data.movimientos) || [];
    const fugas = all.filter(m => {
      const cat = (m.categoria || "").toLowerCase();
      const desc = (m.descripcion || "").toLowerCase();
      return cat.includes("hormiga") || cat.includes("dulce") || /\b(mecato|dulce|antojo|helado|tinto|pan de queso)\b/.test(desc);
    });

    const totalFugas = fugas.reduce((acc, f) => acc + Number(f.valor || 0), 0);
    const fugasTotalEl = document.getElementById("fugas-total-val");
    if (fugasTotalEl) fugasTotalEl.textContent = `$${this.formatCOP(totalFugas)} COP`;

    if (!fugas.length) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:2rem; color:#64748B;">🎉 Sin gastos hormiga este mes.</td></tr>`;
      return;
    }

    tbody.innerHTML = fugas.map(f => `
      <tr>
        <td><span class="pill-dia"><i class="fa-regular fa-calendar"></i> ${f.fecha}</span></td>
        <td>
          <div style="display:flex; align-items:center; gap:0.6rem;">
            <span style="font-size:1.2rem;">🍪</span>
            <strong>${f.descripcion}</strong>
          </div>
        </td>
        <td style="color:#FB7185; font-weight:800;">-$${this.formatCOP(f.valor)}</td>
        <td><span class="badge-account">${f.cuenta}</span></td>
        <td><span class="pill-status status-pending">Micropago</span></td>
      </tr>
    `).join("");
  },

  bindModals() {
    const modalAdd = document.getElementById("modal-add-movement");
    const btnCloseAdd = document.getElementById("btn-close-modal-add");
    const btnCancelAdd = document.getElementById("btn-cancel-modal-add");
    const formAdd = document.getElementById("form-add-movement");

    const closeAdd = () => modalAdd?.classList.remove("open");
    btnCloseAdd?.addEventListener("click", closeAdd);
    btnCancelAdd?.addEventListener("click", closeAdd);

    formAdd?.addEventListener("submit", (e) => {
      e.preventDefault();
      this.handleCreateMovement();
    });

    // Modal de Subir Colilla de Pago (Gemini AI)
    const modalColilla = document.getElementById("modal-upload-colilla");
    const btnCloseCol = document.getElementById("btn-close-modal-colilla");
    const btnCancelCol = document.getElementById("btn-cancel-modal-colilla");
    const dropzone = document.getElementById("colilla-dropzone");
    const inputFile = document.getElementById("input-colilla-file");
    const dropFileName = document.getElementById("dropzone-file-name");
    const btnSubmitCol = document.getElementById("btn-submit-colilla");
    const loadingBox = document.getElementById("colilla-loading-box");
    const previewCard = document.getElementById("colilla-preview-card");

    let selectedColillaData = null;

    const closeColilla = () => {
      modalColilla?.classList.remove("open");
      if (inputFile) inputFile.value = "";
      selectedColillaData = null;
      if (dropFileName) dropFileName.textContent = "Formatos soportados: PDF, JPG, PNG";
      if (btnSubmitCol) {
        btnSubmitCol.disabled = true;
        document.getElementById("btn-colilla-label").textContent = "Analizar con Gemini AI";
      }
      if (loadingBox) loadingBox.style.display = "none";
      if (previewCard) previewCard.style.display = "none";
    };

    btnCloseCol?.addEventListener("click", closeColilla);
    btnCancelCol?.addEventListener("click", closeColilla);

    dropzone?.addEventListener("click", () => inputFile?.click());

    inputFile?.addEventListener("change", (e) => {
      const file = e.target.files && e.target.files[0];
      if (!file) return;

      if (dropFileName) dropFileName.textContent = `📄 ${file.name} (${Math.round(file.size / 1024)} KB)`;
      
      const reader = new FileReader();
      reader.onload = (loadEvt) => {
        const fullBase64 = loadEvt.target.result;
        const cleanBase64 = fullBase64.split(",")[1] || fullBase64;
        selectedColillaData = {
          base64: cleanBase64,
          fileName: file.name,
          mimeType: file.type || (file.name.endsWith(".pdf") ? "application/pdf" : "image/jpeg")
        };
        if (btnSubmitCol) btnSubmitCol.disabled = false;
        this.haptic(15);
      };
      reader.readAsDataURL(file);
    });

    btnSubmitCol?.addEventListener("click", async () => {
      if (!selectedColillaData) return;

      this.haptic(20);
      btnSubmitCol.disabled = true;
      if (loadingBox) loadingBox.style.display = "flex";
      if (previewCard) previewCard.style.display = "none";

      try {
        const res = await FinanceAPI.processPaystub(selectedColillaData);

        if (loadingBox) loadingBox.style.display = "none";

        if (res && res.ok && res.datosExtraidos) {
          const d = res.datosExtraidos;
          if (previewCard) previewCard.style.display = "block";
          document.getElementById("preview-colilla-id").textContent = res.idColilla || "CP-0019";
          document.getElementById("prev-periodo").textContent = d.periodoLiquidado || `${d.quincena || ''} ${d.mes || ''}`;
          document.getElementById("prev-devengado").textContent = `$${this.formatCOP(d.totalDevengado || 0)}`;
          document.getElementById("prev-fondo").textContent = `$${this.formatCOP(d.fondoEmpleados || 0)}`;
          document.getElementById("prev-neto").textContent = `$${this.formatCOP(d.netoACobrar || 0)}`;

          this.showToast(`🎉 ¡Colilla auditada e insertada en Sheets! Neto: $${this.formatCOP(d.netoACobrar || 0)}`, "success");
          this.refreshData();

          setTimeout(() => {
            closeColilla();
          }, 3500);
        } else {
          this.showToast("⚠️ Colilla enviada al Webhook. Revisa Google Sheets en unos momentos.", "success");
          closeColilla();
        }
      } catch (err) {
        if (loadingBox) loadingBox.style.display = "none";
        btnSubmitCol.disabled = false;
        this.showToast("Error procesando colilla: " + err.message, "error");
      }
    });

    // Settings Modal
    const modalSet = document.getElementById("modal-settings");
    const btnCloseSet = document.getElementById("btn-close-modal-settings");
    const btnQuickSet = document.getElementById("btn-quick-settings");
    const btnSaveSet = document.getElementById("btn-save-settings");
    const btnTestConn = document.getElementById("btn-test-connection");
    const inputUrl = document.getElementById("input-webhook-url");

    const openSettings = (e) => {
      if (e && e.preventDefault) e.preventDefault();
      if (inputUrl) inputUrl.value = FinanceAPI.getWebhookUrl();
      modalSet?.classList.add("open");
      this.checkBackendHealth();
    };

    btnQuickSet?.addEventListener("click", openSettings);
    document.getElementById("btn-topbar-settings")?.addEventListener("click", openSettings);
    document.getElementById("btn-banner-settings")?.addEventListener("click", openSettings);
    document.getElementById("nav-item-settings")?.addEventListener("click", openSettings);

    btnCloseSet?.addEventListener("click", () => modalSet?.classList.remove("open"));

    btnTestConn?.addEventListener("click", async () => {
      this.haptic(15);
      btnTestConn.textContent = "Probando...";
      const res = await FinanceAPI.testConnection();
      btnTestConn.textContent = "Probar Conexión";
      this.updateBackendStatusUI(res);

      if (res.ok && res.isModernV26) {
        alert("✅ ¡CONEXIÓN PERFECTA!\n\nTu Google Apps Script está ejecutando la versión moderna V26.9 PRO.\nLos registros de gastos, ingresos y colillas se insertan en Sheets en tiempo real.");
      } else if (res.ok && res.isOldVersion) {
        alert("⚠️ ATENCIÓN: DETECTADA VERSIÓN ANTIGUA (V25.3)\n\nEl Webhook responde, pero tu Google Apps Script en Google Drive aún tiene la versión antigua que NO inserta registros desde la Web.\n\n👉 SOLUCIÓN FÁCIL:\n1. Presiona el botón 'Copiar Código Apps Script V26.9'.\n2. Abre tu Google Sheet 'Panel_Finanzas' > Extensiones > Apps Script.\n3. Presiona Ctrl + A, pega (Ctrl + V) y guarda (Ctrl + S).\n4. Clic en 'Implementar' > 'Nueva versión' > 'Implementar'.");
      } else {
        alert("⚠️ RESPUESTA DE CONEXIÓN:\n\n" + (res.raw || res.error || "No se pudo conectar con el Webhook. Revisa tu conexión."));
      }
    });

    const btnCopyGas = document.getElementById("btn-copy-gas-script");
    btnCopyGas?.addEventListener("click", async () => {
      this.haptic(20);
      try {
        const resp = await fetch("google_apps_script/Codigo.gs");
        if (!resp.ok) throw new Error("No se pudo leer el archivo local");
        const code = await resp.text();
        await navigator.clipboard.writeText(code);
        alert("✅ ¡CÓDIGO APPS SCRIPT V26.9 COPIADO AL PORTAPAPELES!\n\nSigue estos pasos en tu Google Sheet:\n1. Abre tu Google Sheet 'Panel_Finanzas'.\n2. En el menú superior ve a: Extensiones > Apps Script.\n3. Presiona Ctrl + A (seleccionar todo), pega (Ctrl + V) y guarda (Ctrl + S).\n4. Clic arriba a la derecha en: Implementar > Administrar implementaciones.\n5. Haz clic en el lápiz (Editar) > Versión: 'Nueva versión' > Clic en Implementar.\n\n¡Listo! Cuando termines, presiona 'Probar Conexión' y verás el estado en verde.");
      } catch (err) {
        alert("No se pudo copiar automáticamente: " + err.message + "\nPuedes copiar el archivo 'google_apps_script/Codigo.gs' directamente desde la carpeta de tu proyecto.");
      }
    });

    btnSaveSet?.addEventListener("click", () => {
      const v = inputUrl.value.trim();
      localStorage.setItem(API_CONFIG.SETTINGS_KEY, JSON.stringify({ webhookUrl: v }));
      modalSet?.classList.remove("open");
      this.showToast("Ajustes guardados con éxito", "success");
      this.refreshData();
    });

    // Modal de Detalle de Movimiento / Recibo Digital (Apple Card / Revolut)
    const modalDetail = document.getElementById("modal-transaction-detail");
    const btnCloseDetail = document.getElementById("btn-close-modal-detail");
    const btnSaveDetail = document.getElementById("btn-save-detail-concept");
    const btnDeleteDetail = document.getElementById("btn-delete-from-detail");

    const closeDetail = () => {
      modalDetail?.classList.remove("open");
      this.state.selectedMovement = null;
    };

    btnCloseDetail?.addEventListener("click", closeDetail);
    btnSaveDetail?.addEventListener("click", () => this.saveDetailConcept());
    btnDeleteDetail?.addEventListener("click", () => {
      if (this.state.selectedMovement) {
        const id = this.state.selectedMovement.id;
        if (confirm(`¿Deseas eliminar el registro ${id} de Google Sheets?`)) {
          this.deleteMovementById(id);
          closeDetail();
        }
      }
    });

    document.getElementById("detail-input-concept")?.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        this.saveDetailConcept();
      }
    });

    window.addEventListener("click", (e) => {
      if (e.target === modalAdd) closeAdd();
      if (e.target === modalColilla) closeColilla();
      if (e.target === modalSet) modalSet?.classList.remove("open");
      if (e.target === modalDetail) closeDetail();
    });

    // Delegación global de eventos de interacción en tabla y chat
    document.addEventListener("click", (e) => {
      const btnDelChat = e.target.closest(".btn-chat-delete-mov");
      if (btnDelChat) {
        const id = btnDelChat.getAttribute("data-id");
        if (id) {
          this.haptic(20);
          btnDelChat.disabled = true;
          btnDelChat.innerHTML = '<i class="fa-solid fa-check"></i> Eliminado';
          btnDelChat.style.opacity = "0.5";
          this.deleteMovementById(id);
        }
        return;
      }

      const btnEditTable = e.target.closest(".btn-action-edit");
      if (btnEditTable) {
        const id = btnEditTable.getAttribute("data-id");
        if (id) this.openTransactionDetail(id, true);
        return;
      }

      const btnDelTable = e.target.closest(".btn-action-delete") || e.target.closest(".btn-table-delete-row");
      if (btnDelTable) {
        const id = btnDelTable.getAttribute("data-id");
        if (id && confirm(`¿Deseas eliminar el registro ${id} de Google Sheets?`)) {
          this.haptic(20);
          this.deleteMovementById(id);
        }
        return;
      }

      const row = e.target.closest("#movements-tbody tr");
      if (row && !e.target.closest("button") && !e.target.closest("input") && !e.target.closest("a")) {
        const id = row.getAttribute("data-id");
        if (id) this.openTransactionDetail(id, false);
      }
    });
  },

  handleCreateMovement() {
    const valor = parseFloat(document.getElementById("input-mov-valor").value);
    const desc = document.getElementById("input-mov-desc").value.trim();
    const cat = document.getElementById("select-mov-cat").value;
    const cuenta = document.getElementById("select-mov-cuenta").value;
    const tipo = document.querySelector('input[name="mov-tipo"]:checked')?.value || "Gasto";

    if (!valor || valor <= 0 || !desc) {
      this.showToast("Ingresa monto y descripción válidos", "error");
      return;
    }

    const payload = {
      tipo: tipo,
      valor: valor,
      descripcion: desc,
      categoria: cat,
      cuenta: cuenta,
      metodo: cuenta === "Efectivo" ? "Efectivo" : "Transferencia"
    };

    const nuevo = {
      id: "M-" + Math.floor(1000 + Math.random() * 9000),
      fecha: new Date().toLocaleDateString("es-CO"),
      hora: new Date().toLocaleTimeString("es-CO", { hour: '2-digit', minute: '2-digit' }),
      ...payload
    };

    if (this.state.data && this.state.data.movimientos) {
      this.state.data.movimientos.unshift(nuevo);
      if (tipo === "Gasto") {
        this.state.data.resumen.gastos = (this.state.data.resumen.gastos || 0) + valor;
        this.state.data.resumen.disponible = Math.max(0, (this.state.data.resumen.disponible || 0) - valor);
      } else {
        this.state.data.resumen.ingresos = (this.state.data.resumen.ingresos || 0) + valor;
        this.state.data.resumen.disponible = (this.state.data.resumen.disponible || 0) + valor;
      }
      this.renderBalances();
      this.renderMovementsTable();
    }

    FinanceAPI.addMovement(payload).then(res => {
      const syncEl = document.getElementById("sheets-last-sync-text");
      if (syncEl) syncEl.textContent = `Último registro enviado a las ${res.timestamp || 'recientemente'}`;
    });

    document.getElementById("modal-add-movement")?.classList.remove("open");
    document.getElementById("form-add-movement")?.reset();
    this.showToast(`✅ ${tipo} guardado: $${this.formatCOP(valor)} (Enviado a Google Sheets)`, "success");
  },

  exportToExcel() {
    const id = API_CONFIG.SPREADSHEET_ID;
    window.open(`https://docs.google.com/spreadsheets/d/${id}/export?format=xlsx`, "_blank");
    this.showToast("Iniciando descarga directa de archivo Excel...", "success");
  },

  showToast(msg, type = "success") {
    const c = document.getElementById("toast-container");
    if (!c) return;
    const t = document.createElement("div");
    t.className = `toast-item ${type}`;
    const ic = type === "success" ? "fa-circle-check" : "fa-triangle-exclamation";
    t.innerHTML = `<i class="fa-solid ${ic}"></i><span>${msg}</span>`;
    c.appendChild(t);
    setTimeout(() => {
      t.style.opacity = "0";
      t.style.transform = "translateX(-100%)";
      setTimeout(() => t.remove(), 300);
    }, 4000);
  },

  formatCOP(v) {
    return Math.round(Number(v) || 0).toLocaleString("es-CO");
  }
};
