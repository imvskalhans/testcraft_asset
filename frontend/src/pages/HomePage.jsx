import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { colors } from "../constants/theme";
import { formatStatTime } from "../utils/appStats";
import { useApp } from "../context/AppContext";

function StatCard({ label, value, hint, tone = "default" }) {
  const tones = {
    default: { bg: colors.surface, color: colors.text },
    brand: { bg: colors.brandLight, color: colors.brand },
    success: { bg: colors.successLight, color: colors.success },
  };
  const style = tones[tone] ?? tones.default;
  return (
    <div
      style={{
        background: style.bg,
        border: `1px solid ${colors.border}`,
        borderRadius: 12,
        padding: "16px 18px",
      }}
    >
      <div style={{ fontSize: 11, color: colors.muted, marginBottom: 8 }}>{label}</div>
      <div style={{ fontSize: 28, fontWeight: 700, color: style.color, lineHeight: 1 }}>{value}</div>
      {hint && <div style={{ fontSize: 11, color: colors.muted, marginTop: 8 }}>{hint}</div>}
    </div>
  );
}

export default function HomePage() {
  const {
    navigate,
    appStats,
    resetStats,
    currentUser,
    issueKey,
    testCases,
    publishedKeys,
    story,
    configStatus,
  } = useApp();

  const sessionCases = testCases.length;
  const sessionPublished = publishedKeys.length;

  return (
    <>
      <Card title="Welcome back">
        <p style={{ marginTop: 0, fontSize: 13, color: colors.muted, lineHeight: 1.6 }}>
          {currentUser?.displayName ? `Hi ${currentUser.displayName}. ` : ""}
          TestCraft tracks your local QA workflow here. Statistics are stored in this browser so you can see how much work you have done across sessions.
        </p>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <Button primary onClick={() => navigate("story")}>Fetch Jira story</Button>
          <Button onClick={() => navigate("generate")}>Generate tests</Button>
          <Button onClick={() => navigate("publish")}>Publish & link</Button>
          <Button onClick={() => navigate("traceability")}>Traceability</Button>
        </div>
      </Card>

      <Card title="Lifetime statistics">
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
            gap: 12,
            marginBottom: 14,
          }}
        >
          <StatCard label="Test cases generated" value={appStats.totalGenerated} tone="brand" />
          <StatCard label="Published to Zephyr" value={appStats.totalPublished} tone="success" />
          <StatCard label="Jira links created" value={appStats.totalLinked} />
          <StatCard label="Stories fetched" value={appStats.storiesFetched} />
          <StatCard label="Cycles created" value={appStats.cyclesCreated} />
          <StatCard label="AI actions run" value={appStats.aiRuns} />
          <StatCard label="Imported test sets" value={appStats.imports} />
        </div>
        <div style={{ fontSize: 11, color: colors.muted }}>
          Last activity: {formatStatTime(appStats.lastUpdatedAt)}
        </div>
        <div style={{ marginTop: 12 }}>
          <Button onClick={resetStats}>Reset statistics</Button>
        </div>
      </Card>

      <Card title="Current session">
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
            gap: 12,
          }}
        >
          <StatCard label="Active story" value={issueKey} hint={story?.summary || "No story fetched yet"} />
          <StatCard label="Cases in editor" value={sessionCases} hint="Ready to review or publish" />
          <StatCard label="Published this session" value={sessionPublished} hint="Zephyr keys from the latest publish run" />
          <StatCard
            label="Integrations"
            value={configStatus?.jira?.connected && configStatus?.zephyr?.tokenConfigured ? "Ready" : "Check settings"}
            hint={`Jira ${configStatus?.jira?.connected ? "connected" : "not connected"} · Zephyr ${configStatus?.zephyr?.tokenConfigured ? "configured" : "missing token"}`}
          />
        </div>
      </Card>

      <Card title="Recent activity">
        {appStats.recentActivity?.length ? (
          <div style={{ display: "grid", gap: 8 }}>
            {appStats.recentActivity.map((item) => (
              <div
                key={item.id}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: 12,
                  alignItems: "center",
                  padding: "10px 12px",
                  borderRadius: 8,
                  background: colors.surface,
                  border: `1px solid ${colors.border}`,
                  fontSize: 12,
                }}
              >
                <div>
                  <strong>{item.label}</strong>
                  {item.issueKey ? <span style={{ color: colors.muted }}> · {item.issueKey}</span> : null}
                </div>
                <span style={{ color: colors.muted, whiteSpace: "nowrap" }}>
                  {item.count > 1 ? `${item.count} · ` : ""}{formatStatTime(item.at)}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ margin: 0, fontSize: 12, color: colors.muted }}>
            No activity recorded yet. Fetch a story or generate test cases to start building your dashboard.
          </p>
        )}
      </Card>
    </>
  );
}
