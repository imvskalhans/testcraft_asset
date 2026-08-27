import { useEffect, useMemo, useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { useApp } from "../context/AppContext";

function InlineMarkdown({ text }) {
  const renderInline = (value) => value.split(/(\*\*[^*]+\*\*|`[^`]+`)/g).map((part, index) => {
    if (part.startsWith("**") && part.endsWith("**")) return <strong key={index}>{part.slice(2, -2)}</strong>;
    if (part.startsWith("`") && part.endsWith("`")) return <code key={index}>{part.slice(1, -1)}</code>;
    return <span key={index}>{part}</span>;
  });
  return <div style={{ lineHeight: 1.55 }}>{String(text || "").split(/\r?\n/).map((line, index) => {
    if (line.startsWith("### ")) return <h4 key={index} style={{ margin: "14px 0 5px" }}>{renderInline(line.slice(4))}</h4>;
    if (line.startsWith("## ")) return <h3 key={index} style={{ margin: "14px 0 5px" }}>{renderInline(line.slice(3))}</h3>;
    if (line.startsWith("# ")) return <h2 key={index} style={{ margin: "14px 0 5px" }}>{renderInline(line.slice(2))}</h2>;
    if (/^[-*] /.test(line)) return <div key={index} style={{ paddingLeft: 16 }}>• {renderInline(line.slice(2))}</div>;
    return <div key={index} style={{ minHeight: line ? undefined : 8 }}>{renderInline(line)}</div>;
  })}</div>;
}

function markdownToEmailText(text) {
  return String(text || "")
    .replace(/^#{1,6}\s+/gm, "")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/__([^_]+)__/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/^\s*[-*]\s+/gm, "• ")
    .replace(/^\s*\d+\.\s+/gm, "")
    .replace(/\r?\n/g, "\r\n");
}

export default function AiPage() {
  const {
    issueKey, setIssueKey, story, storyText, loading,
    aiActions, review, releaseAi, fetchForAi, releaseAiRun, postAiComment,
  } = useApp();
  const generatedResult = review?.review || releaseAi?.analysis || "";
  const [resultText, setResultText] = useState("");
  const [showPreview, setShowPreview] = useState(true);
  const [emailOpen, setEmailOpen] = useState(false);
  const [jiraCommentOpen, setJiraCommentOpen] = useState(false);
  const [recipient, setRecipient] = useState("");

  useEffect(() => setResultText(generatedResult), [generatedResult]);
  const subject = useMemo(() => `TestCraft AI result for ${issueKey}`, [issueKey]);
  const copyResult = async () => navigator.clipboard.writeText(resultText);
  const openEmail = () => {
    const body = markdownToEmailText(resultText);
    window.location.href = `mailto:${encodeURIComponent(recipient.trim())}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    setEmailOpen(false);
  };
  const confirmJiraComment = () => {
    postAiComment(resultText);
    setJiraCommentOpen(false);
  };

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
        {generatedResult && <div style={{ marginTop: 14 }}>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
            <Button onClick={() => setShowPreview((value) => !value)}>{showPreview ? "Edit result" : "Preview Markdown"}</Button>
            <Button onClick={copyResult}>Copy content</Button>
            <Button onClick={() => setEmailOpen(true)}>Email result</Button>
            <Button primary onClick={() => setJiraCommentOpen(true)}>Post to Jira</Button>
          </div>
          {showPreview ? <div style={{ background: colors.surface, borderRadius: 8, padding: 14, fontSize: 13 }}><InlineMarkdown text={resultText} /></div> : <textarea aria-label="Editable AI result" style={{ ...inputStyle, width: "100%", minHeight: 320, boxSizing: "border-box", fontFamily: "inherit", lineHeight: 1.5 }} value={resultText} onChange={(e) => setResultText(e.target.value)} />}
        </div>}
      </Card>
      {emailOpen && <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.35)", display: "grid", placeItems: "center", zIndex: 10 }}>
        <div style={{ background: "#fff", borderRadius: 10, padding: 20, width: "min(520px, calc(100% - 32px))", boxSizing: "border-box" }}>
          <h3 style={{ marginTop: 0 }}>Email AI result</h3>
          <label style={{ display: "block", fontSize: 12, marginBottom: 10 }}>Recipient<input autoFocus type="email" style={{ ...inputStyle, marginTop: 4, width: "100%", boxSizing: "border-box" }} value={recipient} onChange={(e) => setRecipient(e.target.value)} placeholder="qa-team@example.com" /></label>
          <p style={{ fontSize: 12, color: colors.muted }}>Your default email application will open with the edited result.</p>
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}><Button onClick={() => setEmailOpen(false)}>Cancel</Button><Button primary disabled={!recipient.trim()} onClick={openEmail}>Open email</Button></div>
        </div>
      </div>}
      {jiraCommentOpen && <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.35)", display: "grid", placeItems: "center", zIndex: 10 }}>
        <div style={{ background: "#fff", borderRadius: 10, padding: 20, width: "min(520px, calc(100% - 32px))", boxSizing: "border-box" }}>
          <h3 style={{ marginTop: 0 }}>Post AI result to Jira</h3>
          <p style={{ fontSize: 13, color: colors.muted, lineHeight: 1.5 }}>
            This will add the current AI result as a comment on <strong>{issueKey}</strong>.
          </p>
          <div style={{ background: colors.surface, borderRadius: 8, padding: 10, maxHeight: 180, overflow: "auto", whiteSpace: "pre-wrap", fontSize: 12 }}>
            {resultText}
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 14 }}>
            <Button onClick={() => setJiraCommentOpen(false)}>Cancel</Button>
            <Button primary disabled={loading || !resultText.trim() || !issueKey.trim()} onClick={confirmJiraComment}>Post comment</Button>
          </div>
        </div>
      </div>}
    </>
  );
}
