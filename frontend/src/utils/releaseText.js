export function formatReleaseText(release) {
  if (!release) return "";

  const lines = [
    `CR: ${release.crKey} — ${release.crSummary || "—"}`,
    `Status: ${release.status || "—"}`,
  ];

  if (release.crDescription) {
    lines.push(`Description: ${release.crDescription}`);
  }

  lines.push(`Linked stories: ${release.linkedStories?.length ?? 0}`);

  (release.linkedStories ?? []).forEach((story) => {
    const priority = story.priority ? ` (${story.priority})` : "";
    lines.push(`- ${story.key}: ${story.summary} [${story.status || "—"}]${priority}`);
    if (story.description) {
      lines.push(`  ${story.description}`);
    }
  });

  return lines.join("\n");
}
