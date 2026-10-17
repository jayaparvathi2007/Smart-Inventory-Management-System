/**
 * SMART INVENTORY MANAGEMENT SYSTEM - CORE JAVASCRIPT
 * Supports both local Java Servlet + MySQL backend and seamless cloud deployment.
 */

// Universal API fetch helper with automatic cloud fallback
async function fetchAPI(url, options = {}) {
    const defaultHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    options.headers = {
        ...defaultHeaders,
        ...(options.headers || {})
    };

    // If running on Vercel or static host where Java backend isn't hosted, use cloud mock store directly
    const isCloudHost = window.location.hostname.includes('vercel.app') || 
                         window.location.hostname.includes('github.io') ||
                         window.location.hostname.includes('netlify.app');

    if (!isCloudHost) {
        try {
            const response = await fetch(url, options);

            // Check if unauthorized
            if (response.status === 401) {
                if (document.getElementById('login-form') === null) {
                    window.location.href = 'login.html';
                }
                throw new Error('Session expired. Please log in.');
            }

            if (response.status === 404) {
                // If backend endpoint is missing, fall back to mock store
                return await handleCloudMockApi(url, options);
            }

            const data = await response.json();
            if (!response.ok || data.success === false) {
                throw new Error(data.message || `HTTP Error ${response.status}`);
            }
            return data;
        } catch (err) {
            if (err.message && err.message.includes('Session expired')) throw err;
            console.warn('Local API unavailable, switching to Cloud Mock Engine:', err);
            return await handleCloudMockApi(url, options);
        }
    } else {
        return await handleCloudMockApi(url, options);
    }
}

