import { colors } from "../../constants/theme";

export default function InfoIcon({ onClick, title = "Setup instructions" }) {
  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      style={{
        width: 28,
        height: 28,
        borderRadius: "50%",
        border: `1px solid ${colors.border}`,
        background: colors.brandLight,
        color: colors.brand,
        cursor: "pointer",
        fontWeight: 700,
        fontSize: 14,
        lineHeight: 1,
      }}
    >
      i
    </button>
  );
}
