/**
 * SMART INVENTORY MANAGEMENT SYSTEM - CATEGORIES CONTROLLER
 */

let categoriesList = [];
let categoryToDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
    loadCategories();

    const searchInput = document.getElementById('search-category');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const q = searchInput.value.toLowerCase().trim();
            const filtered = categoriesList.filter(c => 
                c.name.toLowerCase().includes(q) || (c.description && c.description.toLowerCase().includes(q))
            );
            renderCategoriesTable(filtered);
        });
    }
});

async function loadCategories() {
    try {
        const res = await fetchAPI('api/categories');
        categoriesList = res.data || [];
        renderCategoriesTable(categoriesList);
    } catch (err) {
        showToast('Error loading categories: ' + err.message, 'error');
    }
}

function renderCategoriesTable(categories) {
    const tbody = document.getElementById('categories-table-body');
    const countBadge = document.getElementById('category-count-badge');
    if (countBadge) countBadge.textContent = `${categories.length} categories`;

    if (!categories || categories.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                    📂 No categories found. Click "+ Add Category" to create one.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = categories.map(c => `
        <tr>
            <td><strong>#${c.id}</strong></td>
            <td><strong>${escapeHtml(c.name)}</strong></td>
            <td>${escapeHtml(c.description || '-')}</td>
            <td>
                <span class="badge ${c.productCount > 0 ? 'badge-info' : 'badge-secondary'}">
                    ${c.productCount} products
                </span>
            </td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-secondary" onclick="openEditCategory(${c.id})">✏️ Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="confirmDeleteCategory(${c.id}, '${escapeHtml(c.name)}', ${c.productCount})">🗑️ Delete</button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Add Category
async function handleAddCategory(e) {
    e.preventDefault();
    const payload = {
        name: document.getElementById('category-name').value.trim(),
        description: document.getElementById('category-desc').value.trim()
    };

    try {
        await fetchAPI('api/categories', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast('Category created successfully!', 'success');
        closeModal('modal-add-category');
        document.getElementById('form-add-category').reset();
        loadCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Edit Category
function openEditCategory(id) {
    const cat = categoriesList.find(c => c.id === id);
    if (!cat) return;

    document.getElementById('edit-category-id').value = cat.id;
    document.getElementById('edit-category-name').value = cat.name;
    document.getElementById('edit-category-desc').value = cat.description || '';

    openModal('modal-edit-category');
}

async function handleEditCategory(e) {
    e.preventDefault();
    const payload = {
        id: parseInt(document.getElementById('edit-category-id').value),
        name: document.getElementById('edit-category-name').value.trim(),
        description: document.getElementById('edit-category-desc').value.trim()
    };

    try {
        await fetchAPI('api/categories', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        showToast('Category updated successfully!', 'success');
        closeModal('modal-edit-category');
        loadCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// Delete Category
function confirmDeleteCategory(id, name, productCount) {
    if (productCount > 0) {
        alert(`Cannot delete category "${name}" because it currently contains ${productCount} product(s). Please reassign or delete the products first.`);
        return;
    }
    categoryToDeleteId = id;
    document.getElementById('delete-category-name').textContent = name;
    openModal('modal-delete-category');
}

async function executeDeleteCategory() {
    if (!categoryToDeleteId) return;
    try {
        await fetchAPI(`api/categories?id=${categoryToDeleteId}`, {
            method: 'DELETE'
        });
        showToast('Category deleted successfully!', 'success');
        closeModal('modal-delete-category');
        categoryToDeleteId = null;
        loadCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}
