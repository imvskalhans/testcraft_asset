import { request } from "./client";

export const configApi = {
  status: () => request("GET", "/api/config/status"),
  me: () => request("GET", "/api/config/me"),
  setupGuide: () => request("GET", "/api/config/setup-guide"),
  jiraProject: (key) => request("GET", `/api/config/jira-project/${encodeURIComponent(key)}`),
};
