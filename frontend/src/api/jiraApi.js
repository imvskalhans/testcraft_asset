import { request } from "./client";

export const jiraApi = {
  test: () => request("GET", "/api/jira/test"),
  fetchIssue: (key) => request("GET", `/api/jira/issue/${encodeURIComponent(key)}`),
  comment: (issueKey, comment) =>
    request("POST", `/api/jira/comment?issueKey=${encodeURIComponent(issueKey)}`, { comment }),
};
