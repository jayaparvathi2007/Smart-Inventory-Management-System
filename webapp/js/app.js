/**
 * SMART INVENTORY MANAGEMENT SYSTEM - CORE JAVASCRIPT
 */

// Universal API fetch helper
async function fetchAPI(url, options = {}) {
    const defaultHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    options.headers = {
        ...defaultHeaders,
        ...(options.headers || {})
    };

    try {
        const response = await fetch(url, options);

        // Check if unauthorized
        if (response.status === 401) {
            if (!window.location.pathname.endsWith('login.html')) {
                window.location.href = 'login.html';
            }
            throw new Error('Session expired. Please log in.');
        }

        const data = await response.json();
        if (!response.ok || data.success === false) {
            throw new Error(data.message || `HTTP Error ${response.status}`);
        }
        return data;
    } catch (err) {
        console.error('API Error:', err);
        throw err;
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
    // If on login page, don't check or redirect if already authenticated
    const isLoginPage = window.location.pathname.endsWith('login.html');

    try {
        const res = await fetchAPI('api/auth');
        if (res.success && res.data) {
            const user = res.data;
            // Update sidebar user display if elements exist
            const userNameEl = document.getElementById('sidebar-user-name');
            const userRoleEl = document.getElementById('sidebar-user-role');
            const userAvatarEl = document.getElementById('sidebar-user-avatar');

            if (userNameEl) userNameEl.textContent = user.fullName || user.username;
            if (userRoleEl) userRoleEl.textContent = user.role || 'Staff';
            if (userAvatarEl) userAvatarEl.textContent = (user.fullName || user.username || 'U').charAt(0).toUpperCase();

            if (isLoginPage) {
                window.location.href = 'index.html';
            }
        }
    } catch (err) {
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

// Run auth check on page load
document.addEventListener('DOMContentLoaded', () => {
    initAuthCheck();
    setupMobileMenu();
});
