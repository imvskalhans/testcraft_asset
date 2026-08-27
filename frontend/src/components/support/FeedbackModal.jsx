import { useState } from "react";
import Button from "../ui/Button";
import Alert from "../ui/Alert";
import { colors } from "../../constants/theme";
import { inputStyle, labelStyle } from "../../styles/forms";
import api from "../../api";

const SUPPORT_EMAIL = import.meta.env.VITE_SUPPORT_EMAIL || "vsk6645@gmail.com";

export default function FeedbackModal({ currentUser, onClose }) {
  const [rating, setRating] = useState("");
  const [message, setMessage] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");

  const submit = async () => {
    if (!message.trim()) return setError("Please tell us what you think.");
    setSending(true); setError("");
    const username = currentUser?.emailAddress || currentUser?.displayName || "TestCraft user";
    try {
      await api.feedback({ message: message.trim(), rating, username });
    } catch (e) {
      // The email client remains the direct delivery path; server recording is best effort.
    }
    const subject = rating ? `TestCraft feedback — ${rating}` : "TestCraft feedback";
    const body = `From: ${username}\n\n${message.trim()}`;
    window.location.href = `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    setSending(false);
  };

  return <div style={{ position: "fixed", inset: 0, zIndex: 30, background: "rgba(15, 23, 42, .35)", display: "grid", placeItems: "center", padding: 16 }}>
    <div role="dialog" aria-modal="true" aria-labelledby="feedback-title" style={{ width: "min(460px, 100%)", background: colors.card, borderRadius: 14, padding: 20, boxShadow: "0 18px 50px rgba(15,23,42,.2)" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", gap: 12 }}>
        <div><h2 id="feedback-title" style={{ margin: 0, fontSize: 18 }}>Send feedback</h2><p style={{ color: colors.muted, fontSize: 12, margin: "6px 0 16px" }}>A prefilled email will open addressed to the support team.</p></div>
        <button type="button" aria-label="Close feedback" onClick={onClose} style={{ border: 0, background: "transparent", fontSize: 22, cursor: "pointer", color: colors.muted }}>×</button>
      </div>
      {error && <Alert>{error}</Alert>}
      <label style={labelStyle}>How was your experience?</label>
      <div style={{ display: "flex", gap: 8, marginBottom: 14 }}>
        {["Great", "Okay", "Needs work"].map((option) => <button key={option} type="button" onClick={() => setRating(option)} style={{ ...inputStyle, width: "auto", cursor: "pointer", background: rating === option ? colors.brandLight : "#fff", color: rating === option ? colors.brand : colors.text, borderColor: rating === option ? colors.brand : colors.border }}>{option}</button>)}
      </div>
      <label style={labelStyle}>Your feedback<textarea autoFocus rows={5} maxLength={4000} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="What should we improve?" style={{ ...inputStyle, resize: "vertical" }} /></label>
      <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 14 }}><Button onClick={onClose}>Cancel</Button><Button primary disabled={sending || !message.trim()} onClick={submit}>{sending ? "Preparing…" : "Open email"}</Button></div>
    </div>
  </div>;
}
