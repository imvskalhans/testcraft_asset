import { colors } from "../../constants/theme";

export default function SetupGuideModal({ guide, onClose }) {
  if (!guide) return null;

  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.45)",
        display: "grid",
        placeItems: "center",
        zIndex: 1000,
        padding: 24,
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: colors.card,
          borderRadius: 12,
          maxWidth: 640,
          width: "100%",
          maxHeight: "85vh",
          overflow: "auto",
          padding: 24,
          boxShadow: "0 8px 32px rgba(0,0,0,0.15)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: 16 }}>
          <div>
            <h2 style={{ margin: "0 0 6px", fontSize: 18 }}>{guide.title || "Setup Guide"}</h2>
            <p style={{ margin: 0, fontSize: 13, color: colors.muted, lineHeight: 1.5 }}>{guide.summary}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{ border: "none", background: "transparent", fontSize: 20, cursor: "pointer", color: colors.muted }}
          >
            ×
          </button>
        </div>

        <div style={{ fontSize: 12, background: colors.surface, padding: 12, borderRadius: 8, marginBottom: 16 }}>
          <strong>Config file:</strong>
          <code style={{ display: "block", marginTop: 4 }}>{guide.configFile}</code>
        </div>

        <h3 style={{ fontSize: 14, margin: "0 0 10px" }}>Steps</h3>
        <ol style={{ margin: "0 0 16px", paddingLeft: 20, fontSize: 13, lineHeight: 1.6 }}>
          {(guide.steps ?? []).map((step) => (
            <li key={step.order} style={{ marginBottom: 10 }}>
              <strong>{step.title}</strong>
              {step.details && <div style={{ color: colors.muted, marginTop: 2 }}>{step.details}</div>}
              {step.command && (
                <pre style={{ background: "#1e1e1e", color: "#d4d4d4", padding: 8, borderRadius: 6, fontSize: 11, overflow: "auto", marginTop: 6 }}>
                  {step.command}
                </pre>
              )}
            </li>
          ))}
        </ol>

        {(guide.propertyGroups ?? []).length > 0 && (
          <>
            <h3 style={{ fontSize: 14, margin: "0 0 10px" }}>Property groups</h3>
            {(guide.propertyGroups ?? []).map((group) => (
              <div key={group.name} style={{ marginBottom: 12, fontSize: 12 }}>
                <strong>{group.name}</strong> <span style={{ color: colors.muted }}>({group.prefix}*)</span>
                <div style={{ color: colors.muted, marginTop: 4 }}>{(group.keys ?? []).join(" · ")}</div>
              </div>
            ))}
          </>
        )}

        {(guide.links ?? []).length > 0 && (
          <>
            <h3 style={{ fontSize: 14, margin: "16px 0 10px" }}>Links</h3>
            <ul style={{ margin: 0, paddingLeft: 20, fontSize: 13 }}>
              {guide.links.map((link) => (
                <li key={link.url}>
                  <a href={link.url} target="_blank" rel="noreferrer">{link.label}</a>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </div>
  );
}
