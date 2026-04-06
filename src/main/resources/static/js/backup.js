// Backup & Recovery Management JavaScript

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute(['ROLE_MANAGER'])) return;
    
    setupNavigation();
    loadBackupStatistics();
    loadLatestBackup();
    loadBackups();
    loadBackupSettings();
});

async function loadBackupSettings() {
    try {
        const response = await backupAPI.getSettings();
        
        if (response.success && response.data) {
            const settings = response.data;
            document.getElementById('autoBackupEnabled').checked = settings.autoBackupEnabled;
            document.getElementById('retentionDays').value = settings.retentionDays || 90;
            document.getElementById('autoBackupType').value = settings.backupType || 'FULL';
        }
    } catch (error) {
        console.error('Error loading backup settings:', error);
    }
}

async function saveBackupSettings() {
    const data = {
        autoBackupEnabled: document.getElementById('autoBackupEnabled').checked,
        backupFrequency: 'MONTHLY',
        backupDayOfMonth: 1,
        backupTime: '02:00',
        backupType: document.getElementById('autoBackupType').value,
        retentionDays: parseInt(document.getElementById('retentionDays').value) || 90
    };
    
    try {
        const response = await backupAPI.updateSettings(data);
        
        if (response.success) {
            Toast.success('Backup settings saved successfully');
        } else {
            Toast.error(response.message || 'Failed to save settings');
        }
    } catch (error) {
        console.error('Error saving backup settings:', error);
        Toast.error(error.message || 'Failed to save settings');
    }
}

async function triggerManualBackup() {
    if (!confirm('Are you sure you want to trigger a manual backup now?')) {
        return;
    }
    
    try {
        const response = await backupAPI.triggerManual();
        
        if (response.success) {
            Toast.success('Manual backup triggered successfully');
            // Refresh backup list after a short delay
            setTimeout(() => {
                loadBackups();
                loadLatestBackup();
                loadBackupStatistics();
            }, 2000);
        } else {
            Toast.error(response.message || 'Failed to trigger backup');
        }
    } catch (error) {
        console.error('Error triggering manual backup:', error);
        Toast.error(error.message || 'Failed to trigger backup');
    }
}

async function loadBackupStatistics() {
    try {
        const response = await backupAPI.getStatistics();
        
        if (response.success) {
            const stats = response.data;
            document.getElementById('totalBackups').textContent = stats.totalBackups || 0;
            document.getElementById('totalSize').textContent = formatFileSize(stats.totalSize || 0);
        }
    } catch (error) {
        console.error('Error loading backup statistics:', error);
    }
}

