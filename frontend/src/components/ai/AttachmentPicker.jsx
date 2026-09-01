import { useState } from "react";
import Alert from "../ui/Alert";
import { colors } from "../../constants/theme";

export default function AttachmentPicker({ attachments, setAttachments, supportsImageInput }) {
  const [error, setError] = useState("");
  const accept = supportsImageInput ? "image/*,.txt,.md,.csv,.json" : ".txt,.md,.csv,.json";

  const readFiles = async (event) => {
    setError("");
    const files = [...(event.target.files || [])];
    const allowed = files.filter((file) => (supportsImageInput && file.type.startsWith("image/")) || file.type.startsWith("text/") || ["application/json", "text/csv"].includes(file.type) || /\.(txt|md|csv|json)$/i.test(file.name));
    if (allowed.length !== files.length) setError("Some files were skipped. Use images, TXT, Markdown, CSV, or JSON files.");
    if (allowed.length + attachments.length > 5) { setError("Attach up to 5 files at a time."); return; }
    const loaded = await Promise.all(allowed.map((file) => new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => resolve({ name: file.name, mimeType: file.type || "text/plain", data: String(reader.result).split(",")[1] || "" });
      reader.onerror = () => resolve(null);
      reader.readAsDataURL(file);
    })));
    setAttachments((current) => [...current, ...loaded.filter(Boolean)]);
    event.target.value = "";
  };

  return (
    <div style={{ margin: "4px 0 14px", padding: 10, border: `1px solid ${colors.border}`, borderRadius: 10, background: colors.surface }}>
      <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
        <strong style={{ fontSize: 12 }}>Additional files (optional)</strong>
        <label style={{ display: "inline-flex", alignItems: "center", gap: 6, padding: "6px 10px", border: `1px solid ${colors.border}`, borderRadius: 7, background: "#fff", color: colors.text, fontSize: 11, cursor: "pointer", fontWeight: 600 }}>
          Choose files
          <input type="file" accept={accept} multiple onChange={readFiles} style={{ display: "none" }} aria-label="Choose AI context files" />
        </label>
        <span style={{ fontSize: 11, color: colors.muted }}>{supportsImageInput ? "Images and text files" : "Text, Markdown, CSV, or JSON only"}</span>
      </div>
      {attachments.length > 0 && <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginTop: 8 }}>{attachments.map((file, index) => <span key={`${file.name}-${index}`} style={{ fontSize: 11, background: "#fff", border: `1px solid ${colors.border}`, borderRadius: 999, padding: "4px 8px" }}>{file.name} <button type="button" onClick={() => setAttachments((current) => current.filter((_, i) => i !== index))} style={{ border: 0, background: "transparent", cursor: "pointer", color: colors.muted }}>×</button></span>)}</div>}
      {error && <div style={{ marginTop: 8 }}><Alert>{error}</Alert></div>}
      {!supportsImageInput && <div style={{ marginTop: 8 }}><Alert type="info">Images are unavailable for the configured AI provider, so only text-based files can be added.</Alert></div>}
    </div>
  );
}
