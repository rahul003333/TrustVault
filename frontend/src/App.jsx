
import { useEffect, useMemo, useState } from "react";
import {
  ShieldCheck,
  LayoutDashboard,
  Files,
  FolderKanban,
  Upload,
  ClipboardList,
  LogOut,
  Search,
  Menu,
  X,
  FileText,
  FileImage,
  File,
  Download,
  Eye,
  EyeOff,
    Globe,
    GlobeOff,
    Trash2,
  LockKeyhole,
  CloudUpload,
  LoaderCircle,
  CheckCircle2,
  AlertCircle,
  UserRound,
  RefreshCw,
} from "lucide-react";
import "./App.css";

const API_BASE = "http://trustvault-backend-cjkc.onrender.com/api";

function readStoredUser() {
  const token = localStorage.getItem("trustvault_token");

  if (!token) return null;

  return {
    token,
    username: localStorage.getItem("trustvault_username") || "User",
    role: (localStorage.getItem("trustvault_role") || "VIEWER").toUpperCase(),
  };
}

function normalizeList(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.documents)) return data.documents;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

function getDocumentName(doc) {
  return (
      doc.fileName ||
      doc.filename ||
      doc.name ||
      doc.documentName ||
      `Document ${doc.id ?? ""}`
  );
}

function getDocumentId(doc) {
  return doc.id ?? doc.documentId;
}

function getOwnerName(doc) {
  return (
      doc.ownerUsername ||
      doc.owner?.username ||
      doc.uploadedBy ||
      doc.createdBy ||
      "—"
  );
}

function isPublicDocument(doc) {
  return (
      doc?.publiclyShared === true ||
      doc?.publiclyShared === "true"
  );
}

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);

  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function getFileIcon(name) {
  const extension = String(name).split(".").pop().toLowerCase();

  if (["png", "jpg", "jpeg", "gif", "webp"].includes(extension)) {
    return FileImage;
  }

  if (["pdf", "doc", "docx", "txt", "xls", "xlsx"].includes(extension)) {
    return FileText;
  }

  return File;
}

