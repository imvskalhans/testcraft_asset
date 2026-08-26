export function formatStoryDetails(story) {
  if (!story) return "";
  return [
    `Key: ${story.id}`,
    `Summary: ${story.summary}`,
    `Type: ${story.issueType}`,
    `Status: ${story.status}`,
    `Priority: ${story.priority}`,
    `Description: ${story.description ?? ""}`,
    `Acceptance Criteria: ${story.acceptanceCriteria ?? ""}`,
  ].join("\n");
}
