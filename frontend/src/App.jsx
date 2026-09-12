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
import { useEffect, useState } from "react";

const PAGE_COMPONENTS = {
  home: HomePage,
  story: StoryPage,
  generate: GeneratePage,
  publish: PublishPage,
  release: ReleasePage,
  traceability: TraceabilityPage,
  ai: AiPage,
  "pr-review": PRReviewPage,
  "jenkins-log": JenkinsLogPage,
  settings: SettingsPage,
};

const PAGE_REFRESH = {
  story: "Refresh this page — refetch the current Jira issue. Other tabs stay as they are.",
  generate: "Refresh this page — reset generate inputs on this tab. Existing test cases stay.",
  ai: "Refresh this page — reset this AI workspace. Other tabs stay as they are.",
  traceability: "Refresh this page — reset this coverage report. Other tabs stay as they are.",
  release: "Refresh this page — refetch cycles for the current key. Other tabs stay as they are.",
  settings: "Refresh this page — recheck connections. Other tabs stay as they are.",
  "pr-review": "Refresh this page — reset this PR review. Other tabs stay as they are.",
  "jenkins-log": "Refresh this page — reset this Jenkins analysis. Other tabs stay as they are.",
};

function AppRouter() {
  const {
    page, navigate, currentUser, error, success, loading, busyJob, testCases,
    issueKey, crKey, fetchStory, fetchCycles, testConnections,
  } = useApp();
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const activePage = PAGE_COMPONENTS[page] ? page : "home";
  const [visitedPages, setVisitedPages] = useState(() => new Set([activePage]));
  const [workspaceKeys, setWorkspaceKeys] = useState({});

  useEffect(() => {
    setVisitedPages((current) => {
      if (current.has(activePage)) return current;
      const next = new Set(current);
      next.add(activePage);
      return next;
    });
  }, [activePage]);

  const refreshWorkspace = () => {
    setWorkspaceKeys((current) => ({
      ...current,
      [activePage]: (current[activePage] || 0) + 1,
    }));
    if (activePage === "story" && issueKey?.trim()) fetchStory();
    if (activePage === "release" && crKey?.trim()) fetchCycles();
    if (activePage === "settings") testConnections();
  };

  return (
    <AppLayout
      page={page}
      onNavigate={navigate}
      currentUser={currentUser}
      error={error}
      success={success}
      loading={loading}
      busyJob={busyJob}
      feedbackOpen={feedbackOpen}
      onFeedback={() => setFeedbackOpen(true)}
      onCloseFeedback={() => setFeedbackOpen(false)}
      showGlobalMessages={page !== "publish"}
      hasTestCases={testCases.length > 0}
      onRefresh={PAGE_REFRESH[activePage] ? refreshWorkspace : undefined}
      refreshTitle={PAGE_REFRESH[activePage]}
      refreshing={Boolean(busyJob) || loading}
    >
      {Object.entries(PAGE_COMPONENTS).map(([id, Component]) => (
        visitedPages.has(id) ? (
          <div
            key={`${id}-${workspaceKeys[id] || 0}`}
            hidden={activePage !== id}
            aria-hidden={activePage !== id}
          >
            <Component />
          </div>
        ) : null
      ))}
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
