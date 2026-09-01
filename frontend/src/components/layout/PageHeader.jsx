import { useState } from "react";
import { colors } from "../../constants/theme";
import { getNavItem } from "../../constants/navigation";
import InfoIcon from "../settings/InfoIcon";

export default function PageHeader({ page }) {
  const nav = getNavItem(page);
  const [open, setOpen] = useState(false);
  return (
    <>
      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
        <h1 style={{ margin: 0, fontSize: 22 }}>{nav.label}</h1>
        <InfoIcon onClick={() => setOpen((value) => !value)} title={`About ${nav.label}`} />
      </div>
      <p style={{ margin: "0 0 16px", color: colors.muted, fontSize: 13 }}>{nav.subtitle}</p>
      {open && (
        <div style={{ marginBottom: 16, padding: 14, border: `1px solid ${colors.border}`, borderRadius: 10, background: colors.surface, fontSize: 12, lineHeight: 1.6 }}>
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start" }}>
            <strong>What this page does</strong>
            <button type="button" onClick={() => setOpen(false)} aria-label="Close page information" style={{ border: 0, background: "transparent", color: colors.muted, cursor: "pointer", fontSize: 18, lineHeight: 1 }}>×</button>
          </div>
          <p style={{ margin: "6px 0 0", color: colors.muted }}>{nav.details || nav.subtitle}</p>
        </div>
      )}
    </>
  );
}
