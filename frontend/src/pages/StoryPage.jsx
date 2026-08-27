import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import { inputStyle } from "../styles/forms";
import { useApp } from "../context/AppContext";

export default function StoryPage() {
  const { issueKey, setIssueKey, story, storyText, comment, setComment, loading, fetchStory, postComment } = useApp();

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
    </>
  );
}
