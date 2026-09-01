import { colors } from "../../constants/theme";
import Sidebar from "./Sidebar";
import PageHeader from "./PageHeader";
import Alert from "../ui/Alert";
import FeedbackModal from "../support/FeedbackModal";
import Chatbot from "../support/Chatbot";

export default function AppLayout({ page, onNavigate, currentUser, error, success, loading, aiLoading, children, feedbackOpen, onFeedback, onCloseFeedback, showGlobalMessages = true, hasTestCases }) {
  const loadingMessage = aiLoading
    ? "AI is analyzing your request…"
    : page === "generate"
      ? "Generating test cases with AI…"
      : page === "story"
        ? "Fetching Jira issue…"
        : page === "publish"
          ? "Publishing test cases to Zephyr…"
          : page === "release"
            ? "Working with Zephyr test cycles…"
            : "Loading workspace data…";
  return (
    <div
      className="app-shell"
      style={{
        display: "flex",
        minHeight: "100vh",
        fontFamily: "'DM Sans', system-ui, sans-serif",
        background: colors.bg,
        color: colors.text,
      }}
    >
      <Sidebar page={page} onNavigate={onNavigate} currentUser={currentUser} onFeedback={onFeedback} hasTestCases={hasTestCases} />
      <main className="app-main" style={{ flex: 1, padding: 24, overflow: "auto", maxWidth: 980 }}>
        {loading && <div className="app-loading-bar" aria-hidden="true" />}
        <PageHeader page={page} />
        {showGlobalMessages && error && <Alert>{error}</Alert>}
        {showGlobalMessages && success && <Alert type="success">{success}</Alert>}
        {children}
        {(loading || aiLoading) && <div className="app-loading-status" role="status"><span className="app-loading-dot" /> {loadingMessage}</div>}
      </main>
      <Chatbot />
      {feedbackOpen && <FeedbackModal currentUser={currentUser} onClose={onCloseFeedback} />}
    </div>
  );
}
