// API Configuration
const API_BASE_URL = window.location.origin + '/api';

// Token Management - Using sessionStorage so tokens clear when browser closes
const TokenManager = {
    getToken() {
        return sessionStorage.getItem('token');
    },
    
    getRefreshToken() {
        return sessionStorage.getItem('refreshToken');
    },
    
    setToken(token, refreshToken) {
        sessionStorage.setItem('token', token);
        if (refreshToken) {
            sessionStorage.setItem('refreshToken', refreshToken);
        }
    },
    
    clearTokens() {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('refreshToken');
        sessionStorage.removeItem('user');
    },
    
    isAuthenticated() {
        return !!this.getToken();
    }
};

// User Storage - Using sessionStorage so user data clears when browser closes
const UserStorage = {
    getUser() {
        const user = sessionStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },
    
    setUser(user) {
        sessionStorage.setItem('user', JSON.stringify(user));
    },
    
    getRole() {
        const user = this.getUser();
        return user ? user.role : null;
    },
    
    getTier() {
        const user = this.getUser();
        return user ? user.tier : null;
    }
};

// API Client
const apiClient = {
    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const token = TokenManager.getToken();
        
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                ...(token && { 'Authorization': `Bearer ${token}` })
            }
        };
        
        const config = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        };
        
        if (config.body && typeof config.body === 'object') {
            config.body = JSON.stringify(config.body);
        }
        
        try {
            const response = await fetch(url, config);
            
            if (response.status === 401) {
                TokenManager.clearTokens();
                window.location.href = '/login';
                return;
            }
            
            // Check if response is empty
            const contentType = response.headers.get('content-type');
            const contentLength = response.headers.get('content-length');
            
            // If no content or not JSON, handle accordingly
            if (!contentType || !contentType.includes('application/json') || contentLength === '0') {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status} - ${response.statusText}`);
                }
                // Return empty successful response
                return { success: true };
            }
            
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.message || `HTTP error! status: ${response.status}`);
            }
            
            return data;
        } catch (error) {
            // If it's a syntax error (JSON parse error), provide better message
            if (error instanceof SyntaxError) {
                throw new Error('Server returned invalid response. Please try again later.');
            }
            throw error;
        }
    },
    
    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },
    
    post(endpoint, body) {
        return this.request(endpoint, { method: 'POST', body });
    },
    
    put(endpoint, body) {
        return this.request(endpoint, { method: 'PUT', body });
    },
    
    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
};

// Authentication API
const authAPI = {
    async register(data) {
        return apiClient.post('/auth/register', data);
    },
    
    async login(data) {
        return apiClient.post('/auth/login', data);
    },
    
    async verifyEmail(data) {
        return apiClient.post('/auth/verify-email', data);
    },
    
    async resendVerification(email) {
        return apiClient.post(`/auth/resend-verification?email=${encodeURIComponent(email)}`);
    },
    
    async getCurrentUser() {
        return apiClient.get('/auth/me');
    }
};

// Resources API
const resourcesAPI = {
    async search(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        return apiClient.get(`/resources/search?${queryString}`);
    },
    
    async getById(id) {
        return apiClient.get(`/resources/${id}`);
    },
    
    async getContent(id) {
        return apiClient.get(`/resources/${id}/content`);
    },
    
    async create(data) {
        return apiClient.post('/resources', data);
    },
    
    async update(id, data) {
        return apiClient.put(`/resources/${id}`, data);
    },
    
    async delete(id) {
        return apiClient.delete(`/resources/${id}`);
    },
    
    async getCatalogs() {
        return apiClient.get('/resources/catalogs');
    },
    
    async createCatalog(name) {
        return apiClient.post(`/resources/catalogs?name=${encodeURIComponent(name)}`);
    },
    
    async updateCatalog(id, name) {
        return apiClient.put(`/resources/catalogs/${id}?name=${encodeURIComponent(name)}`);
    },
    
    async deleteCatalog(id) {
        return apiClient.delete(`/resources/catalogs/${id}`);
    },
    
    async getRecent() {
        return apiClient.get('/resources/recent');
    },
    
    async createWithFiles(formData) {
        const url = `${API_BASE_URL}/resources/with-files`;
        const token = TokenManager.getToken();
        
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });
        
        if (response.status === 401) {
            TokenManager.clearTokens();
            window.location.href = '/login';
            return;
        }
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP error! status: ${response.status}`);
        }
        
        return data;
    },
    
    async updateWithFiles(id, formData) {
        const url = `${API_BASE_URL}/resources/${id}/with-files`;
        const token = TokenManager.getToken();
        
        const response = await fetch(url, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });
        
        if (response.status === 401) {
            TokenManager.clearTokens();
            window.location.href = '/login';
            return;
        }
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || `HTTP error! status: ${response.status}`);
        }
        
        return data;
    }
};

