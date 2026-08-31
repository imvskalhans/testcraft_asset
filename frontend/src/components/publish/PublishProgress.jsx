import Button from "../ui/Button";
import { colors } from "../../constants/theme";

function statusColor(status) {
  if (status === "success") return colors.success;
  if (status === "error") return colors.danger;
  if (status === "publishing") return colors.brand;
  return colors.muted;
}

export default function PublishProgress({ progress, onRetryFailed, publishing }) {
  if (!progress?.items?.length) return null;

  const completed = progress.items.filter((item) => item.status === "success" || item.status === "error").length;
  const successCount = progress.items.filter((item) => item.status === "success").length;
  const failedCount = progress.items.filter((item) => item.status === "error").length;
  const percent = progress.total ? Math.round((completed / progress.total) * 100) : 0;

  return (
    <div style={{ marginTop: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, fontSize: 12, marginBottom: 8 }}>
        <strong>Publishing progress</strong>
        <span style={{ color: colors.muted }}>
          {completed}/{progress.total} complete · {successCount} succeeded · {failedCount} failed
        </span>
      </div>
      <div
        aria-hidden="true"
        style={{
          height: 10,
          borderRadius: 999,
          background: colors.surface,
          border: `1px solid ${colors.border}`,
          overflow: "hidden",
          marginBottom: 12,
        }}
      >
        <div
          style={{
            width: `${percent}%`,
            height: "100%",
            background: failedCount > 0 && completed === progress.total ? colors.danger : colors.brand,
            transition: "width .2s ease",
          }}
        />
      </div>

      <div style={{ display: "grid", gap: 6 }}>
        {progress.items.map((item) => (
          <div
            key={item.index}
            style={{
              display: "flex",
              justifyContent: "space-between",
              gap: 10,
              alignItems: "flex-start",
              fontSize: 12,
              padding: "8px 10px",
              borderRadius: 8,
              background: colors.surface,
              border: `1px solid ${colors.border}`,
            }}
          >
            <div style={{ minWidth: 0 }}>
              <div style={{ fontWeight: 600 }}>{item.name || `Test case ${item.index + 1}`}</div>
              {item.key && (
                <div style={{ color: colors.muted, marginTop: 2 }}>
                  {item.url ? <a href={item.url} target="_blank" rel="noreferrer">{item.key}</a> : item.key}
                </div>
              )}
              {item.error && <div style={{ color: colors.danger, marginTop: 4 }}>{item.error}</div>}
            </div>
            <span style={{ color: statusColor(item.status), fontWeight: 700, whiteSpace: "nowrap" }}>
              {item.status}
            </span>
          </div>
        ))}
      </div>

      {failedCount > 0 && !publishing && (
        <div style={{ marginTop: 12 }}>
          <Button primary onClick={onRetryFailed}>Retry failed publishes</Button>
        </div>
      )}
    </div>
  );
}
