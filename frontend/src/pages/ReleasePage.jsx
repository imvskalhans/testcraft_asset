import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Chip from "../components/ui/Chip";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { CYCLE_TYPES } from "../constants/options";
import { useApp } from "../context/AppContext";

export default function ReleasePage() {
  const {
    loading, crKey, setCrKey, release, cycleFetch, cyclesCreated,
    cycleTypes, toggleCycleType, cycleLinkKeys, setCycleLinkKeys,
    fetchCycles, createCycles, linkCycles,
  } = useApp();

  return (
    <>
      <Card title="Fetch existing test cycles" step={1}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Start by loading the story/CR and any cycles already in Zephyr.</p>
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <input style={{ ...inputStyle, flex: 1 }} value={crKey} onChange={(e) => setCrKey(e.target.value.toUpperCase())} placeholder="KAN-1" />
          <Button primary disabled={loading} onClick={fetchCycles}>Fetch cycles</Button>
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
          <p style={{ fontSize: 12, color: colors.muted }}>No existing cycles found for this key.</p>
        )}
      </Card>
      <Card title="Create test cycles" step={2}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Select every cycle type you need. Folders are created as Year → Month → story + type.</p>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12 }}>
          {CYCLE_TYPES.map((t) => (
            <Chip key={t} active={cycleTypes.includes(t)} onClick={() => toggleCycleType(t)}>{t}</Chip>
          ))}
        </div>
        <Button primary disabled={loading || !cycleFetch} onClick={createCycles}>Create selected cycle types</Button>
        {cyclesCreated && <p style={{ fontSize: 12, marginTop: 10 }}>{cyclesCreated.message}</p>}
      </Card>
      <Card title="Link test cases, stories, and change tickets" step={3}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>
          After cycles are created, link published tests and Jira keys (stories / CRs) in Zephyr.
        </p>
        <input
          style={{ ...inputStyle, marginBottom: 10 }}
          value={cycleLinkKeys}
          onChange={(e) => setCycleLinkKeys(e.target.value.toUpperCase())}
          placeholder="KAN-1, KAN-2"
          disabled={!cyclesCreated}
        />
        <Button primary disabled={loading || !cyclesCreated} onClick={linkCycles}>Link to Jira / Zephyr</Button>
      </Card>
    </>
  );
}
