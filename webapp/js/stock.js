/**
 * SMART INVENTORY MANAGEMENT SYSTEM - STOCK CONTROLLER
 */

let productsList = [];

document.addEventListener('DOMContentLoaded', () => {
    initStockPage();
});

async function initStockPage() {
    await loadProductsForStock();
    await loadTransactionHistory();

    // Listeners for filters
    document.getElementById('filter-type').addEventListener('change', loadTransactionHistory);
    document.getElementById('filter-product').addEventListener('change', loadTransactionHistory);
    document.getElementById('filter-start-date').addEventListener('change', loadTransactionHistory);
    document.getElementById('filter-end-date').addEventListener('change', loadTransactionHistory);

    // Live stock check when selecting product in Stock Out
    document.getElementById('stock-out-product').addEventListener('change', handleStockOutProductChange);
    document.getElementById('stock-out-qty').addEventListener('input', validateStockOutQuantity);

    // Auto-fill cost when selecting product in Stock In
    document.getElementById('stock-in-product').addEventListener('change', handleStockInProductChange);
}

async function loadProductsForStock() {
    try {
        const res = await fetchAPI('api/products');
        productsList = res.data || [];

        const prodOptions = '<option value="">-- Select Product --</option>' +
            productsList.map(p => `
                <option value="${p.id}" data-cost="${p.costPrice}" data-price="${p.sellingPrice}" data-qty="${p.quantity}">
                    ${escapeHtml(p.sku)} - ${escapeHtml(p.name)} (Stock: ${p.quantity})
                </option>
            `).join('');

        document.getElementById('stock-in-product').innerHTML = prodOptions;
        document.getElementById('stock-out-product').innerHTML = prodOptions;

        // Populate history filter
        const filterProd = document.getElementById('filter-product');
        filterProd.innerHTML = '<option value="">All Products</option>' +
            productsList.map(p => `<option value="${p.id}">${escapeHtml(p.sku)} - ${escapeHtml(p.name)}</option>`).join('');

    } catch (err) {
        showToast('Error loading products list: ' + err.message, 'error');
    }
}

function handleStockInProductChange() {
    const select = document.getElementById('stock-in-product');
    const opt = select.options[select.selectedIndex];
    if (opt && opt.dataset.cost) {
        document.getElementById('stock-in-price').value = opt.dataset.cost;
    }
}

function handleStockOutProductChange() {
    const select = document.getElementById('stock-out-product');
    const opt = select.options[select.selectedIndex];
    const availInfo = document.getElementById('stock-out-avail-info');
    if (opt && opt.value) {
        const qty = parseInt(opt.dataset.qty) || 0;
        const price = opt.dataset.price || '0.00';
        document.getElementById('stock-out-price').value = price;
        availInfo.textContent = `Available Stock: ${qty} units`;
        availInfo.style.color = qty > 0 ? 'var(--primary)' : 'var(--danger)';
    } else {
        availInfo.textContent = '';
    }
    validateStockOutQuantity();
}

function validateStockOutQuantity() {
    const select = document.getElementById('stock-out-product');
    const opt = select.options[select.selectedIndex];
    const qtyInput = document.getElementById('stock-out-qty');
    const warning = document.getElementById('stock-out-warning');
    const submitBtn = document.getElementById('btn-submit-stock-out');

    if (!opt || !opt.value) {
        warning.style.display = 'none';
        return;
    }

    const available = parseInt(opt.dataset.qty) || 0;
    const requested = parseInt(qtyInput.value) || 0;

    if (requested > available) {
        warning.textContent = `⚠️ Cannot dispatch ${requested} units! Only ${available} available in stock.`;
        warning.style.display = 'block';
        submitBtn.disabled = true;
    } else {
        warning.style.display = 'none';
        submitBtn.disabled = false;
    }
}

// Handle Stock In Form Submit
async function handleStockIn(e) {
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
        document.getElementById('form-stock-in').reset();
        await loadProductsForStock();
        await loadTransactionHistory();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Handle Stock Out Form Submit
async function handleStockOut(e) {
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
        document.getElementById('form-stock-out').reset();
        document.getElementById('stock-out-avail-info').textContent = '';
        await loadProductsForStock();
        await loadTransactionHistory();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Load Transaction History
async function loadTransactionHistory() {
    const type = document.getElementById('filter-type').value;
    const productId = document.getElementById('filter-product').value;
    const startDate = document.getElementById('filter-start-date').value;
    const endDate = document.getElementById('filter-end-date').value;

    let url = 'api/stock?';
    if (type) url += `type=${encodeURIComponent(type)}&`;
    if (productId) url += `productId=${encodeURIComponent(productId)}&`;
    if (startDate) url += `startDate=${encodeURIComponent(startDate)}&`;
    if (endDate) url += `endDate=${encodeURIComponent(endDate)}&`;

    try {
        const res = await fetchAPI(url);
        renderTransactionHistory(res.data || []);
    } catch (err) {
        showToast('Error loading history: ' + err.message, 'error');
    }
}

function renderTransactionHistory(transactions) {
    const tbody = document.getElementById('transactions-table-body');
    const countBadge = document.getElementById('trans-count-badge');
    if (countBadge) countBadge.textContent = `${transactions.length} records`;

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                    No stock movements found matching filter criteria.
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
                <td>
                    <strong>${escapeHtml(t.productSku)}</strong>
                    <div>${escapeHtml(t.productName)}</div>
                </td>
                <td>${badge}</td>
                <td><strong>${isStockIn ? '+' : '-'}${formatNumber(t.quantity)}</strong></td>
                <td>${formatCurrency(t.unitPrice)}</td>
                <td>${formatCurrency(t.unitPrice * t.quantity)}</td>
                <td>${escapeHtml(t.referenceNo || '-')}</td>
                <td>
                    <small style="font-weight:600;">${escapeHtml(t.userName || 'System')}</small>
                    ${t.notes ? `<div style="color:var(--text-muted); font-size:0.75rem;">${escapeHtml(t.notes)}</div>` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

// Export CSV for transactions
function exportTransactionsCsv() {
    const type = document.getElementById('filter-type').value;
    const startDate = document.getElementById('filter-start-date').value;
    const endDate = document.getElementById('filter-end-date').value;

    let url = 'api/reports?type=transactions&export=csv';
    if (type) url += `&transType=${encodeURIComponent(type)}`;
    if (startDate) url += `&startDate=${encodeURIComponent(startDate)}`;
    if (endDate) url += `&endDate=${encodeURIComponent(endDate)}`;

    window.location.href = url;
}
