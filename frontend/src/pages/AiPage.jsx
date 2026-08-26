import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { useApp } from "../context/AppContext";

export default function AiPage() {
  const {
    issueKey, setIssueKey, story, storyText, loading,
    aiActions, review, releaseAi, fetchForAi, releaseAiRun,
  } = useApp();

  return (
    <>
      <Card title="Fetch Jira details" step={1}>
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <input style={{ ...inputStyle, flex: 1 }} value={issueKey} onChange={(e) => setIssueKey(e.target.value.toUpperCase())} placeholder="KAN-1" />
          <Button primary disabled={loading} onClick={fetchForAi}>Fetch Jira details</Button>
        </div>
        {story && (
          <div style={{ background: colors.surface, borderRadius: 8, padding: 12, fontSize: 12, whiteSpace: "pre-wrap", maxHeight: 180, overflow: "auto" }}>
            {storyText}
          </div>
        )}
      </Card>
      <Card title="Choose an AI action" step={2}>
        {!story && <Alert type="info">Fetch Jira details first so the AI has something to review.</Alert>}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: 10 }}>
          {aiActions.map((a) => (
            <button
              key={a.id}
              type="button"
              onClick={() => releaseAiRun(a.id)}
              disabled={loading || !story}
              style={{
                textAlign: "left",
                padding: 14,
                borderRadius: 10,
                border: `1px solid ${colors.border}`,
                background: story ? "#fff" : colors.surface,
                cursor: loading || !story ? "not-allowed" : "pointer",
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 700, color: colors.brand, marginBottom: 6 }}>{a.label}</div>
              <div style={{ fontSize: 12, color: colors.muted, lineHeight: 1.45 }}>{a.description || "Run this AI action on the fetched issue."}</div>
            </button>
          ))}
        </div>
        {(review?.review || releaseAi?.analysis) && (
          <div style={{ marginTop: 14, background: colors.surface, borderRadius: 8, padding: 14, fontSize: 13, whiteSpace: "pre-wrap", lineHeight: 1.5 }}>
            {review?.review || releaseAi?.analysis}
          </div>
        )}
      </Card>
    </>
  );
}
