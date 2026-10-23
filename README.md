# TrustVault

TrustVault is a secure document-sharing application built with a Java Spring Boot REST API, Oracle Database, and a React/Vite frontend. It provides JWT authentication, role-based access control, document upload and retrieval, public/private sharing, and audit logging for security-relevant actions.

> **Security:** Never commit passwords, JWT signing secrets, database credentials, or uploaded documents. Keep the local `trustvault-files/` directory out of Git.

## Features

- Stateless JWT authentication and BCrypt password hashing.
- Exactly three roles: `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`.
- Document upload, listing, download, and preview.
- Documents are private by default.
- Public/private visibility controls and ownership checks.
- Audit events for authentication and document actions, including denied access.
- Oracle persistence through Spring Data JPA / Hibernate.
- React + Vite frontend.

## Technology stack

**Backend:** Java, Spring Boot, Spring Security, Spring Data JPA/Hibernate, JWT, BCrypt, Oracle Database XE.

**Frontend:** React, Vite, JavaScript, `lucide-react`.

## Roles and permissions

| Capability | Administrator | Document Owner | Viewer |
|---|---|---|---|
| Log in | Yes | Yes | Yes |
| Upload documents | Yes | Yes | No |
| View own documents | Yes | Yes | No dedicated personal list |
| List visible documents | According to service authorization | Own and public documents | Public documents |
| Download own document | Yes | Yes | Not applicable |
| Download another user's private document | No | No | No |
| Download public document | Yes | Yes | Yes |
| Change visibility | Current controller policy restricts this to Document Owners | Yes, for own documents | No |
| View audit records | Authorized Administrator | No | No |

Access must be enforced by the backend, not only by hiding frontend controls. Keep controller annotations and service checks consistent.

## API overview

The current local backend is configured for `http://localhost:8082`. Verify actual mappings in the controllers if they differ.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/auth/register` | Register a Viewer (confirm request fields in `AuthController`) |
| `POST` | `/api/auth/register-owner` | Register a Document Owner |
| `POST` | `/api/auth/login` | Log in and receive a JWT |
| `POST` | `/api/documents/upload` | Upload a file as `multipart/form-data`, field `file` |
| `GET` | `/api/documents` | List documents visible to the current user |
| `GET` | `/api/documents/my` | List the current user's documents; Administrator and Document Owner support was added during development |
| `GET` | `/api/documents/{id}/download` | Download an authorized document |
| `GET` | `/api/documents/{id}/preview` | Preview supported PDF/image files |
| `PUT` | `/api/documents/{id}/visibility?makePublic=true` | Change visibility; current controller policy restricts this to Document Owners |
| `GET` | `/api/test/roles/viewer` | Test Viewer authorization |
| `GET` | `/api/test/roles/administrator` | Test Administrator authorization |
| `GET` | `/api/test/roles/owner` | Test Document Owner authorization |

Audit route details should be confirmed in the current `AuditController`.

For protected requests, send the login token in the request header:

```http
Authorization: Bearer <your-jwt-token>
```

## Local setup

### Prerequisites

- A JDK compatible with the Maven compiler configuration.
- Maven (or the Maven wrapper if present).
- Oracle Database XE and a database user/schema configured for the application.
- Node.js and npm.

### Configure the backend

Review `src/main/resources/application.properties` or the active configuration file. Configure the Oracle connection and JWT secret using local environment variables or an untracked local configuration file.

Example placeholders (adapt property names to the project configuration):

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:XE
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

These are placeholders, not real credentials. If the JWT implementation expects a Base64-encoded secret, provide a correctly encoded, sufficiently long secret. Never commit production credentials or a real signing secret.

### Run the backend

From the repository root:

```bash
mvn spring-boot:run
```

You can also run the Spring Boot main class from IntelliJ IDEA. Confirm the configured port; the current development setup uses `8082`.

### Run the frontend

Open a terminal in the frontend directory (the directory containing `package.json`):

```bash
npm install
npm run dev
```

The current Vite development URL is `http://localhost:5173/`. Ensure the frontend API base URL points to `http://localhost:8082`.

## Upload rules and storage

- Accepted types in the current service: PDF, PNG, JPEG, and DOCX.
- Current service-level maximum size: 10 MB.
- File bytes are stored in the configured storage directory; metadata is stored in Oracle.
- New documents are private by default.
- Do not commit `trustvault-files/` or any uploaded content.

## Security and audit guidance

- Store password hashes, not plaintext passwords.
- Never log or commit JWTs, passwords, or secrets.
- Enforce authorization in both controller and service layers.
- Log successful and denied security-relevant actions without recording credentials.
- Keep Oracle role constraints aligned with `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`.
- Before deployment, review CORS, file storage permissions, secret management, database privileges, error responses, and audit retention.

## Regression test checklist

1. All three roles can log in.
2. Administrator and Document Owner can upload an allowed file under 10 MB.
3. Viewer upload is denied.
4. A user can download their own document.
5. Other users cannot download a private document they do not own.
6. All roles can download public documents where permitted by service rules.
7. Visibility changes are allowed only to the role and owner authorized by the backend.
8. Unauthorized actions are denied and relevant events are audited.
9. Viewer access to audit records is denied; an authorized Administrator can access them.
10. `trustvault-files/`, database credentials, and JWT secrets are excluded from Git.

Typical results are `201 Created` for successful upload, `200 OK` for successful retrieval, and `403 Forbidden` for authorization failures; verify the actual exception handling in the application.

## Git hygiene

Before committing, run:

```bash
git status
git diff --check
git diff --cached --check
```

Review the staged files carefully. Exclude `trustvault-files/`, `.env` files containing secrets, credentials, JWTs, and generated build output.

## Project status

The development work includes JWT authentication, three-role authorization, document upload/list/download/preview, private-by-default storage, visibility controls, and audit-related checks. Run the regression checklist before claiming all flows are complete or production-ready.

## License

Add a license file if you intend to distribute this project under a specific open-source license.
