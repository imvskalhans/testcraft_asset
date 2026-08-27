import { request } from "./client";

export const zephyrApi = {
  projects: () => request("GET", "/api/zephyr/projects"),
  folders: (projectId) => request("GET", `/api/zephyr/folders/${projectId}`),
  statuses: (projectId) => request("GET", `/api/zephyr/statuses/${projectId}`),
  priorities: (projectId) => request("GET", `/api/zephyr/priorities/${projectId}`),
  validateUser: () => request("GET", "/api/zephyr/validate-user"),
  publish: (payload) => request("POST", "/api/zephyr/publish", payload),
  linkBulk: (testCaseKeys, issueKeys) =>
    request("POST", "/api/zephyr/link-bulk", { testCaseKeys, issueKeys }),
};
