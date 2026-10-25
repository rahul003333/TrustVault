This document records the main prompts used during the development of TrustVault and summarizes the guidance, design decisions, implementation work, and verification outcomes.

The log is intended to make the development process transparent and reproducible. It is not a verbatim transcript of every conversation. Prompt descriptions and assistant-response summaries are reconstructed from the project development history.

## Project Brief — 2026-10-19

**User prompt:**

Build TrustVault, a secure document-sharing application with a real audit trail, from scratch within seven days. Use a suitable technology stack, maintain a public GitHub repository, deploy a live version, commit work progressively, and maintain a `PROMPTS.md` file with at least five dated entries, including at least two entries about authentication, access control, or auditing.

**Assistant response and development direction:**

The project was organized into a Spring Boot REST API, a React/Vite frontend, a relational database, JWT authentication, role-based authorization, secure document storage, and audit logging. The recommended workflow was to implement and verify features incrementally, maintain Git history, document AI-assisted development, and avoid committing secrets or uploaded files.

**Design decisions:**

- Use a layered backend architecture.
- Separate document metadata from the stored file bytes.
- Enforce authorization on the server.
- Commit work as meaningful features are completed.
- Keep `PROMPTS.md` updated throughout development.
- Treat the live deployment and public repository as project deliverables.

## 2026-10-19 — Backend Architecture and Initial Setup

**User prompt:**

Start the TrustVault project from scratch and help implement the application step by step.

**Assistant response summary:**

The backend was structured around Spring Boot, REST controllers, service classes, repositories, entities, and security configuration. The frontend was planned as a React/Vite application. Database persistence and API testing were included in the development workflow.

The initial setup involved resolving Java/Maven configuration problems, creating missing security configuration, starting the application, and testing controller routes.

**Implementation notes:**

- Backend uses Spring Boot and Spring Data JPA.
- Frontend uses React and Vite.
- API requests are tested using Postman.
- Local backend development uses port `8082`.
- Frontend development has used port `5173`.

**Learning outcome:**

Establishing the project structure and verifying that the application starts should precede adding more complex security and document features.

## 2026-10-20 — Define Exactly Three Roles

**User prompt:**

Use exactly three roles: Administrator, Document Owner, and Viewer. Remove the Editor role.

**Assistant response summary:**

The role model was standardized to the application role identifiers `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`. Role checks and database constraints need to use consistent identifiers.

**Implementation decisions:**

- `ADMINISTRATOR`: authorized administrative operations.
- `DOCUMENT_OWNER`: document upload and management within ownership rules.
- `VIEWER`: access to documents explicitly made available to viewers, including public documents where permitted.
- Do not introduce an `EDITOR` role.

**Security focus:**

Role names must agree across the Java enum, Spring Security authorities, service-layer checks, user records, and database constraints. A mismatch can produce unexpected `401 Unauthorized` or `403 Forbidden` responses.

## 2026-10-20 — JWT Authentication and Password Security

**User prompt:**

Implement registration and login, use JWT authentication, hash passwords securely, and protect endpoints using roles.

**Assistant response summary:**

The authentication flow was built around Spring Security, a JWT authentication filter, a JWT service, and a custom user-details service. Passwords are encoded with BCrypt. Protected requests use a bearer token in the `Authorization` header.

The JWT signing secret is supplied through configuration rather than embedded in source code.

**Implementation decisions:**

- Register and authenticate users through the authentication API.
- Store encoded passwords, not plaintext passwords.
- Load authenticated users through the user-details service.
- Use stateless session configuration for JWT-based API requests.
- Apply role restrictions using Spring Security authorization rules and method security.

**Verification history:**

During development, the Viewer role endpoint returned `200 OK`, while attempts to access Administrator- and Document Owner-restricted endpoints returned `403 Forbidden`. These tests helped confirm that role checks were being applied.

**Security focus:**

Never commit JWT signing secrets, passwords, or access tokens. Authentication establishes identity; authorization determines what that identity may do.

## 2026-10-20 — Securely Provision the First Administrator

**User prompt:**

Help create the first TrustVault Administrator securely. Avoid an unrestricted public administrator-registration path and explain how to verify the role and test administrator-only endpoints.

**Assistant response summary:**

The first administrator was provisioned through a controlled process rather than allowing every public registration request to choose the Administrator role. A temporary bootstrap approach was used with the application's password encoder and configuration supplied through environment variables.

The user then verified the administrator role and tested the protected endpoint.

**Verification history:**

- Administrator endpoint test returned `200 OK` after the account was correctly provisioned.
- The user tested authentication and role-protected endpoints using Postman and the application.

**Security focus:**

- Do not expose public self-service administrator registration.
- Avoid hardcoding administrator credentials.
- Remove temporary bootstrap code after provisioning.
- Keep credentials and tokens out of source control.
- Verify the role assignment and test protected endpoints.

