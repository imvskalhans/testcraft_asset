import { buttonStyle } from "../../styles/forms";

export default function Button({ primary, disabled, onClick, children, style }) {
  return (
    <button type="button" style={{ ...buttonStyle(primary, disabled), ...style }} onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}
