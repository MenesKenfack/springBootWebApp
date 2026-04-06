// Purchases JavaScript
let allPurchases = [];
let filteredPurchases = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute(['ROLE_CLIENT'])) return;
    
    setupNavigation();
    setupFilters();
    loadPurchases();
});

function setupFilters() {
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const dateFilter = document.getElementById('dateFilter');
    
    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            applyFilters();
        }, 300));
    }
    
    if (statusFilter) {
        statusFilter.addEventListener('change', () => applyFilters());
    }
    
    if (dateFilter) {
        dateFilter.addEventListener('change', () => applyFilters());
    }
}

async function loadPurchases() {
    try {
        const [historyResponse, recentResponse] = await Promise.all([
            purchaseAPI.getHistory().catch(() => ({ data: [] })),
            purchaseAPI.getRecent().catch(() => ({ data: [] }))
        ]);
        
        allPurchases = historyResponse.data || [];
        if (allPurchases.length === 0) {
            allPurchases = recentResponse.data || [];
        }
        
        applyFilters();
        
    } catch (error) {
        Toast.error('Failed to load purchase history. Please try again.');
    }
}

function applyFilters() {
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const dateFilter = document.getElementById('dateFilter');
    
    const searchTerm = searchInput?.value.toLowerCase() || '';
    const status = statusFilter?.value || '';
    const dateRange = dateFilter?.value || '';
    
    filteredPurchases = allPurchases.filter(purchase => {
        let matches = true;
        
        // Search filter
        if (searchTerm) {
            const resourceTitle = (purchase.resourceTitle || '').toLowerCase();
            const invoice = (purchase.paymentInvoice || purchase.transactionReference || '').toLowerCase();
            matches = matches && (resourceTitle.includes(searchTerm) || invoice.includes(searchTerm));
        }
        
        // Status filter
        if (status) {
            matches = matches && purchase.status === status;
        }
        
        // Date filter
        if (dateRange) {
            const purchaseDate = new Date(purchase.createdAt);
            const now = new Date();
            matches = matches && isDateInRange(purchaseDate, dateRange, now);
        }
        
        return matches;
    });
    
    updateStats(filteredPurchases);
    renderPurchasesTable(filteredPurchases);
}

function isDateInRange(date, range, now) {
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    switch (range) {
        case 'today':
            return date >= startOfDay;
        case 'week':
            const weekAgo = new Date(startOfDay);
            weekAgo.setDate(weekAgo.getDate() - 7);
            return date >= weekAgo;
        case 'month':
            return date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
        case 'year':
            return date.getFullYear() === now.getFullYear();
        default:
            return true;
    }
}

function updateStats(purchases) {
    const totalEl = document.getElementById('totalPurchases');
    const spentEl = document.getElementById('totalSpent');
    
    if (totalEl) {
        totalEl.textContent = purchases.length;
    }
    
    if (spentEl) {
        const total = purchases.reduce((sum, p) => sum + (parseFloat(p.amount) || 0), 0);
        spentEl.textContent = total.toLocaleString() + ' XAF';
    }
}

function renderPurchasesTable(purchases) {
    const tbody = document.getElementById('purchasesTableBody');
    if (!tbody) return;
    
    if (purchases.length === 0) {
        const hasFilters = document.getElementById('searchInput')?.value || 
                          document.getElementById('statusFilter')?.value ||
                          document.getElementById('dateFilter')?.value;
        
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 2rem;">
                    <i class="fas ${hasFilters ? 'fa-search' : 'fa-shopping-bag'}" style="font-size: 2rem; color: var(--text-light); margin-bottom: 1rem;"></i>
                    <p>${hasFilters ? 'No purchases match your filters' : 'No purchases yet'}</p>
                    ${hasFilters ? `
                        <button onclick="clearFilters()" class="btn btn-secondary" style="margin-top: 1rem;">
                            <i class="fas fa-times"></i> Clear Filters
                        </button>
                    ` : `
                        <a href="/resources.html" class="btn btn-primary" style="margin-top: 1rem;">Browse Resources</a>
                    `}
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = purchases.map(purchase => `
        <tr>
            <td>${purchase.paymentInvoice || purchase.transactionReference || 'N/A'}</td>
            <td>${escapeHtml(purchase.resourceTitle || 'Unknown Resource')}</td>
            <td>${purchase.amount ? purchase.amount.toLocaleString() : '0'} XAF</td>
            <td><span class="status-badge ${getStatusClass(purchase.status)}">${purchase.status}</span></td>
            <td>${formatDate(purchase.createdAt)}</td>
            <td>
                ${purchase.status === 'PAID' ? `
                    <button class="btn btn-sm btn-primary" onclick="accessResource('${purchase.resourceTitle}')">
                        <i class="fas fa-book-open"></i> Access
                    </button>
                ` : `
                    <button class="btn btn-sm btn-secondary" onclick="retryPayment(${purchase.paymentID})">
                        <i class="fas fa-redo"></i> Retry
                    </button>
                `}
            </td>
        </tr>
    `).join('');
}

function clearFilters() {
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const dateFilter = document.getElementById('dateFilter');
    
    if (searchInput) searchInput.value = '';
    if (statusFilter) statusFilter.value = '';
    if (dateFilter) dateFilter.value = '';
    
    applyFilters();
}

function getStatusClass(status) {
    switch (status) {
        case 'PAID': return 'status-success';
        case 'PENDING': return 'status-pending';
        case 'FAILED': return 'status-error';
        default: return '';
    }
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function accessResource(resourceTitle) {
    Toast.success(`Accessing ${escapeHtml(resourceTitle)}...`);
}

async function retryPayment(paymentId) {
    try {
        const response = await purchaseAPI.checkout(paymentId);
        if (response.success && response.data.checkoutUrl) {
            window.location.href = response.data.checkoutUrl;
        } else {
            Toast.success('Redirecting to payment...');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to retry payment');
    }
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
