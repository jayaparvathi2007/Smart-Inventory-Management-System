/**
 * SMART INVENTORY MANAGEMENT SYSTEM - PRODUCTS CONTROLLER
 */

let allCategories = [];
let allSuppliers = [];
let currentProducts = [];
let productToDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
    initProductsPage();
});

async function initProductsPage() {
    await loadFilterOptions();
    await loadProducts();

    // Setup search & filter listeners
    const searchInput = document.getElementById('search-product');
    const categoryFilter = document.getElementById('filter-category');
    const statusFilter = document.getElementById('filter-status');

    let debounceTimer;
    searchInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => loadProducts(), 300);
    });

    categoryFilter.addEventListener('change', () => loadProducts());
    statusFilter.addEventListener('change', () => loadProducts());
}

async function loadFilterOptions() {
    try {
        const [catsRes, suppsRes] = await Promise.all([
            fetchAPI('api/categories'),
            fetchAPI('api/suppliers')
        ]);

        allCategories = catsRes.data || [];
        allSuppliers = suppsRes.data || [];

        // Populate Category Filter dropdown
        const filterCat = document.getElementById('filter-category');
        filterCat.innerHTML = '<option value="">All Categories</option>' +
            allCategories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');

        // Populate Form Category Dropdowns
        populateCategoryDropdowns();
        populateSupplierDropdowns();
    } catch (e) {
        console.error('Error loading filter options', e);
    }
}

function populateCategoryDropdowns() {
    const addCat = document.getElementById('product-category');
    const editCat = document.getElementById('edit-product-category');
    const opts = '<option value="">-- Select Category --</option>' +
        allCategories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
    if (addCat) addCat.innerHTML = opts;
    if (editCat) editCat.innerHTML = opts;
}

function populateSupplierDropdowns() {
    const addSupp = document.getElementById('product-supplier');
    const editSupp = document.getElementById('edit-product-supplier');
    const opts = '<option value="">-- None / Select Supplier --</option>' +
        allSuppliers.map(s => `<option value="${s.id}">${escapeHtml(s.name)}</option>`).join('');
    if (addSupp) addSupp.innerHTML = opts;
    if (editSupp) editSupp.innerHTML = opts;
}

async function loadProducts() {
    const query = document.getElementById('search-product').value.trim();
    const categoryId = document.getElementById('filter-category').value;
    const stockStatus = document.getElementById('filter-status').value;

    let url = 'api/products?';
    if (query) url += `query=${encodeURIComponent(query)}&`;
    if (categoryId) url += `categoryId=${encodeURIComponent(categoryId)}&`;
    if (stockStatus) url += `stockStatus=${encodeURIComponent(stockStatus)}&`;

    try {
        const res = await fetchAPI(url);
        currentProducts = res.data || [];
        renderProductsTable(currentProducts);
    } catch (err) {
        showToast('Error loading products: ' + err.message, 'error');
    }
}