## 2026-10-21 — Document Upload and Private-by-Default Behavior

**User prompt:**

Implement document upload with metadata stored in the database and file bytes stored in the configured storage directory. Validate empty files, file size, content types, and safe file paths. Keep documents private by default and audit upload outcomes.

**Assistant response summary:**

The document feature was developed using a document entity, repository, service, and controller. Document metadata is persisted in the database, while file bytes are stored in a server-side directory. The service layer is responsible for ownership and authorization checks.

The initial implementation used Oracle Database; the project was subsequently migrated to PostgreSQL.

**Implementation decisions:**

- Keep uploaded file bytes outside public frontend assets.
- Store document metadata separately from file contents.
- Validate uploaded files and configured size limits.
- Use private-by-default visibility.
- Record upload outcomes in the audit trail.
- Prevent Viewers from uploading documents.

**Security focus:**

Uploaded documents are untrusted input. Validate file size, empty content, content type, and storage paths. Never rely on a client-provided filename as a trusted filesystem path. Uploaded files must not be committed to Git.

## 2026-10-21 — Enforce Ownership and Public/Private Access

**User prompt:**

Review document listing, preview, download, and visibility endpoints so authorization depends on the authenticated user's role, ownership, and the document's public/private state.

**Assistant response summary:**

Authorization was reviewed across the document controller and service layer. The backend must verify the current authenticated identity, the requested document, the ownership relationship, and visibility rules before returning document content or metadata.

**Implementation decisions:**

- Owners can manage their own documents within the role policy.
- Viewers can access public documents where the endpoint allows it.
- Viewers cannot access another user's private documents.
- Visibility changes require authorization.
- Metadata endpoints and download endpoints must follow compatible authorization rules.

**Security focus:**

A hidden frontend button is not an access-control mechanism. Authorization must be enforced by the backend for every protected operation, including requests made directly through Postman.

## 2026-10-21 — Audit Security-Relevant Actions

**User prompt:**

Add or review audit events for authentication and document operations, including successful actions and denied access. Verify that a Viewer cannot access audit records and an authorized Administrator can.

**Assistant response summary:**

Audit logging was included in the document and security workflows. Audit records support tracing document operations and denied access. Administrative audit views must be protected by authorization.

**Implementation decisions:**

- Record security-relevant operations such as uploads, downloads, and access denials.
- Capture the actor, action, resource, outcome, timestamp, and useful context where available.
- Restrict audit-record access to authorized administrators.
- Avoid writing passwords, JWTs, and other secrets to logs.

**Verification history:**

The project database contained audit records for document operations and denied access attempts during testing.

**Security focus:**

Audit logs should help investigate security events without creating a new source of credential leakage.

## 2026-10-23 — Fix Administrator Upload Authorization

**User prompt:**

Diagnose why Administrator uploads return `403 Access denied` even after the controller permits the Administrator role. Inspect service-level role checks and update them so Administrators and Document Owners can upload while Viewers remain blocked.

**Assistant response summary:**

The troubleshooting approach was to inspect authorization at every layer, especially service-level checks that can deny a request even when the controller permits it. The role policy needed to be consistent across controller and service methods.

**Implementation decisions:**

- Allow upload only for authorized Administrator and Document Owner requests.
- Continue to deny Viewer uploads.
- Preserve upload validation and private-by-default behavior.
- Preserve audit logging for success and denial.

**Security focus:**

A controller-level role check does not guarantee that a request will succeed. Every authorization layer must implement the intended policy consistently.

## 2026-10-23 — Fix Administrator Listing and Download Authorization

**User prompt:**

Diagnose `403 Access denied` in the Administrator's My Documents and download flows. Align controller and service authorization so an Administrator can list and download their own documents without gaining access to other users' private documents.

**Assistant response summary:**

The debugging process considered both the download operation and metadata access used to prepare the response. Authorization must be consistent across the document lookup, metadata retrieval, and file-content retrieval paths.

**Implementation decisions:**

- Permit authorized users to access their own documents.
- Keep private-document access restricted by ownership and policy.
- Ensure metadata checks do not contradict download authorization.
- Record denied access attempts.
- Verify both the HTTP response and the application logs during troubleshooting.

**Verification history:**

The user reported successful owner document downloads and a `403 Forbidden` result when a Viewer attempted to download a document that the Viewer was not authorized to access.

**Security focus:**

A denied private-document download is expected behavior when the caller lacks permission. Do not weaken authorization merely to make an HTTP request return `200 OK`.

## 2026-10-23 — Role Regression Testing and Documentation

**User prompt:**

Verify the role behavior after the authorization fixes, update the project documentation, and prepare Git changes without committing uploaded documents.

**Assistant response summary:**

The verification workflow included testing role-protected endpoints, document upload/list/download, and public/private access. Documentation needed to explain the roles, setup, API behavior, and security model. Git changes were to be reviewed before staging and committing.

**Implementation decisions:**

