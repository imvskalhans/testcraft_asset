const STORAGE_PREFIX = "testcraft.aiPrompt.";

export function loadPromptOverride(actionId) {
  try {
    return localStorage.getItem(`${STORAGE_PREFIX}${actionId}`) || "";
  } catch {
    return "";
  }
}

export function savePromptOverride(actionId, prompt) {
  const key = `${STORAGE_PREFIX}${actionId}`;
  if (!prompt?.trim()) {
    localStorage.removeItem(key);
    return "";
  }
  localStorage.setItem(key, prompt);
  return prompt;
}

export function resetPromptOverride(actionId) {
  localStorage.removeItem(`${STORAGE_PREFIX}${actionId}`);
}
