import { useMemo, useState } from "react";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { useApp } from "../context/AppContext";
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
      className="glass-card"
      style={{
        borderRadius: 12,
        padding: "14px 16px",
      }}
    >
      <div style={{ fontSize: 11, color: colors.muted, marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700, color: style.color, lineHeight: 1 }}>{value}</div>
    </div>
  );
}

function StatusBadge({ ok, label, tone }) {
  const success = tone ? tone === "success" : ok;
  const danger = tone === "danger" || (!tone && !ok);
  const warning = tone === "warning";
  return (
    <span
      style={{
        display: "inline-block",
        fontSize: 10,
        fontWeight: 700,
        padding: "3px 8px",
        borderRadius: 999,
        background: danger ? colors.dangerLight : warning ? "#FFF6E5" : success ? colors.successLight : colors.surface,
        color: danger ? colors.danger : warning ? "#8A5B00" : success ? colors.success : colors.muted,
      }}
    >
      {label}
    </span>
  );
}

function executionTone(status) {
  const value = String(status || "").toLowerCase();
  if (value.includes("pass") || value === "ok" || value === "done") return "success";
  if (value.includes("fail")) return "danger";
  if (value.includes("block") || value.includes("wip")) return "warning";
  return "";
}

function csvEscape(value) {
  const text = String(value ?? "");
  if (/[",\n]/.test(text)) return `"${text.replace(/"/g, '""')}"`;
  return text;
}

function exportDashboardCsv(dashboard) {
  const headers = [
    "Work item", "Type", "Status", "Linked cases", "Cycles", "Executions",
    "Passed", "Failed", "Blocked", "Not executed", "Fully traced", "Gaps",
  ];
  const rows = (dashboard.stories ?? []).map((story) => [
    story.storyKey,
    story.issueType,
    story.storyStatus,
    story.linkedTestCaseCount,
    story.cycleCount,
    story.executionCount,
    story.passedCount ?? 0,
    story.failedCount ?? 0,
    story.blockedCount ?? 0,
    story.notExecutedCount ?? 0,
    story.fullyTraced ? "Yes" : "No",
    (story.gaps ?? []).join("; "),
  ]);
  const csv = [headers, ...rows].map((row) => row.map(csvEscape).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${dashboard.issueKey || "traceability"}-coverage.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function coverageTone(value) {
  const coverage = String(value || "").toLowerCase();
  if (coverage === "covered") return "success";
  if (coverage === "partial") return "warning";
  return "danger";
}

function AiCoveragePanel({ analysis, storyKey, onGenerateRecommended }) {
  if (!analysis) return null;
  const recommended = analysis.recommendedTestCases ?? [];
  return (
    <div style={{ marginTop: 14 }}>
      <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>AI coverage analysis</div>
      {analysis.mockMode && (
        <p style={{ fontSize: 11, color: colors.muted, margin: "0 0 8px" }}>
          Mock or heuristic analysis. Configure a live AI provider for a full requirements matrix.
        </p>
      )}
      {analysis.error && <div style={{ marginBottom: 8 }}><Alert>{analysis.error}</Alert></div>}
      {analysis.summary && (
        <p style={{ fontSize: 12, lineHeight: 1.55, margin: "0 0 10px" }}>{analysis.summary}</p>
      )}
      {analysis.mappings?.length > 0 && (
        <div style={{ display: "grid", gap: 6, marginBottom: 10 }}>
          {analysis.mappings.map((mapping, index) => (
            <div key={`${mapping.requirement}-${index}`} style={{ fontSize: 12, padding: "8px 10px", background: colors.surface, borderRadius: 8 }}>
              <StatusBadge
                tone={coverageTone(mapping.coverage)}
                ok={mapping.coverage === "covered"}
                label={mapping.coverage === "covered" ? "Covered" : mapping.coverage === "partial" ? "Partial" : "Missing"}
              />
              <div style={{ marginTop: 6 }}>{mapping.requirement}</div>
              {mapping.testCases?.length > 0 && (
                <div style={{ marginTop: 4, color: colors.muted, fontSize: 11 }}>
                  Linked cases: {mapping.testCases.join(", ")}
                </div>
              )}
              {mapping.notes && <div style={{ marginTop: 4, color: colors.muted, fontSize: 11 }}>{mapping.notes}</div>}
            </div>
          ))}
        </div>
      )}
      {analysis.criticalGaps?.length > 0 && (
        <Alert type="info">
          <div style={{ fontWeight: 600, marginBottom: 4 }}>AI critical gaps</div>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {analysis.criticalGaps.map((gap) => <li key={gap}>{gap}</li>)}
          </ul>
        </Alert>
      )}
      {recommended.length > 0 && (
        <div style={{ marginTop: 10 }}>
          <div style={{ display: "flex", justifyContent: "space-between", gap: 8, alignItems: "center", marginBottom: 8 }}>
            <div style={{ fontSize: 12, fontWeight: 700 }}>Recommended new test cases</div>
            <Button onClick={() => onGenerateRecommended(storyKey, recommended)}>Generate these</Button>
          </div>
          <div style={{ display: "grid", gap: 6 }}>
            {recommended.map((item, index) => (
              <div key={`${item.title}-${index}`} style={{ fontSize: 12, padding: "8px 10px", background: colors.surface, borderRadius: 8 }}>
                <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                  <strong>{item.title}</strong>
                  {item.priority && <StatusBadge tone={item.priority.toLowerCase() === "high" ? "danger" : "warning"} ok={false} label={item.priority} />}
                </div>
                {item.reason && <div style={{ marginTop: 4, color: colors.muted }}>{item.reason}</div>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function StoryRow({ story, expanded, onToggle, onGenerate, onCoverageGap, onCycles, onGenerateRecommended }) {
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
              {story.issueType && <StatusBadge ok label={story.issueType} />}
              <StatusBadge ok={story.hasCoverage} label={story.hasCoverage ? "Coverage" : "No coverage"} />
              <StatusBadge ok={story.hasCycles} label={story.hasCycles ? "Cycles" : "No cycles"} />
              {story.fullyTraced
                ? <StatusBadge ok label="Fully traced" />
                : <StatusBadge ok={false} label="Gaps" />}
            </div>
            <div style={{ fontSize: 12, color: colors.muted, marginBottom: 4 }}>{story.storySummary}</div>
            <div style={{ fontSize: 11, color: colors.muted }}>
              {story.linkedTestCaseCount} linked cases · {story.cycleCount} cycles · {story.executionCount} executions
              {` · Pass ${story.passedCount ?? 0} / Fail ${story.failedCount ?? 0} / Blocked ${story.blockedCount ?? 0} / Not executed ${story.notExecutedCount ?? 0}`}
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
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 12 }}>
            <Button onClick={(event) => { event.stopPropagation(); onGenerate(story.storyKey); }}>Generate cases</Button>
            <Button onClick={(event) => { event.stopPropagation(); onCoverageGap(story.storyKey); }}>AI coverage gap</Button>
            <Button onClick={(event) => { event.stopPropagation(); onCycles(story.storyKey); }}>Test cycles</Button>
          </div>

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

          {story.acceptanceCriteria?.length > 0 && (
            <div style={{ marginTop: 14 }}>
              <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Acceptance criteria</div>
              <div style={{ display: "grid", gap: 6 }}>
                {story.acceptanceCriteria.map((criterion) => {
                  const uncovered = story.uncoveredCriteria?.includes(criterion);
                  return (
                    <div key={criterion} style={{ fontSize: 12, padding: "8px 10px", background: colors.surface, borderRadius: 8 }}>
                      <StatusBadge
                        ok={!uncovered}
                        label={uncovered ? "No title match" : "Possible case match"}
                      />
                      <div style={{ marginTop: 6 }}>{criterion}</div>
                    </div>
                  );
                })}
              </div>
              <p style={{ fontSize: 11, color: colors.muted, margin: "8px 0 0" }}>
                Title matching is a hint only. The AI coverage analysis below maps requirements to linked cases.
              </p>
            </div>
          )}

          <AiCoveragePanel
            analysis={story.aiCoverage}
            storyKey={story.storyKey}
            onGenerateRecommended={onGenerateRecommended}
          />

          <div style={{ marginTop: 14 }}>
            <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 8 }}>Linked test cases</div>
            {story.linkedTestCases?.length ? (
              <>
              <p style={{ fontSize: 11, color: colors.muted, margin: "0 0 8px", lineHeight: 1.5 }}>
                A Jira coverage link (what you see on the story in Jira) is not the same as being in a Zephyr test cycle.
              </p>
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
                      <span
                        title={testCase.inCycle
                          ? "This case appears in a Zephyr test cycle or execution."
                          : "Linked to this Jira work item, but not found in a Zephyr test cycle."}
                      >
                        <StatusBadge ok={testCase.inCycle} label={testCase.inCycle ? "In a test cycle" : "Not in a test cycle"} />
                      </span>
                      {testCase.status && (
                        <StatusBadge
                          tone={executionTone(testCase.status) || undefined}
                          ok={executionTone(testCase.status) === "success"}
                          label={testCase.status}
                        />
                      )}
                      {testCase.url && (
                        <a href={testCase.url} target="_blank" rel="noreferrer">Open ↗</a>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              </>
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
                          {` · Pass ${cycle.passedCount ?? 0} / Fail ${cycle.failedCount ?? 0}`}
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
                            key={`${cycle.key}-${execution.testCaseKey}-${execution.status}`}
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
                            <StatusBadge
                              tone={executionTone(execution.status) || undefined}
                              ok={executionTone(execution.status) === "success"}
                              label={execution.status || "Not Executed"}
                            />
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
  const { issueKey: sessionIssueKey, crKey, openFromTraceability, beginBusyJob, endBusyJob } = useApp();
  const [issueKey, setIssueKey] = useState(sessionIssueKey || crKey || "");
  const [dashboard, setDashboard] = useState(null);
  const [expandedStory, setExpandedStory] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("all");
  const [sortBy, setSortBy] = useState("key");

  const loadDashboard = async () => {
    const jobId = "traceability";
    setLoading(true);
    setError("");
    setDashboard(null);
    setExpandedStory(null);
    beginBusyJob({
      id: jobId,
      title: "Building QA coverage report and AI gap analysis",
      page: "traceability",
      exclusiveAi: true,
    });
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
      endBusyJob(jobId);
    }
  };

  const clearReport = () => {
    setDashboard(null);
    setError("");
    setExpandedStory(null);
    setFilter("all");
    setSortBy("key");
  };

  const visibleStories = useMemo(() => {
    let stories = [...(dashboard?.stories ?? [])];
    if (filter === "gaps") stories = stories.filter((story) => !story.fullyTraced);
    if (filter === "traced") stories = stories.filter((story) => story.fullyTraced);
    if (sortBy === "gaps") {
      stories.sort((a, b) => (b.gaps?.length || 0) - (a.gaps?.length || 0));
    } else if (sortBy === "traced") {
      stories.sort((a, b) => Number(b.fullyTraced) - Number(a.fullyTraced));
    } else {
      stories.sort((a, b) => String(a.storyKey).localeCompare(String(b.storyKey)));
    }
    return stories;
  }, [dashboard, filter, sortBy]);

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
      <Card title="Build a QA coverage report" step={1}>
        <p style={{ marginTop: 0, color: colors.muted, fontSize: 12, lineHeight: 1.6 }}>
          Enter any Jira work-item key — story, change request, epic, task, or sub-task. TestCraft detects its type, checks Jira/Zephyr relationships, then uses AI to map linked cases against the description and acceptance criteria and recommend missing tests.
        </p>
        <div style={{ display: "flex", gap: 8, marginBottom: 12, flexWrap: "wrap" }}>
          <input
            aria-label="Issue key"
            style={{ ...inputStyle, flex: 1, minWidth: 180 }}
            value={issueKey}
            onChange={(e) => setIssueKey(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === "Enter" && loadDashboard()}
            placeholder="e.g. KAN-7"
          />
          <Button primary disabled={loading || !issueKey.trim()} onClick={loadDashboard}>
            {loading ? "Loading…" : dashboard ? "Reload report" : "Load dashboard"}
          </Button>
          {dashboard && (
            <Button disabled={loading} onClick={clearReport}>Clear report</Button>
          )}
        </div>
        {loading && <Alert type="info">Building the traceability report: reading Jira, Zephyr links and cycles, then running AI coverage-gap analysis…</Alert>}
        {error && <Alert>{error}</Alert>}
      </Card>

      {dashboard && (
        <>
          <Card title="Release overview" step={2}>
            <div style={{ marginBottom: 14 }}>
              <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", fontSize: 14, fontWeight: 700 }}>
                {dashboard.issueKey}
                {dashboard.issueType && <StatusBadge ok label={dashboard.issueType} />}
              </div>
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
              <StatCard label="Tracked work items" value={dashboard.totalStories} tone="brand" />
              <StatCard label="With coverage" value={`${dashboard.storiesWithCoverage} (${coveragePct}%)`} tone={coveragePct === 100 ? "success" : "default"} />
              <StatCard label="With cycles" value={`${dashboard.storiesWithCycles} (${cyclePct}%)`} tone={cyclePct === 100 ? "success" : "default"} />
              <StatCard label="Fully traced" value={`${dashboard.storiesFullyTraced} (${tracedPct}%)`} tone={tracedPct === 100 ? "success" : "danger"} />
              <StatCard label="Linked test cases" value={dashboard.totalLinkedTestCases} />
              <StatCard label="Executions" value={dashboard.totalExecutions} />
              <StatCard label="Passed" value={dashboard.totalPassed ?? 0} tone="success" />
              <StatCard label="Failed" value={dashboard.totalFailed ?? 0} tone={(dashboard.totalFailed ?? 0) > 0 ? "danger" : "default"} />
            </div>

            {dashboard.gaps?.length > 0 ? (
              <Alert type="info">
                <div style={{ fontWeight: 600, marginBottom: 6 }}>Release gaps</div>
                <ul style={{ margin: 0, paddingLeft: 18, fontSize: 12 }}>
                  {dashboard.gaps.map((gap) => <li key={gap}>{gap}</li>)}
                </ul>
              </Alert>
            ) : (
              <Alert type="success">All linked stories have coverage, cycles, and passed executions.</Alert>
            )}
          </Card>

          <Card title="Work-item coverage details" step={3}>
            <p style={{ marginTop: 0, color: colors.muted, fontSize: 12 }}>
              {dashboard.message}
            </p>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginBottom: 12 }}>
              <Button onClick={() => setFilter("all")}>{filter === "all" ? "Showing all" : "Show all"}</Button>
              <Button onClick={() => setFilter("gaps")}>{filter === "gaps" ? "Gaps only ✓" : "Gaps only"}</Button>
              <Button onClick={() => setFilter("traced")}>{filter === "traced" ? "Traced only ✓" : "Traced only"}</Button>
              <Button onClick={() => setSortBy(sortBy === "gaps" ? "key" : "gaps")}>
                {sortBy === "gaps" ? "Sorted by gaps" : "Sort by gaps"}
              </Button>
              <Button onClick={() => exportDashboardCsv(dashboard)}>Export CSV</Button>
            </div>
            {visibleStories.map((story) => (
              <StoryRow
                key={story.storyKey}
                story={story}
                expanded={expandedStory === story.storyKey}
                onToggle={() => setExpandedStory(
                  expandedStory === story.storyKey ? null : story.storyKey,
                )}
                onGenerate={(key) => openFromTraceability({ page: "generate", key })}
                onCoverageGap={(key) => openFromTraceability({ page: "ai", key, aiAction: "coverage-gap" })}
                onCycles={(key) => openFromTraceability({ page: "release", key })}
                onGenerateRecommended={(key, recommended) => openFromTraceability({
                  page: "generate",
                  key,
                  additionalPrompt: recommended
                    .map((item, index) => `${index + 1}. ${item.title}${item.reason ? ` — ${item.reason}` : ""}`)
                    .join("\n"),
                })}
              />
            ))}
            {visibleStories.length === 0 && (
              <p style={{ fontSize: 12, color: colors.muted }}>No work items match this filter.</p>
            )}
          </Card>
        </>
      )}
    </>
  );
}
