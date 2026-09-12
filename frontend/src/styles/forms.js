import { colors } from "../constants/theme";

export const inputStyle = {
  width: "100%",
  padding: "9px 11px",
  border: `1px solid ${colors.border}`,
  borderRadius: 10,
  fontSize: 13,
  boxSizing: "border-box",
  background: "rgba(255,255,255,.55)",
};

export const labelStyle = {
  fontSize: 11,
  fontWeight: 600,
  color: colors.muted,
  display: "block",
  marginBottom: 4,
};

export function buttonStyle(primary = false, disabled = false) {
  return {
    padding: "8px 14px",
    borderRadius: 10,
    border: primary ? "none" : "1px solid rgba(255,255,255,.72)",
    background: disabled
      ? "rgba(216, 221, 226, .85)"
      : primary
        ? colors.brand
        : "linear-gradient(180deg, rgba(255,255,255,.78), rgba(255,255,255,.42))",
    color: primary || disabled ? "#fff" : colors.text,
    cursor: disabled ? "not-allowed" : "pointer",
    fontSize: 13,
    fontWeight: 600,
    boxShadow: primary ? "0 8px 18px rgba(24, 95, 165, .22)" : "inset 0 1px 0 rgba(255,255,255,.85)",
    backdropFilter: "blur(16px) saturate(170%)",
  };
}
