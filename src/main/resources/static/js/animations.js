/**
 * Kaksha Digital Library - Professional Animation System
 * Smooth, performant, and accessible animations
 */

(function() {
    'use strict';

    // ================================================
    // SCROLL ANIMATION OBSERVER
    // ================================================
    
    class ScrollAnimator {
        constructor(options = {}) {
            this.options = {
                threshold: 0.1,
                rootMargin: '0px 0px -50px 0px',
                once: true,
                ...options
            };
            
            this.observer = null;
            this.elements = [];
            
            this.init();
        }
        
        init() {
            if (!('IntersectionObserver' in window)) {
                // Fallback for browsers without IntersectionObserver
                this.fallback();
                return;
            }
            
            this.observer = new IntersectionObserver(
                (entries) => this.handleIntersection(entries),
                {
                    threshold: this.options.threshold,
                    rootMargin: this.options.rootMargin
                }
            );
            
            this.observe();
        }
        
        handleIntersection(entries) {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    this.animateElement(entry.target);
                    
                    if (this.options.once) {
                        this.observer.unobserve(entry.target);
                    }
                } else if (!this.options.once) {
                    this.resetElement(entry.target);
                }
            });
        }
        
        animateElement(element) {
            requestAnimationFrame(() => {
                element.classList.add('is-visible');
                
                // Trigger stagger children if applicable
                if (element.classList.contains('stagger-children')) {
                    element.classList.add('is-visible');
                }
                
                // Dispatch custom event
                element.dispatchEvent(new CustomEvent('animated', {
                    detail: { element, type: 'enter' }
                }));
            });
        }
        
        resetElement(element) {
            element.classList.remove('is-visible');
        }
        
        observe() {
            // Observe elements with animate-on-scroll class
            this.elements = document.querySelectorAll('.animate-on-scroll, .stagger-children');
            this.elements.forEach(el => this.observer.observe(el));
        }
        
        refresh() {
            if (this.observer) {
                this.observer.disconnect();
                this.observe();
            }
        }
        
        fallback() {
            // Show all elements immediately if IntersectionObserver not supported
            document.querySelectorAll('.animate-on-scroll, .stagger-children').forEach(el => {
                el.classList.add('is-visible');
            });
        }
        
        destroy() {
            if (this.observer) {
                this.observer.disconnect();
                this.observer = null;
            }
        }
    }

    // ================================================
    // SCROLL PROGRESS INDICATOR
    // ================================================
    
    class ScrollProgress {
        constructor() {
            this.element = null;
            this.init();
        }
        
        init() {
            // Create progress bar element
            this.element = document.createElement('div');
            this.element.className = 'scroll-progress';
            this.element.style.width = '0%';
            document.body.appendChild(this.element);
            
            // Bind scroll event with throttling
            this.throttledUpdate = this.throttle(() => this.update(), 16); // ~60fps
            window.addEventListener('scroll', this.throttledUpdate, { passive: true });
            
            // Initial update
            this.update();
        }
        
        update() {
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            const progress = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
            
            this.element.style.width = `${progress}%`;
        }
        
        throttle(func, limit) {
            let inThrottle;
            return function(...args) {
                if (!inThrottle) {
                    func.apply(this, args);
                    inThrottle = true;
                    setTimeout(() => inThrottle = false, limit);
                }
            };
        }
        
        destroy() {
            window.removeEventListener('scroll', this.throttledUpdate);
            if (this.element && this.element.parentNode) {
                this.element.parentNode.removeChild(this.element);
            }
        }
    }

    // ================================================
    // RIPPLE EFFECT SYSTEM
    // ================================================
    
    class RippleEffect {
        constructor(selector = '.btn, .nav-item, .card') {
            this.selector = selector;
            this.init();
        }
        
        init() {
            document.addEventListener('click', (e) => {
                const target = e.target.closest(this.selector);
                if (target && !target.classList.contains('no-ripple')) {
                    this.createRipple(e, target);
                }
            });
        }
        
        createRipple(event, element) {
            const rect = element.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = event.clientX - rect.left - size / 2;
            const y = event.clientY - rect.top - size / 2;
            
            const ripple = document.createElement('span');
            ripple.style.cssText = `
                position: absolute;
                width: ${size}px;
                height: ${size}px;
                left: ${x}px;
                top: ${y}px;
                background: rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                transform: scale(0);
                animation: ripple 0.6s ease-out;
                pointer-events: none;
            `;
            
            element.style.position = 'relative';
            element.style.overflow = 'hidden';
            element.appendChild(ripple);
            
            setTimeout(() => ripple.remove(), 600);
        }
    }

    // ================================================
    // COUNTER ANIMATION
    // ================================================
    
    class CounterAnimation {
        constructor(element, target, duration = 2000) {
            this.element = element;
            this.target = parseInt(target, 10);
            this.duration = duration;
            this.start = 0;
            this.startTime = null;
            
            this.init();
        }
        
        init() {
            // Use IntersectionObserver to start animation when visible
            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        this.animate();
                        observer.disconnect();
                    }
                });
            }, { threshold: 0.5 });
            
            observer.observe(this.element);
        }
        
        animate() {
            const step = (timestamp) => {
                if (!this.startTime) this.startTime = timestamp;
                const progress = Math.min((timestamp - this.startTime) / this.duration, 1);
                
                // Easing function (ease-out)
                const easeOut = 1 - Math.pow(1 - progress, 3);
                const current = Math.floor(easeOut * this.target);
                
                this.element.textContent = current.toLocaleString();
                
                if (progress < 1) {
                    requestAnimationFrame(step);
                } else {
                    this.element.textContent = this.target.toLocaleString();
                }
            };
            
            requestAnimationFrame(step);
        }
        
        static initAll() {
            document.querySelectorAll('[data-counter]').forEach(el => {
                const target = el.getAttribute('data-counter');
                const duration = parseInt(el.getAttribute('data-duration'), 10) || 2000;
                new CounterAnimation(el, target, duration);
            });
        }
    }

    // ================================================
    // SMOOTH SCROLL NAVIGATION
    // ================================================
    
    class SmoothScroll {
        constructor() {
            this.init();
        }
        
        init() {
            document.addEventListener('click', (e) => {
                const link = e.target.closest('a[href^="#"]');
                if (link) {
                    const targetId = link.getAttribute('href');
                    if (targetId === '#') return;
                    
                    const target = document.querySelector(targetId);
                    if (target) {
                        e.preventDefault();
                        this.scrollTo(target);
                    }
                }
            });
        }
        
        scrollTo(element, offset = 80) {
            const elementPosition = element.getBoundingClientRect().top;
            const offsetPosition = elementPosition + window.pageYOffset - offset;
            
            window.scrollTo({
                top: offsetPosition,
                behavior: 'smooth'
            });
        }
    }

    // ================================================
    // STAGGER ANIMATION HELPER
    // ================================================
    
    class StaggerAnimator {
        static apply(containerSelector, childSelector, animationClass, staggerDelay = 100) {
            const containers = document.querySelectorAll(containerSelector);
            
            containers.forEach(container => {
                const children = container.querySelectorAll(childSelector);
                
                children.forEach((child, index) => {
                    child.style.animationDelay = `${index * staggerDelay}ms`;
                    child.classList.add(animationClass);
                });
            });
        }
        
        static reset(containerSelector, childSelector, animationClass) {
            const containers = document.querySelectorAll(containerSelector);
            
            containers.forEach(container => {
                const children = container.querySelectorAll(childSelector);
                
                children.forEach(child => {
                    child.style.animationDelay = '';
                    child.classList.remove(animationClass);
                });
            });
        }
    }

    // ================================================
    // PARALLAX EFFECT
    // ================================================
    
    class ParallaxEffect {
        constructor(selector = '.parallax') {
            this.selector = selector;
            this.elements = [];
            this.init();
        }
        
        init() {
            this.elements = document.querySelectorAll(this.selector);
            if (this.elements.length === 0) return;
            
            this.throttledUpdate = this.throttle(() => this.update(), 16);
            window.addEventListener('scroll', this.throttledUpdate, { passive: true });
            
            // Check for reduced motion preference
            if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                return;
            }
            
            this.update();
        }
        
        update() {
            const scrollY = window.pageYOffset;
            
            this.elements.forEach(el => {
                const speed = parseFloat(el.dataset.parallaxSpeed) || 0.5;
                const offset = scrollY * speed;
                el.style.transform = `translateY(${offset}px)`;
            });
        }
        
        throttle(func, limit) {
            let inThrottle;
            return function(...args) {
                if (!inThrottle) {
                    func.apply(this, args);
                    inThrottle = true;
                    setTimeout(() => inThrottle = false, limit);
                }
            };
        }
        
        destroy() {
            window.removeEventListener('scroll', this.throttledUpdate);
        }
    }

    // ================================================
    // LOADING STATE ANIMATIONS
    // ================================================
    
    class LoadingStates {
        static show(element, type = 'spinner') {
            if (typeof element === 'string') {
                element = document.querySelector(element);
            }
            
            if (!element) return;
            
            element.classList.add('loading');
            element.dataset.originalContent = element.innerHTML;
            
            let loadingHtml = '';
            
            switch(type) {
                case 'spinner':
                    loadingHtml = '<div class="spinner spinner-sm"></div>';
                    break;
                case 'dots':
                    loadingHtml = '<div class="loading-dots"><span></span><span></span><span></span></div>';
                    break;
                case 'shimmer':
                    element.classList.add('loading-shimmer');
                    return;
                case 'skeleton':
                    loadingHtml = '<div class="skeleton" style="width:100%;height:100%;"></div>';
                    break;
            }
            
            element.innerHTML = loadingHtml;
        }
        
        static hide(element) {
            if (typeof element === 'string') {
                element = document.querySelector(element);
            }
            
            if (!element) return;
            
            element.classList.remove('loading', 'loading-shimmer');
            
            if (element.dataset.originalContent) {
                element.innerHTML = element.dataset.originalContent;
                delete element.dataset.originalContent;
            }
        }
    }

    // ================================================
    // HEART ANIMATION (FAVORITE BUTTON)
    // ================================================
    
    class HeartAnimation {
        constructor() {
            this.init();
        }
        
        init() {
            document.addEventListener('click', (e) => {
                const heartBtn = e.target.closest('.card-favorite-btn, .heart-btn, .heart-animate');
                if (heartBtn) {
                    this.toggle(heartBtn);
                }
            });
        }
        
        toggle(button) {
            button.classList.toggle('active');
            
            // Add animation effect
            const icon = button.querySelector('i');
            if (icon) {
                icon.style.transform = 'scale(1.3)';
                setTimeout(() => {
                    icon.style.transform = '';
                }, 200);
            }
            
            // Dispatch event
            button.dispatchEvent(new CustomEvent('heartToggle', {
                detail: { active: button.classList.contains('active') }
            }));
        }
    }

    // ================================================
    // ACCORDION ANIMATION
    // ================================================
    
    class AccordionAnimation {
        constructor() {
            this.init();
        }
        
        init() {
            document.addEventListener('click', (e) => {
                const header = e.target.closest('.accordion-header, [data-accordion-toggle]');
                if (header) {
                    this.toggle(header);
                }
            });
        }
        
        toggle(header) {
            const content = header.nextElementSibling;
            if (!content) return;
            
            const isExpanded = header.classList.contains('active');
            
            // Close all siblings if this is part of an accordion group
            const parent = header.closest('.accordion, [data-accordion-group]');
            if (parent && parent.dataset.accordionGroup !== 'false') {
                parent.querySelectorAll('.accordion-header, [data-accordion-toggle]').forEach(h => {
                    if (h !== header) {
                        h.classList.remove('active');
                        const c = h.nextElementSibling;
                        if (c) c.classList.remove('expanded');
                    }
                });
            }
            
            // Toggle current
            header.classList.toggle('active', !isExpanded);
            content.classList.toggle('expanded', !isExpanded);
            
            // Smooth scroll if expanding
            if (!isExpanded) {
                setTimeout(() => {
                    const rect = header.getBoundingClientRect();
                    if (rect.top < 100 || rect.bottom > window.innerHeight) {
                        header.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                    }
                }, 300);
            }
        }
    }

    // ================================================
    // TAB ANIMATION
    // ================================================
    
    class TabAnimation {
        constructor() {
            this.init();
        }
        
        init() {
            document.addEventListener('click', (e) => {
                const tab = e.target.closest('.tab-btn, [data-tab]');
                if (tab) {
                    this.switch(tab);
                }
            });
        }
        
        switch(tab) {
            const tabGroup = tab.closest('.tabs-container, [data-tab-group]');
            if (!tabGroup) return;
            
            const targetId = tab.dataset.tab || tab.getAttribute('href');
            if (!targetId) return;
            
            // Deactivate all tabs in group
            tabGroup.querySelectorAll('.tab-btn, [data-tab]').forEach(t => {
                t.classList.remove('active');
            });
            
            // Hide all content panels
            const contentContainer = document.querySelector(
                tabGroup.dataset.tabContent || '[data-tab-content]'
            );
            if (contentContainer) {
                contentContainer.querySelectorAll('.tab-content, [data-tab-panel]').forEach(c => {
                    c.style.display = 'none';
                });
            }
            
            // Activate selected tab
            tab.classList.add('active');
            
            // Show target content with animation
            const targetContent = document.querySelector(targetId) || 
                                 document.querySelector(`[data-tab-panel="${targetId}"]`);
            if (targetContent) {
                targetContent.style.display = 'block';
                targetContent.classList.add('tab-content-animate');
                
                // Remove animation class after it completes
                setTimeout(() => {
                    targetContent.classList.remove('tab-content-animate');
                }, 300);
            }
        }
    }

    // ================================================
    // TOAST NOTIFICATION ENHANCEMENT
    // ================================================
    
    class ToastEnhancement {
        static show(message, type = 'success', duration = 3000) {
            const container = document.querySelector('.toast-container') || (() => {
                const c = document.createElement('div');
                c.className = 'toast-container';
                document.body.appendChild(c);
                return c;
            })();
            
            const toast = document.createElement('div');
            toast.className = `toast ${type} toast-entrance`;
            toast.innerHTML = `
                <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
                <span>${message}</span>
            `;
            
            container.appendChild(toast);
            
            // Auto remove
            setTimeout(() => {
                toast.classList.remove('toast-entrance');
                toast.classList.add('toast-exit');
                setTimeout(() => toast.remove(), 300);
            }, duration);
            
            // Click to dismiss
            toast.addEventListener('click', () => {
                toast.classList.remove('toast-entrance');
                toast.classList.add('toast-exit');
                setTimeout(() => toast.remove(), 300);
            });
        }
    }

    // ================================================
    // MAIN INITIALIZATION
    // ================================================
    
    const AnimationSystem = {
        scrollAnimator: null,
        scrollProgress: null,
        rippleEffect: null,
        parallaxEffect: null,
        
        init() {
            // Initialize scroll animations
            this.scrollAnimator = new ScrollAnimator();
            
            // Initialize scroll progress
            this.scrollProgress = new ScrollProgress();
            
            // Initialize ripple effects
            this.rippleEffect = new RippleEffect();
            
            // Initialize smooth scroll
            new SmoothScroll();
            
            // Initialize parallax
            this.parallaxEffect = new ParallaxEffect();
            
            // Initialize counter animations
            CounterAnimation.initAll();
            
            // Initialize heart animations
            new HeartAnimation();
            
            // Initialize accordion animations
            new AccordionAnimation();
            
            // Initialize tab animations
            new TabAnimation();
            
            // Add entrance animation to body
            document.body.classList.add('page-transition-enter');
            
            // Initialize dashboard entrance if applicable
            if (document.body.classList.contains('dashboard-page')) {
                document.body.classList.add('dashboard-entrance');
            }
            
            console.log('🎬 Kaksha Animation System initialized');
        },
        
        refresh() {
            if (this.scrollAnimator) {
                this.scrollAnimator.refresh();
            }
            CounterAnimation.initAll();
        },
        
        // Expose utility methods
        LoadingStates,
        ToastEnhancement,
        StaggerAnimator,
        CounterAnimation
    };

    // ================================================
    // DOM READY INITIALIZATION
    // ================================================
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => AnimationSystem.init());
    } else {
        AnimationSystem.init();
    }
    
    // Expose to global scope
    window.KakshaAnimations = AnimationSystem;
    
})();
