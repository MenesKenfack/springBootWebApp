---
description: Build and run the Kaksha Digital Library application
---

# Kaksha Digital Library - Development Workflow

This workflow covers compiling, testing, and running the Spring Boot application.

## Prerequisites

- Java 17 or higher installed
- Maven 3.8+ installed
- MySQL database running (optional, can use embedded for testing)

## 1. Compile the Project

Compile the source code and verify there are no compilation errors:

```bash
mvn clean compile
```

// turbo
## 2. Run Tests

Execute unit and integration tests:

```bash
mvn test
```

// turbo
## 3. Start Development Server

Run the application in development mode with hot reload:

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## 4. Build for Production

Create an executable JAR file for production deployment:

```bash
mvn clean package -DskipTests
```

The JAR will be created in `target/library-1.0.0.jar`

## 5. Run Production Build

Execute the production JAR:

```bash
java -jar target/library-1.0.0.jar
```

## Development Tips

- **Database**: Ensure MySQL is running with database `kaksha_db` created
- **Static files**: Changes to `src/main/resources/static` are served immediately
- **Templates**: Thymeleaf templates are cached in production, reload during development
- **Logs**: Check console output or `logs/` directory for application logs

## Troubleshooting

If you encounter port conflicts, change the port in `application.properties`:
```properties
server.port=8081
```
