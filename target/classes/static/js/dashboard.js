// Dashboard JavaScript
let revenueChart = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute()) return;
    
    setupNavigation();
    setupDashboardTitle();
    setupQuickActions();
    setupGlobalSearch();
    loadDashboardData();
    updateDate();
});

function setupDashboardTitle() {
    const user = UserStorage.getUser();
    const titleEl = document.getElementById('dashboardTitle');
    if (!user || !titleEl) return;
    
    const titles = {
        'ROLE_MANAGER': 'Manager Dashboard',
        'ROLE_LIBRARIAN': 'Librarian Dashboard',
        'ROLE_CLIENT': 'My Dashboard'
    };
    
    titleEl.textContent = titles[user.role] || 'Dashboard';
}

function setupQuickActions() {
    const user = UserStorage.getUser();
    if (!user) return;
    
    // Show/hide manager sections
    const managerElements = document.querySelectorAll('.manager-only');
    managerElements.forEach(el => {
        el.style.display = user.role === 'ROLE_MANAGER' ? '' : 'none';
    });
    
    // Show/hide librarian sections
    const librarianElements = document.querySelectorAll('.librarian-only');
    librarianElements.forEach(el => {
        el.style.display = user.role === 'ROLE_LIBRARIAN' ? '' : 'none';
    });
    
    // Show/hide client sections
    const clientElements = document.querySelectorAll('.client-only');
    clientElements.forEach(el => {
        el.style.display = user.role === 'ROLE_CLIENT' ? '' : 'none';
    });
    
    // Specific element visibility for grids
    const managerStats = document.getElementById('managerStats');
    const managerOverview = document.getElementById('managerOverview');
    const managerActivityGrid = document.getElementById('managerActivityGrid');
    const librarianStats = document.getElementById('librarianStats');
    const librarianOverview = document.getElementById('librarianOverview');
    const librarianCategoryCard = document.getElementById('librarianCategoryCard');
    const managerQuickActions = document.getElementById('managerQuickActions');
    const librarianQuickActions = document.getElementById('librarianQuickActions');
    const clientStats = document.getElementById('clientStats');
    const clientOverview = document.getElementById('clientOverview');
    const clientQuickActions = document.getElementById('clientQuickActions');
    const clientActivityCard = document.getElementById('clientActivityCard');
    const clientChartActivityGrid = document.getElementById('clientChartActivityGrid');
    
    if (user.role === 'ROLE_MANAGER') {
        if (managerStats) managerStats.style.display = 'grid';
        if (managerOverview) managerOverview.style.display = 'grid';
        if (managerActivityGrid) managerActivityGrid.style.display = 'grid';
        if (managerQuickActions) managerQuickActions.style.display = 'block';
    } else if (user.role === 'ROLE_LIBRARIAN') {
        if (librarianStats) librarianStats.style.display = 'grid';
        if (librarianOverview) librarianOverview.style.display = 'grid';
        if (librarianCategoryCard) librarianCategoryCard.style.display = 'block';
        if (librarianQuickActions) librarianQuickActions.style.display = 'block';
    } else if (user.role === 'ROLE_CLIENT') {
        if (clientStats) clientStats.style.display = 'grid';
        if (clientOverview) clientOverview.style.display = 'grid';
        if (clientQuickActions) clientQuickActions.style.display = 'block';
        if (clientChartActivityGrid) clientChartActivityGrid.style.display = 'grid';
    }
    
    // Hide purchases for manager
    if (user.role === 'ROLE_MANAGER') {
        const hideForManagerElements = document.querySelectorAll('.hide-for-manager');
        hideForManagerElements.forEach(el => {
            el.style.display = 'none';
        });
    }
}

function setupGlobalSearch() {
    const searchInput = document.getElementById('globalSearchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                performGlobalSearch();
            }
        });
    }
}

function performGlobalSearch() {
    const searchInput = document.getElementById('globalSearchInput');
    const query = searchInput?.value.trim();
    
    if (query) {
        window.location.href = `/resources.html?search=${encodeURIComponent(query)}`;
    } else {
        Toast.warning('Please enter a search term');
    }
}

function updateDate() {
    const dateEl = document.getElementById('currentDate');
    if (dateEl) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        dateEl.textContent = new Date().toLocaleDateString('en-US', options);
    }
}

