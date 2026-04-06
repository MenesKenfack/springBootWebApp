// Authentication JavaScript
let pendingEmail = null;

// Toggle Password Visibility
function togglePassword(fieldId) {
    const field = document.getElementById(fieldId);
    const button = field.parentElement.querySelector('.toggle-password i');
    
    if (field.type === 'password') {
        field.type = 'text';
        button.classList.remove('fa-eye');
        button.classList.add('fa-eye-slash');
    } else {
        field.type = 'password';
        button.classList.remove('fa-eye-slash');
        button.classList.add('fa-eye');
    }
}

// Handle Login Form
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    
    if (loginForm) {
        // Remove any existing listeners by cloning
        const newForm = loginForm.cloneNode(true);
        loginForm.parentNode.replaceChild(newForm, loginForm);
        
        newForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            e.stopImmediatePropagation();
            
            FormValidator.clearAllErrors();
            
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const rememberMe = document.getElementById('rememberMe').checked;
            
            let isValid = true;
            
            if (!email) {
                FormValidator.showError('email', 'Email is required');
                isValid = false;
            } else if (!FormValidator.validateEmail(email)) {
                FormValidator.showError('email', 'Please enter a valid email');
                isValid = false;
            }
            
            if (!password) {
                FormValidator.showError('password', 'Password is required');
                isValid = false;
            }
            
            if (!isValid) {
                Toast.warning('Please fix the errors above');
                return false;
            }
            
            const submitBtn = document.getElementById('loginBtn');
            const btnText = submitBtn.querySelector('.btn-text');
            const btnLoader = submitBtn.querySelector('.btn-loader');
            
            btnText.style.display = 'none';
            btnLoader.style.display = 'inline-flex';
            submitBtn.disabled = true;
            
            try {
                const response = await authAPI.login({ email, password, rememberMe });
                
                if (response.success) {
                    TokenManager.setToken(response.token, response.refreshToken);
                    UserStorage.setUser(response.user);
                    Toast.success('Login successful! Welcome back!');
                    setTimeout(() => {
                        window.location.href = '/dashboard';
                    }, 500);
                } else {
                    Toast.error(response.message || 'Login failed. Please check your credentials.');
                }
            } catch (error) {
                Toast.error(error.message || 'Invalid email or password. Please try again.');
            } finally {
                btnText.style.display = 'inline';
                btnLoader.style.display = 'none';
                submitBtn.disabled = false;
            }
            
            return false;
        });
    }
});

// Handle Register Form
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        FormValidator.clearAllErrors();
        
        const data = {
            username: document.getElementById('username').value.trim(),
            firstName: document.getElementById('firstName').value.trim(),
            lastName: document.getElementById('lastName').value.trim(),
            email: document.getElementById('email').value.trim(),
            dateOfBirth: document.getElementById('dateOfBirth').value,
            password: document.getElementById('password').value,
            confirmPassword: document.getElementById('confirmPassword').value
        };
        
        let isValid = true;
        
        if (!data.username || data.username.length < 3) {
            FormValidator.showError('username', 'Username must be at least 3 characters');
            isValid = false;
        }
        
        if (!data.firstName) {
            FormValidator.showError('firstName', 'First name is required');
            isValid = false;
        }
        
        if (!data.lastName) {
            FormValidator.showError('lastName', 'Last name is required');
            isValid = false;
        }
        
        if (!data.email) {
            FormValidator.showError('email', 'Email is required');
            isValid = false;
        } else if (!FormValidator.validateEmail(data.email)) {
            FormValidator.showError('email', 'Please enter a valid email');
            isValid = false;
        }
        
        if (!data.dateOfBirth) {
            FormValidator.showError('dateOfBirth', 'Date of birth is required');
            isValid = false;
        }
        
        if (!data.password) {
            FormValidator.showError('password', 'Password is required');
            isValid = false;
        } else if (!FormValidator.validatePassword(data.password)) {
            FormValidator.showError('password', 'Password must have min 8 chars, 1 uppercase, 1 lowercase, 1 number');
            isValid = false;
        }
        
        if (data.password !== data.confirmPassword) {
            FormValidator.showError('confirmPassword', 'Passwords do not match');
            isValid = false;
        }
        
        // Validate terms agreement
        const agreeTerms = document.getElementById('agreeTerms');
        if (!agreeTerms || !agreeTerms.checked) {
            FormValidator.showError('agreeTerms', 'You must agree to the Terms and Conditions');
            isValid = false;
        }
        
        if (!isValid) {
            Toast.warning('Please fix the validation errors above');
            return;
        }
            
        const submitBtn = document.getElementById('registerBtn');
        const btnText = submitBtn.querySelector('.btn-text');
        const btnLoader = submitBtn.querySelector('.btn-loader');
        
        btnText.style.display = 'none';
        btnLoader.style.display = 'inline-flex';
        submitBtn.disabled = true;
        
        try {
            const response = await authAPI.register(data);
            
            if (response.success) {
                pendingEmail = data.email;
                Toast.success('Registration successful! Please check your email for verification code.');
                document.getElementById('verificationModal').style.display = 'flex';
            } else {
                Toast.error(response.message || 'Registration failed. Please try again.');
            }
        } catch (error) {
            Toast.error(error.message || 'Registration failed. Please check your information and try again.');
        } finally {
            btnText.style.display = 'inline';
            btnLoader.style.display = 'none';
            submitBtn.disabled = false;
        }
    });
}

