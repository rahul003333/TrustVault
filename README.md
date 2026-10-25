# TrustVault

TrustVault is a secure PDF-sharing application built with Java Spring Boot, Spring Security, React, and a database. It provides JWT authentication, role-based access control, PDF upload and retrieval, public/private sharing, and audit logging for security-relevant actions.

> **PDF-only policy:** TrustVault is designed to accept PDF files only. PNG, JPEG, DOCX, and all other file formats are not supported.

> **Security:** Never commit passwords, JWT signing secrets, database credentials, or uploaded documents. Keep the local `trustvault-files/` directory out of Git.

## Features

- Stateless JWT authentication and BCrypt password hashing.
- Exactly three roles: `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`.
- PDF-only document upload, listing, download, and preview.
- Documents are private by default.
- Public/private visibility controls and ownership checks.
- Audit events for authentication and document actions, including denied access.
- Spring Data JPA/Hibernate persistence.
- React and Vite frontend.
- Deployment using Render.

## Technology Stack

**Backend:** Java, Spring Boot, Spring Security, Spring Data JPA/Hibernate, JWT, BCrypt.

**Frontend:** React, Vite, JavaScript, `lucide-react`.

**Database:** PostgreSQL for the deployed application; Oracle Database XE was used during local development.

**Deployment:** Render.

## Roles and Permissions

| Capability | Administrator | Document Owner | Viewer |
|---|---|---|---|
| Log in | Yes | Yes | Yes |
| Upload PDF files | Yes | Yes | No |
| View own documents | Yes | Yes | No dedicated personal list |
| List visible documents | According to backend authorization | Own and public documents | Public documents |
| Download own document | Yes | Yes | Not applicable |
| Download another user's private document | No | No | No |
| Download public documents | Yes | Yes | Yes |
| Change document visibility | According to backend authorization | For own documents, if authorized | No |
| View audit records | Authorized Administrator | No | No |

All permissions must be enforced by the backend, not only by hiding frontend controls.

## API Overview

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a Viewer |
| POST | `/api/auth/register-owner` | Register a Document Owner |
| POST | `/api/auth/login` | Log in and receive a JWT |

### Documents

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/documents/upload` | Upload a PDF using `multipart/form-data`, field `file` |
| GET | `/api/documents` | List documents visible to the current user |
| GET | `/api/documents/my` | List the current user's documents, where authorized |
| GET | `/api/documents/{id}/download` | Download an authorized PDF |
| GET | `/api/documents/{id}/preview` | Preview a PDF |
| PUT | `/api/documents/{id}/visibility?makePublic=true` | Change visibility, subject to backend authorization |

### Role Testing

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/test/roles/viewer` | Test Viewer authorization |
| GET | `/api/test/roles/administrator` | Test Administrator authorization |
| GET | `/api/test/roles/owner` | Test Document Owner authorization |

Verify the actual endpoint mappings and authorization policies in the current controllers and services before relying on this table.

For protected requests, send the login token in the request header:

```http
Authorization: Bearer <your-jwt-token>
```

## Live Deployment

**Frontend:** https://trustvault-frontend-aly3.onrender.com

**Backend:** https://trustvault-backend-cjkc.onrender.com

**GitHub repository:** https://github.com/rahul003333/TrustVault

The deployed services may need a short time to wake up after inactivity.

## Local Setup

### Prerequisites

- A compatible JDK.
- Maven or the Maven wrapper.
- Node.js and npm.
- A database configured for the active Spring profile.

### Configure the Backend

Review `src/main/resources/application.properties` and the active environment configuration.

Example placeholders:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

These are placeholders, not real credentials. Configure the required values through local environment variables or an untracked configuration file. Never commit production credentials or JWT secrets.

### Run the Backend

From the repository root:

```bash
mvn spring-boot:run
```

Alternatively, run the Spring Boot main class from IntelliJ IDEA. Confirm the port configured for your environment.

### Run the Frontend

Open a terminal in the frontend directory containing `package.json`:

```bash
npm install
npm run dev
```

The local Vite frontend commonly runs at `http://localhost:5173/`. Ensure the frontend API base URL points to the correct backend environment.

## Document Upload, Preview, and Download Policy

- **Upload:** Users can upload files in any format, subject to the application's configured file-size limits.
- **Preview:** Only PDF files can be previewed within TrustVault. Non-PDF files cannot be previewed in the application.
- **Download:** Publicly accessible files can be downloaded in any file format, including images, Word documents, ZIP files, and other file types.
- **Access control:** Download permissions depend on the document's visibility and the user's role. Private documents must remain protected from unauthorized access.
- **File security:** Uploaded files must be handled safely. The application must validate access permissions and avoid executing uploaded files.

**Important:** Being unable to preview a file does not prevent users from downloading it when they have permission to access it.

## Security and Audit Guidance

- Store password hashes, not plaintext passwords.
- Never log or commit JWTs, passwords, or secrets.
- Enforce authorization in both controller and service layers.
- Record successful and denied security-relevant actions without recording credentials.
- Keep database role constraints aligned with `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`.
- Review CORS, file storage persistence, storage permissions, secret management, database privileges, error responses, and audit retention before production use.
- Do not provide unrestricted public Administrator registration.

## Regression Test Checklist

1. All three roles can log in.
2. Administrator and Document Owner can upload a valid PDF within the configured size limit.
3. PNG, JPEG, DOCX, and other non-PDF uploads are rejected by the backend.
4. Viewer upload attempts are denied.
5. An authorized user can download their own PDF.
6. Other users cannot download private PDFs they do not own.
7. Public PDFs can be downloaded only where permitted by backend rules.
8. Visibility changes are allowed only for authorized roles and documents.
9. Unauthorized actions are denied and relevant events are audited.
10. Viewer access to audit records is denied, while an authorized Administrator can access them.
11. `trustvault-files/`, secrets, credentials, and environment files containing sensitive values are excluded from Git.
12. PDF preview works for valid PDF documents.

Typical results may include `201 Created` for successful uploads, `200 OK` for successful retrieval, and `403 Forbidden` for authorization failures. Verify the actual exception handling in the application.

## Git Hygiene

Before committing, run:

```bash
git status
git diff --check
git diff --cached --check
```

Review staged files carefully. Exclude `trustvault-files/`, `.env` files containing secrets, credentials, JWTs, and generated build output.

## Project Status

The application has been deployed, and registration, login, document upload, and download have been reported working during development testing. Verify PDF-only enforcement in both the frontend and backend, PDF preview, persistent file storage, authorization, and audit behavior before claiming production readiness.

## License

Add a license file if you intend to distribute this project under a specific open-source license.