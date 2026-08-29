export const NAV_ITEMS = [
  { id: "home", label: "Home", subtitle: "View workspace statistics, recent activity, and quick actions." },
  { id: "story", label: "Jira Story", subtitle: "Pull a Jira issue and optionally post a QA comment." },
  { id: "generate", label: "Generate Tests", subtitle: "Choose how many cases you need — presets or a custom number such as 1." },
  { id: "publish", label: "Publish & Link", subtitle: "Review generated cases, choose a Zephyr folder, publish, then link to Jira stories." },
  { id: "release", label: "Test Cycles", subtitle: "Fetch existing cycles first, then create the types you need, then link to Jira/Zephyr." },
  { id: "traceability", label: "Traceability", subtitle: "View coverage gaps across stories, linked test cases, cycles, and executions." },
  { id: "ai", label: "AI Actions", subtitle: "Fetch the Jira issue first, then run one AI action at a time." },
  { id: "pr-review", label: "PR Review", subtitle: "Fetch a GitHub or Bitbucket pull request and get actionable AI feedback.", status: "Upcoming" },
  { id: "jenkins-log", label: "Jenkins Logs", subtitle: "Analyze long Jenkins console output and summarize build failures.", status: "Upcoming" },
  { id: "settings", label: "Settings", subtitle: "Verify connections and view setup instructions." },
];

export function getNavItem(id) {
  return NAV_ITEMS.find((n) => n.id === id) ?? NAV_ITEMS[0];
}
