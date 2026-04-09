// Profile JavaScript
document.addEventListener('DOMContentLoaded', () => {
    if (!protectRoute()) return;
    
    setupNavigation();
    loadProfile();
    
    // Setup forms
    const profileForm = document.getElementById('profileForm');
    const passwordForm = document.getElementById('passwordForm');
    
    if (profileForm) {
        profileForm.addEventListener('submit', handleProfileUpdate);
    }
    
    if (passwordForm) {
        passwordForm.addEventListener('submit', handlePasswordChange);
    }
});

async function loadProfile() {
    try {
        console.log('Loading profile...');
        const response = await authAPI.getCurrentUser();
        console.log('Profile response:', response);

        if (response.success && response.user) {
            const user = response.user;
            console.log('User data:', user);

            // Fill form fields with null checks
            const firstNameEl = document.getElementById('profileFirstName');
            const lastNameEl = document.getElementById('profileLastName');
            const usernameEl = document.getElementById('profileUsername');
            const emailEl = document.getElementById('profileEmail');
            const dobEl = document.getElementById('profileDob');
            const roleEl = document.getElementById('profileRole');

            if (firstNameEl) firstNameEl.value = user.firstName || '';
            if (lastNameEl) lastNameEl.value = user.lastName || '';
            if (usernameEl) usernameEl.value = user.username || '';
            if (emailEl) emailEl.value = user.email || '';
            if (dobEl) dobEl.value = user.dateOfBirth || '';
            if (roleEl) roleEl.value = user.role?.replace('ROLE_', '') || '';

            // Update badge
            const tierBadge = document.getElementById('userTierBadge');
            if (tierBadge) {
                tierBadge.textContent = user.tier || 'BASIC';
                tierBadge.className = 'tier-badge ' + (user.tier === 'PREMIUM' ? 'premium' : '');
            }

            // Update subscription section
            const currentPlan = document.getElementById('currentPlan');
            const upgradeBtn = document.getElementById('upgradeBtn');
            const upgradeText = document.getElementById('upgradeText');

            if (currentPlan) {
                currentPlan.textContent = user.tier || 'Basic';
            }

            if (user.tier === 'PREMIUM') {
                if (upgradeBtn) upgradeBtn.style.display = 'none';
                if (upgradeText) upgradeText.textContent = 'You have full access to all resources.';
            }
            
            // Store user data for navigation
            UserStorage.setUser(user);
            setupNavigation();
        } else {
            console.error('Profile response missing user data:', response);
            Toast.error('Failed to load profile: ' + (response.message || 'Unknown error'));
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        Toast.error('Failed to load profile: ' + (error.message || 'Unknown error'));
    }
}

function enableEdit() {
    const fields = ['profileFirstName', 'profileLastName'];
    fields.forEach(id => {
        const field = document.getElementById(id);
        if (field) field.disabled = false;
    });
    
    const actions = document.getElementById('editActions');
    if (actions) actions.style.display = 'flex';
}

function cancelEdit() {
    const fields = ['profileFirstName', 'profileLastName'];
    fields.forEach(id => {
        const field = document.getElementById(id);
        if (field) {
            field.disabled = true;
        }
    });
    
    const actions = document.getElementById('editActions');
    if (actions) actions.style.display = 'none';
    
    // Reload original values
    loadProfile();
}

async function handleProfileUpdate(e) {
    e.preventDefault();
    
    Toast.success('Profile updated successfully!');
    cancelEdit();
}

async function handlePasswordChange(e) {
    e.preventDefault();
    
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmNewPassword = document.getElementById('confirmNewPassword').value;
    
    if (!currentPassword || !newPassword || !confirmNewPassword) {
        Toast.error('All password fields are required');
        return;
    }
    
    if (newPassword !== confirmNewPassword) {
        Toast.error('New passwords do not match');
        return;
    }
    
    if (!FormValidator.validatePassword(newPassword)) {
        Toast.error('Password must have min 8 chars, 1 uppercase, 1 lowercase, 1 number');
        return;
    }
    
    // In a real app, this would call an API
    Toast.success('Password changed successfully!');
    e.target.reset();
}

function upgradeToPremium() {
    // In a real app, this would redirect to payment or process upgrade
    Toast.success('Redirecting to upgrade page...');
    
    // Simulate upgrade
    setTimeout(() => {
        Toast.success('Upgrade to Premium successful!');
        loadProfile();
    }, 1500);
}
