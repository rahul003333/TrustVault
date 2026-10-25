# TrustVault — Secure Document Sharing

TrustVault is a full-stack secure document-sharing application built with Spring Boot, Spring Security, JWT authentication, React, and PostgreSQL. It allows authenticated users to upload, access, download, and share documents according to role-based permissions, document ownership, and public/private visibility rules.

The project focuses on practical application security, protected file storage, authorization, and an audit trail for security-relevant actions.

## Features

- User registration and login.
- JWT-based authentication for protected API requests.
- Password hashing using BCrypt.
- Exactly three application roles:
    - `ADMINISTRATOR`
    - `DOCUMENT_OWNER`
    - `VIEWER`
- Role-based authorization for API endpoints and document operations.
- Document upload, listing, visibility management, and download.
- Private-by-default document sharing.
- Public/private access checks.
- Document ownership enforcement.
- Administrator dashboard and user management.
- Audit logging for document operations and access-control outcomes.
- React/Vite frontend connected to the Spring Boot REST API.
- PostgreSQL persistence using Spring Data JPA and Hibernate.
- Configurable database connection, JWT secret, server port, logging, and upload limits.

## Technology Stack

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- JWT authentication
- BCrypt password hashing
- Maven
- PostgreSQL

### Frontend

- React
- Vite
- JavaScript
- HTML
- CSS

### Development tools

- IntelliJ IDEA
- Git and GitHub
- Postman
- PostgreSQL
- PowerShell

## Application Architecture

The backend follows a layered architecture:

- **Controller layer:** Handles HTTP requests and responses.
- **Service layer:** Implements document operations, ownership checks, and business rules.
- **Repository layer:** Uses Spring Data JPA to access persistent data.
- **Entity layer:** Represents users, documents, and audit records.
- **Security layer:** Handles JWT authentication and role-based authorization.
- **Frontend layer:** Provides login, registration, dashboards, document operations, and administrative screens.

The document metadata is stored in PostgreSQL. Document bytes are stored in a server-side storage directory rather than being served directly from a public frontend directory.

## Role and Permission Model

TrustVault uses exactly three roles.

| Operation | Administrator | Document Owner | Viewer |
|---|---|---|---|
| Authenticate | Yes | Yes | Yes |
| Upload documents | Yes, subject to service authorization | Yes | No |
| Manage own documents | Yes, subject to ownership checks | Yes | No |
| Download own documents | Yes | Yes | No |
| Access public documents | Yes, subject to endpoint rules | Yes | Yes |
| Access another user's private document | Only if explicitly authorized by policy | No by default | No |
| Manage users | Authorized administrator only | No | No |
| Access administrative audit views | Authorized administrator only | No | No |

Actual access is enforced by the backend. Frontend visibility or a hidden button is not treated as a security boundary.

The application checks authentication, roles, ownership, and document visibility before granting access to protected resources.

## Security Design

### Authentication

- Users authenticate through the backend authentication API.
- Successful login returns authentication information including a JWT.
- Protected API requests use the token in the `Authorization` header:

  `Authorization: Bearer <JWT_TOKEN>`

- Passwords are hashed using BCrypt rather than stored in plaintext.
- The JWT signing secret is supplied through the `JWT_SECRET` environment variable.

### Authorization

- Spring Security protects API endpoints.
- Role-based checks restrict administrator, owner, and viewer operations.
- Service-level checks enforce document ownership and visibility rules.
- Unauthorized and forbidden requests must not expose protected document content.
- Authorization must be applied to both document metadata and file-download operations.

### Document storage

- Document metadata is stored in PostgreSQL.
- Uploaded file bytes are stored outside the frontend's public assets.
- Uploaded files must not be committed to Git.
- File paths must be handled safely.
- Upload validation should reject empty files and enforce configured file-size and content-type restrictions.
- Documents are private by default unless the owner explicitly changes their visibility.

### Audit logging

Audit records support investigation of security-relevant operations, including document uploads, downloads, and denied access attempts.

Audit records should capture useful context such as the actor, action, resource, outcome, and timestamp, where available. Passwords, JWTs, and other secrets must not be written to logs.

Administrative audit data must be protected by backend authorization.

## Prerequisites

Install and configure:

- Java compatible with the project's Maven and Spring Boot configuration.
- PostgreSQL.
- Node.js and npm.
- Git.
- IntelliJ IDEA or another Java IDE.
- Postman or a similar API testing tool.

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/rahul003333/TrustVault.git
cd TrustVault
```

### 2. Create the PostgreSQL database

Create a PostgreSQL database named `trustvault` using your PostgreSQL administration tool or SQL client.

For example:

```sql
CREATE DATABASE trustvault;
```

Create or select a PostgreSQL user with the permissions needed by the application. Use your own secure credentials.

**Important:** This database command does not create the application tables. The application's schema configuration and entity mappings determine how the tables are initialized or validated.

### 3. Configure environment variables

The backend reads its configuration from environment variables. Create local environment variables in your terminal or configure them in IntelliJ's Run Configuration.

Example for PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/trustvault"
$env:DB_USERNAME = "your_postgres_username"
$env:DB_PASSWORD = "your_postgres_password"
$env:JWT_SECRET = "your_base64_encoded_jwt_secret"
```

Replace the example values with your actual local configuration.

