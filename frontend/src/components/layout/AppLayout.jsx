import { colors } from "../../constants/theme";
import Sidebar from "./Sidebar";
import PageHeader from "./PageHeader";
import Alert from "../ui/Alert";

export default function AppLayout({ page, onNavigate, currentUser, error, success, loading, children, showGlobalMessages = true }) {
  return (
    <div
      style={{
        display: "flex",
        minHeight: "100vh",
        fontFamily: "'DM Sans', system-ui, sans-serif",
        background: colors.bg,
        color: colors.text,
      }}
    >
      <Sidebar page={page} onNavigate={onNavigate} currentUser={currentUser} />
      <main style={{ flex: 1, padding: 24, overflow: "auto", maxWidth: 980 }}>
        <PageHeader page={page} />
        {showGlobalMessages && error && <Alert>{error}</Alert>}
        {showGlobalMessages && success && <Alert type="success">{success}</Alert>}
        {children}
        {loading && <p style={{ fontSize: 12, color: colors.muted }}>Working…</p>}
      </main>
    </div>
  );
}
