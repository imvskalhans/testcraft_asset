import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Chip from "../components/ui/Chip";
import Alert from "../components/ui/Alert";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { CYCLE_TYPES } from "../constants/options";
import { useApp } from "../context/AppContext";

export default function ReleasePage() {
  const {
    loading, crKey, setCrKey, release, cycleFetch, cyclesCreated,
    cycleCreateMessage, cycleLinkMessage,
    cycleTypes, toggleCycleType, cycleLinkKeys, setCycleLinkKeys,
    fetchCycles, createCycles, linkCycles,
  } = useApp();

  return (
    <>
      <Card title="Choose a story or change request" step={1}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Enter a Jira story or change request to review its Zephyr coverage. Existing cycles are optional — if none are found, you can create new ones below.</p>
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <input style={{ ...inputStyle, flex: 1 }} value={crKey} onChange={(e) => setCrKey(e.target.value.toUpperCase())} placeholder="Enter a change request key, e.g. KAN-7" aria-label="Change request key" />
          <Button primary disabled={loading} onClick={fetchCycles}>Check Zephyr cycles</Button>
        </div>
        {release && (
          <div style={{ fontSize: 12, marginBottom: 10 }}>
            <p style={{ margin: "0 0 6px" }}><strong>{release.crKey}</strong> — {release.crSummary}</p>
            <p style={{ color: colors.muted, margin: 0 }}>{release.linkedStories?.length ?? 0} linked stories · Status: {release.status}</p>
          </div>
        )}
        {cycleFetch?.storyTestCycles?.length > 0 ? (
          <div style={{ fontSize: 12 }}>
            {(cycleFetch.storyTestCycles ?? []).map((story) => (
              <div key={story.storyKey} style={{ borderTop: `1px solid ${colors.border}`, paddingTop: 8, marginTop: 8 }}>
                <strong>{story.storyKey}</strong> {story.storySummary}
                <ul style={{ margin: "6px 0 0", paddingLeft: 18 }}>
                  {(story.testCycles ?? []).length
                    ? story.testCycles.map((cycle) => (
                      <li key={cycle.id || cycle.key}>{cycle.name || cycle.key} · {cycle.status || "—"} · {cycle.testCaseCount ?? 0} cases</li>
                    ))
                    : <li style={{ color: colors.muted }}>No cycles yet</li>}
                </ul>
              </div>
            ))}
          </div>
        ) : cycleFetch && (
          <p style={{ fontSize: 12, color: colors.muted }}>No existing cycles found. You can create new cycles for this story or change request in the next step.</p>
        )}
      </Card>
      <Card title="Create or reuse test cycles" step={2}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Select one or more types. TestCraft creates a new cycle when needed, or reuses a matching cycle. Cycles are created in Zephyr using a Year → Month → story + type structure.</p>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12 }}>
          {CYCLE_TYPES.map((t) => (
            <Chip key={t} active={cycleTypes.includes(t)} onClick={() => toggleCycleType(t)}>{t}</Chip>
          ))}
        </div>
        <Button primary disabled={loading || !cycleFetch} onClick={createCycles}>Create selected cycle types</Button>
        {cycleCreateMessage && <div style={{ marginTop: 10 }}><Alert type={cycleCreateMessage.type}>{cycleCreateMessage.text}</Alert></div>}
        {cyclesCreated && (
          <div style={{ fontSize: 12, marginTop: 10 }}>
            <p>{cyclesCreated.message}</p>
            {(cyclesCreated.createdCycles ?? []).map((cycle) => (
              <div key={cycle.id} style={{ borderTop: `1px solid ${colors.border}`, padding: "8px 0" }}>
                <strong>{cycle.name}</strong> · {cycle.action || "—"}<br />
                Cycle: <strong><code>{cycle.id}</code></strong> · {cycle.name} · Story: {cycle.storyKey} ·{" "}
                <a href={cycle.zephyrUrl} target="_blank" rel="noreferrer">Open Zephyr Test Cycles</a>{" "}
                <a href={cycle.jiraUrl} target="_blank" rel="noreferrer">Open Jira story</a>
              </div>
            ))}
          </div>
        )}
      </Card>
      <Card title="Attach published tests and link Jira" step={3}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>
          After cycles are created, TestCraft attaches published Zephyr test cases and links the cycle to Jira stories or change requests. Publishing and cycle management happen in Zephyr; Jira stores the related traceability links.
        </p>
        <input
          style={{ ...inputStyle, marginBottom: 10 }}
          value={cycleLinkKeys}
          onChange={(e) => setCycleLinkKeys(e.target.value.toUpperCase())}
          placeholder="KAN-1, KAN-2"
          disabled={!cyclesCreated}
        />
        <Button primary disabled={loading || !cyclesCreated} onClick={linkCycles}>Link to Jira / Zephyr</Button>
        {cycleLinkMessage && <div style={{ marginTop: 10 }}><Alert type={cycleLinkMessage.type}>{cycleLinkMessage.text}</Alert></div>}
      </Card>
    </>
  );
}
