import { useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import api from "../api";
import { useApp } from "../context/AppContext";

function Markdown({ text }) {
  return <div style={{ lineHeight: 1.55 }}>{String(text || "").split(/\r?\n/).map((line, i) => {
    const content = line.replace(/\*\*([^*]+)\*\*/g, "$1");
    if (line.startsWith("### ")) return <h4 key={i} style={{ margin: "14px 0 5px" }}>{content.slice(4)}</h4>;
    if (line.startsWith("## ")) return <h3 key={i} style={{ margin: "16px 0 6px" }}>{content.slice(3)}</h3>;
    if (/^[-*] /.test(line)) return <div key={i} style={{ paddingLeft: 16 }}>• {content.slice(2)}</div>;
    return <div key={i} style={{ minHeight: line ? undefined : 8 }}>{content}</div>;
  })}</div>;
}

export default function PRReviewPage() {
  const { busyJob, beginBusyJob, endBusyJob } = useApp();
  const [url, setUrl] = useState("");
  const [focus, setFocus] = useState("");
  const [result, setResult] = useState(null);
  const [editedReview, setEditedReview] = useState("");
  const [preview, setPreview] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const otherAiRunning = Boolean(busyJob?.exclusiveAi && busyJob.page !== "pr-review");
  const runReview = async () => {
    const jobId = "pr-review";
    setLoading(true); setError(""); setResult(null);
    beginBusyJob({ id: jobId, title: "Reviewing pull request with AI", page: "pr-review", exclusiveAi: true });
    try {
      const data = await api.prReview({ url: url.trim(), focus: focus.trim() || undefined });
      setResult(data); setEditedReview(data.review || "");
    } catch (e) { setError(e.message); }
    finally {
      setLoading(false);
      endBusyJob(jobId);
    }
  };

  return <>
    <Alert type="info">Upcoming feature preview — PR fetching and analysis are wired, but this integration is not ready for production use yet.</Alert>
    <Card title="Review a pull request" step={1}>
      <p style={{ marginTop: 0, color: colors.muted, fontSize: 12, lineHeight: 1.6 }}>
        Paste a GitHub or Bitbucket pull request URL. TestCraft fetches its metadata and diff, then checks it for correctness, security, risks, and missing tests.
      </p>
      <label style={{ display: "block", fontSize: 12, fontWeight: 600, marginBottom: 10 }}>Pull request URL
        <input aria-label="Pull request URL" style={{ ...inputStyle, width: "100%", marginTop: 5 }} value={url} onChange={(e) => setUrl(e.target.value)} onKeyDown={(e) => e.key === "Enter" && runReview()} placeholder="https://github.com/org/repo/pull/123" />
      </label>
      <label style={{ display: "block", fontSize: 12, marginBottom: 12 }}>Optional review focus
        <input aria-label="Optional review focus" style={{ ...inputStyle, width: "100%", marginTop: 5 }} value={focus} onChange={(e) => setFocus(e.target.value)} placeholder="e.g. authentication, database migrations, API compatibility" />
      </label>
      <Button primary disabled={loading || otherAiRunning || !url.trim()} onClick={runReview}>{loading ? "Fetching and analyzing…" : result ? "Fetch & review again" : "Fetch & review PR"}</Button>
      {result && <Button disabled={loading} onClick={() => { setResult(null); setEditedReview(""); setError(""); }} style={{ marginLeft: 8 }}>Clear review</Button>}
      {error && <div style={{ marginTop: 12 }}><Alert>{error}</Alert></div>}
    </Card>
    {result && <>
      <Card title="Pull request details" step={2}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))", gap: 10, fontSize: 12 }}>
          <div><strong>Provider</strong><br />{result.provider}</div><div><strong>Repository</strong><br />{result.repository}</div>
          <div><strong>Author</strong><br />{result.author}</div><div><strong>Status</strong><br />{result.status}</div>
          <div><strong>Changed files</strong><br />{result.changedFiles}</div><div><strong>Changes</strong><br />+{result.additions} / −{result.deletions}</div>
        </div>
        <div style={{ marginTop: 12, padding: 12, background: colors.surface, borderRadius: 8, fontSize: 13 }}><strong>{result.title}</strong><br /><a href={result.url} target="_blank" rel="noreferrer">Open pull request ↗</a></div>
      </Card>
      <Card title="AI review" step={3}>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 10 }}>
          <Button onClick={() => setPreview((v) => !v)}>{preview ? "Edit review" : "Preview Markdown"}</Button>
          <Button onClick={() => navigator.clipboard.writeText(editedReview)}>Copy review</Button>
        </div>
        {preview ? <div style={{ background: colors.surface, borderRadius: 8, padding: 14, fontSize: 13 }}><Markdown text={editedReview} /></div> : <textarea aria-label="Editable PR review" style={{ ...inputStyle, width: "100%", minHeight: 360, fontFamily: "inherit", lineHeight: 1.5 }} value={editedReview} onChange={(e) => setEditedReview(e.target.value)} />}
        {result.mockMode && <p style={{ color: colors.muted, fontSize: 11, marginBottom: 0 }}>Mock AI mode is active. Configure a live provider to receive code-specific findings.</p>}
      </Card>
    </>}
  </>;
}