async function loadDashboardData() {
    try {
        const user = UserStorage.getUser();
        
        // Load different data based on user role
        if (user?.role === 'ROLE_MANAGER') {
            await loadManagerDashboardData();
        } else if (user?.role === 'ROLE_LIBRARIAN') {
            await loadLibrarianDashboardData();
        } else {
            await loadClientDashboardData();
        }
    } catch (error) {
        console.error('Failed to load dashboard data:', error);
        Toast.error('Failed to load dashboard data. Please refresh the page.');
    }
}

async function loadManagerDashboardData() {
    try {
        // Load manager stats and data in parallel
        const [statsResponse, usersResponse, reportsResponse, backupsResponse] = await Promise.all([
            apiClient.get('/reports/dashboard-stats'),
            adminAPI.getUsers().catch(() => ({ data: [] })),
            reportsAPI.getAll().catch(() => ({ data: [] })),
            adminAPI.getBackups().catch(() => ({ data: [] }))
        ]);
        
        // Update manager stats
        if (statsResponse.success && statsResponse.data) {
            const stats = statsResponse.data;
            updateStat('totalUsers', stats.totalUsers?.toLocaleString() || '0');
            updateStat('pendingVerifications', stats.pendingVerifications?.toLocaleString() || '0');
            updateStat('totalRevenue', stats.revenueThisMonth ? `${stats.revenueThisMonth.toLocaleString()} XAF` : '0 XAF');
            updateStat('systemHealth', stats.systemHealth || 'Good');
            
            // Update system overview
            document.getElementById('overviewResources').textContent = stats.totalResources?.toLocaleString() || '0';
            document.getElementById('overviewPurchases').textContent = stats.totalPurchases?.toLocaleString() || '0';
            document.getElementById('overviewPremium').textContent = stats.premiumUsers?.toLocaleString() || '0';
            document.getElementById('overviewVerified').textContent = stats.verifiedUsers?.toLocaleString() || '0';
        }
        
        // Update last backup info
        if (backupsResponse.success && backupsResponse.data && backupsResponse.data.length > 0) {
            const latestBackup = backupsResponse.data[0];
            document.getElementById('overviewBackup').textContent = formatDate(latestBackup.createdAt);
        }
        
        // Render recent signups (last 5 users)
        const recentUsers = usersResponse.data?.slice(-5).reverse() || [];
        renderRecentSignups(recentUsers);
        
        // Render recent reports (last 5)
        const recentReports = reportsResponse.data?.slice(0, 5) || [];
        renderRecentReports(recentReports);
        
        // Render management activity
        renderManagementActivity(usersResponse.data || [], reportsResponse.data || [], backupsResponse.data || []);
        
    } catch (error) {
        console.error('Failed to load manager dashboard data:', error);
    }
}

async function loadClientDashboardData() {
    try {
        // Load stats from API and client data in parallel
        const [statsResponse, recentResources, recentPurchases, myListResponse] = await Promise.all([
            apiClient.get('/reports/dashboard-stats'),
            resourcesAPI.getRecent(),
            purchaseAPI.getRecent().catch(() => ({ data: [] })),
            myListAPI.getMyList().catch(() => ({ data: [] }))
        ]);
        
        // Update client stats from API response
        if (statsResponse.success && statsResponse.data) {
            const stats = statsResponse.data;
            updateStat('clientMyPurchases', stats.myPurchases?.toLocaleString() || '0');
            updateStat('clientResourcesAccessed', stats.totalResourcesAccessed?.toLocaleString() || '0');
            updateStat('clientAvailableResources', stats.totalResources?.toLocaleString() || '0');
            updateStat('clientSavedItems', myListResponse.data?.length?.toLocaleString() || '0');
            
            // Update reading demographics
            renderReadingDemographics(stats.resourcesByCategory, stats.favoriteCategory);
        }
        
        // Render my library (purchased resources)
        renderMyLibrary(recentPurchases.data || []);
        
        // Render recommended resources (recent resources not yet purchased)
        renderRecommendedResources(recentResources.data || [], recentPurchases.data || []);
        
        // Render client activity (purchases, saved items)
        renderClientActivity(recentPurchases.data || [], myListResponse.data || []);
        
    } catch (error) {
        console.error('Failed to load client dashboard data:', error);
    }
}

