/**
 * SMART INVENTORY MANAGEMENT SYSTEM - REPORTS CONTROLLER
 */

let currentTab = 'valuation';

document.addEventListener('DOMContentLoaded', () => {
    // Check URL parameters for tab selection
    const urlParams = new URLSearchParams(window.location.search);
    const tabParam = urlParams.get('tab');
    if (tabParam) {
        currentTab = tabParam;
    }

    switchTab(currentTab);
});

function switchTab(tab) {
    currentTab = tab;

    // Update active tab buttons
    document.querySelectorAll('.report-tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tab);
    });

    if (tab === 'valuation') {
        loadValuationReport();
    } else if (tab === 'lowstock') {
        loadLowStockReport();
    } else if (tab === 'transactions') {
        loadTransactionsReport();
    }
}

// 1. Valuation Report
async function loadValuationReport() {
    const container = document.getElementById('report-content-area');
    container.innerHTML = '<div style="text-align:center; padding:3rem;">Generating Valuation Report...</div>';

    try {
        const res = await fetchAPI('api/reports?type=valuation');
        const r = res.data;

        let html = `
            <!-- SUMMARY STATS -->
            <div class="stats-grid" style="margin-bottom: 1.5rem;">
                <div class="stat-card">
                    <div class="stat-icon-wrapper stat-icon-blue">🏷️</div>
                    <div class="stat-info">
                        <div class="stat-title">Catalogued Items</div>
                        <div class="stat-value">${formatNumber(r.totalProducts)}</div>
                        <div class="stat-sub">Active product SKUs</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-wrapper stat-icon-green">📦</div>
                    <div class="stat-info">
                        <div class="stat-title">Total Units On Hand</div>
                        <div class="stat-value">${formatNumber(r.totalUnits)}</div>
                        <div class="stat-sub">Across all inventory</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-wrapper stat-icon-amber">💰</div>
                    <div class="stat-info">
                        <div class="stat-title">Total Cost Value</div>
                        <div class="stat-value">${formatCurrency(r.totalCostValuation)}</div>
                        <div class="stat-sub">Purchase cost investment</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-wrapper stat-icon-blue">📈</div>
                    <div class="stat-info">
                        <div class="stat-title">Estimated Retail Value</div>
                        <div class="stat-value">${formatCurrency(r.totalRetailValuation)}</div>
                        <div class="stat-sub">Potential Gross Margin: ${formatCurrency(r.estimatedMargin)}</div>
                    </div>
                </div>
            </div>

            <!-- VALUATION TABLE -->
            <div class="panel-card">
                <div class="panel-header">
                    <div class="panel-title">
                        <span>📊 Detailed Inventory Valuation Sheet</span>
                    </div>
                    <small style="color:var(--text-muted);">Generated: ${r.generatedAt}</small>
                </div>
                <div class="panel-body" style="padding:0;">
                    <div class="table-responsive">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th>SKU</th>
                                    <th>Product Name</th>
                                    <th>Category</th>
                                    <th>Supplier</th>
                                    <th>Units On Hand</th>
                                    <th>Cost / Unit</th>
                                    <th>Selling / Unit</th>
                                    <th>Total Cost Value</th>
                                    <th>Total Retail Value</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${(r.products || []).map(p => {
                                    let badge = '<span class="badge badge-success">In Stock</span>';
                                    if (p.quantity <= 0) badge = '<span class="badge badge-danger">Out of Stock</span>';
                                    else if (p.quantity <= p.minStockLevel) badge = '<span class="badge badge-warning">Low Stock</span>';

                                    const costVal = p.quantity * p.costPrice;
                                    const retailVal = p.quantity * p.sellingPrice;

                                    return `
                                        <tr>
                                            <td><strong>${escapeHtml(p.sku)}</strong></td>
                                            <td>${escapeHtml(p.name)}</td>
                                            <td>${escapeHtml(p.categoryName || 'General')}</td>
                                            <td>${escapeHtml(p.supplierName || 'N/A')}</td>
                                            <td><strong>${formatNumber(p.quantity)}</strong></td>
                                            <td>${formatCurrency(p.costPrice)}</td>
                                            <td>${formatCurrency(p.sellingPrice)}</td>
                                            <td><strong>${formatCurrency(costVal)}</strong></td>
                                            <td>${formatCurrency(retailVal)}</td>
                                            <td>${badge}</td>
                                        </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = html;
    } catch (err) {
        container.innerHTML = `<div style="color:var(--danger); padding:2rem;">Failed to load report: ${err.message}</div>`;
    }
}