- Keep exactly three roles.
- Test allowed and denied access paths.
- Review staged changes before committing.
- Keep `trustvault-files/` excluded from Git.
- Keep secrets and local configuration out of source control.
- Preserve meaningful commits throughout development.

**Security focus:**

Regression tests should verify both allowed behavior and forbidden behavior. Successful access alone is not sufficient to establish that an authorization system is secure.

## 2026-10-25 — Migrate Oracle Persistence to PostgreSQL

**User prompt:**

Update TrustVault from Oracle Database to PostgreSQL, configure the application through environment variables, test the changes, and prepare the repository for deployment.

**Assistant response summary:**

The PostgreSQL JDBC dependency replaced the Oracle JDBC dependency. The datasource configuration was updated to use a PostgreSQL JDBC URL and environment-based credentials. SQL logging and Spring Security logging were configured to use less verbose defaults.

The `.env.example` file was added as a template for local and deployment configuration.

**Implementation decisions:**

- PostgreSQL is the current database target.
- Local JDBC URL defaults to `jdbc:postgresql://localhost:5432/trustvault`.
- `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` configure the database connection.
- `JWT_SECRET` configures the JWT signing secret.
- `PORT` can override the local backend port.
- `DDL_AUTO` and `SHOW_SQL` allow configuration of schema behavior and SQL logging.
- Real secrets must remain outside the repository.

**Troubleshooting history:**

The initial Maven test failed while loading the Spring application context. The error indicated that Hibernate could not determine the SQL dialect from JDBC metadata. The terminal environment lacked the expected database and JWT environment variables.

PostgreSQL connectivity was checked on port `5432`, and the existing JWT secret was copied from IntelliJ's Run Configuration into the current PowerShell session without sharing the value in chat.

The user subsequently reported that the test command succeeded.

**Security focus:**

- Do not publish the actual JWT secret.
- Do not commit local passwords.
- Do not assume `.env.example` automatically loads environment variables into Spring Boot.
- Verify database schema behavior before production deployment.
- Keep uploaded files outside source control.

## 2026-10-25 — Environment Configuration and Deployment Readiness

**User prompt:**

Review the PostgreSQL migration, replace outdated Oracle documentation, verify the Git changes, and prepare for GitHub updates and deployment.

**Assistant response summary:**

The documentation update was planned to cover PostgreSQL setup, environment variables, backend and frontend startup, role behavior, API testing, storage, and production configuration. Git checks were recommended before committing.

**Implementation decisions:**

- Update `README.md` to describe PostgreSQL instead of Oracle.
- Preserve the earlier development history in this prompt log.
- Verify `.gitignore` excludes `trustvault-files/`.
- Keep `.env` files containing real credentials out of Git.
- Run tests and review `git diff --check`.
- Review `git status` and the exact diff before staging files.
- Commit and push only the intended source and documentation changes.

**Deployment considerations:**

- Configure production PostgreSQL credentials and JWT secret through the hosting provider.
- Use HTTPS and restrict CORS to the actual frontend origin.
- Use persistent storage or object storage for uploaded files.
- Verify database schema creation and migration before using `DDL_AUTO=validate`.
- Review and disable development-only test endpoints before production.
- Do not claim a live deployment until the deployed application has been verified.

## Lessons Learned

1. Authentication and authorization are different: a valid JWT does not grant every operation.
2. Role names must remain consistent across Java code, Spring Security, and the database.
3. Ownership and visibility checks belong on the backend, not only in the frontend.
4. File uploads require validation and safe server-side storage.
5. Audit logs should record useful security context without storing secrets.
6. Local environment variables and IntelliJ Run Configuration settings may differ.
7. A `.env.example` file is documentation, not an automatic environment loader.
8. Database migrations require updates to dependencies, configuration, schema assumptions, and documentation.
9. Tests should cover both successful operations and forbidden requests.
10. Git history should reflect meaningful progress, and secrets and uploaded files must never be committed.

## Current Development Status

The development history available for this log records:

- Spring Boot backend and React/Vite frontend.
- JWT authentication and BCrypt password hashing.
- Three roles: Administrator, Document Owner, and Viewer.
- Document upload, listing, visibility, and download flows.
- Ownership and public/private access controls.
- Audit logging for security-relevant document actions.
- PostgreSQL configuration replacing the previous Oracle configuration.
- A successful Maven test result reported by the user after environment setup.

**Still requiring independent verification before declaring the project complete:**

- A fresh clone can be configured and started using the documented instructions.
- All automated tests pass from a clean checkout.
- PostgreSQL schema and constraints are correct for a fresh installation.
- Uploaded files persist in the intended deployment environment.
- The frontend and backend work together at the deployed URLs.
- Production CORS, secrets, database permissions, and development-only endpoints are reviewed.
- The GitHub repository is public and contains the intended source files.
- A live deployment is accessible and its key workflows have been tested.