// Verify Email
async function verifyEmail() {
    const code = document.getElementById('verificationCode').value.trim();
    
    if (!code || code.length !== 6) {
        Toast.error('Please enter the 6-digit verification code');
        return;
    }
    
    try {
        const response = await authAPI.verifyEmail({
            email: pendingEmail,
            verificationCode: code
        });
        
        if (response.success) {
            Toast.success('Email verified successfully!');
            closeModal();
            window.location.href = '/login.html';
        } else {
            Toast.error(response.message || 'Verification failed');
        }
    } catch (error) {
        Toast.error(error.message || 'Invalid verification code');
    }
}

// Resend Verification Code
async function resendCode() {
    if (!pendingEmail) {
        Toast.error('No pending verification');
        return;
    }
    
    try {
        await authAPI.resendVerification(pendingEmail);
        Toast.success('Verification code resent!');
    } catch (error) {
        Toast.error(error.message || 'Failed to resend code');
    }
}

// Close Modal
function closeModal() {
    document.getElementById('verificationModal').style.display = 'none';
}

// Redirect if already logged in
if (TokenManager.isAuthenticated() && 
    (window.location.pathname === '/login.html' || window.location.pathname === '/register.html')) {
    window.location.href = '/dashboard.html';
}

// Terms and Conditions Modal Functions
async function showTermsModal(event) {
    if (event) event.preventDefault();
    
    const modal = document.getElementById('termsModal');
    const contentDiv = document.getElementById('termsContent');
    
    modal.style.display = 'flex';
    contentDiv.innerHTML = '<p class="text-center"><i class="fas fa-spinner fa-spin"></i> Loading terms...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/terms/active`);
        const data = await response.json();
        
        if (data.success && data.data) {
            document.getElementById('termsModalTitle').textContent = data.data.title || 'Terms and Conditions';
            contentDiv.textContent = data.data.content;
        } else {
            contentDiv.innerHTML = `
                <div class="alert alert-info">
                    <h4>Terms and Conditions</h4>
                    <p>By using Kaksha Digital Library, you agree to:</p>
                    <ul style="margin-left: 1.5rem; line-height: 1.8;">
                        <li>Use the platform for educational purposes only</li>
                        <li>Respect copyright and intellectual property rights</li>
                        <li>Not share your account credentials with others</li>
                        <li>Comply with all applicable laws and regulations</li>
                        <li>Accept our privacy policy and data handling practices</li>
                    </ul>
                    <p style="margin-top: 1rem;">Please contact support for the complete terms and conditions.</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Failed to load terms:', error);
        contentDiv.innerHTML = `
            <div class="alert alert-info">
                <h4>Terms and Conditions</h4>
                <p>By registering, you agree to our Terms of Service and Privacy Policy. 
                Please contact support for complete terms.</p>
            </div>
        `;
    }
}

function closeTermsModal() {
    document.getElementById('termsModal').style.display = 'none';
}

function acceptTerms() {
    document.getElementById('agreeTerms').checked = true;
    FormValidator.clearError('agreeTerms');
    closeTermsModal();
}

// Privacy Policy Modal Functions
function showPrivacyModal(event) {
    if (event) event.preventDefault();
    document.getElementById('privacyModal').style.display = 'flex';
}

function closePrivacyModal() {
    document.getElementById('privacyModal').style.display = 'none';
}
