/**
 * SMART INVENTORY MANAGEMENT SYSTEM - DASHBOARD CONTROLLER
 */

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
    loadCategoriesAndSuppliersForModals();
});

let availableProductsList = [];

async function loadDashboardData() {
    try {
        const res = await fetchAPI('api/dashboard');
        const data = res.data;

        // Populate KPI Cards
        document.getElementById('stat-total-products').textContent = formatNumber(data.totalProducts);
        document.getElementById('stat-total-units').textContent = formatNumber(data.totalStockUnits);
        document.getElementById('stat-total-value').textContent = formatCurrency(data.totalInventoryValue);
        
        const lowStockEl = document.getElementById('stat-low-stock');
        lowStockEl.textContent = formatNumber(data.lowStockCount + data.outOfStockCount);
        if ((data.lowStockCount + data.outOfStockCount) > 0) {
            lowStockEl.style.color = 'var(--danger)';
        }

        // Render Low Stock Alert Table
        renderLowStockTable(data.lowStockProducts);

        // Render Category Progress Bars
        renderCategoryBreakdown(data.categoryStats, data.totalStockUnits);

        // Render Recent Transactions
        renderRecentTransactions(data.recentTransactions);

    } catch (err) {
        showToast('Error loading dashboard: ' + err.message, 'error');
    }
}

function renderLowStockTable(products) {
    const tbody = document.getElementById('low-stock-table-body');
    if (!tbody) return;

    if (!products || products.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    🎉 Great news! All inventory stock levels are healthy.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = products.map(p => {
        const isOutOfStock = p.quantity <= 0;
        const statusBadge = isOutOfStock
            ? '<span class="badge badge-danger">Out of Stock</span>'
            : '<span class="badge badge-warning">Low Stock</span>';
        
        return `
            <tr>
                <td><strong>${escapeHtml(p.sku)}</strong></td>
                <td>${escapeHtml(p.name)}</td>
                <td>${escapeHtml(p.categoryName || 'General')}</td>
                <td><span style="font-weight:700; color:${isOutOfStock ? 'var(--danger)' : 'var(--warning-dark)'}">${p.quantity}</span> / ${p.minStockLevel}</td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="openQuickStockIn(${p.id}, '${escapeHtml(p.name)}', ${p.costPrice})">
                        + Stock In
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderCategoryBreakdown(categories, totalUnits) {
    const container = document.getElementById('category-breakdown-container');
    if (!container) return;

    if (!categories || categories.length === 0) {
        container.innerHTML = '<p style="color:var(--text-muted); font-size:0.85rem;">No category data available.</p>';
        return;
    }

    const maxUnits = totalUnits > 0 ? totalUnits : 1;

    container.innerHTML = categories.map(cat => {
        const percentage = Math.min(100, Math.round((cat.totalStock / maxUnits) * 100));
        return `
            <div class="progress-item">
                <div class="progress-header">
                    <span>${escapeHtml(cat.categoryName)} (${cat.productCount} items)</span>
                    <span>${formatNumber(cat.totalStock)} units (${percentage}%)</span>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar-fill" style="width: ${percentage}%;"></div>
                </div>
            </div>
        `;
    }).join('');
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('recent-transactions-table-body');
    if (!tbody) return;

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No stock movements recorded yet.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = transactions.map(t => {
        const isStockIn = t.transactionType === 'STOCK_IN';
        const badge = isStockIn
            ? '<span class="badge badge-success">⬇️ Stock In</span>'
            : '<span class="badge badge-danger">⬆️ Stock Out</span>';

        return `
            <tr>
                <td>${formatDate(t.transactionDate)}</td>
                <td><strong>${escapeHtml(t.productSku)}</strong> - ${escapeHtml(t.productName)}</td>
                <td>${badge}</td>
                <td><strong>${isStockIn ? '+' : '-'}${formatNumber(t.quantity)}</strong></td>
                <td>${escapeHtml(t.referenceNo || 'N/A')}</td>
                <td><small style="color:var(--text-secondary);">${escapeHtml(t.userName || 'System')}</small></td>
            </tr>
        `;
    }).join('');
}

// Load dropdowns for quick modal actions
async function loadCategoriesAndSuppliersForModals() {
    try {
        const [catsRes, suppsRes, prodsRes] = await Promise.all([
            fetchAPI('api/categories'),
            fetchAPI('api/suppliers'),
            fetchAPI('api/products')
        ]);

        availableProductsList = prodsRes.data || [];

        // Category dropdowns
        const catSelect = document.getElementById('product-category');
        if (catSelect && catsRes.data) {
            catSelect.innerHTML = '<option value="">-- Select Category --</option>' +
                catsRes.data.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
        }

        // Supplier dropdowns
        const suppSelect = document.getElementById('product-supplier');
        if (suppSelect && suppsRes.data) {
            suppSelect.innerHTML = '<option value="">-- None / Select Supplier --</option>' +
                suppsRes.data.map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`).join('');
        }

        // Product dropdowns for Stock In / Out
        const stockInProd = document.getElementById('stock-in-product');
        const stockOutProd = document.getElementById('stock-out-product');
        const prodOptions = '<option value="">-- Select Product --</option>' +
            availableProductsList.map(p => `<option value="${p.id}" data-price="${p.costPrice}" data-stock="${p.quantity}">${escapeHtml(p.sku)} - ${escapeHtml(p.name)} (Stock: ${p.quantity})</option>`).join('');

        if (stockInProd) stockInProd.innerHTML = prodOptions;
        if (stockOutProd) stockOutProd.innerHTML = prodOptions;

    } catch (e) {
        console.error('Error loading dropdown options', e);
    }
}

