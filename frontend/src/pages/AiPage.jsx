import { useEffect, useMemo, useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import AiActionPicker from "../components/ai/AiActionPicker";
import AiActionPanel from "../components/ai/AiActionPanel";
import AiContextBar from "../components/ai/AiContextBar";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { useApp } from "../context/AppContext";
import api from "../api";
import { formatReleaseText } from "../utils/releaseText";
import InfoIcon from "../components/settings/InfoIcon";
import SkippedAttachmentsNotice from "../components/ai/SkippedAttachmentsNotice";

function isSupportedAttachment(attachment) {
  const mime = attachment.mimeType || "";
  return mime.startsWith("image/") || mime.startsWith("text/") || ["application/json", "text/csv"].includes(mime) || /\.(txt|md|csv|json)$/i.test(attachment.name || "");
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
    issueKey, setIssueKey, story, storyText, loading, fetchForAi, postAiComment,
    crKey, setCrKey, release, fetchCrForAi, configStatus,
    setAiLoading,
  } = useApp();

  const [actions, setActions] = useState([]);
  const [actionsError, setActionsError] = useState("");
  const [activeActionId, setActiveActionId] = useState("");
  const [running, setRunning] = useState(false);
  const [runError, setRunError] = useState("");
  const [result, setResult] = useState(null);
  const [emailOpen, setEmailOpen] = useState(false);
  const [jiraCommentOpen, setJiraCommentOpen] = useState(false);
  const [recipient, setRecipient] = useState("");
  const [actionInfoOpen, setActionInfoOpen] = useState(false);

  useEffect(() => {
    api.ai.actions()
      .then((data) => setActions(data.actions ?? []))
      .catch((e) => setActionsError(e.message || "Unable to load AI actions"));
  }, []);

  const activeAction = useMemo(
    () => actions.find((action) => action.id === activeActionId) ?? null,
    [actions, activeActionId],
  );

  const releaseText = useMemo(() => formatReleaseText(release), [release]);
  const jiraCommentTarget = activeActionId === "release-notes" && crKey ? crKey : issueKey;
  const canPostToJira = activeActionId === "release-notes" ? Boolean(release && crKey) : Boolean(story);
  const usesIssueContext = Boolean(activeAction?.supportsJiraContext || activeAction?.supportsCrContext);

  const resultText = result?.result || "";
  const subject = useMemo(() => {
    if (activeActionId === "release-notes" && crKey) {
      return `Release notes draft for ${crKey}`;
    }
    return `TestCraft AI result for ${issueKey}`;
  }, [activeActionId, crKey, issueKey]);

  const openAction = (actionId) => {
    setActiveActionId(actionId);
    setResult(null);
    setRunError("");
    setActionInfoOpen(false);
  };

  const backToPicker = () => {
    setActiveActionId("");
    setResult(null);
    setRunError("");
    setActionInfoOpen(false);
  };

  const runAction = async (payload) => {
    setRunning(true);
    setAiLoading(true);
    setRunError("");
    setResult(null);
    try {
      const jiraAttachments = (usesIssueContext ? (story?.attachments ?? []).filter((attachment) => attachment.data
        && isSupportedAttachment(attachment)
        && (configStatus?.ai?.imageInputSupported || !attachment.mimeType?.startsWith("image/"))) : []);
      const data = await api.ai.run({
        ...payload,
        attachments: [...jiraAttachments, ...(payload.attachments ?? [])].slice(0, 5),
      });
      if (!data.success) throw new Error(data.error || "AI action failed");
      setResult(data);
    } catch (e) {
      setRunError(e.message || "AI action failed");
    } finally {
      setRunning(false);
      setAiLoading(false);
    }
  };

  const openEmail = () => {
    const body = markdownToEmailText(resultText);
    window.location.href = `mailto:${encodeURIComponent(recipient.trim())}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    setEmailOpen(false);
  };

  if (!activeAction) {
    return (
      <Card title="AI Workspace">
        <p style={{ marginTop: 0, marginBottom: 16, fontSize: 13, color: colors.muted, lineHeight: 1.6 }}>
          Choose a focused AI workspace. Add Jira or change-request context when useful, plus supported image or text attachments.
        </p>
        {actionsError && <div style={{ marginBottom: 12 }}><Alert>{actionsError}</Alert></div>}
        <AiActionPicker actions={actions} onSelect={openAction} />
      </Card>
    );
  }

  return (
    <>
      <Card
        title={activeAction.label}
        actions={(
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <InfoIcon onClick={() => setActionInfoOpen((value) => !value)} title={`About ${activeAction.label}`} />
            <Button primary onClick={backToPicker}>← All AI Workspaces</Button>
          </div>
        )}
      >
        <p style={{ marginTop: 0, marginBottom: 4, fontSize: 12, color: colors.muted, lineHeight: 1.6 }}>
          {activeAction.description}
        </p>
        {actionInfoOpen && <Alert type="info"><strong>How this workspace helps:</strong> {activeAction.description} Use the inputs below to give the AI task-specific details; loaded Jira/change-request context and supported attachments are included automatically.</Alert>}

        <AiContextBar
          issueKey={issueKey}
          setIssueKey={setIssueKey}
          story={story}
          storyText={storyText}
          crKey={crKey}
          setCrKey={setCrKey}
          release={release}
          releaseText={releaseText}
          loading={loading}
          onFetchStory={fetchForAi}
          onFetchCr={fetchCrForAi}
          needsStory={Boolean(activeAction.supportsJiraContext)}
          needsCr={Boolean(activeAction.supportsCrContext)}
        />

        <AiActionPanel
          action={activeAction}
          issueKey={issueKey}
          storyText={storyText}
          hasStory={Boolean(story)}
          crKey={crKey}
          releaseText={releaseText}
          hasRelease={Boolean(release)}
          running={running}
          result={result}
          supportsImageInput={Boolean(configStatus?.ai?.imageInputSupported)}
          onRun={runAction}
        />

        {runError && <div style={{ marginTop: 12 }}><Alert>{runError}</Alert></div>}

        {resultText && usesIssueContext && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 14 }}>
            <Button onClick={() => setEmailOpen(true)}>Email result</Button>
            {canPostToJira && (
              <Button primary onClick={() => setJiraCommentOpen(true)}>
                {activeActionId === "release-notes" ? "Post to CR in Jira" : "Post to Jira"}
              </Button>
            )}
          </div>
        )}
        {result && usesIssueContext && <div style={{ marginTop: 14 }}>
          <SkippedAttachmentsNotice
            attachments={story?.attachments}
            supportsImageInput={Boolean(configStatus?.ai?.imageInputSupported)}
          />
        </div>}
      </Card>

      {emailOpen && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.35)", display: "grid", placeItems: "center", zIndex: 10 }}>
          <div style={{ background: "#fff", borderRadius: 10, padding: 20, width: "min(520px, calc(100% - 32px))", boxSizing: "border-box" }}>
            <h3 style={{ marginTop: 0 }}>Email AI result</h3>
            <label style={{ display: "block", fontSize: 12, marginBottom: 10 }}>
              Recipient
              <input autoFocus type="email" style={{ ...inputStyle, marginTop: 4, width: "100%", boxSizing: "border-box" }} value={recipient} onChange={(e) => setRecipient(e.target.value)} placeholder="qa-team@example.com" />
            </label>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
              <Button onClick={() => setEmailOpen(false)}>Cancel</Button>
              <Button primary disabled={!recipient.trim()} onClick={openEmail}>Open email</Button>
            </div>
          </div>
        </div>
      )}

      {jiraCommentOpen && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.35)", display: "grid", placeItems: "center", zIndex: 10 }}>
          <div style={{ background: "#fff", borderRadius: 10, padding: 20, width: "min(520px, calc(100% - 32px))", boxSizing: "border-box" }}>
            <h3 style={{ marginTop: 0 }}>Post AI result to Jira</h3>
            <p style={{ fontSize: 13, color: colors.muted, lineHeight: 1.5 }}>
              This will add the current AI result as a comment on <strong>{jiraCommentTarget}</strong>.
            </p>
            <div style={{ background: colors.surface, borderRadius: 8, padding: 10, maxHeight: 180, overflow: "auto", whiteSpace: "pre-wrap", fontSize: 12 }}>
              {resultText}
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 14 }}>
              <Button onClick={() => setJiraCommentOpen(false)}>Cancel</Button>
              <Button primary disabled={loading || !resultText.trim() || !jiraCommentTarget.trim()} onClick={() => { postAiComment(resultText, jiraCommentTarget); setJiraCommentOpen(false); }}>Post comment</Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
