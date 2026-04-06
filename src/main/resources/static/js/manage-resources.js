// Manage Resources JavaScript
let resources = [];
let currentResourceId = null;
let catalogs = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute(['ROLE_LIBRARIAN', 'ROLE_MANAGER'])) return;
    
    setupNavigation();
    loadResources();
    loadCatalogs();
    setupFilters();
    
    // Setup form submission
    const resourceForm = document.getElementById('resourceForm');
    if (resourceForm) {
        resourceForm.addEventListener('submit', handleFormSubmit);
    }
});

function setupFilters() {
    const searchInput = document.getElementById('searchInput');
    const categoryFilter = document.getElementById('categoryFilter');
    const catalogFilter = document.getElementById('catalogFilter');
    
    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => loadResources(), 300));
    }
    
    if (categoryFilter) {
        categoryFilter.addEventListener('change', () => loadResources());
    }
    
    if (catalogFilter) {
        catalogFilter.addEventListener('change', () => loadResources());
    }
}

async function loadResources() {
    const params = {
        page: 0,
        size: 50
    };
    
    const searchInput = document.getElementById('searchInput');
    const categoryFilter = document.getElementById('categoryFilter');
    const catalogFilter = document.getElementById('catalogFilter');
    
    if (searchInput?.value) {
        params.keyword = searchInput.value;
    }
    
    if (categoryFilter?.value) {
        params.category = categoryFilter.value;
    }
    
    if (catalogFilter?.value) {
        params.catalogId = catalogFilter.value;
    }
    
    try {
        const response = await resourcesAPI.search(params);
        
        if (response.success) {
            resources = response.data.content;
            renderResourcesTable();
        } else {
            Toast.error(response.message || 'Failed to load resources');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to load resources');
    }
}

async function loadCatalogs() {
    try {
        const response = await resourcesAPI.getCatalogs();
        
        if (response.success) {
            catalogs = response.data;
            
            // Populate catalog filter
            const catalogFilter = document.getElementById('catalogFilter');
            const catalogSelect = document.getElementById('catalogId');
            
            const options = catalogs.map(c => `<option value="${c.catalogId}">${escapeHtml(c.catalogName)}</option>`).join('');
            
            if (catalogFilter) {
                catalogFilter.innerHTML = '<option value="">All Catalogs</option>' + options;
            }
            
            if (catalogSelect) {
                catalogSelect.innerHTML = options;
            }
        }
    } catch (error) {
        Toast.warning('Failed to load catalogs. Some features may be limited.');
    }
}

function renderResourcesTable() {
    const tbody = document.getElementById('resourcesTableBody');
    if (!tbody) return;
    
    if (resources.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center;">No resources found</td></tr>`;
        return;
    }
    
    tbody.innerHTML = resources.map(resource => `
        <tr>
            <td>${resource.resourceID}</td>
            <td>${escapeHtml(resource.title)}</td>
            <td>${escapeHtml(resource.author || 'N/A')}</td>
            <td>${resource.category}</td>
            <td>${resource.price ? resource.price.toLocaleString() : '0'} XAF</td>
            <td>${resource.premiumOnly ? '<span class="status-badge status-success">Yes</span>' : '<span class="status-badge">No</span>'}</td>
            <td class="actions-cell">
                <div class="action-buttons">
                    <button class="btn btn-sm btn-view" onclick="viewResource(${resource.resourceID})" title="View Resource">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-edit" onclick="editResource(${resource.resourceID})" title="Edit Resource">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-delete" onclick="deleteResource(${resource.resourceID})" title="Delete Resource">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function viewResource(id) {
    const resource = resources.find(r => r.resourceID === id);
    if (!resource) return;
    
    // Create and show resource details modal
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.style.display = 'flex';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 600px;">
            <div class="modal-header">
                <h3>Resource Details</h3>
                <button type="button" class="close-modal" onclick="this.closest('.modal').remove()">&times;</button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label>Title</label>
                    <input type="text" value="${escapeHtml(resource.title)}" readonly class="form-control">
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Author</label>
                        <input type="text" value="${escapeHtml(resource.author || 'N/A')}" readonly class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Category</label>
                        <input type="text" value="${resource.category}" readonly class="form-control">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>ISBN</label>
                        <input type="text" value="${escapeHtml(resource.isbn || 'N/A')}" readonly class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Price</label>
                        <input type="text" value="${resource.price ? resource.price.toLocaleString() : '0'} XAF" readonly class="form-control">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Publication Year</label>
                        <input type="text" value="${resource.publicationYear || 'N/A'}" readonly class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Premium Only</label>
                        <input type="text" value="${resource.premiumOnly ? 'Yes' : 'No'}" readonly class="form-control">
                    </div>
                </div>
                <div class="form-group">
                    <label>Description</label>
                    <textarea readonly class="form-control" rows="3">${escapeHtml(resource.description || 'N/A')}</textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" onclick="this.closest('.modal').remove()">Close</button>
                <button type="button" class="btn btn-primary" onclick="this.closest('.modal').remove(); editResource(${resource.resourceID});">Edit</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function openAddResourceModal() {
    currentResourceId = null;
    document.getElementById('modalTitle').textContent = 'Add Resource';
    document.getElementById('resourceForm').reset();
    document.getElementById('resourceModal').style.display = 'flex';
}

function editResource(id) {
    const resource = resources.find(r => r.resourceID === id);
    if (!resource) return;
    
    currentResourceId = id;
    document.getElementById('modalTitle').textContent = 'Edit Resource';
    
    // Fill form
    document.getElementById('title').value = resource.title;
    document.getElementById('isbn').value = resource.isbn || '';
    document.getElementById('author').value = resource.author || '';
    document.getElementById('publicationYear').value = resource.publicationYear || '';
    document.getElementById('category').value = resource.category;
    document.getElementById('catalogId').value = resource.catalogId || catalogs[0]?.catalogId || '';
    document.getElementById('price').value = resource.price || '';
    document.getElementById('isPremiumOnly').checked = resource.premiumOnly;
    document.getElementById('description').value = resource.description || '';
    
    // Clear file inputs (cannot pre-populate file inputs)
    document.getElementById('resourceFile').value = '';
    document.getElementById('resourceImage').value = '';
    
    document.getElementById('resourceModal').style.display = 'flex';
}

async function handleFormSubmit(e) {
    e.preventDefault();
    
    const formData = new FormData();
    formData.append('title', document.getElementById('title').value);
    formData.append('isbn', document.getElementById('isbn').value || '');
    formData.append('author', document.getElementById('author').value || '');
    formData.append('publicationYear', document.getElementById('publicationYear').value || '');
    formData.append('category', document.getElementById('category').value);
    formData.append('catalogId', document.getElementById('catalogId').value);
    formData.append('price', document.getElementById('price').value || '0');
    formData.append('isPremiumOnly', document.getElementById('isPremiumOnly').checked);
    formData.append('description', document.getElementById('description').value || '');
    
    // Add files if selected
    const resourceFile = document.getElementById('resourceFile').files[0];
    const resourceImage = document.getElementById('resourceImage').files[0];
    
    if (resourceFile) {
        formData.append('resourceFile', resourceFile);
    }
    if (resourceImage) {
        formData.append('resourceImage', resourceImage);
    }
    
    try {
        let response;
        if (currentResourceId) {
            response = await resourcesAPI.updateWithFiles(currentResourceId, formData);
        } else {
            response = await resourcesAPI.createWithFiles(formData);
        }
        
        if (response.success) {
            Toast.success(currentResourceId ? 'Resource updated successfully' : 'Resource created successfully');
            closeModal();
            loadResources();
        } else {
            Toast.error(response.message || 'Operation failed');
        }
    } catch (error) {
        Toast.error(error.message || 'Operation failed');
    }
}

async function deleteResource(id) {
    if (!confirm('Are you sure you want to delete this resource?')) return;
    
    try {
        const response = await resourcesAPI.delete(id);
        
        if (response.success) {
            Toast.success('Resource deleted successfully');
            loadResources();
        } else {
            Toast.error(response.message || 'Delete failed');
        }
    } catch (error) {
        Toast.error(error.message || 'Delete failed');
    }
}

function closeModal() {
    document.getElementById('resourceModal').style.display = 'none';
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