// Quick Stock In helper
function openQuickStockIn(productId, productName, costPrice) {
    const prodSelect = document.getElementById('stock-in-product');
    if (prodSelect) {
        prodSelect.value = productId;
    }
    const priceInput = document.getElementById('stock-in-price');
    if (priceInput && costPrice) {
        priceInput.value = costPrice;
    }
    openModal('modal-stock-in');
}

// Handle Add Product Submit
async function handleAddProductSubmit(e) {
    e.preventDefault();
    const payload = {
        sku: document.getElementById('product-sku').value.trim(),
        name: document.getElementById('product-name').value.trim(),
        categoryId: parseInt(document.getElementById('product-category').value),
        supplierId: document.getElementById('product-supplier').value ? parseInt(document.getElementById('product-supplier').value) : null,
        costPrice: parseFloat(document.getElementById('product-cost').value) || 0,
        sellingPrice: parseFloat(document.getElementById('product-selling').value) || 0,
        quantity: parseInt(document.getElementById('product-qty').value) || 0,
        minStockLevel: parseInt(document.getElementById('product-min-stock').value) || 10,
        description: document.getElementById('product-desc').value.trim()
    };

    try {
        await fetchAPI('api/products', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast('Product created successfully!', 'success');
        closeModal('modal-add-product');
        document.getElementById('form-add-product').reset();
        loadDashboardData();
        loadCategoriesAndSuppliersForModals();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Handle Quick Stock In Submit
async function handleStockInSubmit(e) {
    e.preventDefault();
    const payload = {
        action: 'in',
        productId: parseInt(document.getElementById('stock-in-product').value),
        quantity: parseInt(document.getElementById('stock-in-qty').value),
        unitPrice: parseFloat(document.getElementById('stock-in-price').value) || 0,
        referenceNo: document.getElementById('stock-in-ref').value.trim(),
        notes: document.getElementById('stock-in-notes').value.trim()
    };

    try {
        await fetchAPI('api/stock', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast('Stock In recorded successfully!', 'success');
        closeModal('modal-stock-in');
        document.getElementById('form-stock-in').reset();
        loadDashboardData();
        loadCategoriesAndSuppliersForModals();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Handle Quick Stock Out Submit
async function handleStockOutSubmit(e) {
    e.preventDefault();
    const payload = {
        action: 'out',
        productId: parseInt(document.getElementById('stock-out-product').value),
        quantity: parseInt(document.getElementById('stock-out-qty').value),
        unitPrice: parseFloat(document.getElementById('stock-out-price').value) || 0,
        referenceNo: document.getElementById('stock-out-ref').value.trim(),
        notes: document.getElementById('stock-out-notes').value.trim()
    };

    try {
        await fetchAPI('api/stock', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast('Stock Out recorded successfully!', 'success');
        closeModal('modal-stock-out');
        document.getElementById('form-stock-out').reset();
        loadDashboardData();
        loadCategoriesAndSuppliersForModals();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
