import { colors } from "../../constants/theme";

const CATEGORY_LABELS = {
  automation: "Automation",
  jira: "Jira",
  release: "Release",
  general: "General",
};

function contextHint(action) {
  const hints = [];
  if (action.supportsJiraContext) hints.push("Jira story");
  if (action.supportsCrContext) hints.push("Change request");
  if (!hints.length) return "No Jira or CR context needed";
  return `Uses ${hints.join(" + ")}`;
}

export default function AiActionPicker({ actions, onSelect }) {
  if (!actions.length) {
    return (
      <p style={{ margin: 0, fontSize: 13, color: colors.muted }}>
        Loading AI actions…
      </p>
    );
  }

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
        gap: 12,
      }}
    >
      {actions.map((action) => (
        <button
          key={action.id}
          type="button"
          onClick={() => onSelect(action.id)}
          style={{
            textAlign: "left",
            padding: 16,
            borderRadius: 12,
            border: `1px solid ${colors.border}`,
            background: "#fff",
            cursor: "pointer",
            transition: "border-color 0.15s ease, box-shadow 0.15s ease",
            boxShadow: "0 1px 2px rgba(16,24,40,0.04)",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = colors.brand;
            e.currentTarget.style.boxShadow = "0 4px 12px rgba(24,95,165,0.12)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = colors.border;
            e.currentTarget.style.boxShadow = "0 1px 2px rgba(16,24,40,0.04)";
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", gap: 8, marginBottom: 8 }}>
            <span style={{ fontSize: 14, fontWeight: 700, color: colors.text, lineHeight: 1.3 }}>
              {action.label}
            </span>
            <span
              style={{
                fontSize: 10,
                fontWeight: 700,
                textTransform: "uppercase",
                letterSpacing: "0.04em",
                color: colors.brand,
                background: colors.brandLight,
                padding: "2px 8px",
                borderRadius: 999,
                whiteSpace: "nowrap",
                height: "fit-content",
              }}
            >
              {CATEGORY_LABELS[action.category] || action.category || "AI"}
            </span>
          </div>
          <p style={{ margin: "0 0 10px", fontSize: 12, color: colors.muted, lineHeight: 1.5 }}>
            {action.description}
          </p>
          <span style={{ fontSize: 11, color: colors.muted }}>
            {contextHint(action)}
          </span>
        </button>
      ))}
    </div>
  );
}
