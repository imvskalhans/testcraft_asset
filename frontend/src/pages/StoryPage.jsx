import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { inputStyle } from "../styles/forms";
import { useApp } from "../context/AppContext";

export default function StoryPage() {
  const { issueKey, setIssueKey, story, storyText, comment, setComment, loading, fetchStory, postComment, navigate } = useApp();

  return (
    <>
      <Card title="Fetch Jira issue">
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <input style={{ ...inputStyle, flex: 1 }} value={issueKey} onChange={(e) => setIssueKey(e.target.value.toUpperCase())} placeholder="Enter a Jira issue key, e.g. KAN-7" aria-label="Jira issue key" />
          <Button primary disabled={loading} onClick={fetchStory}>Fetch</Button>
        </div>
        {story && (
          <div style={{ background: "#F7F8FA", borderRadius: 8, padding: 12, fontSize: 12, whiteSpace: "pre-wrap" }}>
            {storyText}
          </div>
        )}
      </Card>
      <Card title="Post comment to Jira">
        <textarea style={{ ...inputStyle, minHeight: 80, marginBottom: 8 }} value={comment} onChange={(e) => setComment(e.target.value)} placeholder="QA review comment..." />
        <Button primary disabled={loading || !comment.trim()} onClick={postComment}>Post comment</Button>
      </Card>
      {story?.attachments?.length > 0 && (
        <Card title="Jira attachments">
          <p style={{ margin: "0 0 10px", fontSize: 12, color: "#6B6A66" }}>
            These files were fetched with the issue. Supported image and text files can be included automatically in Generate AI Test Cases and AI Workspace context.
          </p>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            {story.attachments.map((attachment, index) => (
              <div key={`${attachment.name}-${index}`} style={{ width: 150, padding: 8, border: "1px solid #E4E2DD", borderRadius: 8, fontSize: 11 }}>
                {attachment.data && attachment.mimeType?.startsWith("image/") && <img src={`data:${attachment.mimeType};base64,${attachment.data}`} alt={attachment.name} style={{ width: "100%", height: 76, objectFit: "cover", borderRadius: 5, marginBottom: 6 }} />}
                <div style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={attachment.name}>{attachment.name}</div>
                <div style={{ color: "#6B6A66", marginTop: 3 }}>{attachment.mimeType}</div>
                {!attachment.data && <div style={{ color: "#9A3412", marginTop: 3 }}>Not downloaded</div>}
              </div>
            ))}
          </div>
        </Card>
      )}
      <Card title="Continue your QA workflow">
        <p style={{ margin: "0 0 10px", fontSize: 12, color: "#6B6A66" }}>
          Use the fetched story as context for generating focused test cases, then review and publish them to Zephyr Scale.
        </p>
        <Button primary disabled={!story} onClick={() => navigate("generate")}>Generate AI test cases →</Button>
      </Card>
    </>
  );
}