// Purchase API
const purchaseAPI = {
    async initiate(resourceId) {
        return apiClient.post('/purchase/initiate', { resourceId });
    },
    
    async checkout(paymentId) {
        return apiClient.post(`/purchase/checkout?paymentId=${paymentId}`);
    },
    
    async getHistory() {
        return apiClient.get('/purchase/history');
    },
    
    async getRecent() {
        return apiClient.get('/purchase/recent');
    }
};

// My List API - Favorites, Reading Progress, and Ratings
const myListAPI = {
    // Favorites
    async getFavorites() {
        return apiClient.get('/my-list/favorites');
    },
    
    async addFavorite(resourceId) {
        return apiClient.post('/my-list/favorites', { resourceId });
    },
    
    async removeFavorite(resourceId) {
        return apiClient.delete(`/my-list/favorites/${resourceId}`);
    },
    
    async isFavorite(resourceId) {
        return apiClient.get(`/my-list/favorites/${resourceId}/check`);
    },
    
    // Reading Progress
    async getReadingProgress() {
        return apiClient.get('/my-list/reading');
    },
    
    async updateReadingProgress(resourceId, progress) {
        return apiClient.put('/my-list/reading', { resourceId, progress });
    },
    
    async getResourceContent(resourceId) {
        return apiClient.get(`/my-list/reading/${resourceId}/content`);
    },
    
    // Ratings
    async getMyRatings() {
        return apiClient.get('/my-list/ratings');
    },
    
    async rateResource(resourceId, rating, comment = '') {
        return apiClient.post('/my-list/ratings', { resourceId, rating, comment });
    },
    
    async updateRating(resourceId, rating, comment = '') {
        return apiClient.put('/my-list/ratings', { resourceId, rating, comment });
    },
    
    async getMyRating(resourceId) {
        return apiClient.get(`/my-list/ratings/${resourceId}`);
    },
    
    // Get complete my list data
    async getMyList() {
        return apiClient.get('/my-list');
    }
};

// Activity Log API
const activityAPI = {
    async getRecent(limit = 10) {
        return apiClient.get(`/activities/recent?limit=${limit}`);
    },
    
    async getSinceLastLogin() {
        return apiClient.get('/activities/since-last-login');
    },
    
    async getSessionActivities(sessionId) {
        return apiClient.get(`/activities/session/${sessionId}`);
    }
};

// Analytics API
const analyticsAPI = {
    async generateReport(dateRange = 'LAST_30_DAYS', reportType = 'SUMMARY') {
        return apiClient.get(`/reports/generate?dateRange=${dateRange}&reportType=${reportType}`);
    }
};

// Admin API
const adminAPI = {
    async getUsers(role) {
        const params = role ? `?role=${role}` : '';
        return apiClient.get(`/admin/users${params}`);
    },
    
    async getUserById(id) {
        return apiClient.get(`/admin/users/${id}`);
    },
    
    async createLibrarian(data) {
        return apiClient.post('/admin/librarians', data);
    },
    
    async createManager(data) {
        return apiClient.post('/admin/managers', data);
    },
    
    async updateUser(id, data) {
        return apiClient.put(`/admin/users/${id}`, data);
    },
    
    async changeUserRole(id, role) {
        return apiClient.put(`/admin/users/${id}/role?role=${role}`);
    },
    
    async changeUserTier(id, tier) {
        return apiClient.put(`/admin/users/${id}/tier?tier=${tier}`);
    },
    
    async deleteUser(id) {
        return apiClient.delete(`/admin/users/${id}`);
    },
    
    async getBackups() {
        return apiClient.get('/admin/backups');
    },
    
    async createBackup(managerId, name, type) {
        return apiClient.post(`/admin/backups?managerId=${managerId}&backupName=${name}&backupType=${type}`);
    }
};

// Reports API
const reportsAPI = {
    async getAll() {
        return apiClient.get('/reports');
    },
    
    async getById(id) {
        return apiClient.get(`/reports/${id}`);
    },
    
    async create(data) {
        return apiClient.post('/reports', data);
    },
    
    async update(id, data) {
        return apiClient.put(`/reports/${id}`, data);
    },
    
    async delete(id) {
        return apiClient.delete(`/reports/${id}`);
    },
    
    async regenerate(id) {
        return apiClient.post(`/reports/${id}/regenerate`);
    },
    
    async download(id) {
        return apiClient.get(`/reports/${id}/download`);
    }
};

