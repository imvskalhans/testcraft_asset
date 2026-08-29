const STORAGE_KEY = "testcraft.appStats";
const MAX_ACTIVITY = 12;

const DEFAULT_STATS = {
  totalGenerated: 0,
  totalPublished: 0,
  totalLinked: 0,
  storiesFetched: 0,
  cyclesCreated: 0,
  aiRuns: 0,
  imports: 0,
  lastUpdatedAt: null,
  recentActivity: [],
};

function readStats() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { ...DEFAULT_STATS, recentActivity: [] };
    const parsed = JSON.parse(raw);
    return { ...DEFAULT_STATS, ...parsed, recentActivity: parsed.recentActivity ?? [] };
  } catch {
    return { ...DEFAULT_STATS, recentActivity: [] };
  }
}

function writeStats(stats) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(stats));
}

export function loadAppStats() {
  return readStats();
}

export function recordAppStat(type, detail = {}) {
  const stats = readStats();
  const count = Number(detail.count) || 1;
  const entry = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    type,
    label: detail.label || type,
    issueKey: detail.issueKey || "",
    count,
    at: Date.now(),
  };

  switch (type) {
    case "generated":
      stats.totalGenerated += count;
      break;
    case "published":
      stats.totalPublished += count;
      break;
    case "linked":
      stats.totalLinked += count;
      break;
    case "story-fetched":
      stats.storiesFetched += count;
      break;
    case "cycles-created":
      stats.cyclesCreated += count;
      break;
    case "ai-run":
      stats.aiRuns += count;
      break;
    case "imported":
      stats.imports += count;
      break;
    default:
      break;
  }

  stats.recentActivity = [entry, ...(stats.recentActivity ?? [])].slice(0, MAX_ACTIVITY);
  stats.lastUpdatedAt = entry.at;
  writeStats(stats);
  return stats;
}

export function resetAppStats() {
  writeStats({ ...DEFAULT_STATS, recentActivity: [] });
  return loadAppStats();
}

export function formatStatTime(timestamp) {
  if (!timestamp) return "—";
  return new Date(timestamp).toLocaleString();
}
