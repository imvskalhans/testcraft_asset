import AppLayout from "./components/layout/AppLayout";
import { AppProvider, useApp } from "./context/AppContext";
import HomePage from "./pages/HomePage";
import StoryPage from "./pages/StoryPage";
import GeneratePage from "./pages/GeneratePage";
import PublishPage from "./pages/PublishPage";
import ReleasePage from "./pages/ReleasePage";
import TraceabilityPage from "./pages/TraceabilityPage";
import AiPage from "./pages/AiPage";
import SettingsPage from "./pages/SettingsPage";
import PRReviewPage from "./pages/PRReviewPage";
import JenkinsLogPage from "./pages/JenkinsLogPage";
import { useState } from "react";

function AppRouter() {
  const { page, navigate, currentUser, error, success, loading, aiLoading, testCases } = useApp();
  const [feedbackOpen, setFeedbackOpen] = useState(false);

  const pages = {
    home: <HomePage />,
    story: <StoryPage />,
    generate: <GeneratePage />,
    publish: <PublishPage />,
    release: <ReleasePage />,
    traceability: <TraceabilityPage />,
    ai: <AiPage />,
    "pr-review": <PRReviewPage />,
    "jenkins-log": <JenkinsLogPage />,
    settings: <SettingsPage />,
  };

  return (
    <AppLayout
      page={page}
      onNavigate={navigate}
      currentUser={currentUser}
      error={error}
      success={success}
      loading={loading}
      feedbackOpen={feedbackOpen}
      onFeedback={() => setFeedbackOpen(true)}
      onCloseFeedback={() => setFeedbackOpen(false)}
      showGlobalMessages={page !== "publish"}
      hasTestCases={testCases.length > 0}
      aiLoading={aiLoading}
    >
      {pages[page] ?? pages.home}
    </AppLayout>
  );
}

export default function App() {
  return (
    <AppProvider>
      <AppRouter />
    </AppProvider>
  );
}
