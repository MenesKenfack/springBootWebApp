// Analytics JavaScript
let revenueTrendChart = null;
let categoryChart = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute(['ROLE_MANAGER'])) return;
    
    setupNavigation();
    generateReport();
    loadReports();
    
    // Setup event listeners
    const dateRange = document.getElementById('dateRange');
    const reportType = document.getElementById('reportType');
    
    if (dateRange) {
        dateRange.addEventListener('change', generateReport);
    }
    
    if (reportType) {
        reportType.addEventListener('change', generateReport);
    }
});

async function generateReport() {
    const dateRange = document.getElementById('dateRange')?.value || 'LAST_30_DAYS';
    const reportType = document.getElementById('reportType')?.value || 'SUMMARY';
    
    try {
        const response = await analyticsAPI.generateReport(dateRange, reportType);
        
        if (response.success) {
            const data = response.data;
            
            // Update summary cards
            updateSummaryCards(data);
            
            // Render charts
            renderRevenueTrendChart(data.dailyStats);
            renderCategoryChart(data.categoryStats);
            
            // Render table
            renderAnalyticsTable(data.dailyStats);
        } else {
            Toast.error(response.message || 'Failed to generate report');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to generate report');
    }
}

function updateSummaryCards(data) {
    const updateEl = (id, value) => {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    };
    
    updateEl('reportTotalResources', data.totalResources || '0');
    updateEl('reportTotalUsers', data.totalUsers || '0');
    updateEl('reportTotalRevenue', (data.totalRevenue || '0').toLocaleString() + ' XAF');
    updateEl('reportTotalPurchases', data.totalPurchases || '0');
}

function renderRevenueTrendChart(dailyStats) {
    const ctx = document.getElementById('revenueTrendChart');
    if (!ctx) return;
    
    if (revenueTrendChart) {
        revenueTrendChart.destroy();
    }
    
    const labels = dailyStats?.map(s => {
        const date = new Date(s.date);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }) || [];
    
    const data = dailyStats?.map(s => s.revenue || 0) || [];
    
    revenueTrendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Revenue (XAF)',
                data: data,
                borderColor: '#4B5320',
                backgroundColor: 'rgba(75, 83, 32, 0.1)',
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: 'rgba(0, 0, 0, 0.05)' }
                },
                x: {
                    grid: { display: false }
                }
            }
        }
    });
}