// 2. Low Stock & Reorder Report
async function loadLowStockReport() {
    const container = document.getElementById('report-content-area');
    container.innerHTML = '<div style="text-align:center; padding:3rem;">Compiling Reorder Priorities...</div>';

    try {
        const res = await fetchAPI('api/reports?type=lowstock');
        const r = res.data;
        const products = r.products || [];

        let html = `
            <div class="panel-card">
                <div class="panel-header">
                    <div class="panel-title">
                        <span>⚠️ Low Stock & Immediate Reorder Schedule</span>
                        <span class="badge badge-danger" style="margin-left:0.5rem;">${r.lowStockCount} items need attention</span>
                    </div>
                    <small style="color:var(--text-muted);">Generated: ${r.generatedAt}</small>
                </div>
                <div class="panel-body" style="padding:0;">
                    <div class="table-responsive">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th>SKU</th>
                                    <th>Product Name</th>
                                    <th>Category</th>
                                    <th>Supplier</th>
                                    <th>Stock Level</th>
                                    <th>Min Threshold</th>
                                    <th>Deficit Units</th>
                                    <th>Est. Reorder Cost</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${products.length === 0 ? `
                                    <tr>
                                        <td colspan="9" style="text-align:center; padding:3rem; color:var(--text-muted);">
                                            🎉 All products are above minimum inventory alert levels!
                                        </td>
                                    </tr>
                                ` : products.map(p => {
                                    const deficit = Math.max(0, p.minStockLevel - p.quantity);
                                    const reorderCost = deficit * p.costPrice;
                                    const isOutOfStock = p.quantity <= 0;

                                    return `
                                        <tr>
                                            <td><strong>${escapeHtml(p.sku)}</strong></td>
                                            <td>${escapeHtml(p.name)}</td>
                                            <td>${escapeHtml(p.categoryName || 'General')}</td>
                                            <td>${escapeHtml(p.supplierName || 'N/A')}</td>
                                            <td><span style="font-weight:700; color:${isOutOfStock ? 'var(--danger)' : 'var(--warning-dark)'}">${p.quantity}</span></td>
                                            <td>${p.minStockLevel}</td>
                                            <td><strong style="color:var(--danger);">+${deficit} units</strong></td>
                                            <td>${formatCurrency(reorderCost)}</td>
                                            <td>
                                                <span class="badge ${isOutOfStock ? 'badge-danger' : 'badge-warning'}">
                                                    ${isOutOfStock ? 'Out of Stock' : 'Low Stock'}
                                                </span>
                                            </td>
                                        </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = html;
    } catch (err) {
        container.innerHTML = `<div style="color:var(--danger); padding:2rem;">Failed to load report: ${err.message}</div>`;
    }
}

// 3. Transactions Audit Report
async function loadTransactionsReport() {
    const container = document.getElementById('report-content-area');
    container.innerHTML = '<div style="text-align:center; padding:3rem;">Compiling Transactions Log...</div>';

    try {
        const res = await fetchAPI('api/reports?type=transactions');
        const r = res.data;
        const transactions = r.transactions || [];

        let html = `
            <div class="panel-card">
                <div class="panel-header">
                    <div class="panel-title">
                        <span>📜 Complete Inventory Movements Log</span>
                        <span class="badge badge-secondary" style="margin-left:0.5rem;">${r.totalCount} records</span>
                    </div>
                    <small style="color:var(--text-muted);">Generated: ${r.generatedAt}</small>
                </div>
                <div class="panel-body" style="padding:0;">
                    <div class="table-responsive">
                        <table class="custom-table">
                            <thead>
                                <tr>
                                    <th>Transaction ID</th>
                                    <th>Date & Time</th>
                                    <th>Product</th>
                                    <th>Type</th>
                                    <th>Quantity</th>
                                    <th>Unit Price</th>
                                    <th>Total Value</th>
                                    <th>Reference #</th>
                                    <th>Authorized User</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${transactions.length === 0 ? `
                                    <tr>
                                        <td colspan="9" style="text-align:center; padding:3rem; color:var(--text-muted);">
                                            No transactions recorded.
                                        </td>
                                    </tr>
                                ` : transactions.map(t => {
                                    const isStockIn = t.transactionType === 'STOCK_IN';
                                    const badge = isStockIn
                                        ? '<span class="badge badge-success">Stock In</span>'
                                        : '<span class="badge badge-danger">Stock Out</span>';

                                    return `
                                        <tr>
                                            <td>#${t.id}</td>
                                            <td>${formatDate(t.transactionDate)}</td>
                                            <td><strong>${escapeHtml(t.productSku)}</strong> - ${escapeHtml(t.productName)}</td>
                                            <td>${badge}</td>
                                            <td><strong>${isStockIn ? '+' : '-'}${formatNumber(t.quantity)}</strong></td>
                                            <td>${formatCurrency(t.unitPrice)}</td>
                                            <td>${formatCurrency(t.unitPrice * t.quantity)}</td>
                                            <td>${escapeHtml(t.referenceNo || '-')}</td>
                                            <td>${escapeHtml(t.userName || 'System')}</td>
                                        </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;

        container.innerHTML = html;
    } catch (err) {
        container.innerHTML = `<div style="color:var(--danger); padding:2rem;">Failed to load report: ${err.message}</div>`;
    }
}

// Print Handler
function printCurrentReport() {
    window.print();
}

// Export CSV
function exportCurrentReportCsv() {
    window.location.href = `api/reports?type=${encodeURIComponent(currentTab)}&export=csv`;
}
