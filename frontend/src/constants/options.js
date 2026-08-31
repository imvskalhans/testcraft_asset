export const TEST_TYPES = ["Functional", "Regression", "Performance", "Security"];
export const CYCLE_TYPES = ["Functional", "Regression", "Performance", "Security"];
export const COUNT_PRESETS = [1, 3, 5, 8, 10, 15, 20];
export const PRIORITIES = ["High", "Medium", "Low", "Normal"];
export const STATUSES = ["Approved", "Draft", "Deprecated"];

export const PROMPT_TYPES = [
  { id: "default", label: "Default", hint: "Solid happy-path coverage", category: "builtin" },
  { id: "advanced", label: "Advanced", hint: "Edge cases and negatives", category: "builtin" },
  { id: "smoke", label: "Smoke", hint: "Fast critical-path checks", category: "builtin" },
  { id: "security", label: "Security", hint: "Auth, input validation, and abuse cases", category: "builtin" },
  { id: "api", label: "API", hint: "Contracts, status codes, and payloads", category: "builtin" },
  { id: "mobile", label: "Mobile", hint: "Responsive UI, gestures, and offline behavior", category: "builtin" },
  { id: "custom", label: "Custom", hint: "Write or save your own instructions", category: "custom" },
];
