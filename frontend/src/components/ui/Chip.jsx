import { colors } from "../../constants/theme";

export default function Chip({ active, children, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        padding: "6px 12px",
        borderRadius: 999,
        border: `1px solid ${active ? colors.brand : colors.border}`,
        background: active ? colors.brand : "#fff",
        color: active ? "#fff" : colors.text,
        cursor: "pointer",
        fontSize: 12,
        fontWeight: 600,
      }}
    >
      {children}
    </button>
  );
}
