/**
 * Theme Toggle Functionality - Sapphire Nightfall Whisper
 * Handles light/dark theme switching with localStorage persistence
 */

(function() {
    'use strict';

    const THEME_KEY = 'kaksha-theme';
    const DARK_THEME = 'dark';
    const LIGHT_THEME = 'light';

    // Initialize theme on page load
    function initTheme() {
        const savedTheme = localStorage.getItem(THEME_KEY);
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        
        // Default to light if no preference saved
        const theme = savedTheme || (prefersDark ? DARK_THEME : LIGHT_THEME);
        
        applyTheme(theme);
        applyRoleTheme();
        createThemeToggle();
    }

    // Apply role-based theme for dashboard pages
    function applyRoleTheme() {
        const body = document.body;
        if (!body.classList.contains('dashboard-page')) return;
        
        const user = window.UserStorage?.getUser?.() || 
                    (typeof UserStorage !== 'undefined' ? UserStorage.getUser() : null);
        if (!user || !user.role) return;
        
        // Remove any existing role theme classes
        body.classList.remove('client-theme', 'manager-theme', 'librarian-theme');
        
        // Apply appropriate theme based on role
        switch (user.role) {
            case 'ROLE_CLIENT':
                body.classList.add('client-theme');
                break;
            case 'ROLE_MANAGER':
                body.classList.add('manager-theme');
                break;
            case 'ROLE_LIBRARIAN':
                body.classList.add('librarian-theme');
                break;
            default:
                body.classList.add('client-theme');
        }
    }

    // Apply theme to document
    function applyTheme(theme) {
        if (theme === LIGHT_THEME) {
            document.documentElement.setAttribute('data-theme', LIGHT_THEME);
            document.body.classList.add('light-theme');
        } else {
            document.documentElement.removeAttribute('data-theme');
            document.body.classList.remove('light-theme');
        }
        localStorage.setItem(THEME_KEY, theme);
    }

    // Toggle between light and dark
    function toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const newTheme = currentTheme === LIGHT_THEME ? DARK_THEME : LIGHT_THEME;
        applyTheme(newTheme);
    }

    // Create and inject theme toggle button
    function createThemeToggle() {
        // Remove existing toggle if present
        const existingToggle = document.querySelector('.theme-toggle');
        if (existingToggle) {
            existingToggle.remove();
        }

        const toggleBtn = document.createElement('button');
        toggleBtn.className = 'theme-toggle';
        toggleBtn.setAttribute('aria-label', 'Toggle theme');
        toggleBtn.innerHTML = `
            <i class="fas fa-moon"></i>
            <i class="fas fa-sun"></i>
        `;
        
        toggleBtn.addEventListener('click', toggleTheme);
        document.body.appendChild(toggleBtn);
    }

    // Listen for system theme changes
    function watchSystemTheme() {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        mediaQuery.addEventListener('change', (e) => {
            // Only auto-switch if user hasn't manually set a preference
            if (!localStorage.getItem(THEME_KEY)) {
                applyTheme(e.matches ? DARK_THEME : LIGHT_THEME);
            }
        });
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initTheme();
            watchSystemTheme();
        });
    } else {
        initTheme();
        watchSystemTheme();
    }

    // Expose theme functions globally for debugging
    window.ThemeManager = {
        setTheme: applyTheme,
        toggle: toggleTheme,
        getCurrentTheme: () => document.documentElement.getAttribute('data-theme') || DARK_THEME,
        applyRoleTheme: applyRoleTheme
    };
})();
