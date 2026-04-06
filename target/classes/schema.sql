-- Kaksha Digital Library Database Schema
-- Run this script to set up the database with test data

CREATE DATABASE IF NOT EXISTS kaksha_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE kaksha_db;

-- ============================================
-- ENUM DEFINITIONS (as lookup tables)
-- ============================================

-- User Roles
CREATE TABLE IF NOT EXISTS user_roles (
    role_name VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100)
);

INSERT INTO user_roles (role_name, description) VALUES
('ROLE_CLIENT', 'Regular library user'),
('ROLE_LIBRARIAN', 'Library staff - can manage resources'),
('ROLE_MANAGER', 'Library administrator - full access');

-- User Tiers
CREATE TABLE IF NOT EXISTS user_tiers (
    tier_name VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100),
    max_resources INT,
    price_per_month DECIMAL(10,2)
);

INSERT INTO user_tiers (tier_name, description, max_resources, price_per_month) VALUES
('BASIC', 'Limited access to free resources', 10, 0),
('PREMIUM', 'Full access to all resources', NULL, 18000);

-- Resource Categories
CREATE TABLE IF NOT EXISTS resource_categories (
    category_name VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100),
    icon_class VARCHAR(50)
);

INSERT INTO resource_categories (category_name, description, icon_class) VALUES
('BOOK', 'Physical and digital books', 'fa-book'),
('JOURNAL', 'Academic and research journals', 'fa-journal-whills'),
('ARTICLE', 'Articles and papers', 'fa-newspaper'),
('VIDEO', 'Educational video content', 'fa-video'),
('AUDIO', 'Audiobooks and podcasts', 'fa-headphones'),
('DOCUMENT', 'Documents and reports', 'fa-file-alt');

-- Payment Status
CREATE TABLE IF NOT EXISTS payment_statuses (
    status_name VARCHAR(20) PRIMARY KEY,
    description VARCHAR(100)
);

INSERT INTO payment_statuses (status_name, description) VALUES
('PENDING', 'Payment awaiting processing'),
('PAID', 'Payment successfully completed'),
('FAILED', 'Payment failed or declined'),
('CANCELLED', 'Payment cancelled by user');

-- ============================================
-- MAIN TABLES
-- ============================================

-- Users (Base table for inheritance)
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_tier VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    role VARCHAR(20) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_code VARCHAR(6),
    verification_code_expiry DATETIME,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    
    INDEX idx_email (email),
    INDEX idx_username (username),
    FOREIGN KEY (user_tier) REFERENCES user_tiers(tier_name),
    FOREIGN KEY (role) REFERENCES user_roles(role_name)
);

