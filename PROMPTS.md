# TrustVault — PROMPTS.md

This file records representative AI-assisted development prompts used during the TrustVault project. Keep it updated as development progresses.

Never include passwords, JWTs, database credentials, signing secrets, or private document contents in this file.

## 2026-10-19 — Project Setup and Architecture

**Prompt:** Help me start TrustVault from scratch as a secure document-sharing application. Plan the backend, database, authentication, authorization, document storage, frontend, and deployment work in small, testable steps.

**Focus:** Establish an incremental implementation plan and a clear application structure.

## 2026-10-19 — Define Exactly Three Roles

**Prompt:** Update TrustVault to use exactly three roles: `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`. Do not include an Editor role. Keep role names consistent across the Java enum, Spring Security authorities, controller annotations, and database constraints.

**Security focus:** Least privilege and consistent role naming across application and database layers.

## 2026-10-20 — JWT Authentication and Password Security

**Prompt:** Help implement stateless JWT authentication with Spring Security, a custom user-details service, and BCrypt password hashing. Explain registration and login flows and how to call protected endpoints with a Bearer token.

**Security focus:** Do not store plaintext passwords or commit JWT secrets. Ensure authorities match the role names used by access checks.

## 2026-10-20 — Securely Provision the First Administrator

**Prompt:** Help create the first TrustVault Administrator securely. Avoid an unrestricted public Administrator-registration path and explain how to verify the role and test Administrator-only endpoints.

**Security focus:** Controlled Administrator provisioning; never expose credentials or tokens in source control.

## 2026-10-21 — PDF Upload and Private-by-Default Behavior

**Prompt:** Update TrustVault so that only PDF files can be uploaded. Reject PNG, JPEG, DOCX, and every other non-PDF file at the backend. Validate empty files, file size, content type, PDF signature, and safe storage paths. Keep documents private by default and audit upload outcomes.

**Security focus:** Only Administrators and Document Owners can upload; Viewers must be denied. Never rely on frontend file filters as the only validation. Keep uploaded files out of Git.

## 2026-10-21 — Enforce Ownership and Public/Private Access

**Prompt:** Review document listing, preview, download, and visibility endpoints so authorization depends on the authenticated user's role, ownership, and the document's public/private state.

**Security focus:** Enforce rules in the service layer as well as the controller. A hidden frontend button is not a security control.

## 2026-10-21 — Audit Security-Relevant Actions

**Prompt:** Add or review audit events for authentication and document operations, including successful actions and denied access. Verify that a Viewer cannot access audit records and an authorized Administrator can.

**Security focus:** Record actor, action, resource, outcome, and useful context without logging passwords, JWTs, or other secrets.

## 2026-10-23 — Fix Administrator Upload Authorization

**Prompt:** Diagnose why Administrator uploads return `403 Access denied` even after the controller permits the Administrator role. Inspect service-level role checks and update them so Administrators and Document Owners can upload while Viewers remain blocked.

**Security focus:** Check every authorization layer and preserve file validation, audit logging, and private-by-default behavior.

## 2026-10-23 — Fix Administrator Listing and Download Authorization

**Prompt:** Diagnose `403 Access denied` in the Administrator's document listing and download flows. Align controller and service authorization so an Administrator can access documents according to the intended policy without bypassing private-document restrictions.

**Security focus:** Check both file-download authorization and metadata authorization used for response headers. Preserve privacy rules and audit denied access.

## 2026-10-23 — Role Regression Testing and Repository Documentation

**Prompt:** Create a regression checklist for Administrator, Document Owner, and Viewer covering login, PDF upload, listing, private/public download, visibility changes, and audit access. Update README.md and PROMPTS.md to document the stack, setup, endpoints, security model, and development prompts.

**Security focus:** Test allowed and denied actions, review staged files before committing, and exclude uploaded documents and secrets from Git.

## 2026-10-25 — Document the PDF-Only File Policy

**Prompt:** Update the TrustVault documentation so PDF is the only supported upload format. Remove documentation that says PNG, JPEG, or DOCX are accepted. Document PDF-only upload, preview, download, backend validation, and regression tests.

**Security focus:** Documentation must match backend and frontend enforcement. Do not claim the application rejects other file types until backend validation and the frontend file picker have been updated and tested.

## Document Upload, Preview, and Download Policy

- **Upload:** Users can upload files in any format, subject to the application's configured file-size limits.
- **Preview:** Only PDF files can be previewed within TrustVault. Non-PDF files cannot be previewed in the application.
- **Download:** Publicly accessible files can be downloaded in any file format, including images, Word documents, ZIP files, and other file types.
- **Access control:** Download permissions depend on the document's visibility and the user's role. Private documents must remain protected from unauthorized access.
- **File security:** Uploaded files must be handled safely. The application must validate access permissions and avoid executing uploaded files.

**Important:** Being unable to preview a file does not prevent users from downloading it when they have permission to access it.
