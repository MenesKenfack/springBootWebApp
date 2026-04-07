// Users Management JavaScript
let users = [];
let currentUserId = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute(['ROLE_MANAGER'])) return;
    
    setupNavigation();
    loadUsers();
    setupFilters();
    
    // Setup form
    const userForm = document.getElementById('userForm');
    if (userForm) {
        userForm.addEventListener('submit', handleUserSubmit);
    }
});

function setupFilters() {
    const searchInput = document.getElementById('searchInput');
    const roleFilter = document.getElementById('roleFilter');
    const tierFilter = document.getElementById('tierFilter');
    
    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => loadUsers(), 300));
    }
    
    if (roleFilter) {
        roleFilter.addEventListener('change', () => loadUsers());
    }
    
    if (tierFilter) {
        tierFilter.addEventListener('change', () => loadUsers());
    }
}

async function loadUsers() {
    const roleFilter = document.getElementById('roleFilter');
    const params = roleFilter?.value ? { role: roleFilter.value } : {};
    
    try {
        const response = await adminAPI.getUsers(params.role);
        
        if (response.success) {
            // Filter by search and tier
            const searchInput = document.getElementById('searchInput');
            const tierFilter = document.getElementById('tierFilter');
            
            users = response.data.filter(user => {
                let matches = true;
                
                if (searchInput?.value) {
                    const search = searchInput.value.toLowerCase();
                    matches = matches && (
                        user.firstName?.toLowerCase().includes(search) ||
                        user.lastName?.toLowerCase().includes(search) ||
                        user.email?.toLowerCase().includes(search) ||
                        user.username?.toLowerCase().includes(search)
                    );
                }
                
                if (tierFilter?.value) {
                    matches = matches && user.userTier === tierFilter.value;
                }
                
                return matches;
            });
            
            renderUsersTable();
        } else {
            Toast.error(response.message || 'Failed to load users');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to load users');
    }
}

function renderUsersTable() {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;
    
    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">No users found</td></tr>';
        return;
    }
    
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.userID}</td>
            <td>${escapeHtml(user.firstName + ' ' + user.lastName)}</td>
            <td>${escapeHtml(user.email)}</td>
            <td><span class="status-badge">${user.role?.replace('ROLE_', '')}</span></td>
            <td><span class="status-badge ${user.userTier === 'PREMIUM' ? 'status-success' : ''}">${user.userTier}</span></td>
            <td><span class="status-badge ${user.status ? 'status-success' : 'status-error'}">${user.status ? 'Active' : 'Inactive'}</span></td>
            <td class="actions-cell">
                <div class="action-buttons">
                    <button class="btn btn-sm btn-view" onclick="viewUser(${user.userID})" title="View User">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-edit" onclick="editUser(${user.userID})" title="Edit User">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-delete" onclick="deleteUser(${user.userID})" title="Delete User">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function viewUser(userId) {
    const user = users.find(u => u.userID === userId);
    if (!user) return;
    
    // Create and show user details modal
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.style.display = 'flex';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 500px;">
            <div class="modal-header">
                <h3>User Details</h3>
                <button type="button" class="close-modal" onclick="this.closest('.modal').remove()">&times;</button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label>Username</label>
                    <input type="text" value="${escapeHtml(user.username)}" readonly class="form-control">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>First Name</label>
                        <input type="text" value="${escapeHtml(user.firstName)}" readonly class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Last Name</label>
                        <input type="text" value="${escapeHtml(user.lastName)}" readonly class="form-control">
                    </div>
                </div>
                <div class="form-group">
                    <label>Email</label>
                    <input type="text" value="${escapeHtml(user.email)}" readonly class="form-control">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Role</label>
                        <input type="text" value="${user.role?.replace('ROLE_', '')}" readonly class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Tier</label>
                        <input type="text" value="${user.userTier}" readonly class="form-control">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Status</label>
                        <input type="text" value="${user.status ? 'Active' : 'Inactive'}" readonly class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Verified</label>
                        <input type="text" value="${user.verified ? 'Yes' : 'No'}" readonly class="form-control">
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="this.closest('.modal').remove()">Close</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function editUser(userId) {
    const user = users.find(u => u.userID === userId);
    if (!user) return;
    
    currentUserId = userId;
    document.getElementById('modalTitle').textContent = 'Edit User';
    document.getElementById('userType').value = user.role;
    
    // Fill form
    document.getElementById('firstName').value = user.firstName;
    document.getElementById('lastName').value = user.lastName;
    document.getElementById('username').value = user.username;
    document.getElementById('dateOfBirth').value = user.dateOfBirth;
    document.getElementById('email').value = user.email;
    document.getElementById('password').value = '';
    document.getElementById('confirmPassword').value = '';
    
    // Make password optional when editing
    document.getElementById('password').removeAttribute('required');
    document.getElementById('confirmPassword').removeAttribute('required');
    document.getElementById('passwordRequired').textContent = '';
    document.getElementById('confirmPasswordRequired').textContent = '';
    
    document.getElementById('userModal').style.display = 'flex';
}

function openAddLibrarianModal() {
    currentUserId = null;
    document.getElementById('modalTitle').textContent = 'Add Librarian';
    document.getElementById('userType').value = 'ROLE_LIBRARIAN';
    document.getElementById('userForm').reset();
    
    // Make password required for new users
    document.getElementById('password').setAttribute('required', '');
    document.getElementById('confirmPassword').setAttribute('required', '');
    document.getElementById('passwordRequired').textContent = '*';
    document.getElementById('confirmPasswordRequired').textContent = '*';
    
    document.getElementById('userModal').style.display = 'flex';
}

function openAddManagerModal() {
    currentUserId = null;
    document.getElementById('modalTitle').textContent = 'Add Manager';
    document.getElementById('userType').value = 'ROLE_MANAGER';
    document.getElementById('userForm').reset();
    
    // Make password required for new users
    document.getElementById('password').setAttribute('required', '');
    document.getElementById('confirmPassword').setAttribute('required', '');
    document.getElementById('passwordRequired').textContent = '*';
    document.getElementById('confirmPasswordRequired').textContent = '*';
    
    document.getElementById('userModal').style.display = 'flex';
}

async function handleUserSubmit(e) {
    e.preventDefault();
    
    const data = {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        username: document.getElementById('username').value,
        dateOfBirth: document.getElementById('dateOfBirth').value,
        email: document.getElementById('email').value,
        password: document.getElementById('password').value,
        confirmPassword: document.getElementById('confirmPassword').value
    };
    
    // Validate passwords only if provided (for updates) or always for new users
    if (data.password || data.confirmPassword) {
        if (data.password !== data.confirmPassword) {
            Toast.error('Passwords do not match');
            return;
        }
    }
    
    // If editing and no password provided, remove it from data
    if (currentUserId && !data.password) {
        delete data.password;
        delete data.confirmPassword;
    }
    
    const userType = document.getElementById('userType').value;
    
    try {
        let response;
        if (currentUserId) {
            // Update existing user
            response = await adminAPI.updateUser(currentUserId, data);
        } else {
            // Create new user
            if (userType === 'ROLE_MANAGER') {
                response = await adminAPI.createManager(data);
            } else {
                response = await adminAPI.createLibrarian(data);
            }
        }
        
        if (response.success) {
            Toast.success(currentUserId ? 'User updated successfully' : 'User created successfully');
            closeModal();
            loadUsers();
        } else {
            Toast.error(response.message || 'Failed to save user');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to save user');
    }
}

async function changeRole(userId, currentRole) {
    const roles = ['ROLE_CLIENT', 'ROLE_LIBRARIAN', 'ROLE_MANAGER'];
    const newRole = prompt(`Enter new role (${roles.join(', ')}):`, currentRole);
    
    if (!newRole || newRole === currentRole) return;
    
    try {
        const response = await adminAPI.changeUserRole(userId, newRole);
        if (response.success) {
            Toast.success('Role updated successfully');
            loadUsers();
        } else {
            Toast.error(response.message || 'Failed to update role');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to update role');
    }
}

async function changeTier(userId, currentTier) {
    const tiers = ['BASIC', 'PREMIUM'];
    const newTier = prompt(`Enter new tier (${tiers.join(', ')}):`, currentTier);
    
    if (!newTier || newTier === currentTier) return;
    
    try {
        const response = await adminAPI.changeUserTier(userId, newTier);
        if (response.success) {
            Toast.success('Tier updated successfully');
            loadUsers();
        } else {
            Toast.error(response.message || 'Failed to update tier');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to update tier');
    }
}

async function deleteUser(userId) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    
    try {
        const response = await adminAPI.deleteUser(userId);
        if (response.success) {
            Toast.success('User deleted successfully');
            loadUsers();
        } else {
            Toast.error(response.message || 'Failed to delete user');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to delete user');
    }
}

function closeModal() {
    document.getElementById('userModal').style.display = 'none';
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
