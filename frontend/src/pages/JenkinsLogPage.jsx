import { useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import api from "../api";

export default function JenkinsLogPage() {
  const [url, setUrl] = useState("");
  const [focus, setFocus] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const run = async () => {
    setLoading(true); setError(""); setResult(null);
    try { setResult(await api.jenkinsLog({ url: url.trim(), focus: focus.trim() || undefined })); }
    catch (e) { setError(e.message); }
    finally { setLoading(false); }
  };
  return <>
    <Alert type="info">Upcoming feature preview — the workflow is wired for Jenkins console analysis, but is not ready for production use yet.</Alert>
    <Card title="Analyze Jenkins console output" step={1}>
      <p style={{ marginTop: 0, color: colors.muted, fontSize: 12, lineHeight: 1.6 }}>Enter a Jenkins job or build URL. Long logs are split into chunks, analyzed separately, and synthesized into one professional report.</p>
      <label style={{ display: "block", fontSize: 12, fontWeight: 600, marginBottom: 10 }}>Jenkins job or build URL
        <input aria-label="Jenkins job or build URL" style={{ ...inputStyle, width: "100%", marginTop: 5 }} value={url} onChange={(e) => setUrl(e.target.value)} onKeyDown={(e) => e.key === "Enter" && run()} placeholder="https://jenkins.example.com/job/my-job/42/" />
      </label>
      <label style={{ display: "block", fontSize: 12, marginBottom: 12 }}>Optional focus
        <input aria-label="Jenkins review focus" style={{ ...inputStyle, width: "100%", marginTop: 5 }} value={focus} onChange={(e) => setFocus(e.target.value)} placeholder="e.g. test failures, deployment, flaky tests" />
      </label>
      <Button primary disabled={loading || !url.trim()} onClick={run}>{loading ? "Fetching and analyzing…" : "Fetch & analyze log"}</Button>
      {error && <div style={{ marginTop: 12 }}><Alert>{error}</Alert></div>}
    </Card>
    {result && <Card title="Jenkins report" step={2}>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: 10, fontSize: 12, marginBottom: 14 }}>
        <div><strong>Job</strong><br />{result.jobName}</div><div><strong>Build</strong><br />#{result.buildNumber}</div><div><strong>Result</strong><br />{result.buildResult}</div><div><strong>Log</strong><br />{result.totalLines} lines · {result.chunksAnalyzed} chunks</div>
      </div>
      <div style={{ background: colors.surface, borderRadius: 8, padding: 14, fontSize: 13, lineHeight: 1.55, whiteSpace: "pre-wrap" }}>{result.summary}</div>
      {result.mockMode && <p style={{ color: colors.muted, fontSize: 11 }}>Mock AI mode is active. Configure a live AI provider for detailed log diagnosis.</p>}
    </Card>}
  </>;
}
