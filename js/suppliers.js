/**
 * SMART INVENTORY MANAGEMENT SYSTEM - SUPPLIERS CONTROLLER
 */

let suppliersList = [];
let supplierToDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
    loadSuppliers();

    const searchInput = document.getElementById('search-supplier');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const q = searchInput.value.toLowerCase().trim();
            const filtered = suppliersList.filter(s => 
                s.name.toLowerCase().includes(q) || 
                (s.contactPerson && s.contactPerson.toLowerCase().includes(q)) ||
                (s.email && s.email.toLowerCase().includes(q)) ||
                (s.phone && s.phone.toLowerCase().includes(q))
            );
            renderSuppliersTable(filtered);
        });
    }
});

async function loadSuppliers() {
    try {
        const res = await fetchAPI('api/suppliers');
        suppliersList = res.data || [];
        renderSuppliersTable(suppliersList);
    } catch (err) {
        showToast('Error loading suppliers: ' + err.message, 'error');
    }
}

function renderSuppliersTable(suppliers) {
    const tbody = document.getElementById('suppliers-table-body');
    const countBadge = document.getElementById('supplier-count-badge');
    if (countBadge) countBadge.textContent = `${suppliers.length} suppliers`;

    if (!suppliers || suppliers.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                    🚚 No suppliers found. Click "+ Add Supplier" to create one.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = suppliers.map(s => `
        <tr>
            <td>
                <strong>${escapeHtml(s.name)}</strong>
                ${s.address ? `<div style="color:var(--text-muted); font-size:0.75rem;">${escapeHtml(s.address)}</div>` : ''}
            </td>
            <td>${escapeHtml(s.contactPerson || '-')}</td>
            <td>${s.email ? `<a href="mailto:${escapeHtml(s.email)}" style="color:var(--primary); text-decoration:none;">${escapeHtml(s.email)}</a>` : '-'}</td>
            <td>${escapeHtml(s.phone || '-')}</td>
            <td>
                <span class="badge ${s.productCount > 0 ? 'badge-info' : 'badge-secondary'}">
                    ${s.productCount} products
                </span>
            </td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-secondary" onclick="openEditSupplier(${s.id})">✏️ Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="confirmDeleteSupplier(${s.id}, '${escapeHtml(s.name)}')">🗑️ Delete</button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Add Supplier
async function handleAddSupplier(e) {
    e.preventDefault();
    const payload = {
        name: document.getElementById('supplier-name').value.trim(),
        contactPerson: document.getElementById('supplier-contact').value.trim(),
        email: document.getElementById('supplier-email').value.trim(),
        phone: document.getElementById('supplier-phone').value.trim(),
        address: document.getElementById('supplier-address').value.trim()
    };

    try {
        await fetchAPI('api/suppliers', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast('Supplier added successfully!', 'success');
        closeModal('modal-add-supplier');
        document.getElementById('form-add-supplier').reset();
        loadSuppliers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Edit Supplier
function openEditSupplier(id) {
    const s = suppliersList.find(sup => sup.id === id);
    if (!s) return;

    document.getElementById('edit-supplier-id').value = s.id;
    document.getElementById('edit-supplier-name').value = s.name;
    document.getElementById('edit-supplier-contact').value = s.contactPerson || '';
    document.getElementById('edit-supplier-email').value = s.email || '';
    document.getElementById('edit-supplier-phone').value = s.phone || '';
    document.getElementById('edit-supplier-address').value = s.address || '';

    openModal('modal-edit-supplier');
}

async function handleEditSupplier(e) {
    e.preventDefault();
    const payload = {
        id: parseInt(document.getElementById('edit-supplier-id').value),
        name: document.getElementById('edit-supplier-name').value.trim(),
        contactPerson: document.getElementById('edit-supplier-contact').value.trim(),
        email: document.getElementById('edit-supplier-email').value.trim(),
        phone: document.getElementById('edit-supplier-phone').value.trim(),
        address: document.getElementById('edit-supplier-address').value.trim()
    };

    try {
        await fetchAPI('api/suppliers', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        showToast('Supplier updated successfully!', 'success');
        closeModal('modal-edit-supplier');
        loadSuppliers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Delete Supplier
function confirmDeleteSupplier(id, name) {
    supplierToDeleteId = id;
    document.getElementById('delete-supplier-name').textContent = name;
    openModal('modal-delete-supplier');
}

async function executeDeleteSupplier() {
    if (!supplierToDeleteId) return;
    try {
        await fetchAPI(`api/suppliers?id=${supplierToDeleteId}`, {
            method: 'DELETE'
        });
        showToast('Supplier deleted successfully!', 'success');
        closeModal('modal-delete-supplier');
        supplierToDeleteId = null;
        loadSuppliers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
