import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Chip from "../components/ui/Chip";
import TestCaseList from "../components/testcases/TestCaseList";
import TestCaseImportExport from "../components/testcases/TestCaseImportExport";
import PromptTemplatePicker from "../components/generate/PromptTemplatePicker";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { TEST_TYPES, COUNT_PRESETS } from "../constants/options";
import { useApp } from "../context/AppContext";
import { useState } from "react";
import AttachmentPicker from "../components/ai/AttachmentPicker";
import SkippedAttachmentsNotice from "../components/ai/SkippedAttachmentsNotice";

export default function GeneratePage() {
  const {
    issueKey, ownerName, loading, doGenerate,
    testCount, setTestCount, testType, setTestType,
    promptType, setPromptType, customPrompt, setCustomPrompt,
    additionalPrompt, setAdditionalPrompt,
    savedPromptTemplates, saveCurrentPromptTemplate, removePromptTemplate,
    importTestCases,
    testCases, expandedCase, setExpandedCase, navigate, configStatus, story,
    updateTestCase, updateStep, addStep, removeStep, removeTestCase, defaultStatus,
  } = useApp();
  const [attachments, setAttachments] = useState([]);

  return (
    <Card title="Generate AI test cases" actions={(
      <div style={{ display: "flex", gap: 8 }}>
        {testCases.length > 0 && <Button onClick={() => navigate("publish")}>Continue to Publish & Link →</Button>}
        <Button primary disabled={loading} onClick={() => doGenerate(attachments)}>Generate</Button>
      </div>
    )}>
      <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Uses story {issueKey}. Review generated cases here, then continue to publish them to Zephyr Scale. Owner: {ownerName}.</p>
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

      <PromptTemplatePicker
        promptType={promptType}
        setPromptType={setPromptType}
        customPrompt={customPrompt}
        setCustomPrompt={setCustomPrompt}
        additionalPrompt={additionalPrompt}
        setAdditionalPrompt={setAdditionalPrompt}
        savedTemplates={savedPromptTemplates}
        onSaveTemplate={saveCurrentPromptTemplate}
        onDeleteTemplate={removePromptTemplate}
      />

      <AttachmentPicker
        attachments={attachments}
        setAttachments={setAttachments}
        supportsImageInput={Boolean(configStatus?.ai?.imageInputSupported)}
      />

      <TestCaseImportExport
        testCases={testCases}
        issueKey={issueKey}
        testType={testType}
        onImport={importTestCases}
        disabled={loading}
      />

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
      {testCases.length > 0 && (
        <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}>
          <Button primary onClick={() => navigate("publish")}>Review and publish →</Button>
        </div>
      )}
      {testCases.length > 0 && <div style={{ marginTop: 14 }}>
        <SkippedAttachmentsNotice
          attachments={story?.attachments}
          supportsImageInput={Boolean(configStatus?.ai?.imageInputSupported)}
        />
      </div>}
    </Card>
  );
}
