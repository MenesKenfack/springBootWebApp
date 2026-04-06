// Resources JavaScript
let currentPage = 0;
let currentFilters = {};

document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute()) return;
    
    setupNavigation();
    setupFilters();
    checkForSearchParam();
    checkForCategoryParam();
    
    // Load sections and resources in parallel
    Promise.all([
        loadResourceSections(),
        loadResources()
    ]);
});

function checkForSearchParam() {
    const urlParams = new URLSearchParams(window.location.search);
    const searchQuery = urlParams.get('search');
    
    if (searchQuery) {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = searchQuery;
            currentPage = 0;
            loadResources();
        }
    }
}

function checkForCategoryParam() {
    const urlParams = new URLSearchParams(window.location.search);
    const category = urlParams.get('category');
    
    if (category) {
        const categoryFilter = document.getElementById('categoryFilter');
        if (categoryFilter) {
            categoryFilter.value = category;
            currentPage = 0;
        }
    }
}

function setupFilters() {
    const searchInput = document.getElementById('searchInput');
    const categoryFilter = document.getElementById('categoryFilter');
    const sortBy = document.getElementById('sortBy');
    
    if (searchInput) {
        searchInput.addEventListener('input', debounce(() => {
            currentPage = 0;
            loadResources();
        }, 300));
    }
    
    if (categoryFilter) {
        categoryFilter.addEventListener('change', () => {
            currentPage = 0;
            loadResources();
        });
    }
    
    if (sortBy) {
        sortBy.addEventListener('change', () => {
            currentPage = 0;
            loadResources();
        });
    }
}

