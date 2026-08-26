import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import InfoIcon from "../components/settings/InfoIcon";
import SetupGuideModal from "../components/settings/SetupGuideModal";
import { colors } from "../constants/theme";
import { useApp } from "../context/AppContext";

export default function SettingsPage() {
  const {
    loading, currentUser, owner, configStatus, connStatus,
    setupGuide, showSetupGuide, setShowSetupGuide, testConnections,
  } = useApp();

  return (
    <>
      <SetupGuideModal guide={showSetupGuide ? setupGuide : null} onClose={() => setShowSetupGuide(false)} />
      <Card
        title="Project setup"
        actions={<InfoIcon onClick={() => setShowSetupGuide(true)} title="View setup instructions" />}
      >
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0, lineHeight: 1.6 }}>
          Teams configure <strong>one file</strong> — no code changes. Click the <strong>i</strong> icon for step-by-step instructions.
        </p>
        <div style={{ fontSize: 12, background: colors.surface, padding: 12, borderRadius: 8, lineHeight: 1.7 }}>
          <div><strong>Backend:</strong> <code>testcraft/backend/src/main/resources/application-local.properties</code></div>
          <div style={{ marginTop: 6 }}><strong>Frontend (optional):</strong> <code>testcraft/frontend/.env</code></div>
          <div style={{ marginTop: 6 }}><strong>Docs:</strong> <code>testcraft/config/SETUP.md</code></div>
        </div>
      </Card>
      <Card title="Current user">
        <p style={{ fontSize: 12, lineHeight: 1.7 }}>
          <strong>{currentUser?.displayName || "Unknown"}</strong><br />
          {currentUser?.emailAddress}<br />
          Owner id: {owner || "—"}
        </p>
      </Card>
      <Card title="Connection status">
        {configStatus && (
          <div style={{ fontSize: 12, lineHeight: 1.7, marginBottom: 12 }}>
            <p><strong>Jira:</strong> {configStatus.jira?.connected ? "connected" : "not connected"} · {configStatus.jira?.baseUrl} · {configStatus.jira?.username}</p>
            <p><strong>Zephyr:</strong> {configStatus.zephyr?.provider} · project {configStatus.zephyr?.defaultProjectKey || "—"} · Scale JWT {configStatus.zephyr?.scaleCloudTokenConfigured ? "configured" : "missing"}</p>
            <p><strong>AI:</strong> {configStatus.ai?.provider}{configStatus.ai?.mockMode ? " (mock mode)" : ""}</p>
          </div>
        )}
        <Button primary disabled={loading} onClick={testConnections}>Test connections</Button>
        {connStatus && (
          <div style={{ marginTop: 12, fontSize: 12 }}>
            <p><strong>Jira:</strong> {typeof connStatus.jira === "string" ? connStatus.jira : "OK"}</p>
            <p><strong>User:</strong> {connStatus.user?.displayName} — {connStatus.user?.email}</p>
          </div>
        )}
      </Card>
    </>
  );
}
