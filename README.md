# Kaksha Digital Library

A full-stack Spring Boot web application for managing a digital library platform. Built with Spring Boot 3.3+, Java 21, MySQL, and vanilla HTML/CSS/JS.

## Features

### Authentication & Authorization
- JWT-based authentication with refresh tokens
- Email verification with 6-digit code
- Role-based access control (Client, Librarian, Manager)
- BCrypt password hashing
- Remember me functionality

### User Management
- Three user types: Client, Librarian, Manager
- User tier system (Basic, Premium)
- Profile management
- Admin user management interface

### Resource Management
- CRUD operations for library resources (books, journals, articles, videos, audio, documents)
- Resource catalog organization
- Search and filter functionality
- Soft delete for resources
- Premium/Free resource distinction

### Payment System
- Campay payment gateway integration (sandbox mode)
- Purchase initiation and checkout
- Webhook handling for payment notifications
- Purchase history tracking

### Analytics & Reporting
- Revenue analytics with charts
- Resource access statistics
- User activity reports
- Category-based insights

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.3+
- **Language**: Java 21
- **Database**: MySQL 8+
- **ORM**: JPA/Hibernate
- **Security**: Spring Security + JWT
- **Email**: Spring Boot Mail (JavaMailSender)
- **Build Tool**: Maven

### Frontend
- **HTML5**: Semantic markup
- **CSS3**: Custom styling with CSS variables
- **JavaScript**: Vanilla JS with Fetch API
- **Charts**: Chart.js for analytics
- **Icons**: Font Awesome

### External APIs
- **Campay**: Payment gateway (sandbox mode)
- **SMTP**: Email service (Gmail configured)

## Project Structure

```
src/main/java/com/kaksha/library/
├── config/
│   ├── CampayConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   ├── MailConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AdminController.java
│   ├── AnalyticsController.java
│   ├── AuthController.java
│   ├── PurchaseController.java
│   └── ResourceController.java
├── dto/
│   ├── AnalyticsReportResponse.java
│   ├── ApiResponse.java
│   ├── AuthResponse.java
│   ├── BackupResponse.java
│   ├── CampayWebhookRequest.java
│   ├── CreateResourceRequest.java
│   ├── InitiatePurchaseRequest.java
│   ├── LoginRequest.java
│   ├── PaymentResponse.java
│   ├── RegisterRequest.java
│   ├── ReportRequest.java
│   ├── ResourceResponse.java
│   ├── UpdateResourceRequest.java
│   ├── UserResponse.java
│   └── VerifyEmailRequest.java
├── exception/
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java
│   ├── PaymentException.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── model/
│   ├── entity/
│   │   ├── Backup.java
│   │   ├── Client.java
│   │   ├── Librarian.java
│   │   ├── LibraryResource.java
│   │   ├── Manager.java
│   │   ├── Payments.java
│   │   ├── ResourceCatalog.java
│   │   └── User.java
│   └── enums/
│       ├── PaymentStatus.java
│       ├── ResourceCategory.java
│       ├── UserRole.java
│       └── UserTier.java
├── repository/
│   ├── BackupRepository.java
│   ├── ClientRepository.java
│   ├── ClientResourceRepository.java
│   ├── LibrarianRepository.java
│   ├── LibraryResourceRepository.java
│   ├── ManagerRepository.java
│   ├── PaymentsRepository.java
│   ├── ResourceCatalogRepository.java
│   └── UserRepository.java
└── service/
    ├── AdminService.java
    ├── AnalyticsService.java
    ├── AuthService.java
    ├── CustomUserDetailsService.java
    ├── MailService.java
    ├── PaymentService.java
    └── ResourceService.java

src/main/resources/
├── static/
│   ├── css/
│   │   └── styles.css
│   ├── js/
│   │   ├── analytics.js
│   │   ├── api.js
│   │   ├── auth.js
│   │   ├── dashboard.js
│   │   ├── manage-resources.js
│   │   ├── profile.js
│   │   ├── purchases.js
│   │   ├── resources.js
│   │   └── users.js
│   ├── analytics.html
│   ├── dashboard.html
│   ├── index.html
│   ├── login.html
│   ├── manage-resources.html
│   ├── profile.html
│   ├── purchases.html
│   ├── register.html
│   ├── resources.html
│   └── users.html
└── application.yml
```

## Setup Instructions

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- MySQL 8.0+
- Gmail account (for email verification)
- Campay sandbox account (for payments)

### 1. Database Setup

```sql
CREATE DATABASE kaksha_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Or the application will auto-create it if configured properly.

### 2. Environment Variables

Set these environment variables or update `application.yml`:

```bash
# Database
export MYSQL_USERNAME=your_mysql_username
export MYSQL_PASSWORD=your_mysql_password

# JWT
export JWT_SECRET=your_256_bit_secret_key_here_make_it_long_and_secure

# Email (Gmail)
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_gmail_app_password

# Campay (Sandbox)
export CAMPAY_API_KEY=your_campay_api_key
export CAMPAY_API_SECRET=your_campay_api_secret
export CAMPAY_WEBHOOK_SECRET=your_webhook_secret
```

### 3. Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd library

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### 4. Access the Application

- **Landing Page**: http://localhost:8080/
- **Login**: http://localhost:8080/login.html
- **Register**: http://localhost:8080/register.html
- **API Base**: http://localhost:8080/api

### 5. Create Initial Manager Account

You need to directly insert a manager into the database for initial access:

```sql
INSERT INTO users (username, first_name, last_name, date_of_birth, email, password, user_tier, role, verified, active, created_at)
VALUES ('admin', 'Admin', 'User', '1990-01-01', 'admin@kaksha.com', '$2a$10$...', 'PREMIUM', 'ROLE_MANAGER', true, true, NOW());

INSERT INTO managers (user_id) VALUES (LAST_INSERT_ID());
```

Generate a BCrypt password using an online tool or Java code.

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `POST /api/auth/verify-email` - Verify email with code
- `GET /api/auth/me` - Get current user

### Resources (Public)
- `GET /api/resources/search` - Search resources
- `GET /api/resources/{id}` - Get resource details
- `GET /api/resources/{id}/content` - Get resource content (auth required)

### Resources (Librarian/Manager)
- `POST /api/resources` - Create resource
- `PUT /api/resources/{id}` - Update resource
- `DELETE /api/resources/{id}` - Delete resource
- `GET/POST /api/resources/catalogs` - Manage catalogs

### Purchases (Client)
- `POST /api/purchase/initiate` - Initiate purchase
- `POST /api/purchase/checkout` - Process checkout
- `GET /api/purchase/history` - Get purchase history

### Webhooks
- `POST /api/purchase/webhook` - Campay payment webhook

### Analytics (Manager)
- `GET /api/reports/generate` - Generate analytics report

### Admin (Manager)
- `GET /api/admin/users` - List users
- `POST /api/admin/librarians` - Create librarian
- `POST /api/admin/managers` - Create manager
- `PUT /api/admin/users/{id}/role` - Change user role
- `PUT /api/admin/users/{id}/tier` - Change user tier
- `DELETE /api/admin/users/{id}` - Delete user

## Security

- All endpoints (except login, register, search) require JWT token in Authorization header
- Role-based access control implemented
- Passwords hashed with BCrypt
- Input validation on all endpoints
- CORS enabled for frontend

## License

MIT License - See LICENSE file for details

## Support

For issues or questions, please contact support@kaksha.com
=======
