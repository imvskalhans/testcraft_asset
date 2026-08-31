import { request } from "./client";

export const traceabilityApi = {
  dashboard: (issueKey) =>
    request("POST", "/api/traceability/dashboard", { issueKey }),
};
