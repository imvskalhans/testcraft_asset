const MAX_STORY_DETAILS = 1800;
const MAX_TEST_CASES = 20;

function truncate(text, maxLength) {
  const value = String(text || "").trim();
  if (value.length <= maxLength) return value;
  return `${value.slice(0, maxLength).trim()}...`;
}

function summarizeTestCase(testCase, index) {
  const name = testCase.testName || `Test case ${index + 1}`;
  const objective = testCase.objective ? ` — ${testCase.objective}` : "";
  const steps = Array.isArray(testCase.steps) ? testCase.steps.filter(Boolean).length : 0;
  const stepText = steps ? ` (${steps} step${steps === 1 ? "" : "s"})` : "";
  return `${name}${objective}${stepText}`;
}

export function buildChatContext({
  page,
  issueKey,
  story,
  storyText,
  testCases,
  publishedKeys,
  publishedTestCaseLinks,
  testType,
}) {
  const summaries = (testCases ?? [])
    .slice(0, MAX_TEST_CASES)
    .map((testCase, index) => summarizeTestCase(testCase, index));

  const published = [...new Set([
    ...(publishedKeys ?? []),
    ...((publishedTestCaseLinks ?? []).map((item) => item.key).filter(Boolean)),
  ])];

  return {
    issueKey: issueKey || "",
    currentPage: page || "",
    storySummary: story?.summary || "",
    storyStatus: story?.status || "",
    storyType: story?.issueType || "",
    storyDetails: truncate(storyText || "", MAX_STORY_DETAILS),
    testCaseCount: testCases?.length ?? 0,
    testType: testType || "",
    testCaseSummaries: summaries,
    publishedKeys: published,
  };
}

export function buildAssistantGreeting(context) {
  if (!context?.issueKey && !context?.testCaseCount) {
    return "Hi! Ask me about the current story, generated test cases, published Zephyr keys, or QA next steps.";
  }

  const parts = [];
  if (context.issueKey) {
    parts.push(`story ${context.issueKey}`);
  }
  if (context.testCaseCount) {
    parts.push(`${context.testCaseCount} generated case${context.testCaseCount === 1 ? "" : "s"}`);
  }
  if (context.publishedKeys?.length) {
    parts.push(`${context.publishedKeys.length} published Zephyr key${context.publishedKeys.length === 1 ? "" : "s"}`);
  }

  return `Hi! I can see ${parts.join(", ")}. Ask me about coverage, publishing, linking, or what to do next.`;
}
