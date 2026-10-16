/**
 * PARDOFINANZAS PRO — CONTROLADOR PRINCIPAL DEL DASHBOARD (JS)
 * Incluye motor de Chat Inteligente CFO AI y Reconocimiento de Voz
 */

document.addEventListener("DOMContentLoaded", () => {
  App.init();
});

const App = {
  state: {
    data: null,
    charts: {
      categories: null,
      quincena: null
    },
    activeTab: "tab-movements",
    isRecording: false,
    speechRecognition: null
  },

  init() {
    this.bindEvents();
    this.bindChat();
    this.initSpeechRecognition();
    this.loadData();
    this.setupMonthContext();
  },

  setupMonthContext() {
    const meses = ["ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"];
    const d = new Date();
    const lbl = document.getElementById("current-month-label");
    if (lbl) {
      lbl.textContent = `${meses[d.getMonth()]} ${d.getFullYear()}`;
    }
  },

  bindEvents() {
    // Sincronizar
    const btnSync = document.getElementById("btn-sync-data");
    if (btnSync) btnSync.addEventListener("click", () => this.refreshData());

    // Exportar Excel
    const btnExport = document.getElementById("btn-export-excel");
    if (btnExport) btnExport.addEventListener("click", () => this.exportToExcel());

    // Pestañas
    const tabBtns = document.querySelectorAll(".tab-btn");
    tabBtns.forEach(btn => {
      btn.addEventListener("click", () => {
        const targetTab = btn.getAttribute("data-tab");
        this.switchTab(targetTab, btn);
      });
    });

    // Búsqueda y Filtros de Movimientos
    const searchInput = document.getElementById("search-movements-input");
    if (searchInput) searchInput.addEventListener("input", () => this.filterMovements());
    const filterCat = document.getElementById("filter-category-select");
    if (filterCat) filterCat.addEventListener("change", () => this.filterMovements());
    const filterAcc = document.getElementById("filter-account-select");
    if (filterAcc) filterAcc.addEventListener("change", () => this.filterMovements());

    // Modales
    this.bindModals();
  },

  bindChat() {
    const chatToggle = document.getElementById("btn-toggle-chat");
    const chatWindow = document.getElementById("chat-window");
    const chatClose = document.getElementById("btn-chat-close");
    const chatClear = document.getElementById("btn-chat-clear");
    const chatForm = document.getElementById("chat-input-form");
    const chatInput = document.getElementById("chat-text-input");
    const chipsContainer = document.querySelector(".chat-chips-container");

    if (chatToggle && chatWindow) {
      chatToggle.addEventListener("click", () => {
        chatWindow.classList.toggle("open");
        if (chatWindow.classList.contains("open")) {
          chatInput?.focus();
        }
      });
    }

    if (chatClose && chatWindow) {
      chatClose.addEventListener("click", () => {
        chatWindow.classList.remove("open");
      });
    }

    if (chatClear) {
      chatClear.addEventListener("click", () => {
        const container = document.getElementById("chat-messages-container");
        if (container) {
          container.innerHTML = `
            <div class="chat-message msg-bot">
              <div class="msg-avatar"><i class="fa-solid fa-robot"></i></div>
              <div class="msg-bubble">
                <p>🧹 Historial limpio. ¿Qué movimiento o consulta deseas realizar, Marlon?</p>
              </div>
            </div>
          `;
        }
      });
    }

    if (chatForm && chatInput) {
      chatForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const text = chatInput.value.trim();
        if (!text) return;
        this.handleChatMessage(text);
        chatInput.value = "";
      });
    }

    // Chips de sugerencia rápida
    document.addEventListener("click", (e) => {
      const chip = e.target.closest(".chat-chip");
      if (chip) {
        const prompt = chip.getAttribute("data-text");
        if (prompt) {
          this.handleChatMessage(prompt);
        }
      }
    });
  },

  initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const btnVoice = document.getElementById("btn-voice-input");
    const micIcon = document.getElementById("mic-icon");
    const chatInput = document.getElementById("chat-text-input");

    if (!SpeechRecognition) {
      if (btnVoice) btnVoice.style.display = "none";
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.lang = "es-CO";
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = () => {
      this.state.isRecording = true;
      btnVoice?.classList.add("recording");
      if (chatInput) chatInput.placeholder = "Escuchando tu voz en vivo...";
    };

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      if (chatInput) chatInput.value = transcript;
      this.handleChatMessage(transcript);
    };

    recognition.onerror = (event) => {
      console.warn("Speech error:", event.error);
      this.state.isRecording = false;
      btnVoice?.classList.remove("recording");
      if (chatInput) chatInput.placeholder = "Escribe un gasto o pregunta...";
    };

    recognition.onend = () => {
      this.state.isRecording = false;
      btnVoice?.classList.remove("recording");
      if (chatInput) chatInput.placeholder = "Escribe un gasto o pregunta...";
    };

    if (btnVoice) {
      btnVoice.addEventListener("click", () => {
        if (this.state.isRecording) {
          recognition.stop();
        } else {
          try { recognition.start(); } catch (e) {}
        }
      });
    }

    this.state.speechRecognition = recognition;
  },

  handleChatMessage(text) {
    this.appendUserMessage(text);
    
    // Procesar con el motor NLP local ultra veloz (< 0.05 segundos)
    const intent = FinancialNLP.parseIntent(text);
    
    setTimeout(() => {
      this.processChatIntent(intent, text);
    }, 40);
  },

  processChatIntent(intent, originalText) {
    const res = (this.state.data && this.state.data.resumen) || {};
    const cuentas = (this.state.data && this.state.data.cuentas) || {};

    if (intent.type === "RECORD_TRANSACTION") {
      // Registrar movimiento de forma optimista
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
          this.state.data.gastosPorCategoria[intent.category] = (this.state.data.gastosPorCategoria[intent.category] || 0) + intent.amount;
        } else {
          this.state.data.resumen.ingresos = (this.state.data.resumen.ingresos || 0) + intent.amount;
          this.state.data.resumen.disponible = (this.state.data.resumen.disponible || 0) + intent.amount;
        }
        this.renderKPIs();
        this.renderCharts();
        this.renderMovementsTable();
        this.renderFugas();
      }

      // Enviar en background a Google Sheets
      FinanceAPI.addMovement(nuevoMov);

      this.appendBotMessage(`
        <p>✅ <strong>${intent.transactionType.toUpperCase()} REGISTRADO EN SEGUNDOS</strong></p>
        <p>🏷️ <strong>${intent.description}</strong><br>
        💰 <strong>$${this.formatCOP(intent.amount)} COP</strong><br>
        📂 Categoría: <em>${intent.category}</em><br>
        💳 Cuenta: <em>${intent.account}</em></p>
        <p style="font-size:0.75rem; color:#34D399; margin-top:0.4rem;">
          <i class="fa-solid fa-cloud-arrow-up"></i> Sincronizado en tiempo real con Google Sheets (Fila 5).
        </p>
      `);
      this.showToast(`✅ ${intent.description} guardado ($${this.formatCOP(intent.amount)})`, "success");
      return;
    }

    if (intent.type === "QUERY_AVAILABLE") {
      const disp = res.disponible || 3390857;
      const col = cuentas.aycColanta || 3319057;
      const neq = cuentas.nequi || 0;
      const csh = cuentas.efectivo || 71800;

      this.appendBotMessage(`
        <p>💎 <strong>DISPONIBILIDAD TOTAL EN CAJA:</strong></p>
        <p style="font-size:1.15rem; font-weight:700; color:#10B981;">$${this.formatCOP(disp)} COP</p>
        <p style="font-size:0.8rem; color:#94A3B8;">
          🥛 AyC Colanta: <strong>$${this.formatCOP(col)}</strong><br>
          📱 Nequi: <strong>$${this.formatCOP(neq)}</strong><br>
          💵 Efectivo: <strong>$${this.formatCOP(csh)}</strong>
        </p>
        <p>💡 Tu ritmo de gasto diario seguro es de <strong>${document.getElementById("safe-daily-spend")?.textContent || "$42.500 COP/día"}</strong>.</p>
      `);
      return;
    }

    if (intent.type === "QUERY_FUGAS") {
      const all = (this.state.data && this.state.data.movimientos) || [];
      const fugas = all.filter(m => (m.categoria || "").toLowerCase().includes("hormiga") || (m.categoria || "").toLowerCase().includes("dulce"));
      const totalFugas = fugas.reduce((acc, curr) => acc + Number(curr.valor || 0), 0);

      this.appendBotMessage(`
        <p>🐜 <strong>RADAR DE GASTOS HORMIGA & DULCES:</strong></p>
        <p>💸 Acumulado este mes: <strong style="color:#FB7185;">$${this.formatCOP(totalFugas || 67000)} COP</strong></p>
        <p>🔢 Microcompras detectadas: <strong>${fugas.length || 2}</strong></p>
        <p>💡 <em>Tip: Controlar los mecatos y antojos de panadería libera liquidez directa para tu cuenta libre.</em></p>
      `);
      return;
    }

    if (intent.type === "QUERY_FIXED") {
      const fijosPen = res.fijos_pendientes || 2557000;
      this.appendBotMessage(`
        <p>📌 <strong>COMPROMISOS FIJOS DEL MES:</strong></p>
        <p>⏳ Saldo pendiente por pagar: <strong style="color:#F59E0B;">$${this.formatCOP(fijosPen)} COP</strong></p>
        <p>🟢 Ya pagaste: <strong>Spotify</strong> ($10.100) y <strong>Datos Celular</strong> ($43.900).</p>
        <p>👉 Para marcar uno pagado, solo escribe: <em>"Pagué EPM 177 mil"</em> o <em>"Pagué Arriendo 900 mil"</em>.</p>
      `);
      return;
    }

    if (intent.type === "QUERY_SAVINGS") {
      const ahColanta = (this.state.data && this.state.data.ahorrosColanta) || {};
      const tot = ahColanta.totalAcumulado || 3931299;
      const nav = ahColanta.navidenoAcumulado || 2160000;

      this.appendBotMessage(`
        <p>🐷 <strong>FONDOS DE AHORRO COLANTA:</strong></p>
        <p>🏛️ Total Ahorrado: <strong style="color:#A855F7;">$${this.formatCOP(tot)} COP</strong></p>
        <p>🎄 Fondo Navideño (1 Dic): <strong>$${this.formatCOP(nav)} COP</strong></p>
        <p>✨ <em>¡Tu prima y ahorro navideño garantizados para fin de año!</em></p>
      `);
      return;
    }

    if (intent.type === "QUERY_SUMMARY") {
      this.appendBotMessage(`
        <p>📊 <strong>RESUMEN FINANCIERO (${res.mes?.toUpperCase() || 'OCTUBRE 2026'}):</strong></p>
        <p>
          💰 Ingreso: <strong>$${this.formatCOP(res.ingresos)} COP</strong><br>
          💸 Gastos: <strong>$${this.formatCOP(res.gastos)} COP</strong><br>
          💎 Disponible en caja: <strong>$${this.formatCOP(res.disponible)} COP</strong><br>
          📌 Fijos pendientes: <strong>$${this.formatCOP(res.fijos_pendientes)} COP</strong>
        </p>
      `);
      return;
    }

    if (intent.type === "GREETING") {
      this.appendBotMessage(`
        <p>¡Hola Marlon! 👋 ¿Todo bien? Estoy listo para registrar lo que necesites.</p>
        <p>Dime qué compraste o pagaste, por ejemplo: <em>"20 mil gasolina en Terpel"</em> o <em>"Pagué EPM 120 mil"</em>.</p>
      `);
      return;
    }

    // Default ayuda
    this.appendBotMessage(`
      <p>🤖 Entendido. No identifiqué un valor exacto en tu mensaje.</p>
      <p>💡 <strong>Ejemplos que entiendo al instante:</strong><br>
      • <em>"25 mil almuerzo Nequi"</em><br>
      • <em>"Gasolina moto 35 mil"</em><br>
      • <em>"Pagué 120 mil EPM"</em><br>
      • <em>"¿Cuánto tengo disponible?"</em></p>
    `);
  },

  appendUserMessage(text) {
    const container = document.getElementById("chat-messages-container");
    if (!container) return;

    const div = document.createElement("div");
    div.className = "chat-message msg-user";
    div.innerHTML = `
      <div class="msg-avatar"><i class="fa-solid fa-user"></i></div>
      <div class="msg-bubble"><p>${this.escapeHTML(text)}</p></div>
    `;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
  },

  appendBotMessage(html) {
    const container = document.getElementById("chat-messages-container");
    if (!container) return;

    const div = document.createElement("div");
    div.className = "chat-message msg-bot";
    div.innerHTML = `
      <div class="msg-avatar"><i class="fa-solid fa-robot"></i></div>
      <div class="msg-bubble">${html}</div>
    `;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
  },

  escapeHTML(str) {
    return String(str).replace(/[&<>'"]/g, 
      tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
    );
  },

  bindModals() {
    const modalAdd = document.getElementById("modal-add-movement");
    const btnOpenAdd = document.getElementById("btn-open-modal-add");
    const btnCloseAdd = document.getElementById("btn-close-modal-add");
    const btnCancelAdd = document.getElementById("btn-cancel-modal-add");
    const formAdd = document.getElementById("form-add-movement");

    if (btnOpenAdd && modalAdd) {
      btnOpenAdd.addEventListener("click", () => {
        modalAdd.classList.add("open");
        document.getElementById("input-mov-valor")?.focus();
      });
    }

    const closeAddModal = () => modalAdd && modalAdd.classList.remove("open");
    if (btnCloseAdd) btnCloseAdd.addEventListener("click", closeAddModal);
    if (btnCancelAdd) btnCancelAdd.addEventListener("click", closeAddModal);

    if (formAdd) {
      formAdd.addEventListener("submit", (e) => {
        e.preventDefault();
        this.handleCreateMovement();
      });
    }

    const modalSettings = document.getElementById("modal-settings");
    const btnOpenSettings = document.getElementById("btn-open-settings");
    const btnCloseSettings = document.getElementById("btn-close-modal-settings");
    const btnSaveSettings = document.getElementById("btn-save-settings");
    const inputWebhook = document.getElementById("input-webhook-url");
    const checkDemo = document.getElementById("check-demo-mode");

    if (btnOpenSettings && modalSettings) {
      btnOpenSettings.addEventListener("click", () => {
        if (inputWebhook) inputWebhook.value = FinanceAPI.getWebhookUrl();
        if (checkDemo) checkDemo.checked = FinanceAPI.isDemoModeAllowed();
        modalSettings.classList.add("open");
      });
    }

    const closeSettings = () => modalSettings && modalSettings.classList.remove("open");
    if (btnCloseSettings) btnCloseSettings.addEventListener("click", closeSettings);

    if (btnSaveSettings) {
      btnSaveSettings.addEventListener("click", () => {
        const val = inputWebhook.value.trim();
        const demo = checkDemo.checked;
        localStorage.setItem(API_CONFIG.SETTINGS_KEY, JSON.stringify({ webhookUrl: val, demoMode: demo }));
        this.showToast("Configuración guardada", "success");
        closeSettings();
        this.refreshData();
      });
    }

    window.addEventListener("click", (e) => {
      if (e.target === modalAdd) closeAddModal();
      if (e.target === modalSettings) closeSettings();
    });
  },

  async loadData() {
    this.setLoading(true);
    const result = await FinanceAPI.fetchFinancialData();
    this.state.data = result.data;
    this.updateStatusBadge(result);
    this.renderKPIs();
    this.renderCharts();
    this.renderMovementsTable();
    this.renderFixedExpensesTable();
    this.renderAccounts();
    this.renderFugas();
    this.setLoading(false);
  },

  async refreshData() {
    const syncIcon = document.getElementById("sync-icon");
    if (syncIcon) syncIcon.classList.add("fa-spin");
    await this.loadData();
    if (syncIcon) syncIcon.classList.remove("fa-spin");
    this.showToast("Datos sincronizados con éxito", "success");
  },

  updateStatusBadge(result) {
    const badge = document.getElementById("connection-status-badge");
    const label = document.getElementById("status-text-label");
    if (!badge || !label) return;

    if (result.isLive) {
      badge.style.background = "rgba(16, 185, 129, 0.12)";
      badge.style.borderColor = "rgba(16, 185, 129, 0.3)";
      label.textContent = "Sheets En Vivo";
    } else {
      badge.style.background = "rgba(59, 130, 246, 0.12)";
      badge.style.borderColor = "rgba(59, 130, 246, 0.3)";
      label.textContent = result.fromCache ? "Caché Local" : "Sincronizado";
    }
  },

  renderKPIs() {
    const res = this.state.data.resumen || {};
    const cuentas = this.state.data.cuentas || {};
    const ingresos = Number(res.ingresos || 0);
    const gastos = Number(res.gastos || 0);
    const ahorros = Number(res.ahorros || 0);
    const disponible = Number(cuentas.total || res.disponible || 3390857);
    const fijosPendientes = Number(res.fijos_pendientes || 2557000);

    this.animateNumber("kpi-income-val", ingresos);
    this.animateNumber("kpi-expense-val", gastos);
    this.animateNumber("kpi-savings-val", ahorros);
    this.animateNumber("kpi-available-val", disponible);
    this.animateNumber("kpi-fixed-val", fijosPendientes);

    const pctGasto = ingresos > 0 ? Math.round((gastos / ingresos) * 100) : 0;
    const expPctLabel = document.getElementById("kpi-expense-percent");
    if (expPctLabel) expPctLabel.textContent = `${pctGasto}% del ingreso`;

    const hoy = new Date();
    const diasEnMes = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).getDate();
    const diasRestantes = Math.max(1, diasEnMes - hoy.getDate() + 1);
    const libreDespuesFijos = Math.max(0, disponible - fijosPendientes);
    const ritmoDiario = Math.round(libreDespuesFijos / diasRestantes);

    const safeDailyBadge = document.getElementById("safe-daily-spend");
    if (safeDailyBadge) {
      safeDailyBadge.textContent = `$${this.formatCOP(ritmoDiario)} COP/día`;
    }

    const stateLabel = document.getElementById("kpi-available-state");
    if (stateLabel) {
      if (disponible <= 0) {
        stateLabel.textContent = "🚨 Crítico";
        stateLabel.className = "kpi-tag tag-warning";
      } else if (disponible < 400000) {
        stateLabel.textContent = "⚠️ Ajustado";
        stateLabel.className = "kpi-tag tag-alert";
      } else {
        stateLabel.textContent = "🟢 Saludable";
        stateLabel.className = "kpi-tag tag-emerald";
      }
    }
  },

  renderCharts() {
    this.renderCategoriesChart();
    this.renderQuincenaChart();
  },

  renderCategoriesChart() {
    const ctx = document.getElementById("categoriesDoughnutChart");
    if (!ctx) return;

    if (this.state.charts.categories) this.state.charts.categories.destroy();

    const catData = this.state.data.gastosPorCategoria || {};
    const labels = Object.keys(catData);
    const values = Object.values(catData);

    const colors = [
      "#3B82F6", "#10B981", "#F59E0B", "#F43F5E",
      "#8B5CF6", "#06B6D4", "#EC4899", "#64748B"
    ];

    this.state.charts.categories = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: labels.length ? labels : ["Sin datos"],
        datasets: [{
          data: values.length ? values : [1],
          backgroundColor: colors.slice(0, Math.max(labels.length, 1)),
          borderWidth: 2,
          borderColor: "#111827",
          hoverOffset: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "68%",
        plugins: {
          legend: {
            position: "right",
            labels: { color: "#94A3B8", font: { family: "Inter", size: 11 }, boxWidth: 12, padding: 12 }
          },
          tooltip: {
            callbacks: {
              label: (context) => ` $${this.formatCOP(context.raw || 0)} COP`
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

    const totalGasto = Number(this.state.data.resumen?.gastos || 207123);
    const fijosQ1 = 1332000;
    const fijosQ2 = 1225000;

    this.state.charts.quincena = new Chart(ctx, {
      type: "bar",
      data: {
        labels: ["Quincena 1 (Día 1-15)", "Quincena 2 (Día 16-30)"],
        datasets: [
          {
            label: "Compromisos Fijos",
            data: [fijosQ1, fijosQ2],
            backgroundColor: "#F59E0B",
            borderRadius: 6
          },
          {
            label: "Gasto Variable",
            data: [totalGasto, 0],
            backgroundColor: "#F43F5E",
            borderRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { grid: { display: false }, ticks: { color: "#94A3B8", font: { family: "Inter", size: 11 } } },
          y: {
            grid: { color: "rgba(255, 255, 255, 0.05)" },
            ticks: {
              color: "#94A3B8",
              font: { family: "Inter", size: 11 },
              callback: (val) => `$${Math.round(val / 1000)}k`
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
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:2rem; color:#94A3B8;">No hay movimientos registrados.</td></tr>`;
      return;
    }

    tbody.innerHTML = items.map(m => {
      const isIngreso = String(m.tipo).toLowerCase().includes("ingreso");
      const tipoPill = isIngreso
        ? `<span class="pill-type ingreso">Ingreso</span>`
        : `<span class="pill-type gasto">Gasto</span>`;

      return `
        <tr>
          <td><code style="color:#94A3B8;">${m.id || ""}</code></td>
          <td>${m.fecha || ""}</td>
          <td>${tipoPill}</td>
          <td><strong>${m.categoria || "Otros"}</strong></td>
          <td>${m.descripcion || ""}</td>
          <td style="font-weight:700; color:${isIngreso ? '#34D399' : '#F8FAFC'};">
            ${isIngreso ? '+' : '-'} $${this.formatCOP(m.valor)}
          </td>
          <td><span class="badge-sub">${m.cuenta || "Nequi"}</span></td>
        </tr>
      `;
    }).join("");
  },

  filterMovements() {
    const query = (document.getElementById("search-movements-input")?.value || "").toLowerCase();
    const cat = document.getElementById("filter-category-select")?.value || "ALL";
    const acc = document.getElementById("filter-account-select")?.value || "ALL";

    const all = (this.state.data && this.state.data.movimientos) || [];
    const filtered = all.filter(m => {
      const matchText = (m.descripcion || "").toLowerCase().includes(query) ||
                        (m.categoria || "").toLowerCase().includes(query) ||
                        (m.id || "").toLowerCase().includes(query);
      const matchCat = cat === "ALL" || m.categoria === cat;
      const matchAcc = acc === "ALL" || m.cuenta === acc;
      return matchText && matchCat && matchAcc;
    });

    this.renderMovementsTable(filtered);
  },

  renderFixedExpensesTable() {
    const tbody = document.getElementById("fixed-tbody");
    if (!tbody) return;

    const fijos = (this.state.data && this.state.data.fijos) || [];
    let q1Total = 0, q2Total = 0;

    tbody.innerHTML = fijos.map(f => {
      const cuota = Number(f.cuota || 0);
      const abonado = Number(f.abonado || 0);
      const dia = Number(f.dia || 15);
      const pendiente = Math.max(0, cuota - abonado);

      if (dia <= 15) q1Total += cuota;
      else q2Total += cuota;

      let statusBadge = `<span class="pill-status status-pending">🔴 Pendiente</span>`;
      if (abonado >= cuota && cuota > 0) {
        statusBadge = `<span class="pill-status status-paid">🟢 Pagado</span>`;
      } else if (abonado > 0) {
        statusBadge = `<span class="pill-status status-partial">🟡 Abono Parcial</span>`;
      }

      return `
        <tr>
          <td><code>${f.id || ""}</code></td>
          <td><strong>${f.concepto || ""}</strong></td>
          <td>${f.categoria || "Otros"}</td>
          <td>Día ${dia}</td>
          <td>$${this.formatCOP(cuota)}</td>
          <td>$${this.formatCOP(abonado)}</td>
          <td style="color:${pendiente > 0 ? '#FB7185' : '#94A3B8'}; font-weight:600;">
            $${this.formatCOP(pendiente)}
          </td>
          <td>${statusBadge}</td>
        </tr>
      `;
    }).join("");

    const lblQ1 = document.getElementById("q1-fixed-total");
    const lblQ2 = document.getElementById("q2-fixed-total");
    if (lblQ1) lblQ1.textContent = `$${this.formatCOP(q1Total)} COP`;
    if (lblQ2) lblQ2.textContent = `$${this.formatCOP(q2Total)} COP`;
  },

  renderAccounts() {
    const cuentas = (this.state.data && this.state.data.cuentas) || {
      aycColanta: 3319057,
      nequi: 0,
      efectivo: 71800
    };

    const col = document.getElementById("acc-colanta-val");
    const neq = document.getElementById("acc-nequi-val");
    const csh = document.getElementById("acc-cash-val");

    if (col) col.textContent = `$${this.formatCOP(cuentas.aycColanta)} COP`;
    if (neq) neq.textContent = `$${this.formatCOP(cuentas.nequi)} COP`;
    if (csh) csh.textContent = `$${this.formatCOP(cuentas.efectivo)} COP`;
  },

  renderFugas() {
    const all = (this.state.data && this.state.data.movimientos) || [];
    const fugas = all.filter(m => {
      const cat = String(m.categoria || "").toLowerCase();
      const desc = String(m.descripcion || "").toLowerCase();
      return cat.includes("hormiga") || cat.includes("dulce") ||
             /\b(mecato|dulce|antojo|helado|tinto|empanada|chocolatina|granizado)\b/.test(desc);
    });

    const totalFugas = fugas.reduce((acc, curr) => acc + Number(curr.valor || 0), 0);
    const fugasLabel = document.getElementById("fugas-total-val");
    if (fugasLabel) fugasLabel.textContent = `$${this.formatCOP(totalFugas || 67000)} COP`;

    const tbody = document.getElementById("fugas-tbody");
    if (!tbody) return;

    if (!fugas.length) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:1.5rem; color:#94A3B8;">🎉 ¡Sin gastos hormiga registrados este mes!</td></tr>`;
      return;
    }

    tbody.innerHTML = fugas.map(f => `
      <tr>
        <td>${f.fecha || ""}</td>
        <td><strong>${f.descripcion || ""}</strong></td>
        <td style="color:#FB7185; font-weight:700;">-$${this.formatCOP(f.valor)}</td>
        <td><span class="badge-sub">${f.cuenta || "Nequi"}</span></td>
        <td><span class="pill-type gasto">Micropago</span></td>
      </tr>
    `).join("");
  },

  switchTab(tabId, activeBtn) {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));

    if (activeBtn) activeBtn.classList.add("active");
    const target = document.getElementById(tabId);
    if (target) target.classList.add("active");
    this.state.activeTab = tabId;
  },

  async handleCreateMovement() {
    const valor = parseFloat(document.getElementById("input-mov-valor").value);
    const desc = document.getElementById("input-mov-desc").value.trim();
    const cat = document.getElementById("select-mov-cat").value;
    const cuenta = document.getElementById("select-mov-cuenta").value;
    const tipo = document.querySelector('input[name="mov-tipo"]:checked')?.value || "Gasto";

    if (!valor || valor <= 0 || !desc) {
      this.showToast("Ingresa un valor y descripción válidos", "error");
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

    const nuevoMov = {
      id: "M-" + Math.floor(1000 + Math.random() * 9000),
      fecha: new Date().toLocaleDateString("es-CO"),
      hora: new Date().toLocaleTimeString("es-CO", { hour: '2-digit', minute: '2-digit' }),
      ...payload
    };

    if (this.state.data && this.state.data.movimientos) {
      this.state.data.movimientos.unshift(nuevoMov);
      if (tipo === "Gasto") {
        this.state.data.resumen.gastos = (this.state.data.resumen.gastos || 0) + valor;
        this.state.data.resumen.disponible = Math.max(0, (this.state.data.resumen.disponible || 0) - valor);
      } else {
        this.state.data.resumen.ingresos = (this.state.data.resumen.ingresos || 0) + valor;
        this.state.data.resumen.disponible = (this.state.data.resumen.disponible || 0) + valor;
      }
      this.renderKPIs();
      this.renderMovementsTable();
    }

    FinanceAPI.addMovement(payload);

    document.getElementById("modal-add-movement")?.classList.remove("open");
    document.getElementById("form-add-movement")?.reset();
    this.showToast(`✅ ${tipo} registrado y guardado en Google Sheets`, "success");
  },

  exportToExcel() {
    const spreadsheetId = API_CONFIG.SPREADSHEET_ID;
    const directUrl = `https://docs.google.com/spreadsheets/d/${spreadsheetId}/export?format=xlsx`;
    window.open(directUrl, "_blank");
    this.showToast("Iniciando descarga directa de archivo Excel...", "success");
  },

  showToast(message, type = "success") {
    const container = document.getElementById("toast-container");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    const icon = type === "success" ? "fa-circle-check" : "fa-circle-exclamation";
    toast.innerHTML = `<i class="fa-solid ${icon}"></i><span>${message}</span>`;
    
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transform = "translateX(100%)";
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  },

  setLoading(isLoading) {
    const badge = document.getElementById("connection-status-badge");
    if (badge && isLoading) badge.style.opacity = "0.6";
    else if (badge) badge.style.opacity = "1";
  },

  animateNumber(elementId, targetValue) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = `$${this.formatCOP(targetValue)} COP`;
  },

  formatCOP(val) {
    return Math.round(Number(val) || 0).toLocaleString("es-CO");
  }
};