async function loadResources() {
    const searchInput = document.getElementById('searchInput');
    const sortBySelect = document.getElementById('sortBy');
    
    const params = {
        page: currentPage,
        size: 12
    };
    
    // Add search keyword
    if (searchInput?.value) {
        params.keyword = searchInput.value;
    }
    
    // Add sort order
    if (sortBySelect?.value) {
        params.sortBy = sortBySelect.value;
    }
    
    // Add filter parameters from currentFilters
    if (currentFilters.year) {
        params.year = currentFilters.year;
    }
    if (currentFilters.minPrice !== undefined) {
        params.minPrice = currentFilters.minPrice;
    }
    if (currentFilters.maxPrice !== undefined) {
        params.maxPrice = currentFilters.maxPrice;
    }
    if (currentFilters.category) {
        params.category = currentFilters.category;
    }
    
    try {
        const response = await resourcesAPI.search(params);
        
        if (response.success) {
            renderResources(response.data.content);
            renderPagination(response.data);
            
            // Update showing text
            const showingText = document.querySelector('.showing-text');
            if (showingText && response.data) {
                const showing = response.data.numberOfElements || 0;
                const total = response.data.totalElements || 0;
                showingText.textContent = `Showing ${showing} from ${total} data`;
            }
        } else {
            Toast.error(response.message || 'Failed to load resources');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to load resources');
    }
}

function renderResources(resources) {
    const container = document.getElementById('resourcesGrid');
    if (!container) return;
    
    // Check if user is logged in
    const token = TokenManager.getToken();
    const user = UserStorage.getUser();
    const isLoggedIn = !!token;
    const isClient = isLoggedIn && user?.role === 'ROLE_CLIENT';
    
    if (resources.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
                <i class="fas fa-search" style="font-size: 3rem; color: var(--text-light); margin-bottom: 1rem;"></i>
                <h3>No resources found</h3>
                <p>Try adjusting your search or filters</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = resources.map(resource => `
        <div class="resource-card" onclick="showResourceDetail(${resource.resourceID})">
            <div class="resource-image" style="position: relative;">
                ${resource.resourceImage ? 
                    `<img src="${resource.resourceImage}" alt="${resource.title}">` : 
                    `<i class="fas fa-${getCategoryIcon(resource.category)}"></i>`
                }
                <button class="favorite-btn" onclick="event.stopPropagation(); toggleFavorite(${resource.resourceID}, this)" 
                    style="position: absolute; top: 0.5rem; right: 0.5rem; background: white; border: none; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.15); transition: all 0.2s; z-index: 10;"
                    onmouseover="this.style.transform='scale(1.1)'" onmouseout="this.style.transform='scale(1)'">
                    <i class="far fa-heart" style="color: #9CA3AF; font-size: 1rem; transition: color 0.2s;"></i>
                </button>
            </div>
            <div class="resource-info">
                <h4>${escapeHtml(resource.title)}</h4>
                <p>${escapeHtml(resource.author || 'Unknown Author')}</p>
                ${renderStarRating(resource.averageRating, resource.ratingCount)}
                <div class="resource-meta">
                    <span class="resource-price">${resource.price ? resource.price.toLocaleString() : '0'} XAF</span>
                    ${resource.premiumOnly ? '<span class="resource-badge">Premium</span>' : ''}
                </div>
            </div>
            <div class="resource-actions" onclick="event.stopPropagation()">
                ${resource.hasFullAccess || resource.price === 0 ? `
                    <button class="btn-read" onclick="readResource(${resource.resourceID})" title="Read">
                        <i class="fas fa-book-open"></i> Read
                    </button>
                ` : isLoggedIn ? `
                    <button class="btn-purchase" onclick="purchaseResource(${resource.resourceID})" title="Purchase">
                        <i class="fas fa-shopping-cart"></i> Buy
                    </button>
                ` : `
                    <button class="btn-purchase" onclick="window.location.href='/login'" title="Login to Purchase">
                        <i class="fas fa-sign-in-alt"></i> Login to Buy
                    </button>
                `}
                <button class="btn-rate" onclick="openRatingModal(${resource.resourceID}, '${escapeHtml(resource.title)}')" title="Rate">
                    <i class="fas fa-star"></i> Rate
                </button>
            </div>
        </div>
    `).join('');
}

function renderPagination(pageData) {
    const container = document.getElementById('pagination');
    if (!container) return;
    
    const totalPages = pageData.totalPages;
    
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // Previous button
    html += `
        <button class="page-btn" ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">
            <i class="fas fa-chevron-left"></i>
        </button>
    `;
    
    // Page numbers
    for (let i = 0; i < totalPages; i++) {
        html += `
            <button class="page-btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">
                ${i + 1}
            </button>
        `;
    }
    
    // Next button
    html += `
        <button class="page-btn" ${currentPage === totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">
            <i class="fas fa-chevron-right"></i>
        </button>
    `;
    
    container.innerHTML = html;
}

function goToPage(page) {
    currentPage = page;
    loadResources();
}

async function showResourceDetail(id) {
    try {
        const response = await resourcesAPI.getContent(id);
        
        if (response.success) {
            const resource = response.data;
            const modal = document.getElementById('resourceModal');
            const titleEl = document.getElementById('modalResourceTitle');
            const bodyEl = document.getElementById('modalResourceBody');
            const footerEl = document.getElementById('modalResourceFooter');
            
            titleEl.textContent = resource.title;
            bodyEl.innerHTML = `
                <div class="resource-detail">
                    <div class="resource-detail-image">
                        ${resource.resourceImage ? 
                            `<img src="${resource.resourceImage}" alt="${resource.title}">` : 
                            `<i class="fas fa-${getCategoryIcon(resource.category)}"></i>`
                        }
                    </div>
                    <div class="resource-detail-info">
                        <p><strong>Author:</strong> ${escapeHtml(resource.author || 'Unknown')}</p>
                        <p><strong>Category:</strong> ${resource.category}</p>
                        <p><strong>Year:</strong> ${resource.publicationYear || 'N/A'}</p>
                        <p><strong>ISBN:</strong> ${resource.isbn || 'N/A'}</p>
                        <p><strong>Price:</strong> ${resource.price ? resource.price.toLocaleString() : '0'} XAF</p>
                        <p><strong>Your Tier:</strong> ${resource.userTier || 'BASIC'}</p>
                        <hr>
                        ${renderResourceContent(resource)}
                    </div>
                </div>
            `;
            
            footerEl.innerHTML = resource.hasFullAccess ? `
                <button class="btn btn-primary" onclick="showReaderModal(${JSON.stringify(resource).replace(/"/g, '&quot;')})">
                    <i class="fas fa-book-open"></i> Read Now
                </button>
            ` : `
                <button class="btn btn-primary" onclick="purchaseResource(${resource.resourceID})">
                    <i class="fas fa-shopping-cart"></i> ${resource.userTier === 'PREMIUM' ? 'Unlock Full Access' : 'Upgrade to Premium'}
                </button>
            `;
            
            modal.style.display = 'flex';
        } else {
            Toast.error(response.message || 'Failed to load resource details');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to load resource details');
    }
}

async function purchaseResource(resourceId) {
    // Check if user is logged in and has client role
    const user = UserStorage.getUser();
    const token = TokenManager.getToken();
    
    if (!token) {
        Toast.error('Please log in to purchase resources');
        window.location.href = '/login';
        return;
    }
    
    if (!user || user.role !== 'ROLE_CLIENT') {
        Toast.error('Only clients can purchase resources. Current role: ' + (user?.role || 'unknown'));
        return;
    }
    
    // Show loading state
    const btn = document.activeElement;
    if (btn && btn.tagName === 'BUTTON') {
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
    }
    
    try {
        console.log('Initiating purchase for resource:', resourceId);
        
        // Step 1: Initiate purchase
        const response = await purchaseAPI.initiate(resourceId);
        console.log('Purchase initiate response:', response);
        
        if (!response || !response.success) {
            throw new Error(response?.message || 'Failed to initiate purchase');
        }
        
        const paymentId = response.data?.paymentId;
        if (!paymentId) {
            throw new Error('Invalid response: no payment ID received');
        }
        
        Toast.success('Purchase initiated! Redirecting to checkout...');
        
        // Step 2: Checkout
        console.log('Proceeding to checkout for payment:', paymentId);
        const checkoutResponse = await purchaseAPI.checkout(paymentId);
        console.log('Checkout response:', checkoutResponse);
        
        if (checkoutResponse.success && checkoutResponse.data?.checkoutUrl) {
            window.location.href = checkoutResponse.data.checkoutUrl;
        } else {
            Toast.success('Purchase initiated. Complete payment to access the resource.');
            // Refresh resources to update UI
            loadResources();
        }
    } catch (error) {
        console.error('Purchase error:', error);
        
        // Provide user-friendly error messages
        let errorMessage = error.message || 'Purchase failed';
        
        if (errorMessage.includes('403') || errorMessage.includes('Forbidden')) {
            errorMessage = 'You do not have permission to purchase. Please ensure you have CLIENT role.';
        } else if (errorMessage.includes('401') || errorMessage.includes('Unauthorized')) {
            errorMessage = 'Your session has expired. Please log in again.';
            TokenManager.clearTokens();
            window.location.href = '/login';
        } else if (errorMessage.includes('already purchased')) {
            errorMessage = 'You have already purchased this resource!';
        } else if (errorMessage.includes('Server returned invalid response')) {
            errorMessage = 'Payment service is temporarily unavailable. Please try again later.';
        }
        
        Toast.error(errorMessage);
    } finally {
        // Reset button state
        if (btn && btn.tagName === 'BUTTON') {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-shopping-cart"></i> Purchase Now';
        }
    }
}

function closeResourceModal() {
    const modal = document.getElementById('resourceModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function getCategoryIcon(category) {
    const icons = {
        'BOOK': 'book',
        'JOURNAL': 'journal-whills',
        'ARTICLE': 'newspaper',
        'VIDEO': 'video',
        'AUDIO': 'headphones',
        'DOCUMENT': 'file-alt'
    };
    return icons[category] || 'book';
}

function renderStarRating(rating, count) {
    const numRating = rating || 0;
    const fullStars = Math.floor(numRating);
    const hasHalfStar = numRating % 1 >= 0.5;
    const displayCount = count || 0;
    
    let starsHtml = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= fullStars) {
            starsHtml += '<i class="fas fa-star star filled"></i>';
        } else if (i === fullStars + 1 && hasHalfStar) {
            starsHtml += '<i class="fas fa-star-half-alt star filled"></i>';
        } else {
            starsHtml += '<i class="far fa-star star"></i>';
        }
    }
    
    return `
        <div class="resource-rating">
            ${starsHtml}
            <span class="rating-count">(${displayCount})</span>
        </div>
    `;
}

async function readResource(resourceId) {
    try {
        const response = await resourcesAPI.getContent(resourceId);
        
        if (response.success) {
            const resource = response.data;
            // Open resource in new tab or show reader modal
            if (resource.fullContent) {
                window.open(resource.fullContent, '_blank');
            } else {
                // Show content in modal
                showReaderModal(resource);
            }
        } else {
            Toast.error(response.message || 'Failed to load resource content');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to read resource');
    }
}

function showReaderModal(resource) {
    const modal = document.getElementById('resourceModal');
    const titleEl = document.getElementById('modalResourceTitle');
    const bodyEl = document.getElementById('modalResourceBody');
    const footerEl = document.getElementById('modalResourceFooter');
    
    titleEl.textContent = resource.title;
    
    // Check if user has full access or is BASIC tier with limited preview
    const isBasicTier = resource.userTier === 'BASIC';
    const hasFullAccess = resource.hasFullAccess;
    
    if (hasFullAccess) {
        // PREMIUM or purchased users get full content
        bodyEl.innerHTML = `
            <div class="resource-reader">
                <div class="access-badge premium">
                    <i class="fas fa-crown"></i> Full Access
                </div>
                <div class="reader-content full-access" style="max-height: 60vh; overflow-y: auto; padding: 1rem; background: var(--bg-abyss); border-radius: 0.5rem; white-space: pre-wrap;">
                    ${escapeHtml(resource.fullContent || resource.description || 'No content available')}
                </div>
            </div>
        `;
        footerEl.innerHTML = `
            <button class="btn btn-secondary" onclick="closeResourceModal()">Close</button>
        `;
    } else if (isBasicTier && resource.previewContent) {
        // BASIC users get encrypted/blurred preview (1/3 visible, 2/3 blurred)
        const previewParts = parsePreviewContent(resource.previewContent);
        bodyEl.innerHTML = `
            <div class="resource-reader">
                <div class="access-badge basic">
                    <i class="fas fa-lock"></i> BASIC Preview (1/3 visible)
                </div>
                <div class="reader-content preview-content" style="max-height: 60vh; overflow-y: auto; padding: 1rem; background: var(--bg-abyss); border-radius: 0.5rem;">
                    <div class="visible-content" style="white-space: pre-wrap;">
                        ${escapeHtml(previewParts.visible)}
                    </div>
                    <div class="encrypted-divider" style="margin: 1rem 0; text-align: center; position: relative;">
                        <div style="border-top: 2px dashed var(--border-color); position: absolute; top: 50%; left: 0; right: 0;"></div>
                        <span style="background: var(--bg-abyss); padding: 0 1rem; position: relative; color: var(--text-light); font-size: 0.75rem;">
                            <i class="fas fa-lock"></i> ENCRYPTED CONTENT
                        </span>
                    </div>
                    <div class="blurred-content" style="filter: blur(5px); opacity: 0.6; user-select: none; pointer-events: none; white-space: pre-wrap;">
                        ${escapeHtml(previewParts.encrypted.substring(0, 200))}...
                    </div>
                </div>
                <div class="upgrade-notice" style="margin-top: 1rem; padding: 1rem; background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.3); border-radius: 0.5rem; text-align: center;">
                    <p style="margin: 0; color: var(--text-medium);">
                        <i class="fas fa-crown" style="color: #FBBF24;"></i>
                        Upgrade to <strong>PREMIUM</strong> or purchase this resource to unlock full content
                    </p>
                </div>
            </div>
        `;
        footerEl.innerHTML = `
            <button class="btn btn-secondary" onclick="closeResourceModal()">Close</button>
            <button class="btn btn-primary" onclick="purchaseResource(${resource.resourceID})" style="background: linear-gradient(135deg, #F59E0B, #D97706); border: none;">
                <i class="fas fa-crown"></i> Upgrade to PREMIUM
            </button>
        `;
    } else {
        // No preview available
        bodyEl.innerHTML = `
            <div class="resource-reader">
                <div class="empty-state" style="text-align: center; padding: 2rem;">
                    <i class="fas fa-lock" style="font-size: 3rem; color: var(--text-light); margin-bottom: 1rem;"></i>
                    <h4>Content Locked</h4>
                    <p style="color: var(--text-medium);">Purchase this resource or upgrade to PREMIUM to view content</p>
                </div>
            </div>
        `;
        footerEl.innerHTML = `
            <button class="btn btn-secondary" onclick="closeResourceModal()">Close</button>
            <button class="btn btn-primary" onclick="purchaseResource(${resource.resourceID})">
                <i class="fas fa-shopping-cart"></i> Purchase Now
            </button>
        `;
    }
    
    modal.style.display = 'flex';
}

/**
 * Parse preview content to extract visible and encrypted parts
 */
function parsePreviewContent(previewContent) {
    const visibleMatch = previewContent.match(/^([\s\S]*?)\[ENCRYPTED_PREVIEW_START\]/);
    const encryptedMatch = previewContent.match(/\[ENCRYPTED_PREVIEW_START\]([\s\S]*?)\[ENCRYPTED_PREVIEW_END\]/);
    
    return {
        visible: visibleMatch ? visibleMatch[1].trim() : previewContent.substring(0, previewContent.length / 3),
        encrypted: encryptedMatch ? encryptedMatch[1].trim() : 'ENCRYPTED_CONTENT'
    };
}

/**
 * Render resource content based on access level
 */
function renderResourceContent(resource) {
    if (resource.hasFullAccess) {
        return `
            <div class="alert alert-success" style="background: rgba(78, 201, 163, 0.15); border: 1px solid rgba(78, 201, 163, 0.3); color: #4EC9A3; padding: 0.75rem 1rem; border-radius: 0.5rem; margin-bottom: 1rem;">
                <i class="fas fa-check-circle"></i> <strong>Full Access</strong> - You can read the complete resource
            </div>
            <div class="content-preview" style="background: var(--bg-abyss); padding: 1rem; border-radius: 0.5rem; border: 1px solid var(--border-color);">
                <p style="margin: 0; white-space: pre-wrap;">${escapeHtml(resource.description || 'No description available')}</p>
            </div>
        `;
    } else if (resource.userTier === 'BASIC' && resource.previewContent) {
        const previewParts = parsePreviewContent(resource.previewContent);
        return `
            <div class="alert alert-warning" style="background: rgba(245, 158, 11, 0.15); border: 1px solid rgba(245, 158, 11, 0.3); color: #FBBF24; padding: 0.75rem 1rem; border-radius: 0.5rem; margin-bottom: 1rem;">
                <i class="fas fa-lock"></i> <strong>BASIC Tier</strong> - Preview only (1/3 visible, 2/3 encrypted)
            </div>
            <div class="content-preview" style="background: var(--bg-abyss); padding: 1rem; border-radius: 0.5rem; border: 1px solid var(--border-color);">
                <p style="margin: 0; white-space: pre-wrap;">${escapeHtml(previewParts.visible)}</p>
                <div style="margin: 0.75rem 0; text-align: center;">
                    <span style="font-size: 0.75rem; color: var(--text-light); background: var(--bg-sapphire); padding: 0.25rem 0.75rem; border-radius: 1rem;">
                        <i class="fas fa-ellipsis-h"></i> Content Encrypted
                    </span>
                </div>
            </div>
        `;
    } else {
        return `
            <div class="alert alert-info" style="background: rgba(74, 110, 219, 0.15); border: 1px solid rgba(74, 110, 219, 0.3); color: #7A9EFF; padding: 0.75rem 1rem; border-radius: 0.5rem; margin-bottom: 1rem;">
                <i class="fas fa-info-circle"></i> Purchase or upgrade to PREMIUM to view content
            </div>
            <p>${escapeHtml(resource.description || 'No description available')}</p>
        `;
    }
}

let currentRatingResourceId = null;

function openRatingModal(resourceId, resourceTitle) {
    currentRatingResourceId = resourceId;
    const modal = document.getElementById('ratingModal');
    const titleEl = document.getElementById('ratingResourceTitle');
    
    titleEl.textContent = `Rate: ${resourceTitle}`;
    
    // Reset stars
    document.querySelectorAll('.rating-star').forEach(star => {
        star.classList.remove('selected');
        star.querySelector('i').classList.remove('fas');
        star.querySelector('i').classList.add('far');
    });
    document.getElementById('ratingComment').value = '';
    
    modal.style.display = 'flex';
}

function closeRatingModal() {
    document.getElementById('ratingModal').style.display = 'none';
    currentRatingResourceId = null;
}

function setRating(rating) {
    document.querySelectorAll('.rating-star').forEach((star, index) => {
        const icon = star.querySelector('i');
        if (index < rating) {
            star.classList.add('selected');
            icon.classList.remove('far');
            icon.classList.add('fas');
        } else {
            star.classList.remove('selected');
            icon.classList.remove('fas');
            icon.classList.add('far');
        }
    });
    document.getElementById('ratingModal').dataset.rating = rating;
}

async function submitRating() {
    if (!currentRatingResourceId) return;
    
    const rating = parseInt(document.getElementById('ratingModal').dataset.rating || '0');
    const comment = document.getElementById('ratingComment').value.trim();
    
    if (rating === 0) {
        Toast.warning('Please select a rating');
        return;
    }
    
    try {
        const response = await myListAPI.rateResource(currentRatingResourceId, rating, comment);
        
        if (response.success) {
            Toast.success('Rating submitted successfully');
            closeRatingModal();
            loadResources(); // Refresh to show updated rating
        } else {
            Toast.error(response.message || 'Failed to submit rating');
        }
    } catch (error) {
        Toast.error(error.message || 'Failed to submit rating');
    }
}

// ================================
// Professional Resource Card Renderer
// ================================

function renderProfessionalCard(resource, options = {}) {
    const { showBadge = true, compact = false } = options;
    
    const hasFullAccess = resource.hasFullAccess || resource.price === 0;
    const isFree = resource.price === 0 || !resource.price;
    const isNew = resource.isNew || (resource.createdAt && isRecentlyAdded(resource.createdAt));
    
    // Determine badge
    let badgeHtml = '';
    if (showBadge) {
        if (resource.premiumOnly) {
            badgeHtml = '<span class="badge-premium"><i class="fas fa-crown"></i> Premium</span>';
        } else if (isNew) {
            badgeHtml = '<span class="badge-new"><i class="fas fa-sparkles"></i> New</span>';
        } else if (isFree) {
            badgeHtml = '<span class="badge-free"><i class="fas fa-gift"></i> Free</span>';
        }
    }
    
    // Check authentication status
    const token = TokenManager.getToken();
    const user = UserStorage.getUser();
    const isLoggedIn = !!token;
    const isClient = isLoggedIn && user?.role === 'ROLE_CLIENT';
    
    // Determine action button
    let actionBtn = '';
    if (hasFullAccess) {
        actionBtn = `
            <button class="card-action-btn read" onclick="event.stopPropagation(); readResource(${resource.resourceID})">
                <i class="fas fa-book-open"></i> Read
            </button>
        `;
    } else if (!isLoggedIn) {
        actionBtn = `
            <button class="card-action-btn buy" onclick="event.stopPropagation(); window.location.href='/login'">
                <i class="fas fa-sign-in-alt"></i> Login to Buy
            </button>
        `;
    } else if (!isClient) {
        // Logged in but not a client (Librarian/Manager)
        actionBtn = `
            <button class="card-action-btn" onclick="event.stopPropagation(); showNonClientMessage()" title="Only clients can purchase">
                <i class="fas fa-lock"></i> Client Only
            </button>
        `;
    } else {
        actionBtn = `
            <button class="card-action-btn buy" onclick="event.stopPropagation(); purchaseResource(${resource.resourceID})">
                <i class="fas fa-shopping-cart"></i> ${isFree ? 'Get Free' : 'Buy'}
            </button>
        `;
    }
    
    // Price display
    const priceHtml = isFree 
        ? '<span class="card-price free"><i class="fas fa-gift"></i> FREE</span>'
        : `<span class="card-price">${resource.price ? resource.price.toLocaleString() : '0'} XAF</span>`;
    
    return `
        <div class="resource-card-pro" onclick="showResourceDetail(${resource.resourceID})">
            <div class="card-badges">
                <div>${badgeHtml}</div>
                <button class="card-favorite-btn" onclick="event.stopPropagation(); toggleFavoritePro(${resource.resourceID}, this)" title="Add to favorites">
                    <i class="far fa-heart"></i>
                </button>
            </div>
            <div class="card-image-wrapper">
                ${resource.resourceImage ? 
                    `<img src="${resource.resourceImage}" alt="${resource.title}" class="card-image">` : 
                    `<div class="card-image-placeholder"><i class="fas fa-${getCategoryIcon(resource.category)}"></i></div>`
                }
                <div class="card-image-overlay"></div>
            </div>
            <div class="card-content">
                <div class="card-category">
                    <i class="fas fa-${getCategoryIcon(resource.category)}"></i>
                    ${resource.category || 'Resource'}
                </div>
                <h3 class="card-title">${escapeHtml(resource.title)}</h3>
                <p class="card-author">${escapeHtml(resource.author || 'Unknown Author')}</p>
                
                <div class="card-stats">
                    <div class="card-stat rating">
                        <i class="fas fa-star"></i>
                        <span>${resource.averageRating ? resource.averageRating.toFixed(1) : '0.0'} (${resource.ratingCount || 0})</span>
                    </div>
                    <div class="card-stat views">
                        <i class="fas fa-eye"></i>
                        <span>${resource.viewCount || 0}</span>
                    </div>
                </div>
                
                <div class="card-footer">
                    ${priceHtml}
                    ${actionBtn}
                </div>
            </div>
        </div>
    `;
}

function renderSectionHeader(title, icon, type, viewAllLink = null) {
    const viewAllHtml = viewAllLink 
        ? `<a href="${viewAllLink}" class="view-all-link">View All <i class="fas fa-chevron-right"></i></a>`
        : '';
    
    return `
        <div class="section-header">
            <h2 class="section-title ${type}">
                <i class="fas fa-${icon}"></i>
                ${title}
            </h2>
            ${viewAllHtml}
        </div>
    `;
}

async function loadResourceSections() {
    // Load all sections in parallel
    await Promise.all([
        loadRecommendedSection(),
        loadNewlyAddedSection(),
        loadTopRatedSection()
    ]);
}

async function loadRecommendedSection() {
    const container = document.getElementById('recommendedSection');
    if (!container) return;
    
    try {
        // Get user preferences or use popular resources as fallback
        const response = await resourcesAPI.search({ size: 10, sort: 'popular' });
        
        if (response.success && response.data.content.length > 0) {
            const resources = response.data.content.slice(0, 6);
            container.innerHTML = `
                <div class="section-card recommended">
                    ${renderSectionHeader('Recommended For You', 'magic', 'recommended', '/resources?sort=popular')}
                    <div class="section-scroll">
                        ${resources.map(r => renderProfessionalCard(r)).join('')}
                    </div>
                </div>
            `;
        } else {
            container.innerHTML = '';
        }
    } catch (error) {
        Toast.error('Failed to load recommended section');
        container.innerHTML = '';
    }
}

async function loadNewlyAddedSection() {
    const container = document.getElementById('newlyAddedSection');
    if (!container) return;
    
    try {
        const response = await resourcesAPI.getRecent();
        
        if (response.success && response.data.length > 0) {
            const resources = response.data.slice(0, 6).map(r => ({ ...r, isNew: true }));
            container.innerHTML = `
                <div class="section-card new">
                    ${renderSectionHeader('Newly Added', 'sparkles', 'new', '/resources?sort=newest')}
                    <div class="section-scroll">
                        ${resources.map(r => renderProfessionalCard(r)).join('')}
                    </div>
                </div>
            `;
        } else {
            container.innerHTML = '';
        }
    } catch (error) {
        Toast.error('Failed to load newly added section');
        container.innerHTML = '';
    }
}

async function loadTopRatedSection() {
    const container = document.getElementById('topRatedSection');
    if (!container) return;
    
    try {
        // Use search with high rating filter or sort by rating
        const response = await resourcesAPI.search({ size: 10 });
        
        if (response.success && response.data.content.length > 0) {
            // Sort by rating
            const resources = response.data.content
                .filter(r => r.averageRating > 0)
                .sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0))
                .slice(0, 6);
            
            if (resources.length > 0) {
                container.innerHTML = `
                    <div class="section-card top-rated">
                        ${renderSectionHeader('Top Rated', 'star', 'top-rated', '/resources?sort=rating')}
                        <div class="section-scroll">
                            ${resources.map(r => renderProfessionalCard(r)).join('')}
                        </div>
                    </div>
                `;
            } else {
                container.innerHTML = '';
            }
        } else {
            container.innerHTML = '';
        }
    } catch (error) {
        Toast.error('Failed to load top rated section');
        container.innerHTML = '';
    }
}

function isRecentlyAdded(dateString) {
    if (!dateString) return false;
    const date = new Date(dateString);
    const now = new Date();
    const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));
    return diffDays <= 7; // Within last 7 days
}