async function loadLibrarianDashboardData() {
    try {
        // Load librarian stats and data in parallel
        const [statsResponse, catalogsResponse, recentResources] = await Promise.all([
            apiClient.get('/reports/dashboard-stats'),
            resourcesAPI.getCatalogs().catch(() => ({ data: [] })),
            resourcesAPI.getRecent().catch(() => ({ data: [] }))
        ]);
        
        // Update librarian stats
        if (statsResponse.success && statsResponse.data) {
            const stats = statsResponse.data;
            updateStat('librarianTotalResources', stats.totalResources?.toLocaleString() || '0');
            
            // Count catalogs from the catalogs response
            const catalogs = catalogsResponse.data || [];
            updateStat('librarianTotalCatalogs', catalogs.length?.toLocaleString() || '0');
            
            // Update overview section
            document.getElementById('libOverviewTotalResources').textContent = stats.totalResources?.toLocaleString() || '0';
            document.getElementById('libOverviewTotalCatalogs').textContent = catalogs.length?.toLocaleString() || '0';
        }
        
        // Calculate premium resources and recent uploads from recent resources
        const resources = recentResources.data || [];
        const premiumCount = resources.filter(r => r.isPremiumOnly).length;
        updateStat('librarianPremiumResources', premiumCount.toLocaleString());
        document.getElementById('libOverviewPremium').textContent = premiumCount.toLocaleString();
        
        // Count resources added this month
        const now = new Date();
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        const thisMonthUploads = resources.filter(r => {
            if (!r.createdAt) return false;
            const created = new Date(r.createdAt);
            return created >= startOfMonth;
        }).length;
        updateStat('librarianRecentUploads', thisMonthUploads.toLocaleString());
        
        // Render category distribution
        renderCategoryDistribution(resources);
        
        // Render recently added resources (for librarian view)
        renderLibrarianRecentResources(resources.slice(0, 5));
        
        // Determine popular category
        const categoryCounts = {};
        resources.forEach(r => {
            categoryCounts[r.category] = (categoryCounts[r.category] || 0) + 1;
        });
        const popularCategory = Object.entries(categoryCounts)
            .sort((a, b) => b[1] - a[1])[0];
        document.getElementById('libOverviewPopularCategory').textContent = 
            popularCategory ? popularCategory[0] : '-';
        
    } catch (error) {
        console.error('Failed to load librarian dashboard data:', error);
    }
}

function renderCategoryDistribution(resources) {
    const container = document.getElementById('categoryDistribution');
    if (!container) return;
    
    if (resources.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: var(--text-medium);">No resources available</p>';
        return;
    }
    
    // Count resources by category
    const categoryCounts = {};
    resources.forEach(r => {
        categoryCounts[r.category] = (categoryCounts[r.category] || 0) + 1;
    });
    
    const categoryIcons = {
        'BOOK': 'fa-book',
        'JOURNAL': 'fa-newspaper',
        'ARTICLE': 'fa-file-alt',
        'VIDEO': 'fa-video',
        'AUDIO': 'fa-headphones'
    };
    
    const categoryColors = {
        'BOOK': '#4B5320',
        'JOURNAL': '#3B82F6',
        'ARTICLE': '#22C55E',
        'VIDEO': '#EAB308',
        'AUDIO': '#8B5CF6'
    };
    
    container.innerHTML = Object.entries(categoryCounts).map(([category, count]) => `
        <div class="category-stat" style="display: flex; align-items: center; gap: 0.75rem; padding: 1rem; background: rgba(75, 83, 32, 0.05); border-radius: 0.5rem;">
            <div style="width: 40px; height: 40px; border-radius: 8px; background: ${categoryColors[category] || '#6B7280'}20; display: flex; align-items: center; justify-content: center;">
                <i class="fas ${categoryIcons[category] || 'fa-file'}" style="color: ${categoryColors[category] || '#6B7280'}; font-size: 1rem;"></i>
            </div>
            <div>
                <div style="font-weight: 600; color: #1F2937;">${count}</div>
                <div style="font-size: 0.75rem; color: #6B7280;">${category}</div>
            </div>
        </div>
    `).join('');
}