Use the existing JWT secret if you need existing tokens to remain valid. Changing the secret invalidates tokens signed with the previous secret.

The repository includes `.env.example` as a configuration template. Spring Boot does not automatically load a `.env` file merely because it exists; supply the variables through your shell, IDE, or deployment platform.

Do not commit real credentials or signing secrets.

### 4. Run the backend

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

If the Maven wrapper is unavailable, use an installed Maven version:

```powershell
mvn spring-boot:run
```

The configured local backend port is `8082` unless overridden by the `PORT` environment variable.

Base URL:

`http://localhost:8082`

### 5. Run backend tests

```powershell
.\mvnw.cmd clean test
```

A successful test run should finish with `BUILD SUCCESS`.

Tests require the configuration and dependencies needed by the test context. If a test loads the real database configuration, ensure PostgreSQL and the required environment variables are available.

### 6. Run the frontend

Open the frontend directory containing `package.json` in a terminal.

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The local Vite frontend has been run at:

`http://localhost:5173`

Use the actual frontend directory in your local checkout. Configure the frontend's API base URL to point to the backend address, and use the correct environment variable name expected by the frontend code.

## API Overview

The following are the principal API areas used during development. Refer to the current controller classes for the exact request methods, payloads, and endpoint paths.

| API area | Purpose |
|---|---|
| `/api/auth` | Authentication and registration |
| `/api/documents` | Document operations |
| `/api/test/roles` | Development role-authorization checks |

The authentication API includes login at:

`POST http://localhost:8082/api/auth/login`

Role test endpoints used during development include:

- `/api/test/roles/viewer`
- `/api/test/roles/owner`
- `/api/test/roles/administrator`

The test endpoints are intended for development and verification. Review whether they should be removed or disabled in a production deployment.

### JWT requests

For protected endpoints, include:

```http
Authorization: Bearer <JWT_TOKEN>
```

Replace the placeholder with a valid token obtained through login. Do not share tokens publicly or commit them to source control.

### Expected authorization behavior

- A valid authenticated request with sufficient permissions should succeed.
- An unauthenticated request should be rejected.
- An authenticated user lacking the required role should be forbidden.
- A Viewer must not gain private-document access by changing frontend controls or guessing a document ID.
- A denied operation must not return the protected file.

The precise status code can depend on the endpoint and security configuration; verify both the HTTP response and application logs during testing.

## Configuration Reference

The current backend configuration supports these settings:

| Variable/property | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded JWT signing secret |
| `PORT` | Backend port; defaults to `8082` |
| `DDL_AUTO` | Hibernate schema behavior; defaults to `update` in the current local configuration |
| `SHOW_SQL` | Controls SQL statement logging; defaults to `false` |
| `SECURITY_LOG_LEVEL` | Spring Security logging level; defaults to `INFO` |

The upload limits configured in the backend are currently:

- Maximum individual file size: `25MB`
- Maximum multipart request size: `30MB`

### Production configuration

Before deploying:

- Set `DDL_AUTO=validate` only after the production schema has been created and verified.
- Use a managed PostgreSQL database or another properly secured PostgreSQL installation.
- Set a strong, unique JWT signing secret.
- Set database credentials using the hosting platform's secret/environment-variable facility.
- Configure CORS to allow only the intended frontend origins.
- Avoid exposing development-only test endpoints.
- Use HTTPS.
- Disable verbose SQL and security debugging in production.
- Configure persistent storage or object storage for uploaded files.
- Verify that uploaded files survive restarts and redeployments.
- Apply database backups, access restrictions, and appropriate retention policies.

Do not assume that a hosting provider's local filesystem is persistent. Uploaded files can be lost if the application uses an ephemeral filesystem.

## Git and Secret Management

The following must not be committed:

- Real `.env` files containing credentials.
- Real JWT signing secrets.
- Database passwords.
- JWT access tokens.
- Uploaded document files in `trustvault-files/`.
- Build output and local IDE configuration.

The project `.gitignore` should exclude these items where applicable.

Before committing:

```powershell
git status
git diff --check
git check-ignore -v trustvault-files/
```

Review all staged changes before committing.

## Development and Verification History

The project was developed incrementally, including:

1. Spring Boot backend setup and layered architecture.
2. Registration, login, password hashing, and JWT authentication.
3. Role-based authorization for Administrator, Document Owner, and Viewer.
4. Secure provisioning of the first Administrator.
5. Document upload, listing, downloading, and visibility management.
6. Ownership and public/private access enforcement.
7. Audit logging and authorization troubleshooting.
8. Administrator document authorization fixes.
9. PostgreSQL migration and environment-based configuration.
10. Documentation and deployment preparation.

Feature descriptions should be kept aligned with the actual code and verified behavior.

## Future Improvements

Potential future enhancements include:

- Automated integration tests for all role and ownership combinations.
- Persistent object storage for uploaded files.
- Document integrity checks and cryptographic hashes.
- Download links with expiration and stronger sharing controls.
- Pagination and filtering for documents and audit records.
- Rate limiting and stronger authentication abuse protections.
- Centralized production logging and monitoring.
- CI checks for tests, dependency vulnerabilities, and secret exposure.
- Deployment health checks and database migration tooling.

## Repository

GitHub: https://github.com/rahul003333/TrustVault