async function toggleFavoritePro(resourceId, btnElement) {
    const icon = btnElement.querySelector('i');
    const isFavorite = icon.classList.contains('fas');
    
    try {
        btnElement.classList.add('active');
        
        if (isFavorite) {
            await myListAPI.removeFavorite(resourceId);
            icon.classList.remove('fas', 'fa-heart');
            icon.classList.add('far', 'fa-heart');
            Toast.success('Removed from favorites');
        } else {
            await myListAPI.addFavorite(resourceId);
            icon.classList.remove('far', 'fa-heart');
            icon.classList.add('fas', 'fa-heart');
            Toast.success('Added to favorites');
        }
    } catch (error) {
        Toast.error('Failed to update favorite');
        btnElement.classList.remove('active');
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
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

// Toggle favorite status for a resource
async function toggleFavorite(resourceId, btnElement) {
    const icon = btnElement.querySelector('i');
    const isFavorite = icon.classList.contains('fas');
    
    try {
        if (isFavorite) {
            // Remove from favorites (mock)
            icon.classList.remove('fas', 'fa-heart');
            icon.classList.add('far', 'fa-heart');
            icon.style.color = '#9CA3AF';
            Toast.success('Removed from favorites');
        } else {
            // Add to favorites (mock)
            icon.classList.remove('far', 'fa-heart');
            icon.classList.add('fas', 'fa-heart');
            icon.style.color = '#EF4444';
            Toast.success('Added to favorites');
        }
        
        // In a real implementation, you would call the API:
        // if (isFavorite) {
        //     await myListAPI.removeFavorite(resourceId);
        // } else {
        //     await myListAPI.addFavorite(resourceId);
        // }
    } catch (error) {
        Toast.error('Failed to update favorite');
    }
}

// ================================
// NEW SIDEBAR FILTER FUNCTIONS
// ================================

// Toggle accordion open/close
function toggleAccordion(header) {
    const content = header.nextElementSibling;
    const icon = header.querySelector('.accordion-icon');
    
    header.classList.toggle('active');
    content.classList.toggle('expanded');
    
    if (header.classList.contains('active')) {
        icon.classList.remove('fa-plus');
        icon.classList.add('fa-minus');
    } else {
        icon.classList.remove('fa-minus');
        icon.classList.add('fa-plus');
    }
}

// Category checkbox toggle
function setupCategoryCheckboxes() {
    const checkboxes = document.querySelectorAll('.category-checkbox input');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            this.closest('.category-checkbox').classList.toggle('checked', this.checked);
        });
    });
}