function renderLibrarianRecentResources(resources) {
    const container = document.getElementById('recentResourcesList');
    const noRecent = document.getElementById('noRecentResources');
    
    if (!container) return;
    
    if (resources.length === 0) {
        container.innerHTML = '';
        if (noRecent) noRecent.style.display = 'block';
        return;
    }
    
    if (noRecent) noRecent.style.display = 'none';
    
    container.innerHTML = resources.map(resource => `
        <div class="resource-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB;">
            <div style="width: 36px; height: 36px; border-radius: 8px; background: rgba(75, 83, 32, 0.1); display: flex; align-items: center; justify-content: center;">
                <i class="fas fa-book" style="color: #4B5320; font-size: 0.875rem;"></i>
            </div>
            <div style="flex: 1; min-width: 0;">
                <div style="font-weight: 600; color: #1F2937; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    ${escapeHtml(resource.title)}
                </div>
                <div style="font-size: 0.75rem; color: #6B7280;">
                    ${escapeHtml(resource.author || 'Unknown Author')} • ${resource.category}
                </div>
            </div>
            <span style="font-size: 0.75rem; padding: 0.25rem 0.5rem; border-radius: 1rem; background: ${resource.isPremiumOnly ? 'rgba(234, 179, 8, 0.2)' : 'rgba(34, 197, 94, 0.2)'}; color: ${resource.isPremiumOnly ? '#EAB308' : '#22C55E'};">
                ${resource.isPremiumOnly ? 'Premium' : 'Free'}
            </span>
        </div>
    `).join('');
}

function renderRecentSignups(users) {
    const container = document.getElementById('recentSignupsList');
    const noSignups = document.getElementById('noSignups');
    
    if (!container) return;
    
    if (users.length === 0) {
        container.innerHTML = '';
        if (noSignups) noSignups.style.display = 'block';
        return;
    }
    
    if (noSignups) noSignups.style.display = 'none';
    
    container.innerHTML = users.map(user => `
        <div class="signup-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB;">
            <div class="signup-avatar" style="width: 36px; height: 36px; border-radius: 50%; background: rgba(75, 83, 32, 0.1); display: flex; align-items: center; justify-content: center;">
                <i class="fas fa-user" style="color: #4B5320; font-size: 0.875rem;"></i>
            </div>
            <div style="flex: 1;">
                <div style="font-weight: 600; color: #1F2937;">${escapeHtml(user.firstName + ' ' + user.lastName)}</div>
                <div style="font-size: 0.875rem; color: #6B7280;">${escapeHtml(user.email)}</div>
            </div>
            <span class="badge ${user.verified ? 'badge-success' : 'badge-warning'}" style="font-size: 0.75rem;">
                ${user.verified ? 'Verified' : 'Pending'}
            </span>
        </div>
    `).join('');
}

function renderRecentReports(reports) {
    const container = document.getElementById('recentReportsList');
    const noReports = document.getElementById('noReports');
    
    if (!container) return;
    
    if (reports.length === 0) {
        container.innerHTML = '';
        if (noReports) noReports.style.display = 'block';
        return;
    }
    
    if (noReports) noReports.style.display = 'none';
    
    const statusColors = {
        'COMPLETED': '#22C55E',
        'PENDING': '#EAB308',
        'GENERATING': '#3B82F6',
        'FAILED': '#EF4444'
    };
    
    container.innerHTML = reports.map(report => `
        <div class="report-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB;">
            <div style="width: 36px; height: 36px; border-radius: 8px; background: rgba(59, 130, 246, 0.1); display: flex; align-items: center; justify-content: center;">
                <i class="fas fa-file-alt" style="color: #3B82F6; font-size: 0.875rem;"></i>
            </div>
            <div style="flex: 1;">
                <div style="font-weight: 600; color: #1F2937;">${escapeHtml(report.reportName)}</div>
                <div style="font-size: 0.875rem; color: #6B7280;">${report.reportType} • ${formatDate(report.createdAt)}</div>
            </div>
            <span style="color: ${statusColors[report.status] || '#6B7280'}; font-size: 0.75rem; font-weight: 500;">
                ${report.status}
            </span>
        </div>
    `).join('');
}

