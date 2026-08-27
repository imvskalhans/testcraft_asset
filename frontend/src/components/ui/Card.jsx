import { colors } from "../../constants/theme";

export default function Card({ title, step, children, actions }) {
  return (
    <div
      className="card-shell"
      style={{
        background: colors.card,
        border: `1px solid ${colors.border}`,
        borderRadius: 12,
        padding: 18,
        marginBottom: 14,
        boxShadow: "0 1px 2px rgba(16,24,40,0.04)",
      }}
    >
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, marginBottom: 12, alignItems: "center" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          {step != null && (
            <span
              style={{
                width: 22,
                height: 22,
                borderRadius: "50%",
                background: colors.brandLight,
                color: colors.brand,
                display: "grid",
                placeItems: "center",
                fontSize: 11,
                fontWeight: 700,
              }}
            >
              {step}
            </span>
          )}
          <h3 style={{ margin: 0, fontSize: 15 }}>{title}</h3>
        </div>
        {actions}
      </div>
      {children}
    </div>
  );
}
