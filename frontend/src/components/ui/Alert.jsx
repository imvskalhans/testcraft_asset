import { colors } from "../../constants/theme";

export default function Alert({ type = "error", children }) {
  const bg = type === "success" ? colors.successLight : type === "info" ? colors.brandLight : colors.dangerLight;
  const color = type === "success" ? colors.success : type === "info" ? colors.brand : colors.danger;
  return (
    <div className="glass-alert" style={{ background: bg, color, padding: "8px 12px", borderRadius: 12, fontSize: 12, marginBottom: 10, lineHeight: 1.5 }}>
      {children}
    </div>
  );
}
