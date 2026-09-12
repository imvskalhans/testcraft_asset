import { colors } from "../../constants/theme";
import { getNavItem } from "../../constants/navigation";
import Sidebar from "./Sidebar";
import PageHeader from "./PageHeader";
import Alert from "../ui/Alert";
import FeedbackModal from "../support/FeedbackModal";
import Chatbot from "../support/Chatbot";

function statusBanner(page, busyJob) {
  if (!busyJob) return null;
  const source = getNavItem(busyJob.page)?.label || busyJob.page;
  if (page === busyJob.page) {
    return `${busyJob.title}…`;
  }
  if (busyJob.exclusiveAi) {
    return `${busyJob.title} is still running in ${source}. Wait for it to finish before starting another AI request.`;
  }
  return `${busyJob.title} is still running in ${source}.`;
}

export default function AppLayout({ page, onNavigate, currentUser, error, success, loading, busyJob, children, feedbackOpen, onFeedback, onCloseFeedback, showGlobalMessages = true, hasTestCases, onRefresh, refreshTitle, refreshing }) {
  const loadingMessage = statusBanner(page, busyJob);
  return (
    <div
      className="app-shell"
      style={{
        display: "flex",
        minHeight: "100vh",
        fontFamily: "'DM Sans', system-ui, sans-serif",
        color: colors.text,
      }}
    >
      <Sidebar page={page} onNavigate={onNavigate} currentUser={currentUser} onFeedback={onFeedback} hasTestCases={hasTestCases} />
      <main className="app-main" style={{ flex: 1, padding: 24, overflow: "auto", maxWidth: 980 }}>
        {loading && <div className="app-loading-bar" aria-hidden="true" />}
        <PageHeader
          page={page}
          onRefresh={onRefresh}
          refreshTitle={refreshTitle}
          refreshing={refreshing}
        />
        {showGlobalMessages && error && <Alert>{error}</Alert>}
        {showGlobalMessages && success && <Alert type="success">{success}</Alert>}
        {children}
        {loadingMessage && <div className="app-loading-status" role="status"><span className="app-loading-dot" /> {loadingMessage}</div>}
      </main>
      <Chatbot />
      {feedbackOpen && <FeedbackModal currentUser={currentUser} onClose={onCloseFeedback} />}
    </div>
  );
}
