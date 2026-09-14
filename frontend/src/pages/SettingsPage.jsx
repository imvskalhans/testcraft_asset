import { useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import InfoIcon from "../components/settings/InfoIcon";
import SetupGuideModal from "../components/settings/SetupGuideModal";
import api from "../api";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { useApp } from "../context/AppContext";

export default function SettingsPage() {
  const {
    loading, currentUser, owner, configStatus, connStatus,
    setupGuide, showSetupGuide, setShowSetupGuide, testConnections,
  } = useApp();
  const [projectKey, setProjectKey] = useState(configStatus?.zephyr?.defaultProjectKey || "");
  const [project, setProject] = useState(null);
  const [projectError, setProjectError] = useState("");
  const [projectLoading, setProjectLoading] = useState(false);

  const findProject = async () => {
    if (!projectKey.trim()) return;
    setProjectLoading(true);
    setProjectError("");
    setProject(null);
    try {
      const found = await api.config.jiraProject(projectKey);
      const [folderResult, statusResult, priorityResult] = await Promise.allSettled([
        api.zephyr.folders(found.id),
        api.zephyr.statuses(found.id),
        api.zephyr.priorities(found.id),
      ]);
      setProject({
        ...found,
        folders: folderResult.status === "fulfilled" ? folderResult.value.folders ?? {} : {},
        folderWarning: folderResult.status === "fulfilled" ? folderResult.value.warning || folderResult.value.error : folderResult.reason?.message,
        statuses: statusResult.status === "fulfilled" ? statusResult.value.statuses ?? {} : {},
        priorities: priorityResult.status === "fulfilled" ? priorityResult.value.priorities ?? {} : {},
      });
    } catch (e) {
      setProjectError(e.message);
    } finally {
      setProjectLoading(false);
    }
  };

  const copyProjectConfig = () => navigator.clipboard.writeText(
    `zephyr.default-project-key=${project.key}\nzephyr.default-project-id=${project.id}`
      + (Object.keys(project.folders ?? {}).length
        ? `\nzephyr.known-folders=${Object.entries(project.folders).map(([name, id]) => `${name}:${id}`).join(",")}`
        : "")
  );

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
      <Card title="Find Zephyr project ID">
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0, lineHeight: 1.6 }}>
          Jira shows project keys (for example, <strong>KAN</strong>), while Zephyr setup also needs the numeric project ID. Enter the Jira key to look it up automatically.
        </p>
        <div style={{ display: "flex", gap: 8 }}>
          <input aria-label="Jira project key" style={{ ...inputStyle, flex: 1 }} value={projectKey} onChange={(e) => setProjectKey(e.target.value.toUpperCase())} onKeyDown={(e) => e.key === "Enter" && findProject()} placeholder="Enter a Jira project key, e.g. KAN" />
          <Button primary disabled={projectLoading || !projectKey.trim()} onClick={findProject}>{projectLoading ? "Looking up…" : "Find project"}</Button>
        </div>
        {projectError && <div style={{ marginTop: 10 }}><Alert>{projectError}</Alert></div>}
        {project && <div style={{ marginTop: 12, background: colors.surface, borderRadius: 8, padding: 12, fontSize: 12, lineHeight: 1.8 }}>
          <div><strong>{project.name || project.key}</strong> ({project.key})</div>
          <div>Project ID: <code>{project.id}</code></div>
          {project.folderWarning && <p style={{ color: colors.muted, margin: "6px 0" }}>{project.folderWarning}</p>}
          <div style={{ marginTop: 6 }}><strong>Folders and subfolders:</strong> {Object.keys(project.folders ?? {}).length || "none found"}</div>
          {Object.keys(project.folders ?? {}).length > 0 && <div style={{ maxHeight: 140, overflow: "auto", marginTop: 4, paddingLeft: 12 }}>
            {Object.entries(project.folders).map(([name, id]) => <div key={`${name}-${id}`}><code>{name}</code> — {id}</div>)}
          </div>}
          {Object.keys(project.statuses ?? {}).length > 0 && <div style={{ marginTop: 6 }}>Statuses: {Object.keys(project.statuses).join(", ")}</div>}
          {Object.keys(project.priorities ?? {}).length > 0 && <div>Priorities: {Object.keys(project.priorities).join(", ")}</div>}
          <Button style={{ marginTop: 8 }} onClick={copyProjectConfig}>Copy configuration lines</Button>
          <p style={{ color: colors.muted, margin: "8px 0 0" }}>The copied lines include folder/subfolder IDs when available. Paste them into <code>application-local.properties</code>, then restart the backend.</p>
        </div>}
      </Card>
      <Card title="Current user">
        <p style={{ fontSize: 12, lineHeight: 1.7 }}>
          <strong>{currentUser?.displayName || "Unknown"}</strong><br />
          {currentUser?.emailAddress}<br />
          <span className="owner-id-label">
            Owner ID: {owner || "—"}
            <span className="owner-id-tooltip">Jira account identifier used to assign Zephyr ownership.</span>
          </span>
        </p>
      </Card>
      <Card title="Setup checklist">
        <div style={{ display: "grid", gap: 8, fontSize: 12 }}>
          <div><strong>Jira:</strong> {configStatus?.jira?.connected ? "✓ Connected" : "○ Connect Jira and verify credentials"}</div>
          <div><strong>Zephyr:</strong> {configStatus?.zephyr?.configured ? "✓ Configured" : "○ Configure the provider and API token"}</div>
          <div><strong>Project:</strong> {configStatus?.zephyr?.defaultProjectKey
            ? `✓ ${configStatus.zephyr.defaultProjectKey} (${configStatus.zephyr.defaultProjectId || "project ID missing"})`
            : "○ Set the default project key and numeric project ID"}</div>
          <div><strong>Folders:</strong> Select a project on Publish & Link and confirm folders load before publishing.</div>
          <div><strong>AI:</strong> {configStatus?.ai?.mockMode ? "Using mock mode — configure an AI provider for live generation" : `Ready for test generation (${configStatus?.ai?.provider || "provider"}${configStatus?.ai?.model ? ` · ${configStatus.ai.model}` : ""}${configStatus?.ai?.imageInputSupported ? "; image input supported" : "; image input unavailable"}). Use Test connections to ping the live model.`}</div>
        </div>
        <p style={{ color: colors.muted, fontSize: 11, lineHeight: 1.5, margin: "10px 0 0" }}>
          This page intentionally shows connection state, URLs, project IDs, and account IDs only. Secrets are used by the backend and are never rendered in the UI.
        </p>
      </Card>
      <Card title="Connection status">
        {configStatus && (
          <div style={{ fontSize: 12, lineHeight: 1.7, marginBottom: 12 }}>
            <p><strong>Jira:</strong> {configStatus.jira?.connected ? "connected" : "not connected"} · {configStatus.jira?.baseUrl} · {configStatus.jira?.username}</p>
            <p><strong>Zephyr:</strong> {configStatus.zephyr?.provider} · project {configStatus.zephyr?.defaultProjectKey || "—"} · Scale JWT {configStatus.zephyr?.scaleCloudTokenConfigured ? "configured" : "missing"}</p>
            <p><strong>AI:</strong> {configStatus.ai?.provider}{configStatus.ai?.mockMode ? " (mock mode)" : ""}{configStatus.ai?.model ? ` · ${configStatus.ai.model}` : ""}</p>
          </div>
        )}
        <Button primary disabled={loading} onClick={testConnections}>Test connections</Button>
        {connStatus && (
          <div style={{ marginTop: 12, fontSize: 12, lineHeight: 1.7 }}>
            <p><strong>Jira:</strong> {typeof connStatus.jira === "string" ? connStatus.jira : (connStatus.jira ? "OK" : "failed")}</p>
            <p><strong>Zephyr:</strong> {connStatus.user?.error
              ? connStatus.user.error
              : (connStatus.user?.displayName
                ? `${connStatus.user.displayName} — ${connStatus.user.email || ""}`
                : "failed")}</p>
            <p><strong>AI:</strong> {connStatus.ai?.success ? "OK" : "failed"}{connStatus.ai?.model ? ` · ${connStatus.ai.model}` : ""}{connStatus.ai?.message ? ` — ${connStatus.ai.message}` : ""}</p>
          </div>
        )}
      </Card>
    </>
  );
}