function renderManagementActivity(users, reports, backups) {
    const container = document.getElementById('managementActivityList');
    const noActivity = document.getElementById('noActivity');
    
    if (!container) return;
    
    // Create activity items from different sources
    const activities = [];
    
    // Add user signup activities
    users.slice(-3).forEach(user => {
        activities.push({
            type: 'user',
            icon: 'user-plus',
            iconColor: '#22C55E',
            bgColor: 'rgba(34, 197, 94, 0.1)',
            title: `New user signup: ${escapeHtml(user.firstName + ' ' + user.lastName)}`,
            time: user.createdAt || new Date().toISOString()
        });
    });
    
    // Add report generation activities
    reports.slice(0, 3).forEach(report => {
        activities.push({
            type: 'report',
            icon: 'file-alt',
            iconColor: '#3B82F6',
            bgColor: 'rgba(59, 130, 246, 0.1)',
            title: `Generated report: ${escapeHtml(report.reportName)}`,
            time: report.createdAt
        });
    });
    
    // Add backup activities
    backups.slice(0, 2).forEach(backup => {
        activities.push({
            type: 'backup',
            icon: 'hdd',
            iconColor: '#8B5CF6',
            bgColor: 'rgba(139, 92, 246, 0.1)',
            title: `System backup: ${escapeHtml(backup.backupName || 'Backup')}`,
            time: backup.createdAt
        });
    });
    
    // Sort by time (newest first)
    activities.sort((a, b) => new Date(b.time) - new Date(a.time));
    
    if (activities.length === 0) {
        container.innerHTML = '';
        if (noActivity) noActivity.style.display = 'block';
        return;
    }
    
    if (noActivity) noActivity.style.display = 'none';
    
    container.innerHTML = activities.slice(0, 10).map(activity => `
        <div class="activity-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB;">
            <div style="width: 32px; height: 32px; border-radius: 50%; background: ${activity.bgColor}; display: flex; align-items: center; justify-content: center;">
                <i class="fas fa-${activity.icon}" style="color: ${activity.iconColor}; font-size: 0.75rem;"></i>
            </div>
            <div style="flex: 1;">
                <div style="font-size: 0.875rem; color: #1F2937;">${activity.title}</div>
            </div>
            <div style="font-size: 0.75rem; color: #9CA3AF;">
                ${formatDate(activity.time)}
            </div>
        </div>
    `).join('');
}

function updateStat(id, value) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = value;
    }
}

function renderRecentResources(resources) {
    const container = document.getElementById('recentResourcesGrid');
    if (!container) return;
    
    container.innerHTML = resources.map(resource => `
        <div class="resource-card" onclick="viewResource(${resource.resourceID})">
            <div class="resource-image">
                ${resource.resourceImage ? 
                    `<img src="${resource.resourceImage}" alt="${resource.title}">` : 
                    `<i class="fas fa-book"></i>`
                }
            </div>
            <div class="resource-info">
                <h4>${escapeHtml(resource.title)}</h4>
                <p>${escapeHtml(resource.author || 'Unknown Author')}</p>
                <div class="resource-meta">
                    <span class="resource-price">${resource.price ? resource.price.toLocaleString() : '0'} XAF</span>
                    <span class="resource-badge">${resource.category}</span>
                </div>
            </div>
        </div>
    `).join('');
}

function renderRecentActivity(activities) {
    const container = document.getElementById('recentActivityList');
    const noActivity = document.getElementById('noActivity');
    
    if (!container) return;
    
    if (activities.length === 0) {
        container.innerHTML = '';
        noActivity.style.display = 'block';
        return;
    }
    
    noActivity.style.display = 'none';
    
    // Combine and sort activities (purchases, resource views, etc.)
    const allActivities = activities.map(purchase => ({
        type: 'purchase',
        icon: 'shopping-cart',
        iconColor: '#4B5320',
        bgColor: 'rgba(75, 83, 32, 0.1)',
        title: `Purchased "${escapeHtml(purchase.resourceTitle || 'Unknown Resource')}"`,
        description: `by ${escapeHtml(purchase.clientName || 'Unknown')}`,
        amount: purchase.amount ? `${purchase.amount.toLocaleString()} XAF` : '',
        date: purchase.date,
        timestamp: new Date(purchase.date).getTime()
    }));
    
    // Sort by date (newest first)
    allActivities.sort((a, b) => b.timestamp - a.timestamp);
    
    // Take only the most recent 10
    const recentActivities = allActivities.slice(0, 10);
    
    container.innerHTML = recentActivities.map(activity => `
        <div class="activity-item" style="display: flex; align-items: center; gap: 1rem; padding: 1rem; border-bottom: 1px solid #E5E7EB; transition: background 0.2s;" onmouseover="this.style.background='#F9FAFB'" onmouseout="this.style.background='transparent'">
            <div class="activity-icon" style="width: 40px; height: 40px; border-radius: 50%; background: ${activity.bgColor}; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
                <i class="fas fa-${activity.icon}" style="color: ${activity.iconColor}; font-size: 0.875rem;"></i>
            </div>
            <div class="activity-content" style="flex: 1; min-width: 0;">
                <div class="activity-title" style="font-weight: 600; color: #1F2937; margin-bottom: 0.25rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    ${activity.title}
                </div>
                <div class="activity-meta" style="font-size: 0.875rem; color: #6B7280; display: flex; gap: 0.5rem; align-items: center;">
                    <span>${activity.description}</span>
                    ${activity.amount ? `<span style="color: #4B5320; font-weight: 500;">• ${activity.amount}</span>` : ''}
                </div>
            </div>
            <div class="activity-time" style="font-size: 0.75rem; color: #9CA3AF; flex-shrink: 0;">
                ${formatDate(activity.date)}
            </div>
        </div>
    `).join('');
}

