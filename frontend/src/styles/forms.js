import { colors } from "../constants/theme";

export const inputStyle = {
  width: "100%",
  padding: "9px 11px",
  border: `1px solid ${colors.border}`,
  borderRadius: 8,
  fontSize: 13,
  boxSizing: "border-box",
  background: "#fff",
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
    borderRadius: 8,
    border: primary ? "none" : `1px solid ${colors.border}`,
    background: disabled ? "#D8DDE2" : primary ? colors.brand : colors.card,
    color: primary || disabled ? "#fff" : colors.text,
    cursor: disabled ? "not-allowed" : "pointer",
    fontSize: 13,
    fontWeight: 600,
  };
}
