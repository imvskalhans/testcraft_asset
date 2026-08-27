import { useState } from "react";
import { colors } from "../../constants/theme";
import { inputStyle } from "../../styles/forms";
import api from "../../api";

export default function Chatbot() {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState([{ role: "bot", text: "Hi! Ask me about Jira, test cases, Zephyr, or QA practices." }]);

  const send = async (event) => {
    event?.preventDefault();
    const text = message.trim();
    if (!text || busy) return;
    setMessage(""); setMessages((current) => [...current, { role: "user", text }]); setBusy(true);
    try {
      const result = await api.chat({ message: text });
      setMessages((current) => [...current, { role: "bot", text: result.answer || "I could not find an answer." }]);
    } catch (e) {
      setMessages((current) => [...current, { role: "bot", text: e.message || "Chat is temporarily unavailable." }]);
    } finally { setBusy(false); }
  };

  return <div className="chatbot" style={{ position: "fixed", right: 22, bottom: 22, zIndex: 15 }}>
    {open && <div style={{ width: "min(360px, calc(100vw - 28px))", height: 440, marginBottom: 10, background: colors.card, border: `1px solid ${colors.border}`, borderRadius: 14, boxShadow: "0 14px 40px rgba(16,24,40,.16)", display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <div style={{ background: colors.brand, color: "#fff", padding: "13px 15px", display: "flex", justifyContent: "space-between", alignItems: "center" }}><div><strong>TestCraft assistant</strong><div style={{ fontSize: 11, opacity: .8 }}>Quick QA guidance</div></div><button type="button" onClick={() => setOpen(false)} aria-label="Close assistant" style={{ color: "#fff", border: 0, background: "transparent", fontSize: 20, cursor: "pointer" }}>×</button></div>
      <div aria-live="polite" style={{ flex: 1, overflow: "auto", padding: 12, display: "grid", alignContent: "start", gap: 9, background: colors.surface }}>{messages.map((item, index) => <div key={index} style={{ justifySelf: item.role === "user" ? "end" : "start", maxWidth: "88%", padding: "9px 11px", borderRadius: 11, background: item.role === "user" ? colors.brandLight : colors.card, color: colors.text, fontSize: 12, lineHeight: 1.45 }}>{item.text}</div>)}{busy && <div style={{ color: colors.muted, fontSize: 12 }}>Thinking…</div>}</div>
      <form onSubmit={send} style={{ display: "flex", gap: 7, padding: 10, borderTop: `1px solid ${colors.border}` }}><input aria-label="Ask the TestCraft assistant" value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Ask a question…" maxLength={2000} style={{ ...inputStyle, flex: 1 }} /><button type="submit" disabled={!message.trim() || busy} style={{ border: 0, borderRadius: 8, background: colors.brand, color: "#fff", padding: "0 13px", cursor: "pointer" }}>Send</button></form>
    </div>}
    <button type="button" onClick={() => setOpen((value) => !value)} aria-label="Open TestCraft assistant" style={{ border: 0, borderRadius: 999, background: colors.brand, color: "#fff", padding: "12px 16px", fontWeight: 700, cursor: "pointer", boxShadow: "0 8px 20px rgba(24,95,165,.28)" }}>✦ Ask assistant</button>
  </div>;
}
