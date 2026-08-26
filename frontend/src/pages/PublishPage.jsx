import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import TestCaseList from "../components/testcases/TestCaseList";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { TEST_TYPES } from "../constants/options";
import { useApp } from "../context/AppContext";

export default function PublishPage() {
  const {
    loading, ownerName, testCases, expandedCase, setExpandedCase,
    updateTestCase, updateStep, addStep, removeStep, removeTestCase, defaultStatus,
    projectError, folderWarning, projects, folders,
    selectedProject, setSelectedProject, selectedFolder, setSelectedFolder,
    publishedKeys, linkIssueKeys, setLinkIssueKeys,
    publishAll, linkPublished,
  } = useApp();

  const folderEntries = Object.entries(folders);

  return (
    <>
      <Card title="Generated test cases to publish" step={1}>
        {!testCases.length && <Alert type="info">Generate test cases first on the Generate Tests page.</Alert>}
        {testCases.length > 0 && (
          <>
            <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Owner: {ownerName}. Edit any field before publishing.</p>
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
            />
          </>
        )}
      </Card>
      <Card title="Where should they be published?" step={2}>
        {projectError && <Alert>{projectError}</Alert>}
        {folderWarning && <Alert type="info">{folderWarning}</Alert>}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
          <label style={{ fontSize: 12 }}>
            Project
            <select style={{ ...inputStyle, marginTop: 4 }} value={selectedProject} onChange={(e) => setSelectedProject(e.target.value)}>
              <option value="">Select a project</option>
              {Object.entries(projects).map(([name, id]) => (
                <option key={id} value={id}>{name}</option>
              ))}
            </select>
          </label>
          <label style={{ fontSize: 12 }}>
            Folder
            <select style={{ ...inputStyle, marginTop: 4 }} value={selectedFolder} onChange={(e) => setSelectedFolder(e.target.value)}>
              <option value="">{folderEntries.length ? "Select a folder" : "No folders found"}</option>
              {folderEntries.map(([name, id]) => (
                <option key={`${name}-${id}`} value={id}>{name}</option>
              ))}
            </select>
          </label>
        </div>
        <div style={{ marginTop: 14 }}>
          <Button primary disabled={loading || !testCases.length} onClick={publishAll}>Publish to Zephyr</Button>
        </div>
      </Card>
      <Card title="Link published tests to stories" step={3}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>
          Linking is available after publish. Enter one or more Jira keys (stories or change tickets).
        </p>
        {publishedKeys.length > 0 && (
          <p style={{ fontSize: 12 }}>Published keys: <strong>{publishedKeys.join(", ")}</strong></p>
        )}
        <input
          style={{ ...inputStyle, marginBottom: 10 }}
          value={linkIssueKeys}
          onChange={(e) => setLinkIssueKeys(e.target.value.toUpperCase())}
          placeholder="KAN-1, KAN-2"
          disabled={!publishedKeys.length}
        />
        <Button primary disabled={loading || !publishedKeys.length} onClick={linkPublished}>Link to stories</Button>
      </Card>
    </>
  );
}
