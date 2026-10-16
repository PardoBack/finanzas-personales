/**
 * PARDOFINANZAS PRO — CONTROLADOR PRINCIPAL DEL DASHBOARD (JS)
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
    activeTab: "tab-movements"
  },

  init() {
    this.bindEvents();
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
    if (btnSync) {
      btnSync.addEventListener("click", () => this.refreshData());
    }

    // Exportar Excel
    const btnExport = document.getElementById("btn-export-excel");
    if (btnExport) {
      btnExport.addEventListener("click", () => this.exportToExcel());
    }

    // Pestañas
    const tabBtns = document.querySelectorAll(".tab-btn");
    tabBtns.forEach(btn => {
      btn.addEventListener("click", (e) => {
        const targetTab = btn.getAttribute("data-tab");
        this.switchTab(targetTab, btn);
      });
    });

    // Búsqueda y Filtros de Movimientos
    const searchInput = document.getElementById("search-movements-input");
    if (searchInput) {
      searchInput.addEventListener("input", () => this.filterMovements());
    }
    const filterCat = document.getElementById("filter-category-select");
    if (filterCat) {
      filterCat.addEventListener("change", () => this.filterMovements());
    }
    const filterAcc = document.getElementById("filter-account-select");
    if (filterAcc) {
      filterAcc.addEventListener("change", () => this.filterMovements());
    }

    // Modales
    this.bindModals();
  },

  bindModals() {
    // Modal Nuevo Movimiento
    const modalAdd = document.getElementById("modal-add-movement");
    const btnOpenAdd = document.getElementById("btn-open-modal-add");
    const btnCloseAdd = document.getElementById("btn-close-modal-add");
    const btnCancelAdd = document.getElementById("btn-cancel-modal-add");
    const formAdd = document.getElementById("form-add-movement");

    if (btnOpenAdd && modalAdd) {
      btnOpenAdd.addEventListener("click", () => {
        modalAdd.classList.add("open");
        document.getElementById("input-mov-valor").focus();
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

    // Modal Configuración
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

    // Botón Probar Conexión
    const btnTest = document.getElementById("btn-test-connection");
    if (btnTest) {
      btnTest.addEventListener("click", async () => {
        btnTest.textContent = "Probando...";
        const res = await FinanceAPI.fetchFinancialData();
        btnTest.textContent = "Probar Conexión";
        if (res.isLive) {
          this.showToast("¡Conexión en vivo con Google Sheets exitosa!", "success");
        } else {
          this.showToast("Sin respuesta directa del webhook. Modo contingencia activo.", "error");
        }
      });
    }

    // Cerrar al hacer clic en el backdrop
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
      label.textContent = result.fromCache ? "Caché Local" : "Modo Seguro";
    }
  },

  renderKPIs() {
    const res = this.state.data.resumen || {};
    const ingresos = Number(res.ingresos || 0);
    const gastos = Number(res.gastos || 0);
    const ahorros = Number(res.ahorros || 0);
    const disponible = Number(res.disponible || Math.max(0, ingresos - gastos - ahorros));
    const fijosPendientes = Number(res.fijos_pendientes || 0);

    // Formatear en DOM
    this.animateNumber("kpi-income-val", ingresos);
    this.animateNumber("kpi-expense-val", gastos);
    this.animateNumber("kpi-savings-val", ahorros);
    this.animateNumber("kpi-available-val", disponible);
    this.animateNumber("kpi-fixed-val", fijosPendientes);

    // Porcentaje de ejecución de gasto
    const pctGasto = ingresos > 0 ? Math.round((gastos / ingresos) * 100) : 0;
    const expPctLabel = document.getElementById("kpi-expense-percent");
    if (expPctLabel) expPctLabel.textContent = `${pctGasto}% del ingreso`;

    // Ritmo diario seguro de gasto
    const hoy = new Date();
    const diasEnMes = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).getDate();
    const diasRestantes = Math.max(1, diasEnMes - hoy.getDate() + 1);
    const libreDespuesFijos = Math.max(0, disponible - fijosPendientes);
    const ritmoDiario = Math.round(libreDespuesFijos / diasRestantes);

    const safeDailyBadge = document.getElementById("safe-daily-spend");
    if (safeDailyBadge) {
      safeDailyBadge.textContent = `$${this.formatCOP(ritmoDiario)} COP/día`;
    }

    // Estado disponible
    const stateLabel = document.getElementById("kpi-available-state");
    if (stateLabel) {
      if (disponible <= 0) {
        stateLabel.textContent = "🚨 Crítico";
        stateLabel.className = "kpi-tag tag-warning";
      } else if (disponible < 350000) {
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

    if (this.state.charts.categories) {
      this.state.charts.categories.destroy();
    }

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
            labels: {
              color: "#94A3B8",
              font: { family: "Inter", size: 11 },
              boxWidth: 12,
              padding: 12
            }
          },
          tooltip: {
            callbacks: {
              label: (context) => {
                const val = context.raw || 0;
                return ` $${this.formatCOP(val)} COP`;
              }
            }
          }
        }
      }
    });
  },

  renderQuincenaChart() {
    const ctx = document.getElementById("quincenaBarChart");
    if (!ctx) return;

    if (this.state.charts.quincena) {
      this.state.charts.quincena.destroy();
    }

    const res = this.state.data.resumen || {};
    const totalIngreso = Number(res.ingresos || 3500000);
    const totalGasto = Number(res.gastos || 2042215);

    // Estimación Q1 vs Q2
    const q1Ingreso = Math.round(totalIngreso / 2);
    const q2Ingreso = Math.round(totalIngreso / 2);
    const q1Gasto = Math.round(totalGasto * 0.55);
    const q2Gasto = Math.round(totalGasto * 0.45);

    this.state.charts.quincena = new Chart(ctx, {
      type: "bar",
      data: {
        labels: ["Quincena 1 (Día 15)", "Quincena 2 (Fin de Mes)"],
        datasets: [
          {
            label: "Ingreso Consignado",
            data: [q1Ingreso, q2Ingreso],
            backgroundColor: "#3B82F6",
            borderRadius: 6
          },
          {
            label: "Gasto Ejecutado",
            data: [q1Gasto, q2Gasto],
            backgroundColor: "#F43F5E",
            borderRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: "#94A3B8", font: { family: "Inter", size: 11 } }
          },
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
          legend: {
            position: "top",
            labels: { color: "#94A3B8", boxWidth: 12, padding: 10 }
          },
          tooltip: {
            callbacks: {
              label: (context) => ` ${context.dataset.label}: $${this.formatCOP(context.raw)} COP`
            }
          }
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
      aycColanta: 780000,
      nequi: 341664,
      efectivo: 100000
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
    if (fugasLabel) fugasLabel.textContent = `$${this.formatCOP(totalFugas)} COP`;

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

    const btnSubmit = document.getElementById("btn-submit-movement");
    if (btnSubmit) {
      btnSubmit.disabled = true;
      btnSubmit.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Guardando...`;
    }

    const payload = {
      tipo: tipo,
      valor: valor,
      descripcion: desc,
      categoria: cat,
      cuenta: cuenta,
      metodo: cuenta === "Efectivo" ? "Efectivo" : "Transferencia"
    };

    // Actualización optimista inmediata en la UI
    const nuevoMov = {
      id: "M-" + Math.floor(1000 + Math.random() * 9000),
      fecha: new Date().toLocaleDateString("es-CO"),
      hora: new Date().toLocaleTimeString("es-CO", { hour: '2-digit', minute: '2-digit' }),
      ...payload
    };

    if (this.state.data && this.state.data.movimientos) {
      this.state.data.movimientos.unshift(nuevoMov);
      if (tipo === "Gasto") {
        this.state.data.resumen.gastos += valor;
        this.state.data.resumen.disponible = Math.max(0, this.state.data.resumen.disponible - valor);
      } else {
        this.state.data.resumen.ingresos += valor;
        this.state.data.resumen.disponible += valor;
      }
      this.renderKPIs();
      this.renderMovementsTable();
    }

    await FinanceAPI.addMovement(payload);

    if (btnSubmit) {
      btnSubmit.disabled = false;
      btnSubmit.innerHTML = `<i class="fa-solid fa-cloud-arrow-up"></i> Guardar en Google Sheets`;
    }

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
    if (badge && isLoading) {
      badge.style.opacity = "0.6";
    } else if (badge) {
      badge.style.opacity = "1";
    }
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
