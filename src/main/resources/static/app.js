const state = {
    token: localStorage.getItem("energyOpsToken") || "",
    user: null,
    dashboard: null,
    sites: [],
    assets: [],
    alerts: [],
    workOrders: [],
    maintenance: [],
    auditLogs: [],
    selectedAssetId: null
};

const loginOverlay = document.querySelector("#login-overlay");
const loginForm = document.querySelector("#login-form");
const logoutButton = document.querySelector("#logout-button");
const toast = document.querySelector("#toast");
const pageTitle = document.querySelector("#page-title");
const sessionName = document.querySelector("#session-name");
const sessionRole = document.querySelector("#session-role");

const formatDateTime = (value) => {
    if (!value) return "Not available";
    return new Intl.DateTimeFormat("en-CA", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "America/Edmonton"
    }).format(new Date(value));
};

const formatDate = (value) => {
    if (!value) return "Not set";
    return new Intl.DateTimeFormat("en-CA", { dateStyle: "medium" }).format(new Date(value));
};

const formatNumber = (value, digits = 1) =>
    new Intl.NumberFormat("en-CA", { maximumFractionDigits: digits, minimumFractionDigits: digits }).format(value ?? 0);

const statusPill = (value) => `<span class="status-pill ${String(value).toLowerCase()}">${value}</span>`;

const showToast = (message, isError = false) => {
    toast.textContent = message;
    toast.classList.remove("hidden");
    toast.style.borderColor = isError ? "rgba(231, 111, 81, 0.32)" : "rgba(244, 162, 97, 0.24)";
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.add("hidden"), 3200);
};