// ================================
// Client Dashboard Functions
// ================================

let readingChart = null;

function renderReadingDemographics(resourcesByCategory, favoriteCategory) {
    // Update favorite category display
    const favCatEl = document.getElementById('favoriteCategory');
    if (favCatEl) {
        favCatEl.textContent = favoriteCategory ? formatCategoryName(favoriteCategory) : 'No purchases yet';
    }
    
    // Get category counts with defaults
    const categories = ['BOOK', 'JOURNAL', 'ARTICLE', 'VIDEO', 'AUDIO'];
    const counts = {};
    let total = 0;
    
    categories.forEach(cat => {
        counts[cat] = resourcesByCategory?.[cat] || 0;
        total += counts[cat];
    });
    
    // Update category count displays
    updateStat('booksCount', counts['BOOK'].toString());
    updateStat('articlesCount', counts['ARTICLE'].toString());
    updateStat('videosCount', counts['VIDEO'].toString());
    updateStat('audioCount', counts['AUDIO'].toString() + (counts['JOURNAL'] > 0 ? `+${counts['JOURNAL']}J` : ''));
    
    // Render chart
    renderReadingChart(counts, total);
}

function renderReadingChart(counts, total) {
    const ctx = document.getElementById('readingDemographicsChart');
    if (!ctx) return;
    
    // If no data, show empty state
    if (total === 0) {
        if (readingChart) {
            readingChart.destroy();
            readingChart = null;
        }
        // Draw empty circle
        const canvas = ctx.getContext('2d');
        canvas.clearRect(0, 0, ctx.width, ctx.height);
        return;
    }
    
    const categoryLabels = {
        'BOOK': 'Books',
        'JOURNAL': 'Journals',
        'ARTICLE': 'Articles',
        'VIDEO': 'Videos',
        'AUDIO': 'Audio'
    };
    
    const categoryColors = {
        'BOOK': '#4B5320',
        'JOURNAL': '#3B82F6',
        'ARTICLE': '#22C55E',
        'VIDEO': '#EAB308',
        'AUDIO': '#8B5CF6'
    };
    
    // Filter out zero counts for cleaner chart
    const dataLabels = [];
    const dataValues = [];
    const dataColors = [];
    
    Object.keys(counts).forEach(cat => {
        if (counts[cat] > 0) {
            dataLabels.push(categoryLabels[cat]);
            dataValues.push(counts[cat]);
            dataColors.push(categoryColors[cat]);
        }
    });
    
    if (readingChart) {
        readingChart.destroy();
    }
    
    readingChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: dataLabels,
            datasets: [{
                data: dataValues,
                backgroundColor: dataColors,
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const percentage = total > 0 ? Math.round((value / total) * 100) : 0;
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            cutout: '60%'
        }
    });
}

function formatCategoryName(category) {
    if (!category) return '-';
    return category.charAt(0) + category.slice(1).toLowerCase();
}

