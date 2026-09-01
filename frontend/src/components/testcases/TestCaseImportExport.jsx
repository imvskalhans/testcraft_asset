import { useRef } from "react";
import Button from "../ui/Button";
import Alert from "../ui/Alert";
import { colors } from "../../constants/theme";
import {
  exportTestCasesCsv,
  exportTestCasesExcel,
  exportTestCasesJson,
  importTestCasesFromFile,
} from "../../utils/testCaseIO";

export default function TestCaseImportExport({
  testCases,
  issueKey,
  testType,
  onImport,
  disabled = false,
  showImport = true,
}) {
  const fileInputRef = useRef(null);
  const hasCases = testCases.length > 0;

  const handleImport = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    try {
      const imported = await importTestCasesFromFile(file, testType);
      onImport(imported);
    } catch (error) {
      onImport(null, error.message || "Import failed");
    }
  };

  return (
    <div style={{ marginBottom: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
        <div>
          <div style={{ fontSize: 12, fontWeight: 600 }}>{showImport ? "Import / export" : "Export test cases"}</div>
          <div style={{ fontSize: 11, color: colors.muted, marginTop: 2 }}>
            {showImport ? "Review test cases outside TestCraft or reuse them across stories." : "Download generated cases for sharing or reuse."}
          </div>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          {showImport && <Button disabled={disabled} onClick={() => fileInputRef.current?.click()}>Import</Button>}
          <Button disabled={disabled || !hasCases} onClick={() => exportTestCasesJson(testCases, issueKey)}>
            JSON
          </Button>
          <Button disabled={disabled || !hasCases} onClick={() => exportTestCasesCsv(testCases, issueKey)}>
            CSV
          </Button>
          <Button disabled={disabled || !hasCases} onClick={() => exportTestCasesExcel(testCases, issueKey)}>
            Excel
          </Button>
        </div>
      </div>
      {showImport && <input
        ref={fileInputRef}
        type="file"
        accept=".json,.csv,.xls,.xlsx,application/json,text/csv"
        style={{ display: "none" }}
        onChange={handleImport}
      />}
      {showImport && !hasCases && (
        <div style={{ marginTop: 10 }}>
          <Alert type="info">Import JSON, CSV, or Excel to load test cases without generating them first.</Alert>
        </div>
      )}
    </div>
  );
}