// Price Range Slider
let minPrice = 3145;
let maxPrice = 33430;
let currentMinPrice = 3145;
let currentMaxPrice = 33430;

function initPriceSlider() {
    const slider = document.querySelector('.range-slider');
    const minThumb = document.getElementById('minThumb');
    const maxThumb = document.getElementById('maxThumb');
    const rangeFill = document.getElementById('priceRangeFill');
    
    if (!slider || !minThumb || !maxThumb) return;
    
    let isDragging = false;
    let currentThumb = null;
    
    function updateSlider() {
        const minPercent = ((currentMinPrice - minPrice) / (maxPrice - minPrice)) * 100;
        const maxPercent = ((currentMaxPrice - minPrice) / (maxPrice - minPrice)) * 100;
        
        minThumb.style.left = minPercent + '%';
        maxThumb.style.left = maxPercent + '%';
        
        if (rangeFill) {
            rangeFill.style.left = minPercent + '%';
            rangeFill.style.width = (maxPercent - minPercent) + '%';
        }
        
        // Update labels
        const minLabel = document.querySelector('.price-min');
        const maxLabel = document.querySelector('.price-max');
        if (minLabel) minLabel.textContent = 'XAF' + currentMinPrice.toLocaleString();
        if (maxLabel) maxLabel.textContent = 'XAF' + currentMaxPrice.toLocaleString();
    }
    
    function handleDrag(e) {
        if (!isDragging || !currentThumb) return;
        
        const rect = slider.getBoundingClientRect();
        const x = (e.type.includes('touch') ? e.touches[0].clientX : e.clientX) - rect.left;
        let percent = (x / rect.width) * 100;
        percent = Math.max(0, Math.min(100, percent));
        
        const value = Math.round(minPrice + (percent / 100) * (maxPrice - minPrice));
        
        if (currentThumb === minThumb) {
            currentMinPrice = Math.min(value, currentMaxPrice - 1000);
        } else {
            currentMaxPrice = Math.max(value, currentMinPrice + 1000);
        }
        
        updateSlider();
    }
    
    function stopDrag() {
        isDragging = false;
        currentThumb = null;
    }
    
    minThumb.addEventListener('mousedown', () => { isDragging = true; currentThumb = minThumb; });
    minThumb.addEventListener('touchstart', () => { isDragging = true; currentThumb = minThumb; });
    
    maxThumb.addEventListener('mousedown', () => { isDragging = true; currentThumb = maxThumb; });
    maxThumb.addEventListener('touchstart', () => { isDragging = true; currentThumb = maxThumb; });
    
    document.addEventListener('mousemove', handleDrag);
    document.addEventListener('touchmove', handleDrag);
    document.addEventListener('mouseup', stopDrag);
    document.addEventListener('touchend', stopDrag);
    
    updateSlider();
}