// Toast Notification Manager
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';
    if (type === 'warning') icon = '🔔';

    toast.innerHTML = `
        <span class="toast-icon">${icon}</span>
        <span class="toast-message">${escapeHtml(message)}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 400);
    }, 3800);
}

// Modal Controllers
function openModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = 'auto';
    }
}

// Close modals when clicking backdrop
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-backdrop')) {
        e.target.classList.remove('active');
        document.body.style.overflow = 'auto';
    }
});

// Formatting Utilities
function formatCurrency(val) {
    const num = parseFloat(val) || 0;
    return '$' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatNumber(val) {
    const num = parseInt(val) || 0;
    return num.toLocaleString('en-US');
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
}

// Session check & UI init
async function initAuthCheck() {
    const isLoginPage = document.getElementById('login-form') !== null;

    try {
        const res = await fetchAPI('api/auth');
        if (res.success && res.data) {
            const user = res.data;
            const userNameEl = document.getElementById('sidebar-user-name');
            const userRoleEl = document.getElementById('sidebar-user-role');
            const userAvatarEl = document.getElementById('sidebar-user-avatar');

            if (userNameEl) userNameEl.textContent = user.fullName || user.username;
            if (userRoleEl) userRoleEl.textContent = user.role || 'Staff';
            if (userAvatarEl) userAvatarEl.textContent = (user.fullName || user.username || 'U').charAt(0).toUpperCase();

            // If already authenticated and on the login screen, forward to dashboard
            if (isLoginPage) {
                window.location.href = 'index.html';
            }
        }
    } catch (err) {
        // If unauthenticated and on a protected page, redirect to login
        if (!isLoginPage) {
            window.location.href = 'login.html';
        }
    }
}

// Logout handler
async function handleLogout() {
    if (!confirm('Are you sure you want to log out?')) return;
    try {
        await fetchAPI('api/auth?action=logout', { method: 'POST' });
    } catch (e) {}
    localStorage.removeItem('SMART_INVENTORY_SESSION');
    window.location.href = 'login.html';
}

// Mobile sidebar toggle
function setupMobileMenu() {
    const menuBtn = document.getElementById('mobile-menu-btn');
    const sidebar = document.querySelector('.sidebar');
    if (menuBtn && sidebar) {
        menuBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    initAuthCheck();
    setupMobileMenu();
});

/* ==========================================================================
   CLOUD ENGINE (Provides instant live demo functionality on Vercel)
   ========================================================================== */

function getCloudStore() {
    const raw = localStorage.getItem('SMART_INVENTORY_STORE');
    if (raw) {
        try { return JSON.parse(raw); } catch (e) {}
    }

    // Default Seed Data matching MySQL schema.sql
    const initialData = {
        users: [
            { id: 1, username: 'admin', password: 'admin123', fullName: 'System Administrator', role: 'Admin' },
            { id: 2, username: 'manager', password: 'manager123', fullName: 'Inventory Manager', role: 'Manager' }
        ],
        categories: [
            { id: 1, name: 'Electronics', description: 'Gadgets, components, peripherals, and consumer electronic devices' },
            { id: 2, name: 'Office Supplies', description: 'Stationery, paper supplies, pens, filing folders, and desk essentials' },
            { id: 3, name: 'Furniture', description: 'Office chairs, standing desks, file cabinets, and ergonomics' },
            { id: 4, name: 'Computer Accessories', description: 'Keyboards, mice, USB hubs, docking stations, and cables' },
            { id: 5, name: 'Networking', description: 'Routers, switches, ethernet cables, and wireless access points' }
        ],
        suppliers: [
            { id: 1, name: 'TechSource Distribution', contactPerson: 'David Miller', email: 'sales@techsource.com', phone: '+1-555-0101', address: '100 Industrial Parkway, Austin, TX' },
            { id: 2, name: 'Apex Office World', contactPerson: 'Sarah Jenkins', email: 'orders@apexoffice.com', phone: '+1-555-0102', address: '45 Commercial Blvd, Chicago, IL' },
            { id: 3, name: 'Global Hardware Corp', contactPerson: 'Rajesh Kumar', email: 'support@globalhardware.com', phone: '+1-555-0103', address: '78 logistics Park, Fremont, CA' },
            { id: 4, name: 'ErgoPro Furnishings', contactPerson: 'Emily Chen', email: 'contact@ergopro.com', phone: '+1-555-0104', address: '220 Design Way, Grand Rapids, MI' }
        ],
        products: [
            { id: 1, sku: 'SKU-ELEC-001', name: 'Wireless Noise-Cancelling Headphones', categoryId: 1, categoryName: 'Electronics', supplierId: 1, supplierName: 'TechSource Distribution', costPrice: 65.00, sellingPrice: 119.99, quantity: 45, minStockLevel: 10, description: 'Premium wireless over-ear Bluetooth headphones' },
            { id: 2, sku: 'SKU-ELEC-002', name: 'USB-C Fast Charging Station 65W', categoryId: 1, categoryName: 'Electronics', supplierId: 1, supplierName: 'TechSource Distribution', costPrice: 18.50, sellingPrice: 39.99, quantity: 12, minStockLevel: 15, description: 'Multi-port GaN fast charger [LOW STOCK]' },
            { id: 3, sku: 'SKU-ELEC-003', name: 'Portable SSD 1TB USB 3.2', categoryId: 1, categoryName: 'Electronics', supplierId: 3, supplierName: 'Global Hardware Corp', costPrice: 55.00, sellingPrice: 89.99, quantity: 4, minStockLevel: 10, description: 'High-speed external SSD [LOW STOCK]' },
            { id: 4, sku: 'SKU-ELEC-004', name: 'Smart 4K Web Camera with Mic', categoryId: 1, categoryName: 'Electronics', supplierId: 1, supplierName: 'TechSource Distribution', costPrice: 32.00, sellingPrice: 64.99, quantity: 0, minStockLevel: 8, description: 'Ultra HD webcam [OUT OF STOCK]' },
            { id: 5, sku: 'SKU-OFFC-001', name: 'Premium Executive Gel Pens (Pack of 12)', categoryId: 2, categoryName: 'Office Supplies', supplierId: 2, supplierName: 'Apex Office World', costPrice: 6.20, sellingPrice: 14.50, quantity: 85, minStockLevel: 20, description: 'Smooth-flowing black ink rollerball pens' },
            { id: 6, sku: 'SKU-OFFC-002', name: 'Heavy Duty Electric Stapler', categoryId: 2, categoryName: 'Office Supplies', supplierId: 2, supplierName: 'Apex Office World', costPrice: 19.00, sellingPrice: 34.99, quantity: 28, minStockLevel: 10, description: 'Automatic electric stapler capacity 30 sheets' },
            { id: 7, sku: 'SKU-FURN-001', name: 'Ergonomic Mesh Desk Chair', categoryId: 3, categoryName: 'Furniture', supplierId: 4, supplierName: 'ErgoPro Furnishings', costPrice: 120.00, sellingPrice: 249.99, quantity: 18, minStockLevel: 5, description: 'High back breathable mesh chair' },
            { id: 8, sku: 'SKU-FURN-002', name: 'Electric Dual-Motor Standing Desk (140x70cm)', categoryId: 3, categoryName: 'Furniture', supplierId: 4, supplierName: 'ErgoPro Furnishings', costPrice: 210.00, sellingPrice: 399.00, quantity: 7, minStockLevel: 8, description: 'Motorized height adjustable standing desk [LOW STOCK]' },
            { id: 9, sku: 'SKU-COMP-001', name: 'Mechanical Wireless Keyboard RGB', categoryId: 4, categoryName: 'Computer Accessories', supplierId: 3, supplierName: 'Global Hardware Corp', costPrice: 42.00, sellingPrice: 79.99, quantity: 32, minStockLevel: 12, description: 'Compact mechanical keyboard with Bluetooth' },
            { id: 10, sku: 'SKU-COMP-002', name: 'Ergonomic Vertical Wireless Mouse', categoryId: 4, categoryName: 'Computer Accessories', supplierId: 3, supplierName: 'Global Hardware Corp', costPrice: 14.00, sellingPrice: 29.99, quantity: 52, minStockLevel: 15, description: 'Optical mouse designed to reduce wrist strain' },
            { id: 11, sku: 'SKU-NETW-001', name: 'Wi-Fi 6 Dual-Band Gigabit Router', categoryId: 5, categoryName: 'Networking', supplierId: 1, supplierName: 'TechSource Distribution', costPrice: 48.00, sellingPrice: 94.99, quantity: 16, minStockLevel: 10, description: 'Wi-Fi 6 wireless router with 4 external antennas' },
            { id: 12, sku: 'SKU-NETW-002', name: '8-Port Gigabit Unmanaged Switch', categoryId: 5, categoryName: 'Networking', supplierId: 1, supplierName: 'TechSource Distribution', costPrice: 15.00, sellingPrice: 28.50, quantity: 0, minStockLevel: 10, description: 'Metal desktop ethernet switch [OUT OF STOCK]' }
        ],
        transactions: [
            { id: 14, productId: 8, productSku: 'SKU-FURN-002', productName: 'Electric Dual-Motor Standing Desk (140x70cm)', transactionType: 'STOCK_OUT', quantity: 3, unitPrice: 399.00, referenceNo: 'INV-2026-107', notes: 'Design agency delivery', userName: 'System Administrator', transactionDate: new Date(Date.now() - 7200000).toISOString() },
            { id: 12, productId: 7, productSku: 'SKU-FURN-001', productName: 'Ergonomic Mesh Desk Chair', transactionType: 'STOCK_OUT', quantity: 2, unitPrice: 249.99, referenceNo: 'INV-2026-106', notes: 'Executive suite order', userName: 'Inventory Manager', transactionDate: new Date(Date.now() - 86400000).toISOString() },
            { id: 8, productId: 4, productSku: 'SKU-ELEC-004', productName: 'Smart 4K Web Camera with Mic', transactionType: 'STOCK_OUT', quantity: 15, unitPrice: 64.99, referenceNo: 'INV-2026-104', notes: 'Clearance order fulfilled', userName: 'Inventory Manager', transactionDate: new Date(Date.now() - 172800000).toISOString() },
            { id: 6, productId: 3, productSku: 'SKU-ELEC-003', productName: 'Portable SSD 1TB USB 3.2', transactionType: 'STOCK_OUT', quantity: 16, unitPrice: 89.99, referenceNo: 'INV-2026-103', notes: 'Department store shipment', userName: 'Inventory Manager', transactionDate: new Date(Date.now() - 259200000).toISOString() },
            { id: 13, productId: 8, productSku: 'SKU-FURN-002', productName: 'Electric Dual-Motor Standing Desk (140x70cm)', transactionType: 'STOCK_IN', quantity: 10, unitPrice: 210.00, referenceNo: 'PO-2026-007', notes: 'Standing desks shipment', userName: 'System Administrator', transactionDate: new Date(Date.now() - 604800000).toISOString() }
        ]
    };

    saveCloudStore(initialData);
    return initialData;
}

function saveCloudStore(store) {
    localStorage.setItem('SMART_INVENTORY_STORE', JSON.stringify(store));
}

async function handleCloudMockApi(url, options) {
    const store = getCloudStore();
    const method = (options.method || 'GET').toUpperCase();
    const parsedUrl = new URL(url, window.location.origin);
    const path = parsedUrl.pathname.replace(/^\/?(webapp\/)?/, '');
    const params = parsedUrl.searchParams;

    // 1. AUTH API
    if (path.includes('api/auth')) {
        const action = params.get('action');
        if (action === 'logout') {
            localStorage.removeItem('SMART_INVENTORY_SESSION');
            return { success: true, message: 'Logged out successfully' };
        }

        if (method === 'POST') {
            const body = options.body ? JSON.parse(options.body) : {};
            const user = store.users.find(u => u.username.toLowerCase() === (body.username || '').toLowerCase().trim());
            if (user && user.password === (body.password || '').trim()) {
                const sessionUser = { id: user.id, username: user.username, fullName: user.fullName, role: user.role };
                localStorage.setItem('SMART_INVENTORY_SESSION', JSON.stringify(sessionUser));
                return { success: true, message: 'Login successful', data: sessionUser };
            }
            throw new Error('Invalid username or password.');
        }

        // GET session
        const sessionRaw = localStorage.getItem('SMART_INVENTORY_SESSION');
        if (sessionRaw) {
            return { success: true, message: 'Authenticated', data: JSON.parse(sessionRaw) };
        }
        throw new Error('Session expired. Please log in.');
    }

    // 2. DASHBOARD API
    if (path.includes('api/dashboard')) {
        const totalProducts = store.products.length;
        let totalUnits = 0;
        let totalCost = 0;
        let totalRetail = 0;
        let lowStockCount = 0;
        let outOfStockCount = 0;

        const catMap = {};
        store.categories.forEach(c => { catMap[c.id] = { categoryName: c.name, productCount: 0, totalStock: 0, totalValue: 0 }; });

        store.products.forEach(p => {
            totalUnits += p.quantity;
            totalCost += p.quantity * p.costPrice;
            totalRetail += p.quantity * p.sellingPrice;
            if (p.quantity <= 0) outOfStockCount++;
            else if (p.quantity <= p.minStockLevel) lowStockCount++;

            if (catMap[p.categoryId]) {
                catMap[p.categoryId].productCount++;
                catMap[p.categoryId].totalStock += p.quantity;
                catMap[p.categoryId].totalValue += p.quantity * p.costPrice;
            }
        });

        const lowStockProducts = store.products
            .filter(p => p.quantity <= p.minStockLevel)
            .sort((a, b) => a.quantity - b.quantity);

        return {
            success: true,
            data: {
                totalProducts,
                totalStockUnits: totalUnits,
                totalInventoryValue: totalCost,
                totalRetailValue: totalRetail,
                lowStockCount,
                outOfStockCount,
                totalCategories: store.categories.length,
                totalSuppliers: store.suppliers.length,
                categoryStats: Object.values(catMap),
                lowStockProducts,
                recentTransactions: store.transactions.slice(0, 8)
            }
        };
    }

    // 3. PRODUCTS API
    if (path.includes('api/products')) {
        const id = params.get('id');
        const action = params.get('action');

        if (method === 'GET') {
            if (id) {
                const prod = store.products.find(p => p.id === parseInt(id));
                if (prod) return { success: true, data: prod };
                throw new Error('Product not found');
            }
            if (action === 'low-stock') {
                return { success: true, data: store.products.filter(p => p.quantity <= p.minStockLevel) };
            }
            const query = (params.get('query') || '').toLowerCase().trim();
            const catId = params.get('categoryId');
            const status = params.get('stockStatus');

            let list = [...store.products];
            if (query) {
                list = list.filter(p => p.name.toLowerCase().includes(query) || p.sku.toLowerCase().includes(query));
            }
            if (catId && catId !== 'ALL') {
                list = list.filter(p => p.categoryId === parseInt(catId));
            }
            if (status === 'OUT_OF_STOCK') list = list.filter(p => p.quantity <= 0);
            else if (status === 'LOW_STOCK') list = list.filter(p => p.quantity > 0 && p.quantity <= p.minStockLevel);
            else if (status === 'IN_STOCK') list = list.filter(p => p.quantity > p.minStockLevel);

            return { success: true, data: list };
        }

        if (method === 'POST') {
            const body = JSON.parse(options.body);
            const cat = store.categories.find(c => c.id === body.categoryId);
            const supp = store.suppliers.find(s => s.id === body.supplierId);
            const newProd = {
                id: Date.now(),
                sku: body.sku,
                name: body.name,
                categoryId: body.categoryId,
                categoryName: cat ? cat.name : 'General',
                supplierId: body.supplierId || null,
                supplierName: supp ? supp.name : 'N/A',
                costPrice: body.costPrice || 0,
                sellingPrice: body.sellingPrice || 0,
                quantity: body.quantity || 0,
                minStockLevel: body.minStockLevel || 10,
                description: body.description || ''
            };
            store.products.unshift(newProd);
            saveCloudStore(store);
            return { success: true, message: 'Product created successfully', data: newProd };
        }

        if (method === 'PUT') {
            const body = JSON.parse(options.body);
            const idx = store.products.findIndex(p => p.id === body.id);
            if (idx >= 0) {
                const cat = store.categories.find(c => c.id === body.categoryId);
                const supp = store.suppliers.find(s => s.id === body.supplierId);
                store.products[idx] = {
                    ...store.products[idx],
                    ...body,
                    categoryName: cat ? cat.name : store.products[idx].categoryName,
                    supplierName: supp ? supp.name : store.products[idx].supplierName
                };
                saveCloudStore(store);
                return { success: true, message: 'Product updated', data: store.products[idx] };
            }
            throw new Error('Product not found');
        }

        if (method === 'DELETE') {
            const deleteId = parseInt(id || (options.body ? JSON.parse(options.body).id : 0));
            store.products = store.products.filter(p => p.id !== deleteId);
            saveCloudStore(store);
            return { success: true, message: 'Product deleted' };
        }
    }

    // 4. CATEGORIES API
    if (path.includes('api/categories')) {
        if (method === 'GET') {
            const list = store.categories.map(c => ({
                ...c,
                productCount: store.products.filter(p => p.categoryId === c.id).length
            }));
            return { success: true, data: list };
        }
        if (method === 'POST') {
            const body = JSON.parse(options.body);
            const newCat = { id: Date.now(), name: body.name, description: body.description };
            store.categories.push(newCat);
            saveCloudStore(store);
            return { success: true, message: 'Category created', data: newCat };
        }
        if (method === 'PUT') {
            const body = JSON.parse(options.body);
            const idx = store.categories.findIndex(c => c.id === body.id);
            if (idx >= 0) {
                store.categories[idx] = { ...store.categories[idx], ...body };
                saveCloudStore(store);
                return { success: true, message: 'Category updated', data: store.categories[idx] };
            }
        }
        if (method === 'DELETE') {
            const delId = parseInt(params.get('id') || JSON.parse(options.body).id);
            store.categories = store.categories.filter(c => c.id !== delId);
            saveCloudStore(store);
            return { success: true, message: 'Category deleted' };
        }
    }

    // 5. SUPPLIERS API
    if (path.includes('api/suppliers')) {
        if (method === 'GET') {
            const list = store.suppliers.map(s => ({
                ...s,
                productCount: store.products.filter(p => p.supplierId === s.id).length
            }));
            return { success: true, data: list };
        }
        if (method === 'POST') {
            const body = JSON.parse(options.body);
            const newSupp = { id: Date.now(), ...body };
            store.suppliers.push(newSupp);
            saveCloudStore(store);
            return { success: true, message: 'Supplier added', data: newSupp };
        }
        if (method === 'PUT') {
            const body = JSON.parse(options.body);
            const idx = store.suppliers.findIndex(s => s.id === body.id);
            if (idx >= 0) {
                store.suppliers[idx] = { ...store.suppliers[idx], ...body };
                saveCloudStore(store);
                return { success: true, message: 'Supplier updated', data: store.suppliers[idx] };
            }
        }
        if (method === 'DELETE') {
            const delId = parseInt(params.get('id') || JSON.parse(options.body).id);
            store.suppliers = store.suppliers.filter(s => s.id !== delId);
            saveCloudStore(store);
            return { success: true, message: 'Supplier deleted' };
        }
    }

    // 6. STOCK MOVEMENTS API
    if (path.includes('api/stock')) {
        if (method === 'GET') {
            const type = params.get('type');
            const pid = params.get('productId');
            let list = [...store.transactions];
            if (type && type !== 'ALL') list = list.filter(t => t.transactionType === type);
            if (pid && pid !== 'ALL') list = list.filter(t => t.productId === parseInt(pid));
            return { success: true, data: list };
        }

        if (method === 'POST') {
            const body = JSON.parse(options.body);
            const prod = store.products.find(p => p.id === body.productId);
            if (!prod) throw new Error('Product not found');

            const isStockIn = body.action === 'in';
            if (!isStockIn && prod.quantity < body.quantity) {
                throw new Error(`Insufficient stock for '${prod.name}'. Available: ${prod.quantity}`);
            }

            prod.quantity = isStockIn ? (prod.quantity + body.quantity) : (prod.quantity - body.quantity);

            const sessionUser = JSON.parse(localStorage.getItem('SMART_INVENTORY_SESSION') || '{}');
            const newTrans = {
                id: Date.now(),
                productId: prod.id,
                productSku: prod.sku,
                productName: prod.name,
                transactionType: isStockIn ? 'STOCK_IN' : 'STOCK_OUT',
                quantity: body.quantity,
                unitPrice: body.unitPrice || (isStockIn ? prod.costPrice : prod.sellingPrice),
                referenceNo: body.referenceNo || 'N/A',
                notes: body.notes || '',
                userName: sessionUser.fullName || sessionUser.username || 'System',
                transactionDate: new Date().toISOString()
            };

            store.transactions.unshift(newTrans);
            saveCloudStore(store);
            return { success: true, message: isStockIn ? 'Stock In recorded successfully' : 'Stock Out recorded successfully' };
        }
    }

    // 7. REPORTS API
    if (path.includes('api/reports')) {
        const type = params.get('type') || 'valuation';
        if (type === 'valuation') {
            let totalCost = 0;
            let totalRetail = 0;
            let totalUnits = 0;
            store.products.forEach(p => {
                totalUnits += p.quantity;
                totalCost += p.quantity * p.costPrice;
                totalRetail += p.quantity * p.sellingPrice;
            });
            return {
                success: true,
                data: {
                    reportType: 'valuation',
                    generatedAt: new Date().toLocaleString(),
                    products: store.products,
                    totalProducts: store.products.length,
                    totalUnits,
                    totalCostValuation: totalCost,
                    totalRetailValuation: totalRetail,
                    estimatedMargin: totalRetail - totalCost
                }
            };
        }
        if (type === 'lowstock') {
            const low = store.products.filter(p => p.quantity <= p.minStockLevel);
            return {
                success: true,
                data: {
                    reportType: 'lowstock',
                    generatedAt: new Date().toLocaleString(),
                    products: low,
                    lowStockCount: low.length
                }
            };
        }
        if (type === 'transactions') {
            return {
                success: true,
                data: {
                    reportType: 'transactions',
                    generatedAt: new Date().toLocaleString(),
                    transactions: store.transactions,
                    totalCount: store.transactions.length
                }
            };
        }
    }

    return { success: true, message: 'OK', data: [] };
}
