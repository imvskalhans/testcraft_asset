import { useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import api from "../api";

function StatCard({ label, value, tone = "default" }) {
  const tones = {
    default: { bg: colors.surface, color: colors.text },
    success: { bg: colors.successLight, color: colors.success },
    danger: { bg: colors.dangerLight, color: colors.danger },
    brand: { bg: colors.brandLight, color: colors.brand },
  };
  const style = tones[tone] ?? tones.default;
  return (
    <div
      style={{
        background: style.bg,
        border: `1px solid ${colors.border}`,
        borderRadius: 10,
        padding: "14px 16px",
      }}
    >
      <div style={{ fontSize: 11, color: colors.muted, marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700, color: style.color, lineHeight: 1 }}>{value}</div>
    </div>
  );
}

function StatusBadge({ ok, label }) {
  return (
    <span
      style={{
        display: "inline-block",
        fontSize: 10,
        fontWeight: 700,
        padding: "3px 8px",
        borderRadius: 999,
        background: ok ? colors.successLight : colors.dangerLight,
        color: ok ? colors.success : colors.danger,
      }}
    >
      {label}
    </span>
  );
}

function StoryRow({ story, expanded, onToggle }) {
  return (
    <div
      style={{
        border: `1px solid ${colors.border}`,
        borderRadius: 10,
        marginBottom: 10,
        overflow: "hidden",
        background: colors.card,
      }}
    >
      <button
        type="button"
        onClick={onToggle}
        style={{
          width: "100%",
          textAlign: "left",
          border: 0,
          background: expanded ? colors.surface : colors.card,
          padding: "14px 16px",
          cursor: "pointer",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "flex-start" }}>
          <div style={{ minWidth: 0 }}>
            <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", marginBottom: 4 }}>
              <strong style={{ fontSize: 13 }}>{story.storyKey}</strong>
              <StatusBadge ok={story.hasCoverage} label={story.hasCoverage ? "Coverage" : "No coverage"} />
              <StatusBadge ok={story.hasCycles} label={story.hasCycles ? "Cycles" : "No cycles"} />
              {story.fullyTraced && <StatusBadge ok label="Fully traced" />}
            </div>
            <div style={{ fontSize: 12, color: colors.muted, marginBottom: 4 }}>{story.storySummary}</div>
            <div style={{ fontSize: 11, color: colors.muted }}>
              {story.linkedTestCaseCount} linked cases · {story.cycleCount} cycles · {story.executionCount} executions
              {story.storyStatus ? ` · Jira: ${story.storyStatus}` : ""}
            </div>
          </div>
          <span style={{ color: colors.brand, fontSize: 12, fontWeight: 600, whiteSpace: "nowrap" }}>
            {expanded ? "Hide" : "Details"} {expanded ? "▴" : "▾"}
          </span>
        </div>
      </button>

      {expanded && (
        <div style={{ padding: "0 16px 16px", borderTop: `1px solid ${colors.border}` }}>
          {story.gaps?.length > 0 && (
            <div style={{ marginTop: 12 }}>
              <Alert type="info">
                <div style={{ fontWeight: 600, marginBottom: 4 }}>Gaps</div>
                <ul style={{ margin: 0, paddingLeft: 18 }}>
                  {story.gaps.map((gap) => <li key={gap}>{gap}</li>)}
                </ul>
              </Alert>
            </div>
          )}

          <div style={{ marginTop: 14 }}>
            <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Linked test cases</div>
            {story.linkedTestCases?.length ? (
              <div style={{ display: "grid", gap: 6 }}>
                {story.linkedTestCases.map((testCase) => (
                  <div
                    key={testCase.key}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      gap: 10,
                      alignItems: "center",
                      fontSize: 12,
                      padding: "8px 10px",
                      background: colors.surface,
                      borderRadius: 8,
                    }}
                  >
                    <div>
                      <strong>{testCase.key}</strong>
                      {testCase.name ? ` — ${testCase.name}` : ""}
                    </div>
                    <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                      <StatusBadge ok={testCase.inCycle} label={testCase.inCycle ? "In cycle" : "Not in cycle"} />
                      {testCase.url && (
                        <a href={testCase.url} target="_blank" rel="noreferrer">Open ↗</a>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ fontSize: 12, color: colors.muted, margin: 0 }}>No test cases linked to this story.</p>
            )}
          </div>

          <div style={{ marginTop: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Test cycles</div>
            {story.testCycles?.length ? (
              <div style={{ display: "grid", gap: 10 }}>
                {story.testCycles.map((cycle) => (
                  <div
                    key={cycle.key || cycle.id}
                    style={{
                      border: `1px solid ${colors.border}`,
                      borderRadius: 8,
                      padding: 12,
                      background: colors.surface,
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 10, marginBottom: 8 }}>
                      <div style={{ fontSize: 12 }}>
                        <strong>{cycle.name || cycle.key}</strong>
                        <div style={{ color: colors.muted, marginTop: 2 }}>
                          {cycle.key || cycle.id} · {cycle.status || "—"} · {cycle.executionCount} execution(s)
                        </div>
                      </div>
                      {cycle.url && (
                        <a href={cycle.url} target="_blank" rel="noreferrer" style={{ fontSize: 12, whiteSpace: "nowrap" }}>
                          Open cycle ↗
                        </a>
                      )}
                    </div>
                    {cycle.executions?.length > 0 && (
                      <div style={{ display: "grid", gap: 4 }}>
                        {cycle.executions.map((execution) => (
                          <div
                            key={`${cycle.key}-${execution.testCaseKey}`}
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              gap: 8,
                              fontSize: 11,
                              padding: "6px 8px",
                              background: colors.card,
                              borderRadius: 6,
                            }}
                          >
                            <span>
                              <code>{execution.testCaseKey}</code>
                              {execution.testCaseName ? ` — ${execution.testCaseName}` : ""}
                            </span>
                            <span style={{ color: colors.muted }}>{execution.status || "—"}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ fontSize: 12, color: colors.muted, margin: 0 }}>No test cycles found for this story.</p>
            )}
          </div>

          {story.jiraUrl && (
            <div style={{ marginTop: 12 }}>
              <a href={story.jiraUrl} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>
                Open {story.storyKey} in Jira ↗
              </a>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function TraceabilityPage() {
  const [issueKey, setIssueKey] = useState("");
  const [dashboard, setDashboard] = useState(null);
  const [expandedStory, setExpandedStory] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const loadDashboard = async () => {
    setLoading(true);
    setError("");
    setDashboard(null);
    setExpandedStory(null);
    try {
      const data = await api.traceability.dashboard(issueKey.trim().toUpperCase());
      if (!data.success) throw new Error(data.error || "Failed to load traceability");
      setDashboard(data);
      if (data.stories?.length === 1) {
        setExpandedStory(data.stories[0].storyKey);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const coveragePct = dashboard?.totalStories
    ? Math.round((dashboard.storiesWithCoverage / dashboard.totalStories) * 100)
    : 0;
  const cyclePct = dashboard?.totalStories
    ? Math.round((dashboard.storiesWithCycles / dashboard.totalStories) * 100)
    : 0;
  const tracedPct = dashboard?.totalStories
    ? Math.round((dashboard.storiesFullyTraced / dashboard.totalStories) * 100)
    : 0;

  return (
    <>
      <Card title="Load traceability" step={1}>
        <p style={{ marginTop: 0, color: colors.muted, fontSize: 12, lineHeight: 1.6 }}>
          Enter a change request or story key. TestCraft loads linked stories, Zephyr coverage links,
          test cycles, and cycle executions, then highlights gaps.
        </p>
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <input
            aria-label="Issue key"
            style={{ ...inputStyle, flex: 1 }}
            value={issueKey}
            onChange={(e) => setIssueKey(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === "Enter" && loadDashboard()}
            placeholder="e.g. KAN-7"
          />
          <Button primary disabled={loading || !issueKey.trim()} onClick={loadDashboard}>
            {loading ? "Loading…" : "Load dashboard"}
          </Button>
        </div>
        {error && <Alert>{error}</Alert>}
      </Card>

      {dashboard && (
        <>
          <Card title="Release overview" step={2}>
            <div style={{ marginBottom: 14 }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>{dashboard.issueKey}</div>
              <div style={{ fontSize: 12, color: colors.muted, marginTop: 4 }}>
                {dashboard.issueSummary}
                {dashboard.issueStatus ? ` · ${dashboard.issueStatus}` : ""}
              </div>
              {dashboard.jiraUrl && (
                <a href={dashboard.jiraUrl} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>
                  Open in Jira ↗
                </a>
              )}
            </div>

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(130px, 1fr))",
                gap: 10,
                marginBottom: 14,
              }}
            >
              <StatCard label="Linked stories" value={dashboard.totalStories} tone="brand" />
              <StatCard label="With coverage" value={`${dashboard.storiesWithCoverage} (${coveragePct}%)`} tone={coveragePct === 100 ? "success" : "default"} />
              <StatCard label="With cycles" value={`${dashboard.storiesWithCycles} (${cyclePct}%)`} tone={cyclePct === 100 ? "success" : "default"} />
              <StatCard label="Fully traced" value={`${dashboard.storiesFullyTraced} (${tracedPct}%)`} tone={tracedPct === 100 ? "success" : "danger"} />
              <StatCard label="Linked test cases" value={dashboard.totalLinkedTestCases} />
              <StatCard label="Executions" value={dashboard.totalExecutions} />
            </div>

            {dashboard.gaps?.length > 0 ? (
              <Alert type="info">
                <div style={{ fontWeight: 600, marginBottom: 6 }}>Release gaps</div>
                <ul style={{ margin: 0, paddingLeft: 18, fontSize: 12 }}>
                  {dashboard.gaps.map((gap) => <li key={gap}>{gap}</li>)}
                </ul>
              </Alert>
            ) : (
              <Alert type="success">All linked stories have coverage, cycles, and executions.</Alert>
            )}
          </Card>

          <Card title="Story traceability" step={3}>
            <p style={{ marginTop: 0, color: colors.muted, fontSize: 12 }}>
              {dashboard.message}
            </p>
            {(dashboard.stories ?? []).map((story) => (
              <StoryRow
                key={story.storyKey}
                story={story}
                expanded={expandedStory === story.storyKey}
                onToggle={() => setExpandedStory(
                  expandedStory === story.storyKey ? null : story.storyKey,
                )}
              />
            ))}
          </Card>
        </>
      )}
    </>
  );
}
