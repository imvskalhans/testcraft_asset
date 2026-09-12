export const AI_ACTIONS = [
  { id: "story-review", label: "Story review" },
  { id: "coverage-gap", label: "Coverage gap analysis" },
  { id: "risk-summary", label: "Risk summary" },
  { id: "dom-locator", label: "DOM locator helper" },
  { id: "test-data", label: "Test data generator" },
  { id: "release-notes", label: "Release notes" },
  { id: "custom", label: "Custom AI action" },
];

export const NAV_ITEMS = [
  { id: "home", label: "Home", group: null, subtitle: "View statistics, recent activity, and quick actions.", details: "Your QA command center. See session progress, lifetime statistics, recent activity, and the next useful action." },
  {
    id: "ai",
    label: "AI Workspace",
    group: "Workspace",
    subtitle: "Run focused AI actions with Jira, change-request, image, and text context.",
    details: "Choose a focused AI task such as story review, coverage gaps, risk summaries, DOM locator help, test data, or release notes. Actions can use the context you load and supported files you attach.",
    children: AI_ACTIONS,
  },
  { id: "story", label: "Fetch Jira Details", group: "Test workflow", subtitle: "Load a Jira work item and optionally post a QA comment.", details: "Fetch a story, change request, epic, task, or sub-task into TestCraft. The loaded details can be reused for AI test-case generation, analysis, and traceability. You can also post a QA comment back to Jira." },
  { id: "generate", label: "Generate AI Test Cases", group: "Test workflow", subtitle: "Create editable AI-generated cases from Jira details and QA instructions.", details: "Select a test style and count, add optional instructions or files, then generate editable test cases from Jira context. Review them here before publishing to Zephyr Scale." },
  { id: "publish", label: "Publish to Zephyr & Link", group: "Test workflow", subtitle: "Review generated cases, publish them to Zephyr Scale, then link them to Jira stories.", details: "This is the delivery step. TestCraft publishes test cases and step scripts to Zephyr Scale, then creates coverage links to Jira stories or change requests. Test-case publishing is currently Zephyr-only." },
  { id: "release", label: "Test Cycle Generation", group: "Release", subtitle: "Check for existing Zephyr cycles or create new ones for a story or change request.", details: "Use this to prepare execution cycles for a story or change request. Existing cycles are useful context but are not required; TestCraft can create or reuse cycles in Zephyr and attach published test cases." },
  { id: "traceability", label: "QA Coverage & Traceability", group: "Release", subtitle: "See whether Jira work items have linked Zephyr cases, cycles, executions, and AI coverage gaps.", details: "Enter any Jira work item — story, change request, epic, task, or sub-task — to build a coverage report. TestCraft checks Jira-linked Zephyr test cases, test cycles, and executions, then uses AI to map those cases to the story description and acceptance criteria and recommend missing tests." },
  { id: "pr-review", label: "PR Review", group: "Tools", subtitle: "Fetch a GitHub or Bitbucket pull request and get actionable AI feedback.", status: "Upcoming", details: "Review pull-request changes for likely defects, risks, and test suggestions. This workspace is planned for a future release." },
  { id: "jenkins-log", label: "Jenkins Logs", group: "Tools", subtitle: "Analyze long Jenkins console output and summarize build failures.", status: "Upcoming", details: "Paste or load Jenkins output to identify likely failure causes and next debugging steps. This workspace is planned for a future release." },
  { id: "settings", label: "Settings", group: "System", subtitle: "Verify connections and view setup instructions.", details: "Check Jira, Zephyr, and AI provider connectivity and review the setup guide. API keys are never displayed in the status summary." },
];

export function getNavItem(id) {
  return NAV_ITEMS.find((n) => n.id === id) ?? NAV_ITEMS[0];
}

export function getAiActionNav(id) {
  return AI_ACTIONS.find((action) => action.id === id) ?? null;
}
