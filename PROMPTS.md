# TrustVault — PROMPTS.md

This file records representative AI-assisted development prompts used during the TrustVault project. Keep it updated as development progresses. Never include passwords, JWTs, database credentials, signing secrets, or private document contents.

## 2026-10-19 — Project setup and architecture

**Prompt:** Help me start TrustVault from scratch as a secure document-sharing application. Plan the backend, database, authentication, authorization, document storage, frontend, and deployment work in small, testable steps.

**Focus:** Establish an incremental implementation plan and a clear application structure.

## 2026-10-19 — Define exactly three roles

**Prompt:** Update TrustVault to use exactly three roles: `ADMINISTRATOR`, `DOCUMENT_OWNER`, and `VIEWER`. Do not include an Editor role. Keep role names consistent across the Java enum, Spring Security authorities, controller annotations, and database constraints.

**Security focus:** Least privilege and consistent role naming across application and database layers.

## 2026-10-20 — JWT authentication and password security

**Prompt:** Help implement stateless JWT authentication with Spring Security, a custom user-details service, and BCrypt password hashing. Explain registration and login flows and how to call protected endpoints with a Bearer token.

**Security focus:** Do not store plaintext passwords or commit JWT secrets. Ensure authorities match the role names used by access checks.

## 2026-10-20 — Securely provision the first Administrator

**Prompt:** Help create the first TrustVault Administrator securely. Avoid an unrestricted public administrator-registration path and explain how to verify the role and test administrator-only endpoints.

**Security focus:** Controlled administrator provisioning; never expose credentials or tokens in source control.

## 2026-10-21 — Document upload and private-by-default behavior

**Prompt:** Implement document upload with metadata stored in Oracle and file bytes stored in the configured storage directory. Validate empty files, file size, content types, and safe file paths. Keep documents private by default and audit upload outcomes.

**Security focus:** Only Administrator and Document Owner can upload; Viewers must be denied. Keep uploaded files out of Git.

## 2026-10-21 — Enforce ownership and public/private access

**Prompt:** Review document listing, preview, download, and visibility endpoints so authorization depends on the authenticated user's role, ownership, and the document's public/private state.

**Security focus:** Enforce rules in the service layer as well as the controller. A hidden frontend button is not a security control.

## 2026-10-21 — Audit security-relevant actions

**Prompt:** Add or review audit events for authentication and document operations, including successful actions and denied access. Verify that a Viewer cannot access audit records and an authorized Administrator can.

**Security focus:** Record actor, action, resource, outcome, and useful context without logging passwords, JWTs, or other secrets.

## 2026-10-23 — Fix Administrator upload authorization

**Prompt:** Diagnose why Administrator uploads return `403 Access denied` even after the controller permits the Administrator role. Inspect service-level role checks and update them so Administrators and Document Owners can upload while Viewers remain blocked.

**Security focus:** Check every authorization layer and preserve file validation, audit logging, and private-by-default behavior.

## 2026-10-23 — Fix Administrator listing and download authorization

**Prompt:** Diagnose `403 Access denied` in the Administrator's My Documents and download flows. Align controller and service authorization so an Administrator can list and download their own documents without gaining access to other users' private documents.

**Security focus:** Check both file-download authorization and metadata authorization used for response headers. Preserve privacy rules and audit denied access.

## 2026-10-23 — Role regression testing and repository documentation

**Prompt:** Create a regression checklist for Administrator, Document Owner, and Viewer covering login, upload, listing, private/public download, visibility changes, and audit access. Update README.md and PROMPTS.md to document the stack, setup, endpoints, security model, and development prompts.

**Security focus:** Test allowed and denied actions, review staged files before committing, and exclude uploaded documents and secrets from Git.
