import { request } from "./client";

export const releaseApi = {
  fetchCr: (crKey) => request("POST", "/api/release-process/fetch-cr", { crKey }),
  getTestCycles: (crKey) => request("POST", "/api/release-process/get-test-cycles", { crKey }),
  getTestCases: (crKey) => request("POST", "/api/release-process/get-test-cases", { crKey }),
  createTestCycles: (crKey, createFolders = true, cycleTypes = ["Functional"], owner = "") =>
    request("POST", "/api/release-process/create-test-cycles", {
      crKey,
      createFolders,
      cycleTypes,
      owner,
    }),
  aiActions: () => request("GET", "/api/release-process/ai-actions"),
  aiAnalysis: (payload) => request("POST", "/api/release-process/ai-analysis", payload),
};