// Refine Search
function refineSearch() {
    // Collect all filter values
    const publisher = document.getElementById('publisherFilter')?.value;
    const year = document.getElementById('yearFilter')?.value;
    const selectedCategories = Array.from(document.querySelectorAll('.category-checkbox.checked input')).map(cb => cb.value);
    
    currentFilters = {
        publisher,
        year,
        categories: selectedCategories,
        minPrice: currentMinPrice,
        maxPrice: currentMaxPrice
    };
    
    currentPage = 0;
    loadResources();
    Toast.success('Filters applied');
}

// Reset Filters
function resetFilters() {
    // Reset dropdowns
    const publisherFilter = document.getElementById('publisherFilter');
    const yearFilter = document.getElementById('yearFilter');
    if (publisherFilter) publisherFilter.value = '';
    if (yearFilter) yearFilter.value = '';
    
    // Reset category checkboxes
    document.querySelectorAll('.category-checkbox input').forEach(checkbox => {
        checkbox.checked = false;
        checkbox.closest('.category-checkbox').classList.remove('checked');
    });
    
    // Reset price slider
    currentMinPrice = minPrice;
    currentMaxPrice = maxPrice;
    initPriceSlider();
    
    // Reset filters and reload
    currentFilters = {};
    currentPage = 0;
    loadResources();
    Toast.success('Filters reset');
}

