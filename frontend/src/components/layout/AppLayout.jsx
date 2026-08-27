import { colors } from "../../constants/theme";
import Sidebar from "./Sidebar";
import PageHeader from "./PageHeader";
import Alert from "../ui/Alert";

export default function AppLayout({ page, onNavigate, currentUser, error, success, loading, children, showGlobalMessages = true }) {
  return (
    <div
      className="app-shell"
      style={{
        display: "flex",
        minHeight: "100vh",
        fontFamily: "'DM Sans', system-ui, sans-serif",
        background: colors.bg,
        color: colors.text,
      }}
    >
      <Sidebar page={page} onNavigate={onNavigate} currentUser={currentUser} />
      <main className="app-main" style={{ flex: 1, padding: 24, overflow: "auto", maxWidth: 980 }}>
        {loading && <div className="app-loading-bar" aria-hidden="true" />}
        <PageHeader page={page} />
        {showGlobalMessages && error && <Alert>{error}</Alert>}
        {showGlobalMessages && success && <Alert type="success">{success}</Alert>}
        {children}
        {loading && <div className="app-loading-status" role="status"><span className="app-loading-dot" /> Syncing with Jira and Zephyr…</div>}
      </main>
    </div>
  );
}
