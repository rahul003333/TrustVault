# TrustVault Frontend

A React-based frontend for **TrustVault**, a secure document-sharing application with role-based access control and audit logging.

## Overview

The TrustVault frontend provides the user interface for interacting with the Spring Boot backend. Users can authenticate, view documents, and perform actions permitted by their assigned role.

## Technology Stack

- **React** — component-based user interface
- **Vite** — frontend development server and build tool
- **JavaScript (ES6+)** — application logic
- **CSS** — styling and responsive layouts
- **Lucide React** — interface icons
- **Fetch API or configured HTTP client** — backend communication, depending on the implementation

## User Roles

TrustVault supports three roles:

- **Administrator** — administrative access and permitted document operations.
- **Document Owner** — manages their documents and sharing settings.
- **Viewer** — accesses documents according to sharing permissions.

The backend is responsible for enforcing authentication and authorization. Hiding a frontend button is not a substitute for backend security.

## Prerequisites

Install the following before running the frontend:

- Node.js (a current supported LTS version)
- npm
- TrustVault backend running locally when API functionality is required

## Getting Started

### 1. Open the frontend directory

```bash
cd frontend
```

### 2. Install dependencies

```bash
npm install
```

### 3. Start the development server

```bash
npm run dev
```

Open the local URL printed by Vite in your terminal. The development server commonly uses:

`http://localhost:5173`

### 4. Build for production

```bash
npm run build
```

Vite generates the production build in the `dist/` directory.

### 5. Preview the production build

```bash
npm run preview
```

## Backend Integration

The Spring Boot backend is configured to run locally on port `8082` in the development setup.

Backend base URL:

`http://localhost:8082`

Ensure the frontend's API requests use the correct backend URL and that the backend's CORS configuration permits the frontend origin.

If the frontend uses an environment variable for the API URL, configure it using the variable name expected by the source code. Do not commit real credentials or secret values.

## Security Guidelines

- Do not store passwords, JWT signing secrets, or database credentials in frontend source code.
- Do not treat client-side role checks as security enforcement.
- Send authentication tokens using the mechanism supported by the backend.
- Handle unauthorized and forbidden responses appropriately.
- Keep uploaded documents and other private data out of the frontend source repository.
- Validate API responses and display useful error messages without exposing sensitive details.

## Project Structure

```text
frontend/
├── public/             # Static public assets
├── src/
│   ├── assets/         # Images and other imported assets
│   ├── App.jsx         # Main React application
│   ├── App.css         # Application styles
│   ├── index.css       # Global styles
│   └── main.jsx        # React entry point
├── .gitignore
├── index.html
├── package.json
├── package-lock.json
└── vite.config.js
```

Additional components, pages, and API utilities may be added as the application develops.

## Development Notes

- Keep reusable UI elements in separate components where appropriate.
- Keep API communication separate from presentation logic when practical.
- Test each role against the backend's actual authorization rules.
- Check `package.json` for the scripts available in the current project.
- Never commit `node_modules/`, generated `dist/` files, or sensitive environment configuration.

## License

Add the project's applicable license information here if a license