// ================================
// UPDATED RENDER FUNCTIONS
// ================================

// Render book card with new design
function renderBookCard(resource) {
    const isFree = !resource.price || resource.price === 0;
    const hasFullAccess = resource.hasFullAccess || isFree;
    const isFavorite = false; // TODO: Check favorites from API
    
    // Generate star rating HTML (4 filled, 1 empty for demo)
    const rating = resource.averageRating || 4;
    let starsHtml = '';
    for (let i = 1; i <= 5; i++) {
        if (i <= rating) {
            starsHtml += '<i class="fas fa-star star filled"></i>';
        } else {
            starsHtml += '<i class="far fa-star star"></i>';
        }
    }
    
    // Special price display for "Terrible Madness" style
    const isOnSale = resource.resourceID === 3; // Demo: 3rd item on sale
    const originalPrice = isOnSale ? resource.price + 1500 : null;
    
    const priceDisplay = isFree 
        ? '<span class="price-current">FREE</span>'
        : isOnSale 
            ? `<span class="price-current">XAF${resource.price?.toLocaleString() || '0'}</span>
               <span class="price-original">XAF${originalPrice?.toLocaleString()}</span>`
            : `<span class="price-current">XAF${resource.price?.toLocaleString() || '0'}</span>`;
    
    return `
        <div class="book-card" onclick="showResourceDetail(${resource.resourceID})">
            <div class="book-card-image">
                ${resource.resourceImage 
                    ? `<img src="${resource.resourceImage}" alt="${escapeHtml(resource.title)}">`
                    : `<div style="width:100%;height:100%;background:linear-gradient(135deg,#7E22CE,#A855F7);display:flex;align-items:center;justify-content:center;color:white;font-size:3rem;"><i class="fas fa-book"></i></div>`
                }
                <button class="heart-btn ${isFavorite ? '' : 'outline'}" onclick="event.stopPropagation(); toggleFavorite(${resource.resourceID}, this)">
                    <i class="${isFavorite ? 'fas' : 'far'} fa-heart"></i>
                </button>
            </div>
            <div class="book-card-content">
                <h3 class="book-title">${escapeHtml(resource.title)}</h3>
                <p class="book-genre">${resource.category || 'BOOK'}</p>
                <div class="star-rating">
                    ${starsHtml}
                </div>
                <div class="book-price">
                    ${priceDisplay}
                </div>
                ${hasFullAccess ? `
                    <button class="btn-buy" style="background: linear-gradient(135deg, #10B981, #059669);" onclick="event.stopPropagation(); showResourceDetail(${resource.resourceID})">
                        <i class="fas fa-book-open"></i> Read
                    </button>
                ` : `
                    <button class="btn-buy" onclick="event.stopPropagation(); handleBuyClick(${resource.resourceID}, ${hasFullAccess}, ${isFree})">
                        <i class="fas fa-shopping-cart"></i> Buy
                    </button>
                `}
            </div>
        </div>
    `;
}

