import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import TestCaseList from "../components/testcases/TestCaseList";
import PublishProgress from "../components/publish/PublishProgress";
import { inputStyle } from "../styles/forms";
import { colors } from "../constants/theme";
import { TEST_TYPES } from "../constants/options";
import { useApp } from "../context/AppContext";

export default function PublishPage() {
  const {
    loading, publishing, ownerName, testCases, expandedCase, setExpandedCase, story,
    updateTestCase, updateStep, addStep, removeStep, removeTestCase, defaultStatus,
    projectError, folderError, folderWarning, projects, folders,
    selectedProject, setSelectedProject, selectedFolder, setSelectedFolder,
    publishedKeys, publishedTestCaseLinks, linkIssueKeys, setLinkIssueKeys,
    publishAll, retryFailedPublishes, publishProgress, linkPublished,
    publishMessage, linkMessage, navigate,
  } = useApp();

  const folderEntries = Object.entries(folders);

  return (
    <>
      <Card title="Publish generated cases to Zephyr" step={1} actions={<Button onClick={() => navigate("generate")}>← Generate AI Test Cases</Button>}>
        {!testCases.length && (
          <Alert type="info">Generate or import test cases first. <Button onClick={() => navigate("generate")}>Go to Generate AI Test Cases</Button></Alert>
        )}
        {testCases.length > 0 && (
          <>
            <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>Owner: {ownerName}. Review and edit any field before publishing to Zephyr Scale. TestCraft currently publishes test cases to Zephyr only.</p>
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
        {folderError && <Alert>{folderError}</Alert>}
        {folderWarning && <Alert type="info">{folderWarning}</Alert>}
        <div className="form-grid" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
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
          <Button primary disabled={loading || publishing || !testCases.length} onClick={publishAll}>
            {publishing ? "Publishing to Zephyr…" : "Publish to Zephyr Scale"}
          </Button>
        </div>
        <PublishProgress
          progress={publishProgress}
          publishing={publishing}
          onRetryFailed={retryFailedPublishes}
        />
        {publishMessage && <div style={{ marginTop: 10 }}><Alert type={publishMessage.type}>{publishMessage.text}</Alert></div>}
      </Card>
      <Card title="Link published tests to stories" step={3}>
        <p style={{ fontSize: 12, color: colors.muted, marginTop: 0 }}>
          Linking is available after publish. Enter one or more Jira keys (stories or change tickets).
          Each Zephyr trace link is bidirectional, so it appears on both the test case and Jira issue.
        </p>
        {story && <div style={{ background: colors.surface, borderRadius: 8, padding: 10, marginBottom: 10, fontSize: 12 }}>
          <strong>{story.key || "Fetched story"}</strong> — {story.summary || "Fetched Jira story"}
        </div>}
        {publishedKeys.length > 0 && (
          <p style={{ fontSize: 12 }}>Published keys: {(publishedTestCaseLinks.length ? publishedTestCaseLinks : publishedKeys.map((key) => ({ key }))).map(({ key, url }) => (
            <span key={key} style={{ marginRight: 8 }}>
              {url ? <a href={url} target="_blank" rel="noreferrer"><strong>{key}</strong></a> : <strong>{key}</strong>}
            </span>
          ))}</p>
        )}
        <input
          style={{ ...inputStyle, marginBottom: 10 }}
          value={linkIssueKeys}
          onChange={(e) => setLinkIssueKeys(e.target.value.toUpperCase())}
          placeholder="KAN-1, KAN-2"
          disabled={!publishedKeys.length}
        />
        <Button primary disabled={loading || publishing || !publishedKeys.length} onClick={linkPublished}>Link to stories</Button>
        {linkMessage && <div style={{ marginTop: 10 }}><Alert type={linkMessage.type}>{linkMessage.text}</Alert></div>}
      </Card>
    </>
  );
}
