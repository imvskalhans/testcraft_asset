import { useEffect, useMemo, useState } from "react";
import Button from "../ui/Button";
import Alert from "../ui/Alert";
import { inputStyle } from "../../styles/forms";
import { colors } from "../../constants/theme";
import { loadPromptOverride, resetPromptOverride, savePromptOverride } from "../../utils/aiPromptStorage";

function InlineMarkdown({ text }) {
  const renderInline = (value) => value.split(/(\*\*[^*]+\*\*|`[^`]+`)/g).map((part, index) => {
    if (part.startsWith("**") && part.endsWith("**")) return <strong key={index}>{part.slice(2, -2)}</strong>;
    if (part.startsWith("`") && part.endsWith("`")) return <code key={index}>{part.slice(1, -1)}</code>;
    return <span key={index}>{part}</span>;
  });
  return (
    <div style={{ lineHeight: 1.55 }}>
      {String(text || "").split(/\r?\n/).map((line, index) => {
        if (line.startsWith("### ")) return <h4 key={index} style={{ margin: "14px 0 5px" }}>{renderInline(line.slice(4))}</h4>;
        if (line.startsWith("## ")) return <h3 key={index} style={{ margin: "14px 0 5px" }}>{renderInline(line.slice(3))}</h3>;
        if (line.startsWith("# ")) return <h2 key={index} style={{ margin: "14px 0 5px" }}>{renderInline(line.slice(2))}</h2>;
        if (/^[-*] /.test(line)) return <div key={index} style={{ paddingLeft: 16 }}>• {renderInline(line.slice(2))}</div>;
        return <div key={index} style={{ minHeight: line ? undefined : 8 }}>{renderInline(line)}</div>;
      })}
    </div>
  );
}

export default function AiActionPanel({
  action,
  issueKey,
  storyText,
  hasStory,
  crKey,
  releaseText,
  hasRelease,
  running,
  result,
  onRun,
}) {
  const savedPrompt = useMemo(
    () => (action ? loadPromptOverride(action.id) : ""),
    [action],
  );
  const [inputs, setInputs] = useState({});
  const [promptTemplate, setPromptTemplate] = useState("");
  const [showPrompt, setShowPrompt] = useState(false);
  const [showResolvedPrompt, setShowResolvedPrompt] = useState(false);
  const [resultText, setResultText] = useState("");
  const [showPreview, setShowPreview] = useState(true);

  useEffect(() => {
    if (!action) return;
    const nextInputs = {};
    (action.inputFields || []).forEach((field) => {
      nextInputs[field.id] = "";
    });
    setInputs(nextInputs);
    setPromptTemplate(savedPrompt || action.defaultPromptTemplate || "");
    setShowPrompt(false);
    setShowResolvedPrompt(false);
    setResultText("");
    setShowPreview(true);
  }, [action, hasStory, hasRelease, savedPrompt]);

  useEffect(() => {
    setResultText(result?.result || "");
  }, [result]);

  if (!action) {
    return <Alert type="info">Select an AI action to begin.</Alert>;
  }

  const promptDirty = promptTemplate.trim() !== (action.defaultPromptTemplate || "").trim();
  const requiredFilled = (action.inputFields || []).every(
    (field) => !field.required || String(inputs[field.id] || "").trim(),
  );
  const includeJiraContext = Boolean(action.supportsJiraContext && (hasStory || issueKey?.trim()));
  const includeCrContext = Boolean(action.supportsCrContext && (hasRelease || crKey?.trim()));
  const jiraReady = !action.supportsJiraContext || includeJiraContext;
  const crReady = !action.supportsCrContext || includeCrContext;
  const canRun = !running && requiredFilled && jiraReady && crReady;

  const updateInput = (fieldId, value) => {
    setInputs((current) => ({ ...current, [fieldId]: value }));
  };

  const restoreDefaultPrompt = () => {
    const defaultPrompt = action.defaultPromptTemplate || "";
    setPromptTemplate(defaultPrompt);
    resetPromptOverride(action.id);
  };

  const persistPrompt = () => {
    savePromptOverride(action.id, promptTemplate);
  };

  const runAction = () => {
    onRun({
      actionId: action.id,
      promptTemplate,
      includeJiraContext,
      includeCrContext,
      issueKey,
      jiraDetails: includeJiraContext ? storyText : "",
      crKey,
      crDetails: includeCrContext ? releaseText : "",
      inputs,
    });
  };

  return (
    <div>
      {(action.inputFields || []).map((field) => (
        <label key={field.id} style={{ display: "block", fontSize: 12, fontWeight: 600, marginBottom: 12 }}>
          {field.label}{field.required ? " *" : ""}
          {field.type === "textarea" ? (
            <textarea
              aria-label={field.label}
              style={{ ...inputStyle, width: "100%", minHeight: field.id === "dom" ? 180 : 110, marginTop: 6, boxSizing: "border-box" }}
              value={inputs[field.id] || ""}
              maxLength={field.maxLength}
              placeholder={field.placeholder}
              onChange={(e) => updateInput(field.id, e.target.value)}
            />
          ) : (
            <input
              aria-label={field.label}
              style={{ ...inputStyle, width: "100%", marginTop: 6, boxSizing: "border-box" }}
              value={inputs[field.id] || ""}
              maxLength={field.maxLength}
              placeholder={field.placeholder}
              onChange={(e) => updateInput(field.id, e.target.value)}
            />
          )}
        </label>
      ))}

      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12 }}>
        <Button onClick={() => setShowPrompt((value) => !value)}>
          {showPrompt ? "Hide prompt template" : "View / edit prompt"}
        </Button>
        {showPrompt && (
          <>
            <Button onClick={restoreDefaultPrompt}>Reset to default</Button>
            <Button onClick={persistPrompt} disabled={!promptDirty}>Save prompt locally</Button>
          </>
        )}
        <Button primary disabled={!canRun} onClick={runAction}>
          {running ? "Running…" : `Run ${action.label}`}
        </Button>
      </div>

      {showPrompt && (
        <div style={{ marginBottom: 14 }}>
          <Alert type="info">
            Prompts are stored only in this browser. TestCraft still applies server-side safety checks before calling the AI provider.
          </Alert>
          <textarea
            aria-label="Prompt template"
            style={{ ...inputStyle, width: "100%", minHeight: 220, boxSizing: "border-box", fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", fontSize: 12, lineHeight: 1.5 }}
            value={promptTemplate}
            onChange={(e) => setPromptTemplate(e.target.value)}
          />
          <p style={{ fontSize: 11, color: colors.muted, margin: "8px 0 0" }}>
            Placeholders: <code>{"{{user_input}}"}</code>, <code>{"{{user_ask}}"}</code>, <code>{"{{dom}}"}</code>, <code>{"{{issue_key}}"}</code>, <code>{"{{jira_context}}"}</code>, <code>{"{{cr_key}}"}</code>, <code>{"{{cr_context}}"}</code>
          </p>
        </div>
      )}

      {result?.resolvedPrompt && (
        <div style={{ marginBottom: 12 }}>
          <Button onClick={() => setShowResolvedPrompt((value) => !value)}>
            {showResolvedPrompt ? "Hide resolved prompt" : "View resolved prompt"}
          </Button>
          {showResolvedPrompt && (
            <pre style={{ marginTop: 10, padding: 12, background: colors.surface, borderRadius: 8, overflow: "auto", fontSize: 11, lineHeight: 1.45 }}>
              {result.resolvedPrompt}
            </pre>
          )}
        </div>
      )}

      {resultText && (
        <div style={{ marginTop: 14 }}>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
            <Button onClick={() => setShowPreview((value) => !value)}>{showPreview ? "Edit result" : "Preview result"}</Button>
            <Button onClick={() => navigator.clipboard.writeText(resultText)}>Copy result</Button>
          </div>
          {result?.mockMode && (
            <div style={{ marginBottom: 10 }}>
              <Alert type="info">Mock AI mode is active. Configure a live provider for real analysis.</Alert>
            </div>
          )}
          {showPreview ? (
            <div style={{ background: colors.surface, borderRadius: 8, padding: 14, fontSize: 13 }}>
              <InlineMarkdown text={resultText} />
            </div>
          ) : (
            <textarea
              aria-label="Editable AI result"
              style={{ ...inputStyle, width: "100%", minHeight: 320, boxSizing: "border-box", fontFamily: "inherit", lineHeight: 1.5 }}
              value={resultText}
              onChange={(e) => setResultText(e.target.value)}
            />
          )}
        </div>
      )}
    </div>
  );
}
