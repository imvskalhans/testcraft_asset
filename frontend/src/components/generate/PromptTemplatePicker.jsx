import { useMemo, useState } from "react";
import Button from "../ui/Button";
import { inputStyle } from "../../styles/forms";
import { colors } from "../../constants/theme";
import { PROMPT_TYPES } from "../../constants/options";
import { BUILTIN_GENERATION_PROMPTS } from "../../constants/generationPrompts";

function TemplateCard({ active, label, hint, onClick, onDelete }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        position: "relative",
        textAlign: "left",
        padding: 12,
        borderRadius: 10,
        border: `1px solid ${active ? colors.brand : colors.border}`,
        background: active ? colors.brandLight : "#fff",
        cursor: "pointer",
      }}
    >
      <div style={{ fontSize: 13, fontWeight: 700, color: active ? colors.brand : colors.text }}>{label}</div>
      <div style={{ fontSize: 11, color: colors.muted, marginTop: 4, paddingRight: onDelete ? 18 : 0 }}>{hint}</div>
      {onDelete && (
        <span
          role="button"
          tabIndex={0}
          aria-label={`Delete ${label}`}
          onClick={(event) => {
            event.stopPropagation();
            onDelete();
          }}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              event.stopPropagation();
              onDelete();
            }
          }}
          style={{
            position: "absolute",
            top: 8,
            right: 8,
            color: colors.muted,
            fontSize: 14,
            lineHeight: 1,
          }}
        >
          ×
        </span>
      )}
    </button>
  );
}

export default function PromptTemplatePicker({
  promptType,
  setPromptType,
  customPrompt,
  setCustomPrompt,
  additionalPrompt,
  setAdditionalPrompt,
  savedTemplates,
  onSaveTemplate,
  onDeleteTemplate,
}) {
  const [saveOpen, setSaveOpen] = useState(false);
  const [templateName, setTemplateName] = useState("");
  const [showPrompt, setShowPrompt] = useState(false);

  const selectedSaved = useMemo(
    () => (promptType.startsWith("saved:") ? savedTemplates.find((item) => `saved:${item.id}` === promptType) : null),
    [promptType, savedTemplates],
  );
  const effectivePrompt = selectedSaved?.prompt
    || (promptType === "custom" ? customPrompt : "")
    || BUILTIN_GENERATION_PROMPTS[promptType]
    || "";
  const displayedPrompt = effectivePrompt;

  const openSaveDialog = () => {
    setTemplateName(selectedSaved?.name ? `${selectedSaved.name} copy` : "");
    setSaveOpen(true);
  };

  const submitSave = () => {
    onSaveTemplate(templateName, effectivePrompt);
    setSaveOpen(false);
    setTemplateName("");
  };

  return (
    <div style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", marginBottom: 8 }}>
        <div style={{ fontSize: 12, fontWeight: 600 }}>Prompt template</div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" }}>
          <Button onClick={() => setShowPrompt((value) => !value)}>
            {showPrompt ? "Hide prompt" : "View / edit prompt"}
          </Button>
          {showPrompt && (
            <Button onClick={openSaveDialog} disabled={!displayedPrompt.trim()}>
              Save prompt locally
            </Button>
          )}
        </div>
      </div>

      <div
        className="form-grid"
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
          gap: 10,
          marginBottom: 12,
        }}
      >
        {PROMPT_TYPES.map((template) => (
          <TemplateCard
            key={template.id}
            active={promptType === template.id}
            label={template.label}
            hint={template.hint}
            onClick={() => setPromptType(template.id)}
          />
        ))}
      </div>

      {savedTemplates.length > 0 && (
        <>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Saved templates</div>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
              gap: 10,
              marginBottom: 12,
            }}
          >
            {savedTemplates.map((template) => (
              <TemplateCard
                key={template.id}
                active={promptType === `saved:${template.id}`}
                label={template.name}
                hint="Your saved prompt"
                onClick={() => setPromptType(`saved:${template.id}`)}
                onDelete={() => onDeleteTemplate(template.id)}
              />
            ))}
          </div>
        </>
      )}

      {showPrompt && (
        <textarea
          aria-label="Generation prompt"
          style={{ ...inputStyle, minHeight: 180, marginBottom: 8, width: "100%", boxSizing: "border-box", lineHeight: 1.5 }}
          value={displayedPrompt}
          onChange={(e) => {
            setPromptType("custom");
            setCustomPrompt(e.target.value);
          }}
          placeholder="Write custom generation instructions, or edit a built-in profile and save it as a named template…"
        />
      )}

      <label style={{ display: "block", fontSize: 12, fontWeight: 600, marginTop: 12 }}>
        Additional instructions (optional)
        <textarea
          aria-label="Additional generation instructions"
          style={{ ...inputStyle, minHeight: 78, marginTop: 6, width: "100%", boxSizing: "border-box", lineHeight: 1.5 }}
          value={additionalPrompt}
          onChange={(e) => setAdditionalPrompt(e.target.value)}
          placeholder="Add coverage, data, environments, acceptance criteria, or constraints without replacing the selected prompt…"
        />
        <span style={{ display: "block", marginTop: 5, fontSize: 11, color: colors.muted }}>
          These instructions are added after the selected prompt when test cases are generated.
        </span>
      </label>

      {showPrompt && (
        <p style={{ margin: "0 0 8px", fontSize: 11, color: colors.muted }}>
          {selectedSaved
            ? <>Using saved template <strong>{selectedSaved.name}</strong>. Editing it switches to a custom prompt.</>
            : "This is the selected profile's instruction. TestCraft still adds the Jira story and the Zephyr JSON output contract on the server. Editing here switches to Custom."}
        </p>
      )}

      {saveOpen && (
        <div
          style={{
            border: `1px solid ${colors.border}`,
            borderRadius: 10,
            padding: 12,
            background: colors.surface,
          }}
        >
          <label style={{ display: "block", fontSize: 12, fontWeight: 600, marginBottom: 8 }}>
            Save as a named profile
            <input
              aria-label="Template name"
              style={{ ...inputStyle, width: "100%", marginTop: 6 }}
              value={templateName}
              onChange={(e) => setTemplateName(e.target.value)}
              placeholder="e.g. Payment smoke pack"
            />
            <span style={{ display: "block", marginTop: 5, fontSize: 11, color: colors.muted }}>
              Stored only in this browser. It appears under Saved templates and can be reused like Default or Smoke.
            </span>
          </label>
          <div style={{ display: "flex", gap: 8 }}>
            <Button primary disabled={!templateName.trim() || !effectivePrompt.trim()} onClick={submitSave}>
              Save template
            </Button>
            <Button onClick={() => setSaveOpen(false)}>Cancel</Button>
          </div>
        </div>
      )}
    </div>
  );
}