// Handle buy button click
function handleBuyClick(resourceId, hasFullAccess, isFree) {
    if (hasFullAccess || isFree) {
        readResource(resourceId);
    } else {
        purchaseResource(resourceId);
    }
}

// Override renderResources to use new book card design
const originalRenderResources = renderResources;
renderResources = function(resources) {
    const container = document.getElementById('resourcesGrid');
    if (!container) return;
    
    // Check if using new layout
    if (container.classList.contains('book-grid')) {
        if (resources.length === 0) {
            container.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
                    <i class="fas fa-search" style="font-size: 3rem; color: #9CA3AF; margin-bottom: 1rem;"></i>
                    <h3>No resources found</h3>
                    <p style="color: #6B7280;">Try adjusting your search or filters</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = resources.map(resource => renderBookCard(resource)).join('');
    } else {
        // Fall back to original renderer
        originalRenderResources(resources);
    }
};

// Tab switching
function setupTabs() {
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            const tabName = tab.dataset.tab;
            currentFilters.tab = tabName;
            currentPage = 0;
            loadResources();
        });
    });
}

// View mode switching
function setupViewModes() {
    const viewBtns = document.querySelectorAll('.view-btn');
    const grid = document.getElementById('resourcesGrid');
    
    viewBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            viewBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            const title = btn.getAttribute('title');
            if (title.includes('List')) {
                grid.className = 'book-grid list-view';
            } else if (title.includes('Small')) {
                grid.className = 'book-grid small-grid';
                grid.style.gridTemplateColumns = 'repeat(6, 1fr)';
            } else {
                grid.className = 'book-grid';
                grid.style.gridTemplateColumns = '';
            }
        });
    });
}

// Initialize new functionality
function initNewFeatures() {
    setupCategoryCheckboxes();
    initPriceSlider();
    setupTabs();
    setupViewModes();
    
    // Load sidebar metadata
    loadSidebarData();
    
    // Load filter options from database
    loadYearOptions();
    loadCategoryOptions();
}

// ================================
// FILTER OPTIONS LOADING
// ================================

// Load available years from database
async function loadYearOptions() {
    const yearSelect = document.getElementById('yearFilter');
    if (!yearSelect) return;
    
    try {
        const response = await apiClient.get('/resources/years');
        
        if (response.success && response.data && response.data.length > 0) {
            // Clear existing options except the first one
            while (yearSelect.options.length > 1) {
                yearSelect.remove(1);
            }
            
            // Add years from database
            response.data.forEach(year => {
                if (year) { // Only add valid years
                    const option = document.createElement('option');
                    option.value = year;
                    option.textContent = year;
                    yearSelect.appendChild(option);
                }
            });
        }
    } catch (error) {
        // Silently fail for year options - not critical
    }
}

// Load available categories from database
async function loadCategoryOptions() {
    const categoryGrid = document.getElementById('categoryGrid');
    if (!categoryGrid) return;
    
    try {
        const response = await apiClient.get('/resources/categories');
        
        if (response.success && response.data && response.data.length > 0) {
            categoryGrid.innerHTML = response.data.map((cat, index) => `
                <label class="category-checkbox ${index < 2 ? 'checked' : ''}">
                    <input type="checkbox" ${index < 2 ? 'checked' : ''} value="${cat.value}">
                    <span class="check-indicator"><i class="fas fa-check"></i></span>
                    <span class="category-label">${cat.label}</span>
                    <span class="category-count" style="color: #9CA3AF; font-size: 0.75rem; margin-left: auto;">(${cat.count || 0})</span>
                </label>
            `).join('');
            
            // Re-initialize checkbox handlers
            setupCategoryCheckboxes();
        }
    } catch (error) {
        Toast.error('Failed to load categories');
        // Fallback to default categories
        categoryGrid.innerHTML = `
            <label class="category-checkbox checked">
                <input type="checkbox" checked value="BOOK">
                <span class="check-indicator"><i class="fas fa-check"></i></span>
                <span class="category-label">Book</span>
            </label>
            <label class="category-checkbox">
                <input type="checkbox" value="JOURNAL">
                <span class="check-indicator"><i class="fas fa-check"></i></span>
                <span class="category-label">Journal</span>
            </label>
            <label class="category-checkbox">
                <input type="checkbox" value="ARTICLE">
                <span class="check-indicator"><i class="fas fa-check"></i></span>
                <span class="category-label">Article</span>
            </label>
            <label class="category-checkbox">
                <input type="checkbox" value="VIDEO">
                <span class="check-indicator"><i class="fas fa-check"></i></span>
                <span class="category-label">Video</span>
            </label>
            <label class="category-checkbox">
                <input type="checkbox" value="AUDIO">
                <span class="check-indicator"><i class="fas fa-check"></i></span>
                <span class="category-label">Audio</span>
            </label>
            <label class="category-checkbox">
                <input type="checkbox" value="DOCUMENT">
                <span class="check-indicator"><i class="fas fa-check"></i></span>
                <span class="category-label">Document</span>
            </label>
        `;
        setupCategoryCheckboxes();
    }
}

// ================================
// SIDEBAR METADATA LOADING
// ================================

async function loadSidebarData() {
    try {
        await Promise.all([
            loadBestSales(),
            loadMostCommented(),
            loadNewestBooks(),
            loadFeatured(),
            loadWatchHistory(),
            loadBestBooks()
        ]);
    } catch (error) {
        Toast.error('Failed to load sidebar data');
    }
}

// Load Best Sales (most purchased/popular resources)
async function loadBestSales() {
    const listEl = document.getElementById('bestSalesList');
    const countEl = document.querySelector('#bestSalesTitle .count');
    
    if (!listEl) return;
    
    try {
        // Get resources sorted by popularity (using most accessed/purchased logic)
        const response = await resourcesAPI.search({ sortBy: 'popular', size: 5 });
        
        if (response.success && response.data.content.length > 0) {
            const resources = response.data.content.slice(0, 5);
            
            if (countEl) countEl.textContent = response.data.totalElements || resources.length;
            
            listEl.innerHTML = resources.map(r => 
                `<a href="#" class="book-link" onclick="event.preventDefault(); showResourceDetail(${r.resourceID})">${escapeHtml(r.title)}</a>`
            ).join('');
        } else {
            listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No data available</p>';
        }
    } catch (error) {
        Toast.error('Failed to load best sales');
        listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">Failed to load</p>';
    }
}

