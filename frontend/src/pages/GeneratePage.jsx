import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Chip from "../components/ui/Chip";
import TestCaseList from "../components/testcases/TestCaseList";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { TEST_TYPES, COUNT_PRESETS, PROMPT_TYPES } from "../constants/options";
import { useApp } from "../context/AppContext";

export default function GeneratePage() {
  const {
    issueKey, ownerName, loading, doGenerate,
    testCount, setTestCount, testType, setTestType,
    promptType, setPromptType, customPrompt, setCustomPrompt,
    testCases, expandedCase, setExpandedCase,
    updateTestCase, updateStep, addStep, removeStep, removeTestCase, defaultStatus,
  } = useApp();

  return (
    <Card title="Generate test cases" actions={<Button primary disabled={loading} onClick={doGenerate}>Generate</Button>}>
      <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Uses story {issueKey}. Owner for later publish: {ownerName}.</p>
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>How many test cases?</div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          {COUNT_PRESETS.map((n) => (
            <Chip key={n} active={Number(testCount) === n} onClick={() => setTestCount(n)}>{n}</Chip>
          ))}
          <label style={{ fontSize: 12, color: colors.muted, display: "flex", alignItems: "center", gap: 8 }}>
            Or type
            <input
              type="number"
              min={1}
              max={50}
              style={{ ...inputStyle, width: 80 }}
              value={testCount}
              onChange={(e) => setTestCount(e.target.value === "" ? "" : Math.max(1, Number(e.target.value)))}
            />
          </label>
        </div>
      </div>
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Test type</div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          {TEST_TYPES.map((t) => (
            <Chip key={t} active={testType === t} onClick={() => setTestType(t)}>{t}</Chip>
          ))}
        </div>
      </div>
      <div className="form-grid" style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10, marginBottom: 12 }}>
        {PROMPT_TYPES.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setPromptType(t.id)}
            style={{
              textAlign: "left",
              padding: 12,
              borderRadius: 10,
              border: `1px solid ${promptType === t.id ? colors.brand : colors.border}`,
              background: promptType === t.id ? colors.brandLight : "#fff",
              cursor: "pointer",
            }}
          >
            <div style={{ fontSize: 13, fontWeight: 700, color: promptType === t.id ? colors.brand : colors.text }}>{t.label}</div>
            <div style={{ fontSize: 11, color: colors.muted, marginTop: 4 }}>{t.hint}</div>
          </button>
        ))}
      </div>
      {promptType === "custom" && (
        <textarea
          style={{ ...inputStyle, minHeight: 80, marginBottom: 12 }}
          value={customPrompt}
          onChange={(e) => setCustomPrompt(e.target.value)}
          placeholder="Describe extra coverage, data, or environments to include…"
        />
      )}
      <TestCaseList
        testCases={testCases}
        expandedCase={expandedCase}
        setExpandedCase={setExpandedCase}
        updateTestCase={updateTestCase}
        updateStep={updateStep}
        addStep={addStep}
        removeStep={removeStep}
        removeTestCase={removeTestCase}
        defaultStatus={defaultStatus}
        testTypes={TEST_TYPES}
        hint="Review and edit each test case below. Changes are kept when you go to Publish & Link."
      />
    </Card>
  );
}
