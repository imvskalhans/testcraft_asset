import { colors } from "../../constants/theme";
import { inputStyle, labelStyle, buttonStyle } from "../../styles/forms";
import { PRIORITIES, STATUSES } from "../../constants/options";

export default function TestCaseEditor({
  testCase,
  index,
  expanded,
  onToggle,
  onChange,
  onStepChange,
  onAddStep,
  onRemoveStep,
  onRemove,
  defaultStatus,
  testTypes,
}) {
  return (
    <div style={{ border: `1px solid ${colors.border}`, borderRadius: 10, marginBottom: 10, overflow: "hidden" }}>
      <button
        type="button"
        onClick={onToggle}
        style={{
          width: "100%",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          padding: "10px 12px",
          border: "none",
          background: expanded ? colors.brandLight : colors.surface,
          cursor: "pointer",
          textAlign: "left",
        }}
      >
        <span style={{ fontSize: 13, fontWeight: 600 }}>
          {index + 1}. {testCase.testName || "Untitled test case"}
        </span>
        <span style={{ fontSize: 11, color: colors.muted }}>{expanded ? "▲" : "▼"}</span>
      </button>
      {expanded && (
        <div style={{ padding: 12, display: "grid", gap: 10 }}>
          <label style={labelStyle}>
            Test name
            <input style={inputStyle} value={testCase.testName || ""} onChange={(e) => onChange("testName", e.target.value)} />
          </label>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10 }}>
            <label style={labelStyle}>
              Type
              <select style={inputStyle} value={testCase.testType || testTypes[0]} onChange={(e) => onChange("testType", e.target.value)}>
                {testTypes.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </label>
            <label style={labelStyle}>
              Priority
              <select style={inputStyle} value={testCase.priority || "Normal"} onChange={(e) => onChange("priority", e.target.value)}>
                {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </label>
            <label style={labelStyle}>
              Status
              <select style={inputStyle} value={testCase.status || defaultStatus} onChange={(e) => onChange("status", e.target.value)}>
                {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </label>
          </div>
          <label style={labelStyle}>
            Objective
            <textarea style={{ ...inputStyle, minHeight: 60 }} value={testCase.objective || ""} onChange={(e) => onChange("objective", e.target.value)} />
          </label>
          <label style={labelStyle}>
            Precondition
            <textarea style={{ ...inputStyle, minHeight: 50 }} value={testCase.preCondition || ""} onChange={(e) => onChange("preCondition", e.target.value)} />
          </label>
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
              <span style={labelStyle}>Steps</span>
              <button type="button" style={buttonStyle(false)} onClick={onAddStep}>+ Add step</button>
            </div>
            {(testCase.steps || []).map((step, stepIndex) => (
              <div key={stepIndex} style={{ display: "flex", gap: 8, marginBottom: 6 }}>
                <span style={{ fontSize: 12, color: colors.muted, paddingTop: 10, minWidth: 20 }}>{stepIndex + 1}.</span>
                <textarea
                  style={{ ...inputStyle, flex: 1, minHeight: 44 }}
                  value={step}
                  onChange={(e) => onStepChange(stepIndex, e.target.value)}
                  placeholder={`Step ${stepIndex + 1}`}
                />
                <button type="button" style={{ ...buttonStyle(false), color: colors.danger }} onClick={() => onRemoveStep(stepIndex)}>✕</button>
              </div>
            ))}
          </div>
          <label style={labelStyle}>
            Expected result
            <textarea style={{ ...inputStyle, minHeight: 50 }} value={testCase.expectedResult || ""} onChange={(e) => onChange("expectedResult", e.target.value)} />
          </label>
          <button type="button" style={{ ...buttonStyle(false), color: colors.danger, justifySelf: "start" }} onClick={onRemove}>
            Remove test case
          </button>
        </div>
      )}
    </div>
  );
}