export default function App() {
  const [user, setUser] = useState(() => readStoredUser());
  const [activePage, setActivePage] = useState("documents");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const [isRegister, setIsRegister] = useState(false);
  const [selectedRole, setSelectedRole] = useState("VIEWER");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [documents, setDocuments] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);

  const [search, setSearch] = useState("");
  const [file, setFile] = useState(null);
  const [uploadVisibility, setUploadVisibility] = useState("PRIVATE");

  const [previewDocument, setPreviewDocument] = useState(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [previewLoading, setPreviewLoading] = useState(false);

  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [pageLoading, setPageLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("success");

  const role = user?.role || "VIEWER";

  const isAdmin = role === "ADMINISTRATOR" || role === "ADMIN";

  const canUpload =
      role === "DOCUMENT_OWNER" ||
      role === "OWNER" ||
      isAdmin;

  // Viewers cannot access My Docs or Upload Document.
  const canAccessMyDocs = canUpload;

  async function apiRequest(path, options = {}) {
    const token = user?.token || localStorage.getItem("trustvault_token");
    const headers = new Headers(options.headers || {});

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }

    if (options.body && !(options.body instanceof FormData)) {
      headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    });

    if (response.status === 204) return null;

    const contentType = response.headers.get("content-type") || "";
    let data = null;

    if (contentType.includes("application/json")) {
      data = await response.json();
    } else if (!options.download) {
      data = await response.text();
    }

    if (!response.ok) {
      const errorMessage =
          data?.message ||
          data?.error ||
          (typeof data === "string" && data) ||
          `Request failed (${response.status})`;

      throw new Error(errorMessage);
    }

    if (options.download) {
      return response.blob();
    }

    return data;
  }

  function showMessage(text, type = "success") {
    setMessage(text);
    setMessageType(type);
  }

  function closePreview() {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    setPreviewUrl("");
    setPreviewDocument(null);
    setPreviewLoading(false);
  }

  function logout() {
    closePreview();

    localStorage.removeItem("trustvault_token");
    localStorage.removeItem("trustvault_username");
    localStorage.removeItem("trustvault_role");

    setUser(null);
    setDocuments([]);
    setAuditLogs([]);
    setActivePage("documents");
    setPassword("");
    setMessage("");
  }


  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setMessage("");

    try {
      const path = isRegister
          ? selectedRole === "DOCUMENT_OWNER"
              ? "/auth/register-owner"
              : "/auth/register"
          : "/auth/login";

      const body = isRegister
          ? { username, email, password }
          : { username, password };

      const response = await fetch(`${API_BASE}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      const contentType = response.headers.get("content-type") || "";
      const data = contentType.includes("application/json")
          ? await response.json()
          : await response.text();

      if (!response.ok) {
        throw new Error(
            data?.message ||
            data?.error ||
            (typeof data === "string" && data) ||
            "Authentication failed"
        );
      }

      if (isRegister) {
        showMessage(
            `${selectedRole === "DOCUMENT_OWNER" ? "Document Owner" : "Viewer"} account created. Please sign in.`
        );
        setIsRegister(false);
        setPassword("");
      } else {
        const token =
            data?.token ||
            data?.accessToken ||
            data?.jwt ||
            data?.access_token;

        if (!token) {
          throw new Error(
              "The login response did not contain a token. Check LoginResponse.java."
          );
        }

        const loggedInUser = {
          token,
          username: data?.username || username,
          role: String(data?.role || "VIEWER").toUpperCase(),
        };

        localStorage.setItem("trustvault_token", loggedInUser.token);
        localStorage.setItem("trustvault_username", loggedInUser.username);
        localStorage.setItem("trustvault_role", loggedInUser.role);

        setUser(loggedInUser);
        setActivePage("documents");
        setPassword("");
        showMessage(`Welcome back, ${loggedInUser.username}!`);
      }
    } catch (error) {
      showMessage(error.message || "Something went wrong.", "error");
    } finally {
      setLoading(false);
    }
  }


  async function loadDocuments() {
    if (!user) return;

    setPageLoading(true);

    try {
      const endpoint =
          activePage === "my-docs" && canAccessMyDocs
              ? "/documents/my"
              : "/documents";

      const data = await apiRequest(endpoint);
      setDocuments(normalizeList(data));
    } catch (error) {
      setDocuments([]);
      showMessage(`Could not load documents: ${error.message}`, "error");
    } finally {
      setPageLoading(false);
    }
  }

  async function loadAuditLogs() {
    if (!user || !isAdmin) return;

    setPageLoading(true);

    try {
      const data = await apiRequest("/audit");
      setAuditLogs(normalizeList(data));
    } catch (error) {
      setAuditLogs([]);
      showMessage(`Could not load audit logs: ${error.message}`, "error");
    } finally {
      setPageLoading(false);
    }
  }

  useEffect(() => {
    if (!user) return;

    if (
        (activePage === "my-docs" && !canAccessMyDocs) ||
        (activePage === "upload" && !canUpload) ||
        (activePage === "audit" && !isAdmin)
    ) {
      setActivePage("documents");
      return;
    }

    if (activePage === "audit") {
      loadAuditLogs();
      return;
    }

    if (activePage === "documents" || activePage === "my-docs") {
      loadDocuments();
    }

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.token, activePage]);

  function navigate(page) {
    if (page === "my-docs" && !canAccessMyDocs) return;
    if (page === "upload" && !canUpload) return;
    if (page === "audit" && !isAdmin) return;

    setActivePage(page);
    setSidebarOpen(false);
    setSearch("");
    setMessage("");
  }

  async function uploadDocument(event) {
    event.preventDefault();

    if (!canUpload) {
      showMessage("Your role cannot upload documents.", "error");
      return;
    }

    if (!file) {
      showMessage("Please choose a file to upload.", "error");
      return;
    }

    setBusy(true);
    setMessage("");

    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("visibility", uploadVisibility);

      await apiRequest("/documents/upload", {
        method: "POST",
        body: formData,
      });

      setFile(null);
      event.target.reset();
      showMessage("Document uploaded successfully.");
      setActivePage("my-docs");
    } catch (error) {
      showMessage(`Upload failed: ${error.message}`, "error");
    } finally {
      setBusy(false);
    }
  }

  async function downloadDocument(doc) {
    const id = getDocumentId(doc);
    if (id == null) return;

    try {
      const blob = await apiRequest(`/documents/${id}/download`, {
        download: true,
      });

      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = url;
      link.download = getDocumentName(doc);

      document.body.appendChild(link);
      link.click();
      link.remove();

      URL.revokeObjectURL(url);
    } catch (error) {
      showMessage(`Download failed: ${error.message}`, "error");
    }
  }

  async function previewDocumentFile(doc) {
    const id = getDocumentId(doc);
    if (id == null) return;

    setPreviewDocument(doc);
    setPreviewLoading(true);
    setPreviewUrl("");

    try {
      const blob = await apiRequest(`/documents/${id}/preview`, {
        download: true,
      });

      setPreviewUrl(URL.createObjectURL(blob));
    } catch (error) {
      showMessage(`Preview failed: ${error.message}`, "error");
      setPreviewDocument(null);
    } finally {
      setPreviewLoading(false);
    }
  }


  async function changeVisibility(doc) {
    const id = getDocumentId(doc);

    if (id == null) {
      showMessage("Could not find the document ID.", "error");
      return;
    }

    if (!canUpload) {
      showMessage(
          "Only authorized document owners can change visibility.",
          "error"
      );
      return;
    }

    const makePublic = !isPublicDocument(doc);

    try {
      await apiRequest(
          `/documents/${id}/visibility?makePublic=${makePublic}`,
          {
            method: "PUT",
          }
      );

      showMessage(
          `Document is now ${makePublic ? "public" : "private"}.`
      );

      await loadDocuments();
    } catch (error) {
      showMessage(
          `Could not change visibility: ${error.message}`,
          "error"
      );
    }
  }


  async function deleteDocument(doc) {
    const id = getDocumentId(doc);

    if (id == null) {
      showMessage("Could not find the document ID.", "error");
      return;
    }

    if (
        role !== "ADMINISTRATOR" &&
        role !== "ADMIN" &&
        role !== "DOCUMENT_OWNER" &&
        role !== "OWNER"
    ) {
      showMessage(
          "Only Administrators and Document Owners can delete documents.",
          "error"
      );
      return;
    }

    const name = getDocumentName(doc);

    if (!window.confirm(`Are you sure you want to permanently delete "${name}"?`)) {
      return;
    }

    try {
      await apiRequest(`/documents/${id}`, {
        method: "DELETE",
      });

      showMessage(`"${name}" was deleted successfully.`);

      if (previewDocument && getDocumentId(previewDocument) === id) {
        closePreview();
      }

      await loadDocuments();
    } catch (error) {
      showMessage(`Could not delete document: ${error.message}`, "error");
    }
  }

  const filteredDocuments = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return documents;

    return documents.filter((doc) => {
      const name = getDocumentName(doc).toLowerCase();
      const owner = getOwnerName(doc).toLowerCase();

      return name.includes(query) || owner.includes(query);
    });
  }, [documents, search]);

  const pageTitles = {
    documents: "Documents",
    "my-docs": "My Docs",
    upload: "Upload Document",
    audit: "Audit Logs",
  };

  const pageDescriptions = {
    documents: "Browse documents available to your account.",
    "my-docs": "View and manage your own documents.",
    upload: "Securely upload a document to TrustVault.",
    audit: "Review recorded activity across the platform.",
  };

  if (!user) {
    return (
        <div className="auth-page">
          <div className="auth-brand-panel">
            <div className="brand-lockup">
              <div className="brand-icon">
                <ShieldCheck size={30} />
              </div>
              <div>
                <h1>TrustVault</h1>
                <p>Secure document sharing</p>
              </div>
            </div>

            <div className="auth-hero">
              <span className="eyebrow">YOUR DOCUMENTS. YOUR CONTROL.</span>
              <h2>Security and trust in every document.</h2>
              <p>
                Keep your files organized, share them with confidence, and
                maintain visibility into document activity.
              </p>

              <div className="auth-feature">
                <LockKeyhole size={20} />
                <div>
                  <strong>Secure access</strong>
                  <span>Token-based authentication</span>
                </div>
              </div>

              <div className="auth-feature">
                <Files size={20} />
                <div>
                  <strong>Organized documents</strong>
                  <span>Your files in one place</span>
                </div>
              </div>

              <div className="auth-feature">
                <ClipboardList size={20} />
                <div>
                  <strong>Audit visibility</strong>
                  <span>Activity records for administrators</span>
                </div>
              </div>
            </div>

            <p className="auth-footer">TrustVault · Secure by design</p>
          </div>

          <div className="auth-form-panel">
            <form className="auth-card" onSubmit={handleSubmit}>
              <div className="mobile-brand">
                <ShieldCheck size={28} />
                <strong>TrustVault</strong>
              </div>

              <span className="eyebrow">
              {isRegister ? "CREATE YOUR ACCOUNT" : "WELCOME BACK"}
            </span>

              <h2>{isRegister ? "Create an account" : "Sign in to TrustVault"}</h2>

              <p className="muted">
                {isRegister
                    ? "Register to start managing your documents."
                    : "Enter your credentials to access your workspace."}
              </p>

              {message && (
                  <div className={`notice ${messageType}`}>
                    {messageType === "error" ? (
                        <AlertCircle size={18} />
                    ) : (
                        <CheckCircle2 size={18} />
                    )}
                    <span>{message}</span>
                  </div>
              )}


              {isRegister && (
                  <>
                    <label className="form-field">
                      <span>Account type</span>
                      <select
                          value={selectedRole}
                          onChange={(event) => setSelectedRole(event.target.value)}
                          required
                      >
                        <option value="VIEWER">Viewer — view available documents</option>
                        <option value="DOCUMENT_OWNER">
                          Document Owner — upload and manage documents
                        </option>
                      </select>
                    </label>

                    <label className="form-field">
                      <span>Email address</span>
                      <input
                          type="email"
                          value={email}
                          onChange={(event) => setEmail(event.target.value)}
                          placeholder="you@example.com"
                          required
                      />
                    </label>
                  </>
              )}


              <label className="form-field">
                <span>Username</span>
                <input
                    value={username}
                    onChange={(event) => setUsername(event.target.value)}
                    placeholder="Enter your username"
                    autoComplete="username"
                    required
                />
              </label>

              <label className="form-field">
                <span>Password</span>
                <input
                    type="password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Enter your password"
                    autoComplete={isRegister ? "new-password" : "current-password"}
                    required
                />
              </label>

              <button className="primary-button auth-submit" disabled={loading}>
                {loading && <LoaderCircle className="spin" size={18} />}
                {loading
                    ? "Please wait..."
                    : isRegister
                        ? "Create account"
                        : "Sign in"}
              </button>

              <p className="auth-switch">
                {isRegister
                    ? "Already have an account?"
                    : "Don't have an account?"}{" "}
                <button
                    type="button"
                    className="text-button"
                    onClick={() => {
                      setIsRegister(!isRegister);
                      setMessage("");
                    }}
                >
                  {isRegister ? "Sign in" : "Create account"}
                </button>
              </p>
            </form>
          </div>
        </div>
    );
  }

  function renderDocumentCard(doc) {
    const id = getDocumentId(doc);
    const name = getDocumentName(doc);
    const Icon = getFileIcon(name);
    const publicDoc = isPublicDocument(doc);

    return (
        <article className="document-card" key={id ?? name}>
          <div className="document-card-top">
            <div className="file-icon">
              <Icon size={23} />
            </div>

            <span>
  {isPublicDocument(doc) ? "Public" : "Private"}
</span>
          </div>

          <h3 title={name}>{name}</h3>

          <p className="document-owner">
            <UserRound size={14} /> {getOwnerName(doc)}
          </p>

          <div className="document-meta">
            <span>Added {formatDate(doc.uploadedAt || doc.createdAt)}</span>
          </div>

          <div className="document-actions">
            <button
                className="icon-button"
                title="Preview document"
                aria-label={`Preview ${name}`}
                onClick={() => previewDocumentFile(doc)}
            >
              <Eye size={17} />
            </button>

            <button
                className="icon-button"
                title="Download document"
                aria-label={`Download ${name}`}
                onClick={() => downloadDocument(doc)}
            >
              <Download size={17} />
            </button>

            {(role === "ADMINISTRATOR" ||
                role === "ADMIN" ||
                ((role === "DOCUMENT_OWNER" || role === "OWNER") &&
                    activePage === "my-docs")) && (
                <button
                    className="icon-button"
                    title={publicDoc ? "Make private" : "Make public"}
                    onClick={() => changeVisibility(doc)}
                >
                  {publicDoc ? <GlobeOff size={17} /> : <Globe size={17} />}
                </button>
            )}

            {(role === "ADMINISTRATOR" ||
                role === "ADMIN" ||
                ((role === "DOCUMENT_OWNER" || role === "OWNER") &&
                    activePage === "my-docs")) && (
                <button
                    className="icon-button delete-button"
                    title="Delete document"
                    aria-label={`Delete ${name}`}
                    onClick={() => deleteDocument(doc)}
                >
                  <Trash2 size={17} />
                </button>
            )}

          </div>
        </article>
    );
  }

  function renderDocumentsPage() {
    const mine = activePage === "my-docs";

    return (
        <>
          <div className="content-toolbar">
            <div className="search-box">
              <Search size={18} />
              <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search documents..."
                  aria-label="Search documents"
              />
            </div>

            <button
                className="secondary-button"
                onClick={loadDocuments}
                disabled={pageLoading}
            >
              <RefreshCw size={16} />
              Refresh
            </button>
          </div>

          <div className="section-heading">
            <div>
              <h2>{mine ? "Your documents" : "All documents"}</h2>
              <p>
                {mine
                    ? "Documents returned by your account's My Docs endpoint."
                    : "Documents available to your account."}
              </p>
            </div>

            <span className="count-pill">
            {filteredDocuments.length}{" "}
              {filteredDocuments.length === 1 ? "document" : "documents"}
          </span>
          </div>

          {pageLoading ? (
              <div className="empty-state">
                <LoaderCircle className="spin" size={30} />
                <p>Loading documents...</p>
              </div>
          ) : filteredDocuments.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">
                  <Files size={30} />
                </div>
                <h3>{search ? "No matching documents" : "No documents yet"}</h3>
                <p>
                  {search
                      ? "Try another search term."
                      : mine
                          ? "Documents you own will appear here."
                          : "Available documents will appear here."}
                </p>

                {!search && canUpload && (
                    <button
                        className="primary-button"
                        onClick={() => navigate("upload")}
                    >
                      <Upload size={17} /> Upload a document
                    </button>
                )}
              </div>
          ) : (
              <div className="documents-grid">
                {filteredDocuments.map(renderDocumentCard)}
              </div>
          )}
        </>
    );
  }

  function renderUploadPage() {
    return (
        <div className="upload-layout">
          <form className="upload-card" onSubmit={uploadDocument}>
            <div className="upload-heading-icon">
              <CloudUpload size={28} />
            </div>

            <h2>Upload a document</h2>

            <p className="muted">
              Select a file to add it to your TrustVault workspace.
            </p>

            <label className="drop-zone">
              <input
                  type="file"
                  onChange={(event) => setFile(event.target.files?.[0] || null)}
              />

              <CloudUpload size={36} />
              <strong>{file ? file.name : "Choose a file to upload"}</strong>

              <span>
              {file
                  ? `${(file.size / 1024 / 1024).toFixed(2)} MB`
                  : "Click here to browse your computer"}
            </span>
            </label>

            <label className="form-field">
              <span>Initial visibility</span>

              <select
                  value={uploadVisibility}
                  onChange={(event) => setUploadVisibility(event.target.value)}
              >
                <option value="PRIVATE">Private</option>
                <option value="PUBLIC">Public</option>
              </select>

              <small>
                Only choose Public if your backend supports public document access.
              </small>
            </label>

            <button
                className="primary-button upload-submit"
                type="submit"
                disabled={busy || !file}
            >
              {busy ? (
                  <LoaderCircle className="spin" size={18} />
              ) : (
                  <Upload size={18} />
              )}

              {busy ? "Uploading..." : "Upload document"}
            </button>
          </form>

          <aside className="upload-info">
            <div className="info-icon">
              <ShieldCheck size={23} />
            </div>

            <h3>Keep your files protected</h3>

            <p>
              TrustVault uses authenticated requests to protect document
              operations. The backend must enforce permissions for every upload,
              preview, download, and visibility change.
            </p>
          </aside>
        </div>
    );
  }

  function renderAuditPage() {
    if (!isAdmin) {
      return (
          <div className="empty-state">
            <LockKeyhole size={32} />
            <h3>Administrator access required</h3>
            <p>Only administrators can view audit logs.</p>
          </div>
      );
    }

    return (
        <>
          <div className="content-toolbar">
            <p className="muted">
              Recorded activity returned by your audit endpoint.
            </p>

            <button
                className="secondary-button"
                onClick={loadAuditLogs}
                disabled={pageLoading}
            >
              <RefreshCw size={16} /> Refresh
            </button>
          </div>

          {pageLoading ? (
              <div className="empty-state">
                <LoaderCircle className="spin" size={28} />
                <p>Loading audit logs...</p>
              </div>
          ) : auditLogs.length === 0 ? (
              <div className="empty-state">
                <ClipboardList size={32} />
                <h3>No audit logs found</h3>
                <p>
                  There are no records to display, or the endpoint returned no entries.
                </p>
              </div>
          ) : (
              <div className="audit-table-wrap">
                <table className="audit-table">
                  <thead>
                  <tr>
                    <th>Activity</th>
                    <th>User</th>
                    <th>Document</th>
                    <th>Date</th>
                  </tr>
                  </thead>

                  <tbody>
                  {auditLogs.map((log, index) => (
                      <tr key={log.id ?? index}>
                        <td>
                          {log.action || log.eventType || log.activity || "Activity"}
                        </td>
                        <td>{log.username || log.user?.username || "—"}</td>
                        <td>
                          {log.documentName || log.document?.fileName || "—"}
                        </td>
                        <td>
                          {formatDate(
                              log.timestamp || log.createdAt || log.eventTime
                          )}
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          )}
        </>
    );
  }

  return (
      <div className="app-shell">
        {sidebarOpen && (
            <button
                className="sidebar-backdrop"
                aria-label="Close navigation"
                onClick={() => setSidebarOpen(false)}
            />
        )}

        <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
          <div className="sidebar-brand">
            <div className="brand-icon">
              <ShieldCheck size={25} />
            </div>

            <div>
              <h2>TrustVault</h2>
              <span>Secure workspace</span>
            </div>

            <button
                className="mobile-close icon-button"
                onClick={() => setSidebarOpen(false)}
                aria-label="Close sidebar"
            >
              <X size={20} />
            </button>
          </div>

          <div className="nav-label">WORKSPACE</div>

          <nav className="sidebar-nav">
            <button
                className={activePage === "documents" ? "nav-item active" : "nav-item"}
                onClick={() => navigate("documents")}
            >
              <LayoutDashboard size={19} />
              <span>Documents</span>
            </button>

            {canAccessMyDocs && (
                <button
                    className={activePage === "my-docs" ? "nav-item active" : "nav-item"}
                    onClick={() => navigate("my-docs")}
                >
                  <FolderKanban size={19} />
                  <span>My Docs</span>
                </button>
            )}

            {canUpload && (
                <button
                    className={activePage === "upload" ? "nav-item active" : "nav-item"}
                    onClick={() => navigate("upload")}
                >
                  <Upload size={19} />
                  <span>Upload Document</span>
                </button>
            )}

            {isAdmin && (
                <>
                  <div className="nav-label nav-label-spaced">ADMINISTRATION</div>

                  <button
                      className={activePage === "audit" ? "nav-item active" : "nav-item"}
                      onClick={() => navigate("audit")}
                  >
                    <ClipboardList size={19} />
                    <span>Audit Logs</span>
                  </button>
                </>
            )}
          </nav>

          <div className="sidebar-bottom">
            <div className="security-note">
              <LockKeyhole size={18} />
              <div>
                <strong>Secure workspace</strong>
                <span>Access-controlled documents</span>
              </div>
            </div>

            <div className="sidebar-user">
              <div className="avatar">
                {(user.username || "U").charAt(0).toUpperCase()}
              </div>

              <div className="user-info">
                <strong>{user.username}</strong>
                <span>{user.role.replaceAll("_", " ")}</span>
              </div>

              <button
                  className="icon-button logout-button"
                  title="Sign out"
                  onClick={logout}
              >
                <LogOut size={18} />
              </button>
            </div>
          </div>
        </aside>

        <main className="main-area">
          <header className="topbar">
            <button
                className="icon-button mobile-menu"
                onClick={() => setSidebarOpen(true)}
                aria-label="Open navigation"
            >
              <Menu size={22} />
            </button>

            <div className="breadcrumb">
              <span>Workspace</span>
              <span className="breadcrumb-separator">/</span>
              <strong>{pageTitles[activePage]}</strong>
            </div>

            <div className="topbar-right">
              <div className="topbar-status">
                <span className="status-dot" />
                Secure session
              </div>

              <div className="topbar-avatar">
                {(user.username || "U").charAt(0).toUpperCase()}
              </div>
            </div>
          </header>

          <div className="page-content">
            <section className="welcome-banner">
              <div>
                <span className="eyebrow">TRUSTVAULT WORKSPACE</span>

                <h1>
                  {activePage === "documents"
                      ? `Welcome back, ${user.username}`
                      : pageTitles[activePage]}
                </h1>

                <p>{pageDescriptions[activePage]}</p>
              </div>

              <div className="banner-art">
                <ShieldCheck size={64} strokeWidth={1.25} />
              </div>
            </section>

            {message && (
                <div className={`notice page-notice ${messageType}`}>
                  {messageType === "error" ? (
                      <AlertCircle size={18} />
                  ) : (
                      <CheckCircle2 size={18} />
                  )}

                  <span>{message}</span>

                  <button
                      className="icon-button"
                      onClick={() => setMessage("")}
                      aria-label="Dismiss message"
                  >
                    <X size={16} />
                  </button>
                </div>
            )}

            {activePage === "documents" && (
                <section className="stats-grid">
                  <div className="stat-card">
                    <div className="stat-icon purple">
                      <Files size={21} />
                    </div>
                    <div>
                      <span>Available documents</span>
                      <strong>{documents.length}</strong>
                    </div>
                  </div>

                  <div className="stat-card">
                    <div className="stat-icon green">
                      <Eye size={21} />
                    </div>
                    <div>
                      <span>Public documents</span>
                      <strong>{documents.filter(isPublicDocument).length}</strong>
                    </div>
                  </div>

                  <div className="stat-card">
                    <div className="stat-icon blue">
                      <LockKeyhole size={21} />
                    </div>
                    <div>
                      <span>Private documents</span>
                      <strong>
                        {documents.filter((doc) => !isPublicDocument(doc)).length}
                      </strong>
                    </div>
                  </div>
                </section>
            )}

            <section className="page-panel">
              {activePage === "documents" && renderDocumentsPage()}

              {activePage === "my-docs" &&
                  canAccessMyDocs &&
                  renderDocumentsPage()}

              {activePage === "upload" &&
                  (canUpload ? (
                      renderUploadPage()
                  ) : (
                      <div className="empty-state">
                        <LockKeyhole size={32} />
                        <h3>Upload access required</h3>
                        <p>Your account does not have permission to upload documents.</p>
                        <button
                            className="primary-button"
                            onClick={() => navigate("documents")}
                        >
                          Back to Documents
                        </button>
                      </div>
                  ))}

              {activePage === "audit" && renderAuditPage()}
            </section>

            <footer className="app-footer">
              <span>© {new Date().getFullYear()} TrustVault</span>
              <span>
              <LockKeyhole size={13} /> Secure document sharing
            </span>
            </footer>
          </div>
        </main>

        {previewDocument && (
            <div className="modal-backdrop" onClick={closePreview}>
              <div
                  className="preview-modal"
                  onClick={(event) => event.stopPropagation()}
              >
                <div className="preview-header">
                  <div>
                    <h2>{getDocumentName(previewDocument)}</h2>
                    <p>Document preview</p>
                  </div>

                  <button
                      className="icon-button"
                      onClick={closePreview}
                      aria-label="Close preview"
                  >
                    <X size={21} />
                  </button>
                </div>

                <div className="preview-body">
                  {previewLoading ? (
                      <div className="empty-state">
                        <LoaderCircle className="spin" size={30} />
                        <p>Preparing preview...</p>
                      </div>
                  ) : previewUrl ? (
                      <iframe
                          title={`Preview ${getDocumentName(previewDocument)}`}
                          src={previewUrl}
                          className="preview-frame"
                      />
                  ) : (
                      <div className="empty-state">
                        <FileText size={32} />
                        <p>Preview is unavailable for this file.</p>

                        <button
                            className="primary-button"
                            onClick={() => downloadDocument(previewDocument)}
                        >
                          <Download size={17} /> Download instead
                        </button>
                      </div>
                  )}
                </div>
              </div>
            </div>
        )}
      </div>
  );
}
