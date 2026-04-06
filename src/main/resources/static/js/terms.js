// Terms & Conditions Management JavaScript

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute(['ROLE_MANAGER'])) return;
    
    setupNavigation();
    loadTerms();
    loadActiveTerms();
});

async function loadTerms() {
    try {
        const response = await termsAPI.getAll();
        
        if (response.success) {
            renderTermsTable(response.data);
        } else {
            Toast.error(response.message || 'Failed to load terms');
        }
    } catch (error) {
        Toast.error('Failed to load terms');
    }
}

async function loadActiveTerms() {
    try {
        const response = await termsAPI.getActive();
        
        if (response.success) {
            renderActiveTerms(response.data);
        } else {
            document.getElementById('activeTermsContent').innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle"></i> No active terms currently set.
                </div>
            `;
        }
    } catch (error) {
        Toast.error('Failed to load active terms');
        document.getElementById('activeTermsContent').innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle"></i> No active terms currently set.
            </div>
        `;
    }
}

function renderActiveTerms(terms) {
    const content = document.getElementById('activeTermsContent');
    
    content.innerHTML = `
        <div class="active-terms-display">
            <div class="terms-header">
                <h4>${escapeHtml(terms.title)}</h4>
                <div class="terms-meta">
                    <span class="badge badge-success">Version ${terms.version}</span>
                    <span class="text-muted">
                        <i class="fas fa-calendar"></i> Effective: ${formatDate(terms.effectiveDate)}
                    </span>
                </div>
            </div>
            <div class="terms-preview-text" style="max-height: 200px; overflow-y: auto; margin-top: 1rem; padding: 1rem; background: var(--bg-abyss); border-radius: 0.5rem;">
                ${escapeHtml(terms.content.substring(0, 500))}${terms.content.length > 500 ? '...' : ''}
            </div>
            <div class="terms-actions" style="margin-top: 1rem;">
                <button class="btn btn-sm btn-secondary" onclick="viewTerms(${terms.termsId})">
                    <i class="fas fa-eye"></i> View Full Terms
                </button>
            </div>
        </div>
    `;
}

function renderTermsTable(terms) {
    const tbody = document.getElementById('termsTableBody');
    
    if (terms.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center">
                    <div class="empty-state">
                        <i class="fas fa-file-alt" style="font-size: 2rem; color: var(--text-light); margin-bottom: 0.5rem;"></i>
                        <p>No terms versions created yet</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = terms.map(term => `
        <tr class="${term.active ? 'active-row' : ''}">
            <td><strong>${term.version}</strong></td>
            <td>${escapeHtml(term.title)}</td>
            <td>
                ${term.active ? 
                    '<span class="badge badge-success"><i class="fas fa-check"></i> Active</span>' : 
                    '<span class="badge badge-secondary">Inactive</span>'
                }
            </td>
            <td>${formatDate(term.effectiveDate)}</td>
            <td>${formatDateTime(term.updatedAt || term.createdAt)}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn-view" onclick="viewTerms(${term.termsId})" title="View">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn-edit" onclick="editTerms(${term.termsId})" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    ${!term.active ? `
                        <button class="btn-activate" onclick="activateTerms(${term.termsId})" title="Activate">
                            <i class="fas fa-check-circle"></i>
                        </button>
                        <button class="btn-delete" onclick="deleteTerms(${term.termsId})" title="Delete">
                            <i class="fas fa-trash"></i>
                        </button>
                    ` : ''}
                </div>
            </td>
        </tr>
    `).join('');
}

function openTermsModal() {
    document.getElementById('termsForm').reset();
    document.getElementById('termsId').value = '';
    document.getElementById('modalTitle').textContent = 'Create New Terms';
    document.getElementById('termsModal').style.display = 'flex';
}

function closeTermsModal() {
    document.getElementById('termsModal').style.display = 'none';
}

async function editTerms(id) {
    try {
        const response = await termsAPI.getById(id);
        
        if (response.success) {
            const terms = response.data;
            document.getElementById('termsId').value = terms.termsId;
            document.getElementById('version').value = terms.version;
            document.getElementById('title').value = terms.title;
            document.getElementById('content').value = terms.content;
            document.getElementById('isActive').checked = terms.active;
            
            if (terms.effectiveDate) {
                const date = new Date(terms.effectiveDate);
                const formatted = date.toISOString().slice(0, 16);
                document.getElementById('effectiveDate').value = formatted;
            }
            
            document.getElementById('modalTitle').textContent = 'Edit Terms';
            document.getElementById('termsModal').style.display = 'flex';
        } else {
            Toast.error(response.message || 'Failed to load terms');
        }
    } catch (error) {
        Toast.error('Failed to load terms for editing');
    }
}

async function viewTerms(id) {
    try {
        const response = await termsAPI.getById(id);
        
        if (response.success) {
            const terms = response.data;
            document.getElementById('viewModalTitle').textContent = terms.title;
            document.getElementById('viewVersionBadge').textContent = `Version ${terms.version}`;
            document.getElementById('viewEffectiveDate').innerHTML = `<i class="fas fa-calendar"></i> Effective: ${formatDate(terms.effectiveDate)}`;
            document.getElementById('viewContent').textContent = terms.content;
            
            const badge = document.getElementById('viewVersionBadge');
            if (terms.active) {
                badge.className = 'badge badge-success';
                badge.innerHTML = `<i class="fas fa-check"></i> Version ${terms.version} (Active)`;
            } else {
                badge.className = 'badge badge-secondary';
                badge.textContent = `Version ${terms.version}`;
            }
            
            document.getElementById('viewTermsModal').style.display = 'flex';
        } else {
            Toast.error(response.message || 'Failed to load terms');
        }
    } catch (error) {
        Toast.error('Failed to load terms');
    }
}

function closeViewTermsModal() {
    document.getElementById('viewTermsModal').style.display = 'none';
}

async function saveTerms() {
    const form = document.getElementById('termsForm');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const termsId = document.getElementById('termsId').value;
    const data = {
        version: document.getElementById('version').value,
        title: document.getElementById('title').value,
        content: document.getElementById('content').value,
        isActive: document.getElementById('isActive').checked
    };
    
    const effectiveDate = document.getElementById('effectiveDate').value;
    if (effectiveDate) {
        data.effectiveDate = new Date(effectiveDate).toISOString();
    }
    
    try {
        let response;
        if (termsId) {
            response = await termsAPI.update(termsId, data);
        } else {
            response = await termsAPI.create(data);
        }
        
        if (response.success) {
            Toast.success(termsId ? 'Terms updated successfully' : 'Terms created successfully');
            closeTermsModal();
            loadTerms();
            loadActiveTerms();
        } else {
            Toast.error(response.message || 'Failed to save terms');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to save terms');
    }
}

async function activateTerms(id) {
    if (!confirm('Are you sure you want to activate this version? This will deactivate the current active terms.')) {
        return;
    }
    
    try {
        const response = await termsAPI.activate(id);
        
        if (response.success) {
            Toast.success('Terms activated successfully');
            loadTerms();
            loadActiveTerms();
        } else {
            Toast.error(response.message || 'Failed to activate terms');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to activate terms');
    }
}

async function deleteTerms(id) {
    if (!confirm('Are you sure you want to delete these terms? This action cannot be undone.')) {
        return;
    }
    
    try {
        const response = await termsAPI.delete(id);
        
        if (response.success) {
            Toast.success('Terms deleted successfully');
            loadTerms();
            loadActiveTerms();
        } else {
            Toast.error(response.message || 'Failed to delete terms');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to delete terms');
    }
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Define termsAPI if it doesn't exist
if (typeof termsAPI === 'undefined') {
    window.termsAPI = {
        async getAll() {
            return apiClient.get('/terms');
        },
        
        async getActive() {
            return apiClient.get('/terms/active');
        },
        
        async getById(id) {
            return apiClient.get(`/terms/${id}`);
        },
        
        async create(data) {
            return apiClient.post('/terms', data);
        },
        
        async update(id, data) {
            return apiClient.put(`/terms/${id}`, data);
        },
        
        async delete(id) {
            return apiClient.delete(`/terms/${id}`);
        },
        
        async activate(id) {
            return apiClient.post(`/terms/${id}/activate`);
        }
    };
}
