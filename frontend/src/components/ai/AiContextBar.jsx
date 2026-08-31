import { useEffect, useState } from "react";
import Button from "../ui/Button";
import { inputStyle } from "../../styles/forms";
import { colors } from "../../constants/theme";

function StatusDot({ active }) {
  return (
    <span
      aria-hidden="true"
      style={{
        width: 7,
        height: 7,
        borderRadius: "50%",
        background: active ? colors.success : colors.border,
        display: "inline-block",
        flexShrink: 0,
      }}
    />
  );
}

export default function AiContextBar({
  issueKey,
  setIssueKey,
  story,
  storyText,
  crKey,
  setCrKey,
  release,
  releaseText,
  loading,
  onFetchStory,
  onFetchCr,
  needsStory,
  needsCr,
}) {
  const [open, setOpen] = useState(false);

  const hasStory = Boolean(story);
  const hasRelease = Boolean(release);

  useEffect(() => {
    if ((needsStory && !hasStory) || (needsCr && !hasRelease)) {
      setOpen(true);
    }
  }, [needsStory, needsCr, hasStory, hasRelease]);

  if (!needsStory && !needsCr) {
    return null;
  }

  return (
    <div style={{ marginBottom: 16 }}>
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 8,
          flexWrap: "wrap",
          padding: "8px 10px",
          borderRadius: 10,
          border: `1px solid ${colors.border}`,
          background: colors.surface,
        }}
      >
        <span style={{ fontSize: 11, fontWeight: 700, color: colors.muted, textTransform: "uppercase", letterSpacing: "0.04em" }}>
          Context
        </span>

        {needsStory && (
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 6,
              padding: "4px 10px",
              borderRadius: 999,
              border: `1px solid ${hasStory ? colors.success : colors.border}`,
              background: hasStory ? colors.successLight : "#fff",
              color: hasStory ? colors.success : colors.text,
              fontSize: 12,
              cursor: "pointer",
            }}
          >
            <StatusDot active={hasStory} />
            {hasStory ? issueKey : "Jira story"}
          </button>
        )}

        {needsCr && (
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 6,
              padding: "4px 10px",
              borderRadius: 999,
              border: `1px solid ${hasRelease ? colors.success : colors.border}`,
              background: hasRelease ? colors.successLight : "#fff",
              color: hasRelease ? colors.success : colors.text,
              fontSize: 12,
              cursor: "pointer",
            }}
          >
            <StatusDot active={hasRelease} />
            {hasRelease ? crKey : "Change request"}
          </button>
        )}

        <button
          type="button"
          onClick={() => setOpen((value) => !value)}
          style={{
            marginLeft: "auto",
            border: "none",
            background: "transparent",
            color: colors.brand,
            fontSize: 12,
            fontWeight: 600,
            cursor: "pointer",
            padding: "4px 0",
          }}
        >
          {open ? "Hide" : "Add context"}
        </button>
      </div>

      {!open && ((needsStory && !hasStory) || (needsCr && !hasRelease)) && (
        <p style={{ margin: "6px 0 0", fontSize: 11, color: colors.danger }}>
          {[
            needsStory && !hasStory ? "Fetch a Jira story for this action." : null,
            needsCr && !hasRelease ? "Fetch a change request for this action." : null,
          ].filter(Boolean).join(" ")}
        </p>
      )}

      {open && (
        <div
          style={{
            marginTop: 8,
            padding: 12,
            borderRadius: 10,
            border: `1px solid ${colors.border}`,
            background: "#fff",
          }}
        >
          <div style={{ display: "grid", gap: 10 }}>
            {needsStory && (
              <>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <input
                    aria-label="Jira story key"
                    style={{ ...inputStyle, flex: 1, fontSize: 12, padding: "7px 10px" }}
                    value={issueKey}
                    onChange={(e) => setIssueKey(e.target.value.toUpperCase())}
                    placeholder="Story key, e.g. KAN-1"
                  />
                  <Button disabled={loading || !issueKey.trim()} onClick={onFetchStory}>
                    {hasStory ? "Refresh" : "Fetch"}
                  </Button>
                </div>
                {hasStory && (
                  <p style={{ margin: 0, fontSize: 11, color: colors.muted, lineHeight: 1.45 }}>
                    {story.summary || storyText.split("\n")[0]}
                  </p>
                )}
              </>
            )}

            {needsCr && (
              <>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <input
                    aria-label="Change request key"
                    style={{ ...inputStyle, flex: 1, fontSize: 12, padding: "7px 10px" }}
                    value={crKey}
                    onChange={(e) => setCrKey(e.target.value.toUpperCase())}
                    placeholder="CR key, e.g. KAN-7"
                  />
                  <Button disabled={loading || !crKey.trim()} onClick={onFetchCr}>
                    {hasRelease ? "Refresh" : "Fetch"}
                  </Button>
                </div>
                {hasRelease && (
                  <p style={{ margin: 0, fontSize: 11, color: colors.muted, lineHeight: 1.45 }}>
                    {release.crSummary}
                    {release.linkedStories?.length ? ` · ${release.linkedStories.length} linked stories` : ""}
                  </p>
                )}
              </>
            )}
          </div>

          {((needsStory && hasStory) || (needsCr && hasRelease)) && (
            <details style={{ marginTop: 10, fontSize: 11, color: colors.muted }}>
              <summary style={{ cursor: "pointer", fontWeight: 600 }}>Preview loaded context</summary>
              <pre style={{ margin: "8px 0 0", whiteSpace: "pre-wrap", maxHeight: 120, overflow: "auto", background: colors.surface, padding: 8, borderRadius: 8 }}>
                {[
                  needsStory && hasStory ? storyText : "",
                  needsCr && hasRelease ? releaseText : "",
                ].filter(Boolean).join("\n\n---\n\n")}
              </pre>
            </details>
          )}
        </div>
      )}
    </div>
  );
}