-- Clients (extends Users)
CREATE TABLE IF NOT EXISTS clients (
    user_id BIGINT PRIMARY KEY,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Librarians (extends Users)
CREATE TABLE IF NOT EXISTS librarians (
    user_id BIGINT PRIMARY KEY,
    department VARCHAR(100),
    employee_id VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Managers (extends Users)
CREATE TABLE IF NOT EXISTS managers (
    user_id BIGINT PRIMARY KEY,
    department VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Resource Catalogs
CREATE TABLE IF NOT EXISTS resource_catalogs (
    catalog_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    catalog_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Library Resources
CREATE TABLE IF NOT EXISTS library_resources (
    resource_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    isbn VARCHAR(20),
    publication_year INT,
    author VARCHAR(255),
    category VARCHAR(20) NOT NULL,
    resource_file VARCHAR(255),
    resource_image VARCHAR(255),
    description TEXT,
    price DECIMAL(10,2),
    is_premium_only BOOLEAN NOT NULL DEFAULT FALSE,
    catalog_id BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    
    INDEX idx_resource_id (resource_id),
    INDEX idx_title (title),
    INDEX idx_isbn (isbn),
    INDEX idx_author (author),
    INDEX idx_category (category),
    INDEX idx_deleted (deleted),
    FOREIGN KEY (category) REFERENCES resource_categories(category_name),
    FOREIGN KEY (catalog_id) REFERENCES resource_catalogs(catalog_id)
);

-- Client Resource Access (Many-to-Many)
CREATE TABLE IF NOT EXISTS client_resources (
    client_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_via VARCHAR(20) NOT NULL DEFAULT 'PURCHASE', -- PURCHASE, PREMIUM, PROMOTION
    PRIMARY KEY (client_id, resource_id),
    FOREIGN KEY (client_id) REFERENCES clients(user_id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES library_resources(resource_id) ON DELETE CASCADE
);

-- Payments
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    resource_id BIGINT,
    payment_details VARCHAR(255),
    payment_invoice VARCHAR(255),
    transaction_reference VARCHAR(100) UNIQUE,
    payment_method VARCHAR(50),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    campay_payment_url VARCHAR(500),
    campay_token VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    paid_at DATETIME,
    
    INDEX idx_client (client_id),
    INDEX idx_resource (resource_id),
    INDEX idx_status (status),
    INDEX idx_reference (transaction_reference),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (client_id) REFERENCES clients(user_id),
    FOREIGN KEY (resource_id) REFERENCES library_resources(resource_id),
    FOREIGN KEY (status) REFERENCES payment_statuses(status_name)
);

-- Backups (Manager operations)
CREATE TABLE IF NOT EXISTS backups (
    backup_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    manager_id BIGINT NOT NULL,
    backup_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    backup_name VARCHAR(255) NOT NULL,
    backup_path VARCHAR(255),
    backup_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (manager_id) REFERENCES managers(user_id)
);

-- System Backups (for new backup management system)
CREATE TABLE IF NOT EXISTS system_backups (
    backup_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    backup_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description TEXT,
    tables_included VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    error_message TEXT,
    created_by BIGINT,
    
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- System Backup Settings (for automatic backup configuration)
CREATE TABLE IF NOT EXISTS system_backup_settings (
    setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auto_backup_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    backup_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    backup_day_of_month INT DEFAULT 1,
    backup_time VARCHAR(10) NOT NULL DEFAULT '02:00',
    backup_type VARCHAR(20) NOT NULL DEFAULT 'FULL',
    retention_days INT DEFAULT 90,
    last_modified_at DATETIME,
    modified_by BIGINT,
    
    FOREIGN KEY (modified_by) REFERENCES users(user_id)
);

-- System Terms & Conditions
CREATE TABLE IF NOT EXISTS system_terms (
    terms_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    effective_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    
    INDEX idx_is_active (is_active),
    INDEX idx_effective_date (effective_date),
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
);

-- Generated Reports (for analytics section)
CREATE TABLE IF NOT EXISTS generated_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(255) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    date_range VARCHAR(50) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_at DATETIME,
    created_by BIGINT,
    
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_report_type (report_type),
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- ============================================
-- TEST DATA
-- ============================================

-- Test Users (Password: Test@123 for all users)
-- BCrypt hash for "Test@123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG

INSERT INTO users (username, first_name, last_name, date_of_birth, email, password, user_tier, role, verified, active) VALUES
-- Managers
('admin_manager', 'Alice', 'Johnson', '1985-03-15', 'admin@kaksha.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_MANAGER', TRUE, TRUE),
('manager_sarah', 'Sarah', 'Williams', '1988-07-22', 'sarah.manager@kaksha.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_MANAGER', TRUE, TRUE),

-- Librarians
('librarian_john', 'John', 'Davis', '1990-11-08', 'john.lib@kaksha.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_LIBRARIAN', TRUE, TRUE),
('librarian_mary', 'Mary', 'Brown', '1992-05-18', 'mary.lib@kaksha.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_LIBRARIAN', TRUE, TRUE),
('librarian_peter', 'Peter', 'Wilson', '1987-12-03', 'peter.lib@kaksha.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_LIBRARIAN', TRUE, TRUE),

-- Clients (Basic Tier)
('client_emma', 'Emma', 'Thompson', '1995-02-14', 'emma@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'BASIC', 'ROLE_CLIENT', TRUE, TRUE),
('client_james', 'James', 'Miller', '1989-09-25', 'james@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'BASIC', 'ROLE_CLIENT', TRUE, TRUE),
('client_sophia', 'Sophia', 'Garcia', '1996-06-30', 'sophia@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'BASIC', 'ROLE_CLIENT', TRUE, TRUE),
('client_liam', 'Liam', 'Martinez', '1991-04-12', 'liam@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'BASIC', 'ROLE_CLIENT', FALSE, TRUE),

-- Clients (Premium Tier)
('client_olivia', 'Olivia', 'Anderson', '1986-08-19', 'olivia@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_CLIENT', TRUE, TRUE),
('client_noah', 'Noah', 'Taylor', '1993-11-05', 'noah@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_CLIENT', TRUE, TRUE),
('client_ava', 'Ava', 'Thomas', '1997-03-22', 'ava@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjXdtkcB/c/KcK8Laa1sB5w3v8FQOLG', 'PREMIUM', 'ROLE_CLIENT', TRUE, TRUE);

-- Insert into role-specific tables
INSERT INTO managers (user_id, department) VALUES
(1, 'Administration'),
(2, 'Operations');

INSERT INTO librarians (user_id, department, employee_id) VALUES
(3, 'Fiction', 'LIB001'),
(4, 'Science', 'LIB002'),
(5, 'Technology', 'LIB003');

INSERT INTO clients (user_id, status) VALUES
(6, TRUE),
(7, TRUE),
(8, TRUE),
(9, TRUE),
(10, TRUE),
(11, TRUE),
(12, TRUE);

-- Resource Catalogs
INSERT INTO resource_catalogs (catalog_name, description) VALUES
('Computer Science', 'Programming, algorithms, and computer science fundamentals'),
('Fiction & Literature', 'Novels, short stories, and literary works'),
('Science & Research', 'Scientific journals, research papers, and articles'),
('Business & Economics', 'Business strategies, economics, and management'),
('History & Culture', 'Historical events, cultural studies, and biographies'),
('Education', 'Textbooks, educational materials, and learning resources'),
('Arts & Design', 'Art history, design principles, and creative works');

-- Library Resources with static file references (matching actual files in /static/files and /static/images)
INSERT INTO library_resources (title, isbn, publication_year, author, category, resource_file, resource_image, description, price, is_premium_only, catalog_id) VALUES

-- Computer Science - Programming Languages & Frameworks
('Java Programming Guide', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Java.pdf', '/images/Java.png', 'Comprehensive Java programming guide covering core concepts, object-oriented programming, collections, and advanced topics.', 0, FALSE, 1),

('Python Programming Fundamentals', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Python.pdf', '/images/Python.png', 'Learn Python programming from basics to advanced. Covers data types, control structures, functions, modules, and more.', 0, FALSE, 1),

('C Programming Language', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/C.pdf', '/images/C.png', 'Complete guide to C programming language. Covers syntax, pointers, memory management, and system programming.', 0, FALSE, 1),

('C# Programming Essentials', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/C#.pdf', '/images/C+.png', 'Essential C# programming guide for .NET development. Covers classes, LINQ, async programming, and more.', 18000, FALSE, 1),

('Spring Framework Mastery', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Spring-Framework.pdf', '/images/Spring-Framework.png', 'Master Spring Framework for enterprise Java development. Covers IoC, AOP, Spring Boot, and microservices.', 30000, TRUE, 1),

('Android Development Guide', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Android.pdf', '/images/Android.jpg', 'Complete Android app development guide using Java and Kotlin. Covers UI design, APIs, and publishing.', 24000, TRUE, 1),

('iOS Development with Swift', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/iOS.pdf', '/images/iOS.png', 'Build iOS applications for iPhone and iPad. Covers Swift programming, UIKit, SwiftUI, and App Store deployment.', 24000, TRUE, 1),

-- Web Development
('HTML5 Complete Reference', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/HTML5.pdf', '/images/HTML5.png', 'Complete HTML5 reference guide. Covers semantic elements, forms, multimedia, canvas, and modern web standards.', 0, FALSE, 1),

('CSS Styling Guide', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/CSS.pdf', '/images/CSS.png', 'Master CSS for modern web design. Covers selectors, layout, flexbox, grid, animations, and responsive design.', 0, FALSE, 1),

('JavaScript Essentials', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/JavaScript.pdf', '/images/JS.png', 'Essential JavaScript programming guide. Covers ES6+, DOM manipulation, async programming, and modern JS patterns.', 0, FALSE, 1),

('TypeScript Programming', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/TypeScript.pdf', '/images/TypeScript.png', 'Learn TypeScript for scalable JavaScript applications. Covers types, interfaces, generics, and compiler configuration.', 12000, FALSE, 1),

('PHP Web Development', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/PHP.pdf', '/images/PHP.jpg', 'Web development with PHP. Covers syntax, database integration, security, and popular frameworks like Laravel.', 0, FALSE, 1),

-- Database & DevOps
('MySQL Database Guide', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/MySQL.pdf', '/images/MySQL.jpg', 'Complete MySQL database guide. Covers SQL queries, indexing, optimization, replication, and administration.', 0, FALSE, 1),

('MongoDB NoSQL Database', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/MongoDB.pdf', '/images/MongoDB.png', 'Learn MongoDB NoSQL database. Covers document model, CRUD operations, aggregation, and scaling.', 18000, TRUE, 1),

('Linux System Administration', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Linux.pdf', '/images/Linux.jpg', 'Linux system administration guide. Covers command line, file systems, networking, security, and shell scripting.', 0, FALSE, 1),

('Git Version Control', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Git.pdf', '/images/Git.png', 'Master Git version control. Covers commits, branches, merging, rebasing, and collaboration workflows.', 0, FALSE, 1),

-- Algorithms & Computer Science
('Data Structures and Algorithms', NULL, 2024, 'Kaksha Library', 'BOOK', '/files/Algorithms.pdf', '/images/Algorithms.jpg', 'Comprehensive algorithms and data structures guide. Covers sorting, searching, trees, graphs, and complexity analysis.', 30000, TRUE, 1);

-- Sample Payments and Purchases
INSERT INTO payments (client_id, resource_id, payment_details, payment_invoice, transaction_reference, payment_method, amount, status, paid_at) VALUES
-- Olivia (Premium) - Multiple purchases (Spring Framework, Android, iOS)
(10, 5, 'Purchase: Spring Framework Mastery', 'INV-SPR-001', 'KAKSHA-TRX001', 'Campay', 30000, 'PAID', NOW() - INTERVAL 5 DAY),
(10, 6, 'Purchase: Android Development Guide', 'INV-AND-002', 'KAKSHA-TRX002', 'Campay', 24000, 'PAID', NOW() - INTERVAL 3 DAY),

-- Noah (Premium) - Some purchases (TypeScript, MongoDB)
(11, 11, 'Purchase: TypeScript Programming', 'INV-TYP-003', 'KAKSHA-TRX003', 'Campay', 12000, 'PAID', NOW() - INTERVAL 7 DAY),

-- Emma (Basic) - Free resource access only
-- No payments for free resources

-- James (Basic) - One purchase (MongoDB)
(7, 14, 'Purchase: MongoDB NoSQL Database', 'INV-MDB-004', 'KAKSHA-TRX004', 'Campay', 18000, 'PAID', NOW() - INTERVAL 2 DAY),

-- Sophia (Basic) - Pending purchase (Algorithms)
(8, 17, 'Purchase: Data Structures and Algorithms', 'INV-ALG-005', 'KAKSHA-TRX005', 'Campay', 30000, 'PENDING', NULL),

-- Failed payment example (iOS Development)
(9, 7, 'Purchase: iOS Development with Swift', 'INV-IOS-006', 'KAKSHA-TRX006', 'Campay', 24000, 'FAILED', NULL);

-- Client Resource Access (granted resources)
INSERT INTO client_resources (client_id, resource_id, granted_at, granted_via) VALUES
-- Premium clients get access to premium resources
(10, 5, NOW() - INTERVAL 5 DAY, 'PURCHASE'),
(10, 6, NOW() - INTERVAL 3 DAY, 'PURCHASE'),
(10, 14, NOW() - INTERVAL 30 DAY, 'PREMIUM'), -- Premium tier access
(10, 17, NOW() - INTERVAL 30 DAY, 'PREMIUM'),

(11, 11, NOW() - INTERVAL 7 DAY, 'PURCHASE'),
(11, 14, NOW() - INTERVAL 30 DAY, 'PREMIUM'),
(11, 5, NOW() - INTERVAL 30 DAY, 'PREMIUM'),

(12, 6, NOW() - INTERVAL 30 DAY, 'PREMIUM'),
(12, 7, NOW() - INTERVAL 30 DAY, 'PREMIUM'),

-- Basic clients with purchases
(7, 14, NOW() - INTERVAL 2 DAY, 'PURCHASE'),

-- Free resources accessible to all (no explicit entry needed, handled by app logic)

-- Sample Backups
INSERT INTO backups (manager_id, backup_date, backup_name, backup_path, backup_type, status) VALUES
(1, NOW() - INTERVAL 1 DAY, 'Daily_Backup_2024_01_15', '/backups/daily_2024_01_15.sql', 'DAILY', 'COMPLETED'),
(1, NOW() - INTERVAL 7 DAY, 'Weekly_Backup_2024_01_08', '/backups/weekly_2024_01_08.sql', 'WEEKLY', 'COMPLETED'),
(2, NOW() - INTERVAL 30 DAY, 'Monthly_Backup_2023_12_15', '/backups/monthly_2023_12_15.sql', 'MONTHLY', 'COMPLETED'),
(1, NOW() - INTERVAL 1 HOUR, 'Manual_Backup_Pre_Update', '/backups/manual_pre_update.sql', 'MANUAL', 'COMPLETED');

-- ============================================
-- VIEWS FOR REPORTING
-- ============================================

CREATE VIEW view_resource_summary AS
SELECT 
    r.resource_id,
    r.title,
    r.author,
    r.category,
    rc.catalog_name,
    r.price,
    r.is_premium_only,
    r.deleted,
    COUNT(DISTINCT cr.client_id) as access_count,
    COUNT(DISTINCT CASE WHEN p.status = 'PAID' THEN p.payment_id END) as purchase_count
FROM library_resources r
JOIN resource_catalogs rc ON r.catalog_id = rc.catalog_id
LEFT JOIN client_resources cr ON r.resource_id = cr.resource_id
LEFT JOIN payments p ON r.resource_id = p.resource_id AND p.status = 'PAID'
WHERE r.deleted = FALSE
GROUP BY r.resource_id;

CREATE VIEW view_revenue_summary AS
SELECT 
    DATE_FORMAT(p.created_at, '%Y-%m') as month,
    COUNT(*) as total_payments,
    SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END) as revenue,
    COUNT(CASE WHEN p.status = 'PAID' THEN 1 END) as successful_payments,
    COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END) as failed_payments
FROM payments p
GROUP BY DATE_FORMAT(p.created_at, '%Y-%m')
ORDER BY month DESC;

CREATE VIEW view_user_summary AS
SELECT 
    u.role,
    u.user_tier,
    COUNT(*) as user_count,
    COUNT(CASE WHEN u.verified = TRUE THEN 1 END) as verified_count,
    COUNT(CASE WHEN u.active = TRUE THEN 1 END) as active_count
FROM users u
GROUP BY u.role, u.user_tier;
