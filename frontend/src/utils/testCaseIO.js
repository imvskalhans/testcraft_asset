const EXPORT_VERSION = 1;

function normalizeTestCase(raw, fallbackType = "Functional") {
  const steps = Array.isArray(raw?.steps)
    ? raw.steps.map((step) => String(step ?? "").trim()).filter(Boolean)
    : String(raw?.steps || "")
      .split(/\||\n/)
      .map((step) => step.trim())
      .filter(Boolean);

  return {
    testName: String(raw?.testName || raw?.name || "Untitled test").trim(),
    objective: String(raw?.objective || "").trim(),
    preCondition: String(raw?.preCondition || raw?.precondition || "").trim(),
    expectedResult: String(raw?.expectedResult || "").trim(),
    priority: String(raw?.priority || "Medium").trim() || "Medium",
    status: String(raw?.status || "Draft").trim() || "Draft",
    testType: String(raw?.testType || fallbackType).trim() || fallbackType,
    steps: steps.length ? steps : [""],
    key: raw?.key || "",
  };
}

export function normalizeImportedTestCases(items, fallbackType = "Functional") {
  if (!Array.isArray(items)) {
    throw new Error("Imported file must contain a list of test cases");
  }
  const normalized = items
    .map((item) => normalizeTestCase(item, fallbackType))
    .filter((item) => item.testName);
  if (!normalized.length) {
    throw new Error("No valid test cases found in the imported file");
  }
  return normalized;
}

function escapeCsv(value) {
  const text = String(value ?? "");
  if (/[",\n\r]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

function downloadBlob(filename, content, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function exportTestCasesJson(testCases, issueKey = "testcases") {
  const payload = {
    version: EXPORT_VERSION,
    exportedAt: new Date().toISOString(),
    issueKey,
    testCases,
  };
  downloadBlob(
    `${issueKey}-testcases.json`,
    JSON.stringify(payload, null, 2),
    "application/json",
  );
}

export function exportTestCasesCsv(testCases, issueKey = "testcases") {
  const headers = [
    "testName",
    "objective",
    "preCondition",
    "expectedResult",
    "priority",
    "status",
    "testType",
    "steps",
  ];
  const rows = testCases.map((tc) => [
    tc.testName,
    tc.objective,
    tc.preCondition,
    tc.expectedResult,
    tc.priority,
    tc.status,
    tc.testType,
    (tc.steps || []).join(" | "),
  ]);
  const csv = [
    headers.join(","),
    ...rows.map((row) => row.map(escapeCsv).join(",")),
  ].join("\n");
  downloadBlob(`${issueKey}-testcases.csv`, `\uFEFF${csv}`, "text/csv;charset=utf-8");
}

export function exportTestCasesExcel(testCases, issueKey = "testcases") {
  const header = `
    <tr>
      <th>testName</th>
      <th>objective</th>
      <th>preCondition</th>
      <th>expectedResult</th>
      <th>priority</th>
      <th>status</th>
      <th>testType</th>
      <th>steps</th>
    </tr>
  `;
  const rows = testCases.map((tc) => `
    <tr>
      <td>${escapeHtml(tc.testName)}</td>
      <td>${escapeHtml(tc.objective)}</td>
      <td>${escapeHtml(tc.preCondition)}</td>
      <td>${escapeHtml(tc.expectedResult)}</td>
      <td>${escapeHtml(tc.priority)}</td>
      <td>${escapeHtml(tc.status)}</td>
      <td>${escapeHtml(tc.testType)}</td>
      <td>${escapeHtml((tc.steps || []).join(" | "))}</td>
    </tr>
  `).join("");
  const html = `
    <html>
      <head><meta charset="UTF-8" /></head>
      <body>
        <table border="1">${header}${rows}</table>
      </body>
    </html>
  `;
  downloadBlob(`${issueKey}-testcases.xls`, html, "application/vnd.ms-excel");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function parseCsv(text) {
  const rows = [];
  let row = [];
  let cell = "";
  let inQuotes = false;

  for (let i = 0; i < text.length; i += 1) {
    const char = text[i];
    const next = text[i + 1];

    if (char === '"') {
      if (inQuotes && next === '"') {
        cell += '"';
        i += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (!inQuotes && char === ",") {
      row.push(cell);
      cell = "";
      continue;
    }

    if (!inQuotes && (char === "\n" || char === "\r")) {
      if (char === "\r" && next === "\n") i += 1;
      row.push(cell);
      rows.push(row);
      row = [];
      cell = "";
      continue;
    }

    cell += char;
  }

  if (cell.length || row.length) {
    row.push(cell);
    rows.push(row);
  }

  return rows.filter((entry) => entry.some((value) => String(value).trim()));
}

function csvRowsToTestCases(rows, fallbackType) {
  if (!rows.length) return [];
  const [headerRow, ...dataRows] = rows;
  const headers = headerRow.map((value) => String(value).trim().toLowerCase());
  const index = (name, aliases = []) => {
    const candidates = [name, ...aliases].map((item) => item.toLowerCase());
    return headers.findIndex((header) => candidates.includes(header));
  };

  const nameIdx = index("testname", ["name"]);
  const objectiveIdx = index("objective");
  const preIdx = index("precondition", ["precondition"]);
  const expectedIdx = index("expectedresult", ["expected result"]);
  const priorityIdx = index("priority");
  const statusIdx = index("status");
  const typeIdx = index("testtype", ["type"]);
  const stepsIdx = index("steps");

  return dataRows.map((row) => normalizeTestCase({
    testName: nameIdx >= 0 ? row[nameIdx] : row[0],
    objective: objectiveIdx >= 0 ? row[objectiveIdx] : "",
    preCondition: preIdx >= 0 ? row[preIdx] : "",
    expectedResult: expectedIdx >= 0 ? row[expectedIdx] : "",
    priority: priorityIdx >= 0 ? row[priorityIdx] : "Medium",
    status: statusIdx >= 0 ? row[statusIdx] : "Draft",
    testType: typeIdx >= 0 ? row[typeIdx] : fallbackType,
    steps: stepsIdx >= 0 ? String(row[stepsIdx] || "").split("|").map((step) => step.trim()).filter(Boolean) : [],
  }, fallbackType));
}

export async function importTestCasesFromFile(file, fallbackType = "Functional") {
  const name = file?.name?.toLowerCase() || "";
  const text = await file.text();

  if (name.endsWith(".json") || text.trim().startsWith("{") || text.trim().startsWith("[")) {
    const parsed = JSON.parse(text);
    const items = Array.isArray(parsed) ? parsed : parsed.testCases;
    return normalizeImportedTestCases(items, fallbackType);
  }

  if (name.endsWith(".csv") || name.endsWith(".xls") || name.endsWith(".xlsx") || text.includes(",")) {
    const rows = parseCsv(text.replace(/^\uFEFF/, ""));
    const cases = csvRowsToTestCases(rows, fallbackType);
    return normalizeImportedTestCases(cases, fallbackType);
  }

  throw new Error("Unsupported file format. Use JSON, CSV, or Excel.");
}
