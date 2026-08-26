import { colors } from "../../constants/theme";
import { getNavItem } from "../../constants/navigation";

export default function PageHeader({ page }) {
  const nav = getNavItem(page);
  return (
    <>
      <h1 style={{ margin: "0 0 6px", fontSize: 22 }}>{nav.label}</h1>
      <p style={{ margin: "0 0 16px", color: colors.muted, fontSize: 13 }}>{nav.subtitle}</p>
    </>
  );
}