// Load Most Commented (resources with most ratings/reviews)
async function loadMostCommented() {
    const listEl = document.getElementById('mostCommentedList');
    const countEl = document.querySelector('#mostCommentedTitle .count');
    
    if (!listEl) return;
    
    try {
        // Get all resources and sort by rating count
        const response = await resourcesAPI.search({ size: 100 });
        
        if (response.success && response.data.content.length > 0) {
            // Sort by rating count (most commented first)
            const sorted = response.data.content
                .filter(r => r.ratingCount > 0)
                .sort((a, b) => (b.ratingCount || 0) - (a.ratingCount || 0))
                .slice(0, 5);
            
            if (countEl) countEl.textContent = sorted.length;
            
            if (sorted.length > 0) {
                listEl.innerHTML = sorted.map(r => 
                    `<a href="#" class="book-link" onclick="event.preventDefault(); showResourceDetail(${r.resourceID})">${escapeHtml(r.title)} <span style="color: #9CA3AF; font-size: 0.75rem;">(${r.ratingCount || 0} reviews)</span></a>`
                ).join('');
            } else {
                listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No reviews yet</p>';
            }
        } else {
            listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No data available</p>';
        }
    } catch (error) {
        Toast.error('Failed to load most commented');
        listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">Failed to load</p>';
    }
}

// Load Newest Books
async function loadNewestBooks() {
    const listEl = document.getElementById('newestBooksList');
    const countEl = document.querySelector('#newestBooksTitle .count');
    
    if (!listEl) return;
    
    try {
        const response = await resourcesAPI.getRecent();
        
        if (response.success && response.data.length > 0) {
            const resources = response.data.slice(0, 5);
            
            if (countEl) countEl.textContent = response.data.length;
            
            listEl.innerHTML = resources.map(r => 
                `<a href="#" class="book-link" onclick="event.preventDefault(); showResourceDetail(${r.resourceID})">${escapeHtml(r.title)} <span style="color: #7E22CE; font-size: 0.75rem;">NEW</span></a>`
            ).join('');
        } else {
            listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No new books</p>';
        }
    } catch (error) {
        Toast.error('Failed to load newest books');
        listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">Failed to load</p>';
    }
}

// Load Featured (premium or highlighted resources)
async function loadFeatured() {
    const listEl = document.getElementById('featuredList');
    const countEl = document.querySelector('#featuredTitle .count');
    
    if (!listEl) return;
    
    try {
        // Get featured resources (premium or random selection)
        const response = await resourcesAPI.search({ size: 20 });
        
        if (response.success && response.data.content.length > 0) {
            // Filter premium resources or take random selection
            let featured = response.data.content.filter(r => r.premiumOnly);
            
            // If no premium, take first 5
            if (featured.length === 0) {
                featured = response.data.content.slice(0, 5);
            } else {
                featured = featured.slice(0, 5);
            }
            
            if (countEl) countEl.textContent = response.data.totalElements || featured.length;
            
            listEl.innerHTML = featured.map(r => 
                `<a href="#" class="book-link" onclick="event.preventDefault(); showResourceDetail(${r.resourceID})">${escapeHtml(r.title)} ${r.premiumOnly ? '<i class="fas fa-crown" style="color: #F59E0B; font-size: 0.75rem;"></i>' : ''}</a>`
            ).join('');
        } else {
            listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No featured books</p>';
        }
    } catch (error) {
        Toast.error('Failed to load featured books');
        listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">Failed to load</p>';
    }
}

// Load Watch History (user's accessed resources)
async function loadWatchHistory() {
    const listEl = document.getElementById('watchHistoryList');
    const countEl = document.querySelector('#watchHistoryTitle .count');
    
    if (!listEl) return;
    
    try {
        // Try to get user's reading history
        const response = await myListAPI.getReadingProgress();
        
        if (response.success && response.data && response.data.length > 0) {
            const history = response.data.slice(0, 5);
            
            if (countEl) countEl.textContent = response.data.length;
            
            listEl.innerHTML = history.map(h => 
                `<a href="#" class="book-link" onclick="event.preventDefault(); showResourceDetail(${h.resourceId})">${escapeHtml(h.resourceTitle || 'Unknown')}</a>`
            ).join('');
        } else {
            if (countEl) countEl.textContent = '0';
            listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No history yet</p>';
        }
    } catch (error) {
        if (countEl) countEl.textContent = '0';
        listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">Sign in to see history</p>';
    }
}

// Load Best Books (highest rated)
async function loadBestBooks() {
    const listEl = document.getElementById('bestBooksList');
    const countEl = document.querySelector('#bestBooksTitle .count');
    
    if (!listEl) return;
    
    try {
        // Get all resources and sort by rating
        const response = await resourcesAPI.search({ size: 100 });
        
        if (response.success && response.data.content.length > 0) {
            // Sort by average rating (highest first)
            const sorted = response.data.content
                .filter(r => r.averageRating > 0)
                .sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0))
                .slice(0, 5);
            
            if (countEl) countEl.textContent = sorted.length;
            
            if (sorted.length > 0) {
                listEl.innerHTML = sorted.map(r => 
                    `<a href="#" class="book-link" onclick="event.preventDefault(); showResourceDetail(${r.resourceID})">${escapeHtml(r.title)} <span style="color: #F59E0B; font-size: 0.75rem;"><i class="fas fa-star"></i> ${r.averageRating.toFixed(1)}</span></a>`
                ).join('');
            } else {
                listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No rated books yet</p>';
            }
        } else {
            listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">No data available</p>';
        }
    } catch (error) {
        Toast.error('Failed to load best books');
        listEl.innerHTML = '<p class="empty-message" style="padding: 0.5rem 0; color: #9CA3AF; font-size: 0.875rem;">Failed to load</p>';
    }
}

// Call init on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    setTimeout(initNewFeatures, 100);
});

// Show message for non-client users trying to purchase
function showNonClientMessage() {
    Toast.warning('Only clients can purchase resources. Librarians and Managers have full access to all resources.');
}

