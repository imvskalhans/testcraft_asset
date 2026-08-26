const BASE = import.meta.env.VITE_API_URL ?? "";

export async function request(method, path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!res.ok) {
    const msg = typeof data === "string" ? data : data?.error ?? data?.message ?? res.statusText;
    throw new Error(msg || `Request failed (${res.status})`);
  }

  return data;
}