async function apiFetch(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (!(options.body instanceof FormData) && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    if (state.token) {
        headers.set("Authorization", `Bearer ${state.token}`);
    }

    const response = await fetch(path, { ...options, headers });
    if (!response.ok) {
        let message = `Request failed with status ${response.status}`;
        try {
            const body = await response.json();
            message = body.message || message;
        } catch (error) {
            const text = await response.text();
            if (text) message = text;
        }
        throw new Error(message);
    }

    if (response.headers.get("content-type")?.includes("text/csv")) {
        return response.text();
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function applySession() {
    sessionName.textContent = state.user ? state.user.fullName : "Not signed in";
    sessionRole.textContent = state.user ? `${state.user.role} • ${state.user.homeProvince}` : "Awaiting login";
}

async function login(username, password) {
    const auth = await apiFetch("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password })
    });
    state.token = auth.token;
    state.user = auth.user;
    localStorage.setItem("energyOpsToken", state.token);
    applySession();
    loginOverlay.classList.add("hidden");
    await refreshData();
    showToast(`Welcome back, ${auth.user.fullName}.`);
}

function clearSession() {
    state.token = "";
    state.user = null;
    localStorage.removeItem("energyOpsToken");
    applySession();
    loginOverlay.classList.remove("hidden");
}

async function restoreSession() {
    if (!state.token) {
        loginOverlay.classList.remove("hidden");
        return;
    }

    try {
        state.user = await apiFetch("/api/auth/me");
        applySession();
        loginOverlay.classList.add("hidden");
        await refreshData();
    } catch (error) {
        clearSession();
    }
}

async function refreshData() {
    const [dashboard, sites, assets, alerts, workOrders, maintenance, auditLogs] = await Promise.all([
        apiFetch("/api/dashboard/overview"),
        apiFetch("/api/sites"),
        apiFetch("/api/assets"),
        apiFetch("/api/alerts"),
        apiFetch("/api/work-orders"),
        apiFetch("/api/maintenance-records"),
        apiFetch("/api/audit-logs").catch(() => [])
    ]);

    state.dashboard = dashboard;
    state.sites = sites;
    state.assets = assets;
    state.alerts = alerts;
    state.workOrders = workOrders;
    state.maintenance = maintenance;
    state.auditLogs = auditLogs;

    if (!state.selectedAssetId && assets.length) {
        state.selectedAssetId = assets[0].id;
    }

    renderAll();
    if (state.selectedAssetId) {
        await loadAssetDetail(state.selectedAssetId);
    }
}

function renderAll() {
    renderMetrics();
    renderSiteRisk();
    renderAssetRisk();
    renderRecentQueues();
    renderSites();
    renderAssets();
    renderAlerts();
    renderWorkOrders();
    renderMaintenance();
    renderAudit();
    renderTrend();
    populateFormOptions();
}

function renderMetrics() {
    document.querySelector("#metric-grid").innerHTML = state.dashboard.metrics.map((metric) => `
        <article class="metric-card ${metric.tone}">
            <p class="label">${metric.label}</p>
            <strong>${metric.value}</strong>
            <p class="muted">${metric.detail}</p>
        </article>
    `).join("");
}

function renderSiteRisk() {
    document.querySelector("#site-risk-list").innerHTML = state.dashboard.siteRisk.map((site) => `
        <article class="risk-card">
            <div class="risk-card-header">
                <div>
                    <strong>${site.siteName}</strong>
                    <p class="muted">${site.province} • ${site.siteId}</p>
                </div>
                ${statusPill(`${site.openAlerts} open alerts`)}
            </div>
            <p class="muted">Avg health ${formatNumber(site.averageHealthScore)} | Peak risk ${formatNumber(site.peakFailureRisk * 100)}%</p>
            <div class="risk-bar"><span style="width:${Math.max(8, site.peakFailureRisk * 100)}%"></span></div>
        </article>
    `).join("");
}

function renderAssetRisk() {
    document.querySelector("#asset-risk-table").innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Asset</th>
                    <th>Site</th>
                    <th>Health</th>
                    <th>Risk</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                ${state.dashboard.assetRisk.map((asset) => `
                    <tr>
                        <td><button class="table-action-button" type="button" data-asset-select="${asset.assetId}">${asset.assetName}</button><br><span class="muted">${asset.assetType}</span></td>
                        <td>${asset.siteName}</td>
                        <td>${formatNumber(asset.latestHealthScore)}</td>
                        <td>${formatNumber(asset.latestFailureRisk * 100)}%</td>
                        <td>${statusPill(asset.status)}</td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;
}

function renderRecentQueues() {
    document.querySelector("#recent-alerts-list").innerHTML = state.dashboard.recentAlerts.map((alert) => `
        <div class="mini-row">
            <div class="row-between">
                <strong>${alert.alertCode}</strong>
                ${statusPill(alert.priority)}
            </div>
            <p>${alert.assetName} • ${alert.alertType}</p>
            <p class="muted">${formatDateTime(alert.createdAt)}</p>
        </div>
    `).join("");

    document.querySelector("#recent-workorders-list").innerHTML = state.dashboard.openWorkOrders.map((workOrder) => `
        <div class="mini-row">
            <div class="row-between">
                <strong>${workOrder.workOrderCode}</strong>
                ${statusPill(workOrder.status)}
            </div>
            <p>${workOrder.title}</p>
            <p class="muted">${workOrder.assignedTo || "Unassigned"} • Due ${formatDate(workOrder.dueDate)}</p>
        </div>
    `).join("");
}

function renderSites() {
    document.querySelector("#site-table").innerHTML = buildTable(
        ["Site", "City", "Province", "Timezone", "Status"],
        state.sites.map((site) => `
            <tr>
                <td><strong>${site.name}</strong><br><span class="muted">${site.id}</span></td>
                <td>${site.city}</td>
                <td>${site.province}</td>
                <td>${site.timezone}</td>
                <td>${statusPill(site.status)}</td>
            </tr>
        `)
    );
}

function renderAssets() {
    document.querySelector("#asset-table").innerHTML = buildTable(
        ["Asset", "Site", "Health", "Risk", "Latest Reading"],
        state.assets.map((asset) => `
            <tr>
                <td><button class="table-action-button" type="button" data-asset-select="${asset.id}">${asset.name}</button><br><span class="muted">${asset.assetType} • ${asset.id}</span></td>
                <td>${asset.siteName}</td>
                <td>${formatNumber(asset.latestHealthScore)}</td>
                <td>${formatNumber(asset.latestFailureRisk * 100)}%</td>
                <td>${formatDateTime(asset.latestReadingAt)}</td>
            </tr>
        `)
    );
}

async function loadAssetDetail(assetId) {
    state.selectedAssetId = assetId;
    const detail = await apiFetch(`/api/assets/${assetId}`);
    document.querySelector("#asset-detail-title").textContent = `${detail.asset.name} • ${detail.asset.assetType}`;
    document.querySelector("#asset-detail-body").classList.remove("empty-state");
    document.querySelector("#asset-detail-body").innerHTML = `
        <div class="detail-card">
            <div class="row-between">
                <div>
                    <strong>${detail.asset.siteName}</strong>
                    <p class="muted">${detail.asset.id} • ${detail.asset.status}</p>
                </div>
                ${statusPill(`${formatNumber(detail.asset.latestFailureRisk * 100)}% risk`)}
            </div>
            <p class="muted">Health ${formatNumber(detail.asset.latestHealthScore)} • Last reading ${formatDateTime(detail.asset.latestReadingAt)}</p>
        </div>
        <div class="detail-card">
            <p class="label">Recent Telemetry</p>
            ${buildTable(
                ["Time", "Temp", "Pressure", "Risk", "Alert"],
                detail.recentReadings.slice(0, 8).map((reading) => `
                    <tr>
                        <td>${formatDateTime(reading.timestamp)}</td>
                        <td>${formatNumber(reading.temperatureC)} deg C</td>
                        <td>${formatNumber(reading.pressureKpa)} kPa</td>
                        <td>${formatNumber(reading.predictedFailureRisk * 100)}%</td>
                        <td>${reading.alertType ? statusPill(reading.alertType) : "<span class='muted'>None</span>"}</td>
                    </tr>
                `)
            )}
        </div>
        <div class="detail-card">
            <p class="label">Active Alerts</p>
            ${detail.activeAlerts.length ? detail.activeAlerts.map((alert) => `<p>${alert.alertCode} • ${alert.alertType} • ${alert.status}</p>`).join("") : "<p class='muted'>No active alerts.</p>"}
        </div>
    `;
}

function renderAlerts() {
    document.querySelector("#alert-table").innerHTML = buildTable(
        ["Alert", "Asset", "Priority", "Status", "Assigned", "Actions"],
        state.alerts.map((alert) => `
            <tr>
                <td><strong>${alert.alertCode}</strong><br><span class="muted">${alert.alertType}</span><br><span class="muted">${formatDateTime(alert.createdAt)}</span></td>
                <td>${alert.assetName}<br><span class="muted">${alert.siteName}</span></td>
                <td>${statusPill(alert.priority)}</td>
                <td>${statusPill(alert.status)}</td>
                <td>${alert.assignedTo || "<span class='muted'>Unassigned</span>"}</td>
                <td>
                    <div class="double-stack">
                        <button class="tiny-button" type="button" data-alert-action="ACKNOWLEDGE" data-alert-id="${alert.id}">Acknowledge</button>
                        <button class="tiny-button" type="button" data-alert-action="RESOLVE" data-alert-id="${alert.id}">Resolve</button>
                        <button class="tiny-button" type="button" data-alert-action="ASSIGN" data-alert-id="${alert.id}">Assign to morgan.tech</button>
                    </div>
                </td>
            </tr>
        `)
    );
}

function renderWorkOrders() {
    document.querySelector("#workorder-table").innerHTML = buildTable(
        ["Work Order", "Asset", "Priority", "Status", "Assigned", "Due"],
        state.workOrders.map((workOrder) => `
            <tr>
                <td><strong>${workOrder.workOrderCode}</strong><br><span class="muted">${workOrder.title}</span></td>
                <td>${workOrder.assetName}<br><span class="muted">${workOrder.siteName}</span></td>
                <td>${statusPill(workOrder.priority)}</td>
                <td>
                    ${statusPill(workOrder.status)}
                    <div class="double-stack">
                        <button class="tiny-button" type="button" data-wo-status="IN_PROGRESS" data-wo-id="${workOrder.id}">Mark In Progress</button>
                        <button class="tiny-button" type="button" data-wo-status="COMPLETED" data-wo-id="${workOrder.id}">Complete</button>
                    </div>
                </td>
                <td>${workOrder.assignedTo || "<span class='muted'>Unassigned</span>"}</td>
                <td>${formatDate(workOrder.dueDate)}</td>
            </tr>
        `)
    );
}

function renderMaintenance() {
    document.querySelector("#maintenance-table").innerHTML = buildTable(
        ["Record", "Work Order", "Root Cause", "Downtime", "Created"],
        state.maintenance.map((record) => `
            <tr>
                <td><strong>${record.id}</strong></td>
                <td>${record.workOrderCode}</td>
                <td>${record.rootCause}</td>
                <td>${record.downtimeMinutes ?? "-"} min</td>
                <td>${formatDateTime(record.createdAt)}</td>
            </tr>
        `)
    );
}

function renderAudit() {
    document.querySelector("#audit-table").innerHTML = buildTable(
        ["Time", "Actor", "Action", "Entity", "Details"],
        state.auditLogs.map((log) => `
            <tr>
                <td>${formatDateTime(log.createdAt)}</td>
                <td>${log.actor}</td>
                <td>${log.action}</td>
                <td>${log.entityType} / ${log.entityId}</td>
                <td>${log.details || "-"}</td>
            </tr>
        `)
    );
}

function populateFormOptions() {
    const assetOptions = state.assets.map((asset) => `<option value="${asset.id}">${asset.name} (${asset.id})</option>`).join("");
    const siteOptions = state.sites.map((site) => `<option value="${site.id}">${site.name}</option>`).join("");
    const alertOptions = state.alerts
        .filter((alert) => alert.status !== "RESOLVED")
        .map((alert) => `<option value="${alert.id}">${alert.alertCode} • ${alert.assetName} • ${alert.alertType}</option>`)
        .join("");
    const workOrderOptions = state.workOrders.map((workOrder) => `<option value="${workOrder.id}">${workOrder.workOrderCode} • ${workOrder.title}</option>`).join("");

    const readingForm = document.querySelector("#reading-form");
    readingForm.assetId.innerHTML = assetOptions;
    readingForm.siteId.innerHTML = siteOptions;
    syncReadingSite();
    if (!readingForm.timestamp.value) {
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        readingForm.timestamp.value = now.toISOString().slice(0, 16);
    }

    const workOrderForm = document.querySelector("#workorder-form");
    workOrderForm.alertId.innerHTML = `<option value="">No linked alert</option>${alertOptions}`;
    workOrderForm.assetId.innerHTML = `<option value="">Use alert asset</option>${assetOptions}`;
    document.querySelector("#maintenance-form").workOrderId.innerHTML = workOrderOptions;
}

function renderTrend() {
    const canvas = document.querySelector("#trend-canvas");
    const context = canvas.getContext("2d");
    const series = state.dashboard.trend || [];
    context.clearRect(0, 0, canvas.width, canvas.height);
    if (!series.length) return;

    const padding = { top: 30, right: 32, bottom: 48, left: 54 };
    const width = canvas.width - padding.left - padding.right;
    const height = canvas.height - padding.top - padding.bottom;

    context.strokeStyle = "rgba(255,255,255,0.08)";
    context.lineWidth = 1;
    for (let step = 0; step <= 4; step += 1) {
        const y = padding.top + (height / 4) * step;
        context.beginPath();
        context.moveTo(padding.left, y);
        context.lineTo(canvas.width - padding.right, y);
        context.stroke();
    }

    drawLine(context, series.map((point) => point.averageHealthScore), width, height, padding, "#8ecae6");
    drawLine(context, series.map((point) => point.averageFailureRisk * 100), width, height, padding, "#f4a261");

    context.fillStyle = "rgba(255,255,255,0.75)";
    context.font = "12px Segoe UI";
    series.forEach((point, index) => {
        const x = padding.left + (width / Math.max(1, series.length - 1)) * index;
        context.fillText(point.label.replace("Mar ", ""), x - 18, canvas.height - 18);
    });
}

function drawLine(context, values, width, height, padding, colour) {
    const max = Math.max(...values, 1);
    const min = Math.min(...values, 0);
    context.beginPath();
    context.strokeStyle = colour;
    context.lineWidth = 3;
    values.forEach((value, index) => {
        const x = padding.left + (width / Math.max(1, values.length - 1)) * index;
        const y = padding.top + height - ((value - min) / Math.max(1, max - min)) * height;
        if (index === 0) {
            context.moveTo(x, y);
        } else {
            context.lineTo(x, y);
        }
    });
    context.stroke();
}

function syncReadingSite() {
    const readingForm = document.querySelector("#reading-form");
    const asset = state.assets.find((item) => item.id === readingForm.assetId.value);
    if (asset) {
        readingForm.siteId.value = asset.siteId;
    }
}

function buildTable(headers, rows) {
    return `
        <table>
            <thead>
                <tr>${headers.map((header) => `<th>${header}</th>`).join("")}</tr>
            </thead>
            <tbody>${rows.join("")}</tbody>
        </table>
    `;
}

document.querySelectorAll(".nav-link").forEach((button) => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".nav-link").forEach((link) => link.classList.remove("active"));
        document.querySelectorAll(".panel-section").forEach((section) => section.classList.remove("active"));
        button.classList.add("active");
        document.querySelector(`[data-panel="${button.dataset.section}"]`).classList.add("active");
        pageTitle.textContent = button.textContent;
    });
});

document.addEventListener("click", async (event) => {
    const demoUser = event.target.closest(".demo-user");
    const assetButton = event.target.closest("[data-asset-select]");
    const alertAction = event.target.closest("[data-alert-action]");
    const workOrderAction = event.target.closest("[data-wo-status]");

    if (demoUser) {
        loginForm.username.value = demoUser.dataset.username;
        loginForm.password.value = demoUser.dataset.password;
    }

    if (assetButton) {
        await loadAssetDetail(assetButton.dataset.assetSelect);
        document.querySelector('[data-section="assets"]').click();
    }

    if (alertAction) {
        try {
            await apiFetch(`/api/alerts/${alertAction.dataset.alertId}`, {
                method: "PATCH",
                body: JSON.stringify({
                    action: alertAction.dataset.alertAction,
                    assignedTo: alertAction.dataset.alertAction === "ASSIGN" ? "morgan.tech" : null,
                    notes: "Updated from the Canadian operations dashboard."
                })
            });
            await refreshData();
            showToast("Alert updated.");
        } catch (error) {
            showToast(error.message, true);
        }
    }

    if (workOrderAction) {
        try {
            await apiFetch(`/api/work-orders/${workOrderAction.dataset.woId}`, {
                method: "PATCH",
                body: JSON.stringify({ status: workOrderAction.dataset.woStatus })
            });
            await refreshData();
            showToast("Work order updated.");
        } catch (error) {
            showToast(error.message, true);
        }
    }
});

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        await login(loginForm.username.value.trim(), loginForm.password.value);
    } catch (error) {
        showToast(error.message, true);
    }
});

logoutButton.addEventListener("click", () => {
    clearSession();
    showToast("Signed out.");
});

document.querySelector("#reading-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const selectedAssetId = form.assetId.value;
    try {
        await apiFetch("/api/sensor-readings", {
            method: "POST",
            body: JSON.stringify({
                assetId: selectedAssetId,
                siteId: form.siteId.value,
                timestamp: new Date(form.timestamp.value).toISOString(),
                temperatureC: Number(form.temperatureC.value),
                pressureKpa: Number(form.pressureKpa.value),
                vibrationMmS: Number(form.vibrationMmS.value),
                currentA: Number(form.currentA.value),
                flowRateM3H: Number(form.flowRateM3H.value)
            })
        });
        form.reset();
        await refreshData();
        await loadAssetDetail(selectedAssetId || state.selectedAssetId);
        populateFormOptions();
        showToast("Manual reading ingested.");
    } catch (error) {
        showToast(error.message, true);
    }
});

document.querySelector("#reading-form").assetId.addEventListener("change", syncReadingSite);

document.querySelector("#workorder-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        await apiFetch("/api/work-orders", {
            method: "POST",
            body: JSON.stringify({
                alertId: form.alertId.value ? Number(form.alertId.value) : null,
                assetId: form.assetId.value || null,
                title: form.title.value,
                description: form.description.value,
                priority: form.priority.value,
                assignedTo: form.assignedTo.value || null,
                dueDate: form.dueDate.value || null
            })
        });
        form.reset();
        await refreshData();
        populateFormOptions();
        showToast("Work order created.");
    } catch (error) {
        showToast(error.message, true);
    }
});

document.querySelector("#maintenance-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        await apiFetch("/api/maintenance-records", {
            method: "POST",
            body: JSON.stringify({
                workOrderId: Number(form.workOrderId.value),
                rootCause: form.rootCause.value,
                actionTaken: form.actionTaken.value,
                downtimeMinutes: form.downtimeMinutes.value ? Number(form.downtimeMinutes.value) : null,
                partsReplaced: form.partsReplaced.value || null,
                notes: form.notes.value || null
            })
        });
        form.reset();
        await refreshData();
        populateFormOptions();
        showToast("Maintenance record saved.");
    } catch (error) {
        showToast(error.message, true);
    }
});

document.querySelector("#export-alerts-button").addEventListener("click", async () => {
    try {
        const csv = await apiFetch("/api/alerts/export", { headers: { Accept: "text/csv" } });
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "alerts-export.csv";
        link.click();
        URL.revokeObjectURL(url);
        showToast("Alert CSV exported.");
    } catch (error) {
        showToast(error.message, true);
    }
});

restoreSession();
