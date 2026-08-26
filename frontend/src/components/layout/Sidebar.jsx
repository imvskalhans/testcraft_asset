import { colors } from "../../constants/theme";
import { NAV_ITEMS } from "../../constants/navigation";
import { initials } from "../../utils/initials";

export default function Sidebar({ page, onNavigate, currentUser }) {
  return (
    <aside
      style={{
        width: 228,
        background: colors.card,
        borderRight: `1px solid ${colors.border}`,
        padding: 16,
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 18 }}>
        <div
          style={{
            width: 32,
            height: 32,
            background: colors.brand,
            color: "#fff",
            borderRadius: 8,
            display: "grid",
            placeItems: "center",
            fontWeight: 700,
          }}
        >
          TC
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 14 }}>TestCraft</div>
          <div style={{ fontSize: 11, color: colors.muted }}>QA Platform</div>
        </div>
      </div>

      <nav style={{ flex: 1 }}>
        {NAV_ITEMS.map((n) => (
          <div
            key={n.id}
            onClick={() => onNavigate(n.id)}
            style={{
              padding: "9px 10px",
              borderRadius: 8,
              marginBottom: 4,
              cursor: "pointer",
              background: page === n.id ? colors.brandLight : "transparent",
              color: page === n.id ? colors.brand : colors.muted,
              fontWeight: page === n.id ? 600 : 400,
              fontSize: 13,
            }}
          >
            {n.label}
          </div>
        ))}
      </nav>

      <div
        style={{
          display: "flex",
          gap: 8,
          alignItems: "center",
          background: colors.surface,
          border: `1px solid ${colors.border}`,
          borderRadius: 10,
          padding: 10,
          marginTop: 12,
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: "50%",
            background: colors.brand,
            color: "#fff",
            display: "grid",
            placeItems: "center",
            fontSize: 11,
            fontWeight: 700,
          }}
        >
          {initials(currentUser?.displayName)}
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 12, fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            {currentUser?.displayName || "Loading user…"}
          </div>
          <div style={{ fontSize: 10, color: colors.muted, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            {currentUser?.emailAddress || currentUser?.error || ""}
          </div>
        </div>
      </div>
    </aside>
  );
}
