import { useEffect, useMemo, useState } from "react";
import { colors } from "../../constants/theme";
import { NAV_ITEMS } from "../../constants/navigation";
import { initials } from "../../utils/initials";
import { useApp } from "../../context/AppContext";

function toggleKey(current, key) {
  const next = new Set(current);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  return next;
}

export default function Sidebar({ page, onNavigate, currentUser, onFeedback, hasTestCases }) {
  const { activeAiAction } = useApp();
  const groups = useMemo(() => {
    const visible = NAV_ITEMS.filter((item) => item.group && (item.id !== "publish" || hasTestCases));
    const ordered = [];
    visible.forEach((item) => {
      const current = ordered[ordered.length - 1];
      if (!current || current.name !== item.group) {
        ordered.push({ name: item.group, items: [item] });
      } else {
        current.items.push(item);
      }
    });
    return ordered;
  }, [hasTestCases]);

  const homeItem = NAV_ITEMS.find((item) => item.id === "home");
  const activeGroup = groups.find((group) => group.items.some((item) => item.id === page))?.name;
  const [openGroups, setOpenGroups] = useState(() => new Set(groups.map((group) => group.name)));
  const [openNested, setOpenNested] = useState(() => new Set(["ai"]));

  useEffect(() => {
    if (activeGroup) {
      setOpenGroups((current) => new Set(current).add(activeGroup));
    }
    if (page === "ai") {
      setOpenNested((current) => new Set(current).add("ai"));
    }
  }, [activeGroup, page]);

  return (
    <aside
      className="app-sidebar"
      style={{
        width: 248,
        padding: 16,
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 18 }}>
        <div
          style={{
            width: 32,
            height: 32,
            background: colors.brand,
            color: "#fff",
            borderRadius: 8,
            display: "grid",
            placeItems: "center",
            fontWeight: 700,
          }}
        >
          TC
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 14 }}>TestCraft</div>
          <div style={{ fontSize: 11, color: colors.muted }}>QA Platform</div>
        </div>
      </div>

      <nav style={{ flex: 1, overflow: "auto" }}>
        {homeItem && (
          <button
            type="button"
            className={`nav-home${page === "home" ? " active" : ""}`}
            onClick={() => onNavigate("home")}
            aria-current={page === "home" ? "page" : undefined}
          >
            {homeItem.label}
          </button>
        )}

        {groups.map((group) => {
          const open = openGroups.has(group.name);
          return (
            <div key={group.name} className="nav-group" style={{ marginBottom: 8 }}>
              <button
                type="button"
                className="nav-group-heading"
                aria-expanded={open}
                onClick={() => setOpenGroups((current) => toggleKey(current, group.name))}
              >
                <span>{group.name}</span>
                <span className="nav-group-chevron" aria-hidden="true">{open ? "▴" : "▾"}</span>
              </button>
              {open && group.items.map((item) => {
                const nestedOpen = openNested.has(item.id);
                const children = item.children || [];
                if (children.length) {
                  return (
                    <div key={item.id}>
                      <button
                        type="button"
                        className={`nav-nested-heading${page === item.id ? " active" : ""}`}
                        aria-expanded={nestedOpen}
                        onClick={() => {
                          const next = toggleKey(openNested, item.id);
                          setOpenNested(next);
                          if (next.has(item.id) && page !== item.id) onNavigate(item.id);
                        }}
                      >
                        <span>{item.label}</span>
                        <span className="nav-group-chevron" aria-hidden="true">{nestedOpen ? "▴" : "▾"}</span>
                      </button>
                      {nestedOpen && (
                        <div className="nav-children">
                          {children.map((child) => (
                            <button
                              key={child.id}
                              type="button"
                              className={`nav-item nav-action${page === item.id && activeAiAction === child.id ? " active" : ""}`}
                              onClick={() => onNavigate(item.id, { aiAction: child.id })}
                              aria-current={page === item.id && activeAiAction === child.id ? "page" : undefined}
                            >
                              {child.label}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  );
                }
                return (
                  <button
                    key={item.id}
                    type="button"
                    className={`nav-item${page === item.id ? " active" : ""}`}
                    onClick={() => onNavigate(item.id)}
                    style={{
                      display: "block",
                      width: "100%",
                      textAlign: "left",
                      border: 0,
                      padding: "9px 10px",
                      borderRadius: 8,
                      marginBottom: 4,
                      cursor: "pointer",
                      color: page === item.id ? colors.brand : colors.muted,
                      fontWeight: page === item.id ? 600 : 400,
                      fontSize: 13,
                    }}
                    aria-current={page === item.id ? "page" : undefined}
                  >
                    <span style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 6 }}>
                      {item.label}
                      {item.status && <span style={{ fontSize: 9, fontWeight: 600, padding: "2px 5px", borderRadius: 5, background: "#FFF3D6", color: "#8A5A00" }}>{item.status}</span>}
                    </span>
                  </button>
                );
              })}
            </div>
          );
        })}
      </nav>

      <button type="button" className="glass-footer-btn" onClick={onFeedback} style={{ width: "100%", textAlign: "left", padding: "9px 10px", borderRadius: 8, color: colors.brand, cursor: "pointer", fontSize: 12, fontWeight: 600 }}>
        ✉ Send feedback
      </button>

      <div
        className="user-panel"
        style={{
          display: "flex",
          gap: 8,
          alignItems: "center",
          borderRadius: 10,
          padding: 10,
          marginTop: 12,
        }}
      >
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: "50%",
            background: colors.brand,
            color: "#fff",
            display: "grid",
            placeItems: "center",
            fontSize: 11,
            fontWeight: 700,
          }}
        >
          {initials(currentUser?.displayName)}
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 12, fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            {currentUser?.displayName || "Loading user…"}
          </div>
          <div style={{ fontSize: 10, color: colors.muted, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            {currentUser?.emailAddress || currentUser?.error || ""}
          </div>
        </div>
      </div>
    </aside>
  );
}