function renderMyLibrary(purchases) {
    const container = document.getElementById('myLibraryList');
    const noLibrary = document.getElementById('noLibraryItems');
    
    if (!container) return;
    
    // Get unique resources from purchases
    const uniqueResources = [];
    const seenIds = new Set();
    purchases.forEach(purchase => {
        if (purchase.resourceID && !seenIds.has(purchase.resourceID)) {
            seenIds.add(purchase.resourceID);
            uniqueResources.push(purchase);
        }
    });
    
    if (uniqueResources.length === 0) {
        container.innerHTML = '';
        if (noLibrary) noLibrary.style.display = 'block';
        return;
    }
    
    if (noLibrary) noLibrary.style.display = 'none';
    
    container.innerHTML = uniqueResources.slice(0, 5).map(purchase => `
        <div class="library-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB; cursor: pointer; transition: background 0.2s;" 
             onclick="viewResource(${purchase.resourceID})"
             onmouseover="this.style.background='#F9FAFB'" 
             onmouseout="this.style.background='transparent'">
            <div style="width: 40px; height: 40px; border-radius: 8px; background: rgba(75, 83, 32, 0.1); display: flex; align-items: center; justify-content: center;">
                <i class="fas fa-book" style="color: #4B5320; font-size: 0.875rem;"></i>
            </div>
            <div style="flex: 1; min-width: 0;">
                <div style="font-weight: 600; color: #1F2937; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    ${escapeHtml(purchase.resourceTitle || 'Unknown Resource')}
                </div>
                <div style="font-size: 0.75rem; color: #6B7280;">
                    Purchased on ${formatDate(purchase.date)}
                </div>
            </div>
            <i class="fas fa-chevron-right" style="color: #9CA3AF; font-size: 0.75rem;"></i>
        </div>
    `).join('');
}

function renderRecommendedResources(allResources, purchasedResources) {
    const container = document.getElementById('recommendedResourcesList');
    const noRecommendations = document.getElementById('noRecommendations');
    
    if (!container) return;
    
    // Filter out already purchased resources
    const purchasedIds = new Set(purchasedResources.map(p => p.resourceID));
    const recommendations = allResources.filter(r => !purchasedIds.has(r.resourceID)).slice(0, 5);
    
    if (recommendations.length === 0) {
        container.innerHTML = '';
        if (noRecommendations) noRecommendations.style.display = 'block';
        return;
    }
    
    if (noRecommendations) noRecommendations.style.display = 'none';
    
    const categoryIcons = {
        'BOOK': 'fa-book',
        'JOURNAL': 'fa-newspaper',
        'ARTICLE': 'fa-file-alt',
        'VIDEO': 'fa-video',
        'AUDIO': 'fa-headphones'
    };
    
    const categoryColors = {
        'BOOK': '#4B5320',
        'JOURNAL': '#3B82F6',
        'ARTICLE': '#22C55E',
        'VIDEO': '#EAB308',
        'AUDIO': '#8B5CF6'
    };
    
    container.innerHTML = recommendations.map(resource => `
        <div class="recommendation-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB; cursor: pointer; transition: background 0.2s;" 
             onclick="viewResource(${resource.resourceID})"
             onmouseover="this.style.background='#F9FAFB'" 
             onmouseout="this.style.background='transparent'">
            <div style="width: 40px; height: 40px; border-radius: 8px; background: ${categoryColors[resource.category] || '#6B7280'}15; display: flex; align-items: center; justify-content: center;">
                <i class="fas ${categoryIcons[resource.category] || 'fa-book'}" style="color: ${categoryColors[resource.category] || '#6B7280'}; font-size: 0.875rem;"></i>
            </div>
            <div style="flex: 1; min-width: 0;">
                <div style="font-weight: 600; color: #1F2937; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    ${escapeHtml(resource.title)}
                </div>
                <div style="font-size: 0.75rem; color: #6B7280;">
                    ${escapeHtml(resource.author || 'Unknown Author')} • ${resource.price ? resource.price.toLocaleString() + ' XAF' : 'Free'}
                </div>
            </div>
            <span style="font-size: 0.75rem; padding: 0.25rem 0.5rem; border-radius: 1rem; background: ${resource.isPremiumOnly ? 'rgba(234, 179, 8, 0.2)' : 'rgba(34, 197, 94, 0.2)'}; color: ${resource.isPremiumOnly ? '#EAB308' : '#22C55E'};">
                ${resource.isPremiumOnly ? 'Premium' : 'Free'}
            </span>
        </div>
    `).join('');
}

