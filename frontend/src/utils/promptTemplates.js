const STORAGE_KEY = "testcraft.promptTemplates";

function readStore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function writeStore(templates) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(templates));
}

export function loadSavedPromptTemplates() {
  return readStore().sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0));
}

export function savePromptTemplate({ name, prompt, id }) {
  const trimmedName = String(name || "").trim();
  const trimmedPrompt = String(prompt || "").trim();
  if (!trimmedName) throw new Error("Template name is required");
  if (!trimmedPrompt) throw new Error("Template prompt is required");

  const templates = readStore();
  const now = Date.now();
  const existingIndex = id ? templates.findIndex((item) => item.id === id) : -1;

  if (existingIndex >= 0) {
    templates[existingIndex] = {
      ...templates[existingIndex],
      name: trimmedName,
      prompt: trimmedPrompt,
      updatedAt: now,
    };
    writeStore(templates);
    return templates[existingIndex];
  }

  const created = {
    id: `saved-${now}`,
    name: trimmedName,
    prompt: trimmedPrompt,
    createdAt: now,
    updatedAt: now,
  };
  templates.push(created);
  writeStore(templates);
  return created;
}

export function deletePromptTemplate(id) {
  const templates = readStore().filter((item) => item.id !== id);
  writeStore(templates);
  return templates;
}

export function getSavedPromptTemplate(id) {
  return readStore().find((item) => item.id === id) ?? null;
}
