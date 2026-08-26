import TestCaseEditor from "./TestCaseEditor";

export default function TestCaseList({
  testCases,
  expandedCase,
  setExpandedCase,
  updateTestCase,
  updateStep,
  addStep,
  removeStep,
  removeTestCase,
  defaultStatus,
  testTypes,
  hint,
}) {
  if (!testCases.length) return null;
  return (
    <div style={{ marginTop: 12 }}>
      {hint && <p style={{ fontSize: 12, color: "#6B6A66", marginTop: 0 }}>{hint}</p>}
      {testCases.map((tc, i) => (
        <TestCaseEditor
          key={i}
          testCase={tc}
          index={i}
          expanded={expandedCase === i}
          onToggle={() => setExpandedCase(expandedCase === i ? -1 : i)}
          onChange={(field, value) => updateTestCase(i, field, value)}
          onStepChange={(stepIndex, value) => updateStep(i, stepIndex, value)}
          onAddStep={() => addStep(i)}
          onRemoveStep={(stepIndex) => removeStep(i, stepIndex)}
          onRemove={() => removeTestCase(i)}
          defaultStatus={defaultStatus}
          testTypes={testTypes}
        />
      ))}
    </div>
  );
}