function renderProductsTable(products) {
    const tbody = document.getElementById('products-table-body');
    const countEl = document.getElementById('product-count-badge');
    if (countEl) countEl.textContent = `${products.length} products`;

    if (!products || products.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                    🔍 No products found matching your search criteria.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = products.map(p => {
        let badge = '<span class="badge badge-success">In Stock</span>';
        if (p.quantity <= 0) {
            badge = '<span class="badge badge-danger">Out of Stock</span>';
        } else if (p.quantity <= p.minStockLevel) {
            badge = '<span class="badge badge-warning">Low Stock</span>';
        }

        return `
            <tr>
                <td><strong>${escapeHtml(p.sku)}</strong></td>
                <td>
                    <div style="font-weight: 600;">${escapeHtml(p.name)}</div>
                    ${p.description ? `<small style="color:var(--text-muted);">${escapeHtml(p.description)}</small>` : ''}
                </td>
                <td>${escapeHtml(p.categoryName || 'General')}</td>
                <td>${escapeHtml(p.supplierName || 'N/A')}</td>
                <td>${formatCurrency(p.costPrice)}</td>
                <td><strong>${formatCurrency(p.sellingPrice)}</strong></td>
                <td>
                    <span style="font-weight:700; ${p.quantity <= p.minStockLevel ? 'color:var(--danger)' : ''}">
                        ${formatNumber(p.quantity)}
                    </span>
                    <small style="color:var(--text-muted);">/ min ${p.minStockLevel}</small>
                </td>
                <td>${badge}</td>
                <td>
                    <div class="action-buttons">
                        <button class="btn btn-sm btn-outline" title="Stock In" onclick="openStockInModal(${p.id})">⬇️</button>
                        <button class="btn btn-sm btn-outline" title="Stock Out" onclick="openStockOutModal(${p.id})">⬆️</button>
                        <button class="btn btn-sm btn-secondary" title="Edit" onclick="openEditModal(${p.id})">✏️</button>
                        <button class="btn btn-sm btn-danger" title="Delete" onclick="confirmDeleteProduct(${p.id}, '${escapeHtml(p.name)}')">🗑️</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// Add Product
async function handleAddProduct(e) {
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
        showToast('Product added successfully!', 'success');
        closeModal('modal-add-product');
        document.getElementById('form-add-product').reset();
        loadProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Edit Product
async function openEditModal(productId) {
    const product = currentProducts.find(p => p.id === productId);
    if (!product) return;

    document.getElementById('edit-product-id').value = product.id;
    document.getElementById('edit-product-sku').value = product.sku;
    document.getElementById('edit-product-name').value = product.name;
    document.getElementById('edit-product-category').value = product.categoryId;
    document.getElementById('edit-product-supplier').value = product.supplierId || '';
    document.getElementById('edit-product-cost').value = product.costPrice;
    document.getElementById('edit-product-selling').value = product.sellingPrice;
    document.getElementById('edit-product-min-stock').value = product.minStockLevel;
    document.getElementById('edit-product-desc').value = product.description || '';

    openModal('modal-edit-product');
}

async function handleEditProduct(e) {
    e.preventDefault();
    const payload = {
        id: parseInt(document.getElementById('edit-product-id').value),
        sku: document.getElementById('edit-product-sku').value.trim(),
        name: document.getElementById('edit-product-name').value.trim(),
        categoryId: parseInt(document.getElementById('edit-product-category').value),
        supplierId: document.getElementById('edit-product-supplier').value ? parseInt(document.getElementById('edit-product-supplier').value) : null,
        costPrice: parseFloat(document.getElementById('edit-product-cost').value) || 0,
        sellingPrice: parseFloat(document.getElementById('edit-product-selling').value) || 0,
        minStockLevel: parseInt(document.getElementById('edit-product-min-stock').value) || 10,
        description: document.getElementById('edit-product-desc').value.trim()
    };

    try {
        await fetchAPI('api/products', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        showToast('Product updated successfully!', 'success');
        closeModal('modal-edit-product');
        loadProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Delete Product
function confirmDeleteProduct(id, name) {
    productToDeleteId = id;
    document.getElementById('delete-product-name').textContent = name;
    openModal('modal-delete-confirm');
}

async function executeDeleteProduct() {
    if (!productToDeleteId) return;
    try {
        await fetchAPI(`api/products?id=${productToDeleteId}`, {
            method: 'DELETE'
        });
        showToast('Product deleted successfully!', 'success');
        closeModal('modal-delete-confirm');
        productToDeleteId = null;
        loadProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Stock In / Out shortcuts
function openStockInModal(productId) {
    const product = currentProducts.find(p => p.id === productId);
    if (!product) return;
    document.getElementById('stock-in-product-id').value = product.id;
    document.getElementById('stock-in-product-name').textContent = `${product.sku} - ${product.name} (Current: ${product.quantity})`;
    document.getElementById('stock-in-cost').value = product.costPrice;
    openModal('modal-stock-in');
}

async function handleStockInSubmit(e) {
    e.preventDefault();
    const payload = {
        action: 'in',
        productId: parseInt(document.getElementById('stock-in-product-id').value),
        quantity: parseInt(document.getElementById('stock-in-qty').value),
        unitPrice: parseFloat(document.getElementById('stock-in-cost').value) || 0,
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
        loadProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openStockOutModal(productId) {
    const product = currentProducts.find(p => p.id === productId);
    if (!product) return;
    document.getElementById('stock-out-product-id').value = product.id;
    document.getElementById('stock-out-product-name').textContent = `${product.sku} - ${product.name} (Available: ${product.quantity})`;
    document.getElementById('stock-out-price').value = product.sellingPrice;
    openModal('modal-stock-out');
}

async function handleStockOutSubmit(e) {
    e.preventDefault();
    const payload = {
        action: 'out',
        productId: parseInt(document.getElementById('stock-out-product-id').value),
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
        loadProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
