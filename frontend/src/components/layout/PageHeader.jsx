import { useState } from "react";
import { colors } from "../../constants/theme";
import { getAiActionNav, getNavItem } from "../../constants/navigation";
import { useApp } from "../../context/AppContext";
import InfoIcon from "../settings/InfoIcon";
import RefreshIcon from "./RefreshIcon";

export default function PageHeader({ page, onRefresh, refreshTitle, refreshing }) {
  const nav = getNavItem(page);
  const { activeAiAction } = useApp();
  const actionNav = page === "ai" ? getAiActionNav(activeAiAction) : null;
  const [open, setOpen] = useState(false);
  return (
    <>
      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
        <h1 style={{ margin: 0, fontSize: 22 }}>{actionNav ? actionNav.label : nav.label}</h1>
        <InfoIcon onClick={() => setOpen((value) => !value)} title={`About ${nav.label}`} />
        <div style={{ flex: 1 }} />
        {onRefresh && (
          <RefreshIcon
            onClick={onRefresh}
            disabled={refreshing}
            title={refreshTitle || "Refresh this page only — the rest of your session stays as it is"}
          />
        )}
      </div>
      <p style={{ margin: "0 0 16px", color: colors.muted, fontSize: 13 }}>{nav.subtitle}</p>
      {open && (
        <div className="glass-card" style={{ marginBottom: 16, padding: 14, borderRadius: 14, fontSize: 12, lineHeight: 1.6 }}>
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
