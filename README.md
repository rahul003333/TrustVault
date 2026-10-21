# TrustVault — Secure Document Management System

TrustVault is a role-based document management application built with Java and Spring Boot. It provides secure user authentication and allows authorized document owners to upload, view, and download their documents.

## Features

- User registration and login
- JWT-based authentication
- BCrypt password hashing
- Role-based access control
- Secure document upload and download
- Document metadata stored in Oracle Database
- File ownership verification
- Protected REST APIs

## Technology Stack

**Backend**
- Java
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- REST APIs
- JWT authentication

**Database**
- Oracle Database

**Development Tools**
- IntelliJ IDEA
- Postman
- Git and GitHub

**Frontend**
- React frontend planned / in progress

## User Roles

TrustVault supports three roles:

1. **ADMINISTRATOR** — Administrative access to authorized system operations.
2. **DOCUMENT_OWNER** — Uploads, lists, and downloads their own documents.
3. **VIEWER** — Can authenticate but cannot access document-owner-only operations.

## API Endpoints

Base URL: `http://localhost:8082`

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a Viewer |
| POST | `/api/auth/register-owner` | Register a Document Owner |
| POST | `/api/auth/login` | Authenticate and obtain a JWT |
| POST | `/api/documents/upload` | Upload a document |
| GET | `/api/documents/my` | List the current owner's documents |
| GET | `/api/documents/{id}/download` | Download an owned document |

Protected endpoints require a valid JWT in the Authorization header:

`Authorization: Bearer <your-token>`

## Running the Backend

1. Install a compatible JDK, Maven (if not using the Maven wrapper), and Oracle Database.
2. Clone the repository.
3. Configure the Oracle database connection and JWT secret using local configuration or environment variables.
4. Run the Spring Boot application from IntelliJ IDEA or with the Maven wrapper.
5. Test the API endpoints using Postman.

Never commit database passwords, JWT signing secrets, or uploaded private documents to GitHub.

## Document Storage

Uploaded files are stored in a local directory, while their metadata is maintained in Oracle Database. The storage directory can be configured using the `trustvault.storage-dir` property.

## Security

- JWT-based stateless authentication
- BCrypt password hashing
- Role-based endpoint restrictions
- Document ownership checks
- File size and allowed content-type validation

## Project Status

**Backend:** Core authentication, authorization, and document management implemented and tested.

**Frontend:** React UI planned / in progress.

## Future Enhancements

- Complete React dashboards for each role
- Administrator management interface
- Document deletion and additional audit features
- Automated backend and frontend tests
- Deployment and production configuration

## Author

Developed as a secure document management project using Java, Spring Boot, Oracle Database, and Spring Security.