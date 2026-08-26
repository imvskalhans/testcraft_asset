import AppLayout from "./components/layout/AppLayout";
import { AppProvider, useApp } from "./context/AppContext";
import StoryPage from "./pages/StoryPage";
import GeneratePage from "./pages/GeneratePage";
import PublishPage from "./pages/PublishPage";
import ReleasePage from "./pages/ReleasePage";
import AiPage from "./pages/AiPage";
import SettingsPage from "./pages/SettingsPage";

function AppRouter() {
  const { page, navigate, currentUser, error, success, loading } = useApp();

  const pages = {
    story: <StoryPage />,
    generate: <GeneratePage />,
    publish: <PublishPage />,
    release: <ReleasePage />,
    ai: <AiPage />,
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
    >
      {pages[page] ?? pages.story}
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
