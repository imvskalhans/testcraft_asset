import { request } from "./client";

export const aiApi = {
  actions: () => request("GET", "/api/ai/actions"),
  run: (payload) => request("POST", "/api/ai/run", payload),
};