function renderCategoryChart(categoryStats) {
    const ctx = document.getElementById('categoryChart');
    if (!ctx) return;
    
    if (categoryChart) {
        categoryChart.destroy();
    }
    
    const labels = categoryStats?.map(s => s.category) || [];
    const data = categoryStats?.map(s => s.count) || [];
    
    categoryChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: [
                    '#4B5320',
                    '#5D6532',
                    '#6A7337',
                    '#929A68',
                    '#3A3F28',
                    '#A9AF8B'
                ]
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

function renderAnalyticsTable(dailyStats) {
    const tbody = document.getElementById('analyticsTableBody');
    if (!tbody) return;
    
    if (!dailyStats || dailyStats.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center;">No data available</td></tr>';
        return;
    }
    
    tbody.innerHTML = dailyStats.map(stat => `
        <tr>
            <td>${formatDate(stat.date)}</td>
            <td>${stat.accesses || 0}</td>
            <td>${stat.purchases || 0}</td>
            <td>${(stat.revenue || 0).toLocaleString()} XAF</td>
        </tr>
    `).join('');
}

// Reports Management Functions
async function loadReports() {
    try {
        const response = await reportsAPI.getAll();
        
        if (response.success) {
            renderReportsTable(response.data);
        } else {
            Toast.error(response.message || 'Failed to load reports');
        }
    } catch (error) {
        console.error('Error loading reports:', error);
        Toast.error('Failed to load reports');
    }
}

function renderReportsTable(reports) {
    const tbody = document.getElementById('reportsTableBody');
    const noReportsMessage = document.getElementById('noReportsMessage');
    
    if (!tbody) return;
    
    if (!reports || reports.length === 0) {
        tbody.innerHTML = '';
        if (noReportsMessage) noReportsMessage.style.display = 'block';
        return;
    }
    
    if (noReportsMessage) noReportsMessage.style.display = 'none';
    
    tbody.innerHTML = reports.map(report => `
        <tr>
            <td><strong>${escapeHtml(report.reportName)}</strong></td>
            <td><span class="badge badge-secondary">${formatReportType(report.reportType)}</span></td>
            <td>${formatDateRange(report.dateRange)}</td>
            <td>${renderReportStatus(report.status)}</td>
            <td>${formatFileSize(report.fileSize || 0)}</td>
            <td>${formatDate(report.createdAt)}</td>
            <td>
                <div class="action-buttons">
                    ${report.status === 'COMPLETED' ? `
                        <button class="btn-view" onclick="downloadReport(${report.reportId})" title="Download">
                            <i class="fas fa-download"></i>
                        </button>
                    ` : ''}
                    <button class="btn-edit" onclick="editReport(${report.reportId})" title="Edit">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-delete" onclick="deleteReport(${report.reportId})" title="Delete">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function formatReportType(type) {
    const types = {
        'SUMMARY': 'Summary',
        'DETAILED': 'Detailed',
        'REVENUE': 'Revenue',
        'USAGE': 'Usage',
        'USER_ACTIVITY': 'User Activity',
        'RESOURCE_ACCESS': 'Resource Access'
    };
    return types[type] || type;
}

function formatDateRange(range) {
    const ranges = {
        'LAST_7_DAYS': 'Last 7 Days',
        'LAST_30_DAYS': 'Last 30 Days',
        'THIS_MONTH': 'This Month',
        'LAST_MONTH': 'Last Month',
        'LAST_90_DAYS': 'Last 90 Days',
        'THIS_YEAR': 'This Year'
    };
    return ranges[range] || range;
}

function renderReportStatus(status) {
    const statusClasses = {
        'PENDING': 'badge badge-warning',
        'GENERATING': 'badge badge-info',
        'COMPLETED': 'badge badge-success',
        'FAILED': 'badge badge-danger'
    };
    
    const statusIcons = {
        'PENDING': 'fa-clock',
        'GENERATING': 'fa-spinner fa-spin',
        'COMPLETED': 'fa-check',
        'FAILED': 'fa-times'
    };
    
    const className = statusClasses[status] || 'badge badge-secondary';
    const icon = statusIcons[status] || 'fa-circle';
    
    return `<span class="${className}"><i class="fas ${icon}"></i> ${status}</span>`;
}

function formatFileSize(bytes) {
    if (bytes === 0) return '-';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function openReportModal(reportId = null) {
    const modal = document.getElementById('reportModal');
    const form = document.getElementById('reportForm');
    const title = document.getElementById('reportModalTitle');
    
    if (form) form.reset();
    document.getElementById('reportId').value = reportId || '';
    
    if (reportId) {
        title.textContent = 'Edit Report';
        // Load report data for editing
        loadReportForEdit(reportId);
    } else {
        title.textContent = 'Generate New Report';
    }
    
    if (modal) modal.style.display = 'flex';
}

async function loadReportForEdit(reportId) {
    try {
        const response = await reportsAPI.getById(reportId);
        
        if (response.success && response.data) {
            const report = response.data;
            document.getElementById('reportName').value = report.reportName || '';
            document.getElementById('reportType').value = report.reportType || 'SUMMARY';
            document.getElementById('reportDateRange').value = report.dateRange || 'LAST_30_DAYS';
            document.getElementById('reportDescription').value = report.description || '';
        }
    } catch (error) {
        console.error('Error loading report for edit:', error);
        Toast.error('Failed to load report details');
    }
}

function closeReportModal() {
    const modal = document.getElementById('reportModal');
    if (modal) modal.style.display = 'none';
}

async function generateNewReport() {
    const form = document.getElementById('reportForm');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const reportId = document.getElementById('reportId').value;
    const data = {
        reportName: document.getElementById('reportName').value,
        reportType: document.getElementById('reportType').value,
        dateRange: document.getElementById('reportDateRange').value,
        description: document.getElementById('reportDescription').value
    };
    
    try {
        let response;
        if (reportId) {
            response = await reportsAPI.update(reportId, data);
        } else {
            response = await reportsAPI.create(data);
        }
        
        if (response.success) {
            Toast.success(reportId ? 'Report updated successfully' : 'Report generation started');
            closeReportModal();
            loadReports();
        } else {
            Toast.error(response.message || 'Failed to save report');
        }
    } catch (error) {
        console.error('Error saving report:', error);
        Toast.error(error.message || 'Failed to save report');
    }
}

async function editReport(reportId) {
    openReportModal(reportId);
}

async function deleteReport(reportId) {
    if (!confirm('Are you sure you want to delete this report? This action cannot be undone.')) {
        return;
    }
    
    try {
        const response = await reportsAPI.delete(reportId);
        
        if (response.success) {
            Toast.success('Report deleted successfully');
            loadReports();
        } else {
            Toast.error(response.message || 'Failed to delete report');
        }
    } catch (error) {
        console.error('Error deleting report:', error);
        Toast.error(error.message || 'Failed to delete report');
    }
}

async function downloadReport(reportId) {
    try {
        const response = await reportsAPI.download(reportId);
        
        if (response.success) {
            Toast.success('Report download started...');
            // In a real implementation, this would trigger the actual file download
            // window.location.href = response.data;
        } else {
            Toast.error(response.message || 'Failed to download report');
        }
    } catch (error) {
        console.error('Error downloading report:', error);
        Toast.error(error.message || 'Failed to download report');
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Reports API
if (typeof reportsAPI === 'undefined') {
    window.reportsAPI = {
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
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function exportReport() {
    // In a real app, this would generate a CSV or PDF
    Toast.success('Report export started...');
    
    // Simulate download
    setTimeout(() => {
        Toast.success('Report exported successfully!');
    }, 1500);
}