function renderClientActivity(purchases, savedItems) {
    const container = document.getElementById('clientActivityList');
    const noActivity = document.getElementById('noClientActivity');
    
    if (!container) return;
    
    // Create activity items from purchases and saved items
    const activities = [];
    
    // Add purchase activities
    purchases.slice(0, 5).forEach(purchase => {
        activities.push({
            type: 'purchase',
            icon: 'shopping-bag',
            iconColor: '#4B5320',
            bgColor: 'rgba(75, 83, 32, 0.1)',
            title: `Purchased "${escapeHtml(purchase.resourceTitle || 'Unknown Resource')}"`,
            time: purchase.date,
            timestamp: purchase.date ? new Date(purchase.date).getTime() : 0
        });
    });
    
    // Add saved item activities
    savedItems.slice(0, 5).forEach(item => {
        activities.push({
            type: 'saved',
            icon: 'heart',
            iconColor: '#EAB308',
            bgColor: 'rgba(234, 179, 8, 0.1)',
            title: `Added "${escapeHtml(item.title || 'Unknown Resource')}" to My List`,
            time: item.createdAt || item.addedAt,
            timestamp: item.createdAt ? new Date(item.createdAt).getTime() : 
                      item.addedAt ? new Date(item.addedAt).getTime() : 0
        });
    });
    
    // Sort by time (newest first)
    activities.sort((a, b) => b.timestamp - a.timestamp);
    
    if (activities.length === 0) {
        container.innerHTML = '';
        if (noActivity) noActivity.style.display = 'block';
        return;
    }
    
    if (noActivity) noActivity.style.display = 'none';
    
    container.innerHTML = activities.slice(0, 10).map(activity => `
        <div class="activity-item" style="display: flex; align-items: center; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid #E5E7EB;">
            <div style="width: 32px; height: 32px; border-radius: 50%; background: ${activity.bgColor}; display: flex; align-items: center; justify-content: center;">
                <i class="fas fa-${activity.icon}" style="color: ${activity.iconColor}; font-size: 0.75rem;"></i>
            </div>
            <div style="flex: 1;">
                <div style="font-size: 0.875rem; color: #1F2937;">${activity.title}</div>
            </div>
            <div style="font-size: 0.75rem; color: #9CA3AF;">
                ${formatDate(activity.time)}
            </div>
        </div>
    `).join('');
}

function loadRevenueChart() {
    const ctx = document.getElementById('revenueChart');
    if (!ctx) return;
    
    const days = parseInt(document.getElementById('chartPeriod')?.value || '30');
    const labels = generateDateLabels(days);
    const data = generateRandomData(days, 100, 1000);
    
    if (revenueChart) {
        revenueChart.destroy();
    }
    
    revenueChart = new Chart(ctx, {
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
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

function generateDateLabels(days) {
    const labels = [];
    const today = new Date();
    for (let i = days - 1; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        labels.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    }
    return labels;
}

function generateRandomData(count, min, max) {
    return Array.from({ length: count }, () => Math.floor(Math.random() * (max - min + 1)) + min);
}

function viewResource(id) {
    window.location.href = `/resources.html?id=${id}`;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// ================================
// Browse Catalog Functions
// ================================

async function loadCatalogCounts() {
    try {
        const response = await resourcesAPI.search({ size: 1000 });
        if (!response.success || !response.data.content) return;
        
        const resources = response.data.content;
        const counts = {
            all: resources.length,
            books: resources.filter(r => r.category === 'BOOK').length,
            journals: resources.filter(r => r.category === 'JOURNAL').length,
            articles: resources.filter(r => r.category === 'ARTICLE').length,
            videos: resources.filter(r => r.category === 'VIDEO').length,
            audio: resources.filter(r => r.category === 'AUDIO').length
        };
        
        // Update catalog count displays
        updateCatalogCount('allResourcesCount', counts.all);
        updateCatalogCount('booksCount', counts.books);
        updateCatalogCount('journalsCount', counts.journals);
        updateCatalogCount('articlesCount', counts.articles);
        updateCatalogCount('videosCount', counts.videos);
        updateCatalogCount('audioCount', counts.audio);
    } catch (error) {
        console.log('Failed to load catalog counts');
    }
}

function updateCatalogCount(elementId, count) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = count > 0 ? `${count} items` : 'Coming soon';
    }
}

function browseCatalog(catalogType) {
    const catalogMap = {
        'all': '',
        'books': 'BOOK',
        'journals': 'JOURNAL',
        'articles': 'ARTICLE',
        'videos': 'VIDEO',
        'audio': 'AUDIO'
    };
    
    const category = catalogMap[catalogType];
    if (category) {
        window.location.href = `/resources?category=${category}`;
    } else {
        window.location.href = '/resources';
    }
}
