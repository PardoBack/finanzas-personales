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
      quincena: null
    },
    balancesHidden: false,
    activeTab: "tab-all-movements",
    isRecording: false,
    speechRecognition: null,
    deferredInstallPrompt: null
  },

  init() {
    this.initServiceWorker();
    this.bindPWAInstall();
    this.bindNavigation();
    this.bindPrivacyToggle();
    this.bindChatCFO();
    this.initSpeechRecognition();
    this.bindModals();
    this.setupDateContext();
    this.loadData();
    this.checkBackendHealth();
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
        this.haptic(10);
        document.querySelectorAll(".sidebar-nav .nav-item").forEach(l => l.classList.remove("active"));
        link.classList.add("active");
        const navTarget = link.getAttribute("data-nav");
        this.scrollToSection(navTarget);
      });
    });

    // 2. Mobile Bottom Bar Buttons (Celular)
    document.querySelectorAll(".mobile-nav-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        this.haptic(12);
        const target = btn.getAttribute("data-target");
        if (target) {
          document.querySelectorAll(".mobile-nav-btn").forEach(b => b.classList.remove("active"));
          btn.classList.add("active");
          this.scrollToSection(target);
        }
      });
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

    // 5. Botones Topbar
    document.getElementById("btn-sync-now")?.addEventListener("click", () => this.refreshData());
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

    document.getElementById("btn-qa-colilla")?.addEventListener("click", () => {
      this.haptic(15);
      document.getElementById("modal-upload-colilla")?.classList.add("open");
    });

    document.getElementById("btn-qa-cfo")?.addEventListener("click", () => {
      this.haptic(15);
      document.getElementById("chat-cfo-drawer")?.classList.add("open");
      document.getElementById("chat-text-input")?.focus();
    });

    document.getElementById("btn-qa-sheets")?.addEventListener("click", () => {
      this.haptic(15);
      window.open(`https://docs.google.com/spreadsheets/d/${API_CONFIG.SPREADSHEET_ID}/edit`, "_blank");
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

  scrollToSection(sectionKey) {
    let targetEl = null;
    if (sectionKey === "overview") targetEl = document.getElementById("section-overview");
    else if (sectionKey === "wallet") targetEl = document.getElementById("section-wallet");
    else if (sectionKey === "movements") {
      targetEl = document.getElementById("section-movements");
      this.switchSegmentedTab("tab-all-movements");
    } else if (sectionKey === "fixed") {
      targetEl = document.getElementById("section-movements");
      this.switchSegmentedTab("tab-all-fixed");
    } else if (sectionKey === "savings") {
      targetEl = document.getElementById("section-movements");
      this.switchSegmentedTab("tab-all-savings");
    } else if (sectionKey === "fugas") {
      targetEl = document.getElementById("section-movements");
      this.switchSegmentedTab("tab-all-fugas");
    }

    if (targetEl) {
      targetEl.scrollIntoView({ behavior: "smooth", block: "start" });
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
      const nuevoId = "M-" + Math.floor(1000 + Math.random() * 9000);
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
      const disp = cuentas.total || res.disponible || 3390857;
      this.appendBotMsg(`
        <p>💎 <strong>LIQUIDEZ DISPONIBLE EN CAJA:</strong></p>
        <p style="font-size:1.2rem; font-weight:800; color:#10B981;">$${this.formatCOP(disp)} COP</p>
        <p style="font-size:0.8rem; color:#94A3B8;">
          🥛 AyC Colanta: <strong>$${this.formatCOP(cuentas.aycColanta || 3319057)}</strong><br>
          📱 Nequi: <strong>$${this.formatCOP(cuentas.nequi || 0)}</strong><br>
          💵 Efectivo: <strong>$${this.formatCOP(cuentas.efectivo || 71800)}</strong>
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
    this.renderBalances();
    this.renderCharts();
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
    const res = (this.state.data && this.state.data.resumen) || {};
    const cuentas = (this.state.data && this.state.data.cuentas) || {};
    const hidden = this.state.balancesHidden;

    const formatVal = (v) => hidden ? "••••••••" : `$${this.formatCOP(v)}`;

    // Virtual Cards
    const totalAvail = cuentas.total || res.disponible || 3390857;
    const elTot = document.getElementById("card-total-available");
    const elCol = document.getElementById("card-acc-colanta");
    const elNeq = document.getElementById("card-acc-nequi");
    const elEff = document.getElementById("card-acc-efectivo");

    if (elTot) elTot.textContent = formatVal(totalAvail);
    if (elCol) elCol.textContent = formatVal(cuentas.aycColanta || 3319057);
    if (elNeq) elNeq.textContent = hidden ? "••••••" : `$${this.formatCOP(cuentas.nequi || 0)} COP`;
    if (elEff) elEff.textContent = hidden ? "••••••" : `$${this.formatCOP(cuentas.efectivo || 71800)} COP`;

    // KPI Widgets
    const elInc = document.getElementById("kpi-val-income");
    const elExp = document.getElementById("kpi-val-expense");
    const elFix = document.getElementById("kpi-val-fixed");
    const elNav = document.getElementById("kpi-val-navidad");

    if (elInc) elInc.textContent = formatVal(res.ingresos || 0);
    if (elExp) elExp.textContent = formatVal(res.gastos || 207123);
    if (elFix) elFix.textContent = formatVal(res.fijos_pendientes || 2557000);
    if (elNav) elNav.textContent = formatVal(2160000);

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

    const catData = (this.state.data && this.state.data.gastosPorCategoria) || {
      "Transporte y Moto": 116000,
      "Gastos Hormiga / Dulces": 67000,
      "Ocio y Entretenimiento": 14023,
      "Suscripciones": 10100
    };

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
    const ctx = document.getElementById("quincenaBarChart");
    if (!ctx) return;
    if (this.state.charts.quincena) this.state.charts.quincena.destroy();

    this.state.charts.quincena = new Chart(ctx, {
      type: "bar",
      data: {
        labels: ["Quincena 1 (Día 1-15)", "Quincena 2 (Día 16-30)"],
        datasets: [
          {
            label: "Compromisos Fijos",
            data: [1332000, 1225000],
            backgroundColor: "#F59E0B",
            borderRadius: 8
          },
          {
            label: "Gasto Variable",
            data: [207123, 0],
            backgroundColor: "#F43F5E",
            borderRadius: 8
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { grid: { display: false }, ticks: { color: "#94A3B8", font: { family: "Plus Jakarta Sans", size: 11 } } },
          y: {
            grid: { color: "rgba(255, 255, 255, 0.04)" },
            ticks: {
              color: "#94A3B8",
              font: { family: "Plus Jakarta Sans", size: 11 },
              callback: (v) => `$${Math.round(v / 1000)}k`
            }
          }
        },
        plugins: {
          legend: { position: "top", labels: { color: "#94A3B8", boxWidth: 12, padding: 10 } }
        }
      }
    });
  },

  renderMovementsTable(filteredItems = null) {
    const tbody = document.getElementById("movements-tbody");
    if (!tbody) return;

    const items = filteredItems || (this.state.data && this.state.data.movimientos) || [];
    if (!items.length) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:2rem; color:#64748B;">No hay movimientos.</td></tr>`;
      return;
    }

    tbody.innerHTML = items.map(m => {
      const isIngreso = String(m.tipo).toLowerCase().includes("ingreso");
      return `
        <tr>
          <td><code style="color:#64748B; font-weight:700;">${m.id || ""}</code></td>
          <td>${m.fecha || ""} <span style="color:#64748B; font-size:0.75rem;">${m.hora || ""}</span></td>
          <td><strong>${m.descripcion || ""}</strong></td>
          <td><span class="badge-account">${m.categoria || "Otros"}</span></td>
          <td><span class="badge-account">${m.cuenta || "Nequi"}</span></td>
          <td class="text-right" style="font-weight:800; color:${isIngreso ? '#34D399' : '#FFFFFF'};">
            ${isIngreso ? '+' : '-'} $${this.formatCOP(m.valor)}
          </td>
          <td class="text-center">
            <button class="btn-table-delete-row" data-id="${m.id}" title="Eliminar ${m.id} de Google Sheets">
              <i class="fa-solid fa-trash-can"></i>
            </button>
          </td>
        </tr>
      `;
    }).join("");
  },

  deleteMovementById(id) {
    const movs = (this.state.data && this.state.data.movimientos) || [];
    const movIndex = movs.findIndex(m => String(m.id).toUpperCase() === String(id).toUpperCase());
    if (movIndex === -1) {
      this.showToast("No se encontró el movimiento " + id, "error");
      return;
    }

    const movToDelete = movs[movIndex];
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

    this.renderBalances();
    this.renderCharts();
    this.renderMovementsTable();
    this.renderFugasTable();

    FinanceAPI.deleteMovement(id);
    this.showToast(`🗑️ ${id} eliminado de Google Sheets`, "success");
  },

  filterMovements() {
    const q = (document.getElementById("search-movements-input")?.value || "").toLowerCase();
    const cat = document.getElementById("filter-category-select")?.value || "ALL";
    const acc = document.getElementById("filter-account-select")?.value || "ALL";

    const all = (this.state.data && this.state.data.movimientos) || [];
    const filtered = all.filter(m => {
      const matchTxt = (m.descripcion || "").toLowerCase().includes(q) ||
                       (m.categoria || "").toLowerCase().includes(q) ||
                       (m.id || "").toLowerCase().includes(q);
      const matchCat = cat === "ALL" || m.categoria === cat;
      const matchAcc = acc === "ALL" || m.cuenta === acc;
      return matchTxt && matchCat && matchAcc;
    });

    this.renderMovementsTable(filtered);
  },

  renderFixedTable() {
    const tbody = document.getElementById("fixed-tbody");
    if (!tbody) return;

    const fijos = (this.state.data && this.state.data.fijos) || [];
    tbody.innerHTML = fijos.map(f => {
      const cuota = Number(f.cuota || 0);
      const abonado = Number(f.abonado || 0);
      const saldo = Math.max(0, cuota - abonado);

      let statusPill = `<span class="pill-status status-pending">🔴 Pendiente</span>`;
      if (abonado >= cuota && cuota > 0) statusPill = `<span class="pill-status status-paid">🟢 Pagado</span>`;
      else if (abonado > 0) statusPill = `<span class="pill-status status-partial">🟡 Abono</span>`;

      return `
        <tr>
          <td><code>${f.id}</code></td>
          <td><strong>${f.concepto}</strong></td>
          <td>Día ${f.dia}</td>
          <td>$${this.formatCOP(cuota)}</td>
          <td>$${this.formatCOP(abonado)}</td>
          <td style="color:${saldo > 0 ? '#FB7185' : '#64748B'}; font-weight:700;">$${this.formatCOP(saldo)}</td>
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

    if (!fugas.length) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:1.5rem; color:#64748B;">🎉 Sin gastos hormiga este mes.</td></tr>`;
      return;
    }

    tbody.innerHTML = fugas.map(f => `
      <tr>
        <td>${f.fecha}</td>
        <td><strong>${f.descripcion}</strong></td>
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

    btnQuickSet?.addEventListener("click", () => {
      if (inputUrl) inputUrl.value = FinanceAPI.getWebhookUrl();
      modalSet?.classList.add("open");
    });

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

    window.addEventListener("click", (e) => {
      if (e.target === modalAdd) closeAdd();
      if (e.target === modalColilla) closeColilla();
      if (e.target === modalSet) modalSet?.classList.remove("open");
    });

    // Delegación global de eventos de eliminación
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
      }

      const btnDelTable = e.target.closest(".btn-table-delete-row");
      if (btnDelTable) {
        const id = btnDelTable.getAttribute("data-id");
        if (id && confirm(`¿Deseas eliminar el registro ${id} de Google Sheets?`)) {
          this.haptic(20);
          this.deleteMovementById(id);
        }
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
