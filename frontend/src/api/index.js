import { request } from "./client";
import { configApi } from "./configApi";
import { jiraApi } from "./jiraApi";
import { zephyrApi } from "./zephyrApi";
import { releaseApi } from "./releaseApi";

export const api = {
  config: configApi,
  jira: jiraApi,
  zephyr: zephyrApi,
  release: releaseApi,
  aiReview: (payload) => request("POST", "/api/ai-review", payload),
  generate: (payload) => request("POST", "/api/generate", payload),
  email: (payload) => request("POST", "/api/email", payload),
};

export default api;
