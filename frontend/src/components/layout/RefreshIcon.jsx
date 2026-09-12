import { colors } from "../../constants/theme";

export default function RefreshIcon({ onClick, title = "Refresh this page", disabled = false }) {
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      onClick={onClick}
      disabled={disabled}
      className="glass-card"
      style={{
        width: 28,
        height: 28,
        borderRadius: "50%",
        color: colors.brand,
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.55 : 1,
        display: "grid",
        placeItems: "center",
        padding: 0,
      }}
    >
      <svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2.5 8a5.5 5.5 0 0 1 9.4-3.9L14 6" />
        <path d="M14 2.5V6h-3.5" />
        <path d="M13.5 8a5.5 5.5 0 0 1-9.4 3.9L2 10" />
        <path d="M2 13.5V10h3.5" />
      </svg>
    </button>
  );
}