async function loadLatestBackup() {
    try {
        const response = await backupAPI.getLatest();
        
        if (response.success && response.data) {
            const backup = response.data;
            document.getElementById('latestBackupContent').innerHTML = `
                <div class="active-terms-display">
                    <div class="terms-header">
                        <h4>${escapeHtml(backup.backupName)}</h4>
                        <div class="terms-meta">
                            <span class="badge badge-success">${backup.backupType}</span>
                            <span class="text-muted">
                                <i class="fas fa-calendar"></i> ${formatDateTime(backup.createdAt)}
                            </span>
                        </div>
                    </div>
                    <div class="terms-preview-text" style="margin-top: 1rem;">
                        <p><strong>Size:</strong> ${formatFileSize(backup.fileSize || 0)}</p>
                        <p><strong>Status:</strong> ${renderBackupStatus(backup.status)}</p>
                        ${backup.description ? `<p><strong>Description:</strong> ${escapeHtml(backup.description)}</p>` : ''}
                    </div>
                </div>
            `;
            document.getElementById('lastBackup').textContent = formatDate(backup.createdAt);
        } else {
            document.getElementById('latestBackupContent').innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle"></i> No backups available. Create your first backup!
                </div>
            `;
            document.getElementById('lastBackup').textContent = 'Never';
        }
    } catch (error) {
        console.error('Error loading latest backup:', error);
        document.getElementById('latestBackupContent').innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle"></i> No backups available.
            </div>
        `;
    }
}

async function loadBackups() {
    try {
        const response = await backupAPI.getAll();
        
        if (response.success) {
            renderBackupsTable(response.data);
        } else {
            Toast.error(response.message || 'Failed to load backups');
        }
    } catch (error) {
        console.error('Error loading backups:', error);
        Toast.error('Failed to load backups');
    }
}

function renderBackupsTable(backups) {
    const tbody = document.getElementById('backupsTableBody');
    
    if (backups.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center">
                    <div class="empty-state">
                        <i class="fas fa-database" style="font-size: 2rem; color: var(--text-light); margin-bottom: 0.5rem;"></i>
                        <p>No backups created yet</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = backups.map(backup => `
        <tr>
            <td><strong>${escapeHtml(backup.backupName)}</strong></td>
            <td><span class="badge badge-secondary">${backup.backupType}</span></td>
            <td>${renderBackupStatus(backup.status)}</td>
            <td>${formatFileSize(backup.fileSize || 0)}</td>
            <td>${formatDate(backup.createdAt)}</td>
            <td>
                <div class="action-buttons">
                    ${backup.status === 'COMPLETED' ? `
                        <button class="btn-activate" onclick="restoreBackup(${backup.backupId})" title="Restore">
                            <i class="fas fa-undo"></i>
                        </button>
                    ` : ''}
                    <button class="btn-delete" onclick="deleteBackup(${backup.backupId})" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function renderBackupStatus(status) {
    const statusClasses = {
        'PENDING': 'badge badge-warning',
        'IN_PROGRESS': 'badge badge-info',
        'COMPLETED': 'badge badge-success',
        'FAILED': 'badge badge-danger',
        'RESTORED': 'badge badge-primary'
    };
    
    const statusIcons = {
        'PENDING': 'fa-clock',
        'IN_PROGRESS': 'fa-spinner fa-spin',
        'COMPLETED': 'fa-check',
        'FAILED': 'fa-times',
        'RESTORED': 'fa-undo'
    };
    
    const className = statusClasses[status] || 'badge badge-secondary';
    const icon = statusIcons[status] || 'fa-circle';
    
    return `<span class="${className}"><i class="fas ${icon}"></i> ${status}</span>`;
}

function openBackupModal() {
    document.getElementById('backupForm').reset();
    document.getElementById('backupId').value = '';
    document.getElementById('modalTitle').textContent = 'Create New Backup';
    document.getElementById('backupModal').style.display = 'flex';
}

function closeBackupModal() {
    document.getElementById('backupModal').style.display = 'none';
}

async function createBackup() {
    const form = document.getElementById('backupForm');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const data = {
        backupName: document.getElementById('backupName').value,
        backupType: document.getElementById('backupType').value,
        description: document.getElementById('description').value,
        tablesIncluded: document.getElementById('tablesIncluded').value
    };
    
    try {
        const response = await backupAPI.create(data);
        
        if (response.success) {
            Toast.success('Backup created successfully');
            closeBackupModal();
            loadBackups();
            loadLatestBackup();
            loadBackupStatistics();
        } else {
            Toast.error(response.message || 'Failed to create backup');
        }
    } catch (error) {
        console.error('Error creating backup:', error);
        Toast.error(error.message || 'Failed to create backup');
    }
}

async function restoreBackup(id) {
    if (!confirm('Are you sure you want to restore this backup? This will replace current data.')) {
        return;
    }
    
    try {
        const response = await backupAPI.restore(id);
        
        if (response.success) {
            Toast.success('Backup restored successfully');
            loadBackups();
        } else {
            Toast.error(response.message || 'Failed to restore backup');
        }
    } catch (error) {
        console.error('Error restoring backup:', error);
        Toast.error(error.message || 'Failed to restore backup');
    }
}

async function deleteBackup(id) {
    if (!confirm('Are you sure you want to delete this backup? This action cannot be undone.')) {
        return;
    }
    
    try {
        const response = await backupAPI.delete(id);
        
        if (response.success) {
            Toast.success('Backup deleted successfully');
            loadBackups();
            loadLatestBackup();
            loadBackupStatistics();
        } else {
            Toast.error(response.message || 'Failed to delete backup');
        }
    } catch (error) {
        console.error('Error deleting backup:', error);
        Toast.error(error.message || 'Failed to delete backup');
    }
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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

// Backup API
if (typeof backupAPI === 'undefined') {
    window.backupAPI = {
        async getAll() {
            return apiClient.get('/backups');
        },
        
        async getLatest() {
            return apiClient.get('/backups/latest');
        },
        
        async getStatistics() {
            return apiClient.get('/backups/statistics');
        },
        
        async create(data) {
            return apiClient.post('/backups', data);
        },
        
        async delete(id) {
            return apiClient.delete(`/backups/${id}`);
        },
        
        async restore(id) {
            return apiClient.post(`/backups/${id}/restore`);
        },
        
        async getSettings() {
            return apiClient.get('/backups/settings');
        },
        
        async updateSettings(data) {
            return apiClient.put('/backups/settings', data);
        },
        
        async triggerManual() {
            return apiClient.post('/backups/trigger');
        }
    };
}