// Toast Notification System
const Toast = {
    container: null,
    
    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },
    
    show(message, type = 'success', duration = 3000) {
        this.init();
        
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
            <span>${message}</span>
        `;
        
        this.container.appendChild(toast);
        
        setTimeout(() => {
            toast.style.animation = 'slideIn 0.3s ease reverse';
            setTimeout(() => toast.remove(), 300);
        }, duration);
    },
    
    success(message) {
        this.show(message, 'success');
    },
    
    error(message) {
        this.show(message, 'error');
    },
    
    warning(message) {
        this.show(message, 'warning');
    }
};

// Form Validation
const FormValidator = {
    validateEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return re.test(email);
    },
    
    validatePassword(password) {
        // Min 8 chars, 1 uppercase, 1 lowercase, 1 digit
        const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
        return re.test(password);
    },
    
    showError(fieldId, message) {
        const errorElement = document.getElementById(`${fieldId}Error`);
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.classList.add('visible');
        }
        const field = document.getElementById(fieldId);
        if (field) {
            field.style.borderColor = 'var(--error-color)';
        }
    },
    
    clearError(fieldId) {
        const errorElement = document.getElementById(`${fieldId}Error`);
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.classList.remove('visible');
        }
        const field = document.getElementById(fieldId);
        if (field) {
            field.style.borderColor = '';
        }
    },
    
    clearAllErrors() {
        document.querySelectorAll('.error-message').forEach(el => {
            el.textContent = '';
            el.classList.remove('visible');
        });
        document.querySelectorAll('input, select, textarea').forEach(el => {
            el.style.borderColor = '';
        });
    }
};

// Navigation Protection
function protectRoute(allowedRoles = []) {
    if (!TokenManager.isAuthenticated()) {
        window.location.href = '/login';
        return false;
    }
    
    if (allowedRoles.length > 0) {
        const userRole = UserStorage.getRole();
        if (!allowedRoles.includes(userRole)) {
            window.location.href = '/dashboard';
            return false;
        }
    }
    
    return true;
}

// Setup Navigation based on Role
function setupNavigation() {
    const role = UserStorage.getRole();
    
    // Show/hide nav items based on role
    const navItems = {
        'resourcesNav': ['ROLE_CLIENT', 'ROLE_LIBRARIAN'],
        'purchasesNav': ['ROLE_CLIENT'],
        'myListNav': ['ROLE_CLIENT'],
        'analyticsNav': ['ROLE_MANAGER'],
        'manageResourcesNav': ['ROLE_LIBRARIAN'],
        'catalogsNav': ['ROLE_LIBRARIAN'],
        'usersNav': ['ROLE_MANAGER'],
        'termsNav': ['ROLE_MANAGER'],
        'backupNav': ['ROLE_MANAGER']
    };
    
    for (const [id, allowedRoles] of Object.entries(navItems)) {
        const el = document.getElementById(id);
        if (el) {
            el.style.display = allowedRoles.includes(role) ? 'flex' : 'none';
        }
    }
    
    // Update user info
    const user = UserStorage.getUser();
    if (user) {
        const userNameEl = document.getElementById('userName');
        if (userNameEl) {
            userNameEl.textContent = user.firstName || user.username;
        }
    }
}

// Logout Function
async function logout() {
    try {
        // Call backend logout to log the activity
        await apiClient.post('/auth/logout', {});
    } catch (error) {
        console.error('Logout API call failed:', error);
    } finally {
        TokenManager.clearTokens();
        window.location.href = '/login';
    }
}

// Sidebar Toggle
function initSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebarToggle');
    
    // Create overlay element if it doesn't exist
    let sidebarOverlay = document.querySelector('.sidebar-overlay');
    if (!sidebarOverlay) {
        sidebarOverlay = document.createElement('div');
        sidebarOverlay.className = 'sidebar-overlay';
        sidebarOverlay.id = 'sidebarOverlay';
        document.body.appendChild(sidebarOverlay);
    }
    
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('active');
            sidebar.classList.toggle('open');
            sidebarOverlay.classList.toggle('active');
        });
        
        // Close sidebar when clicking on overlay
        sidebarOverlay.addEventListener('click', () => {
            sidebar.classList.remove('active');
            sidebar.classList.remove('open');
            sidebarOverlay.classList.remove('active');
        });
        
        // Close sidebar when clicking a nav item on mobile
        const navItems = sidebar.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', () => {
                if (window.innerWidth <= 768) {
                    sidebar.classList.remove('active');
                    sidebar.classList.remove('open');
                    sidebarOverlay.classList.remove('active');
                }
            });
        });
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
});
