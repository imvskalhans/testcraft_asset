import Alert from "../ui/Alert";

function isSupported(attachment) {
  const mime = attachment.mimeType || "";
  return mime.startsWith("image/") || mime.startsWith("text/") || ["application/json", "text/csv"].includes(mime) || /\.(txt|md|csv|json)$/i.test(attachment.name || "");
}

export default function SkippedAttachmentsNotice({ attachments = [], supportsImageInput }) {
  const skipped = attachments.filter((attachment) => !attachment.data || !isSupported(attachment) || (attachment.mimeType?.startsWith("image/") && !supportsImageInput));
  if (!skipped.length) return null;

  return (
    <Alert type="info">
      <strong>Skipped during analysis:</strong> These Jira attachments were not included, so results were based on the supported files and available Jira context.
      <ul style={{ margin: "6px 0 0", paddingLeft: 18 }}>
        {skipped.map((attachment, index) => {
          const reason = !attachment.data
            ? "could not be downloaded"
            : !isSupported(attachment)
              ? "file type is not supported"
              : "image input is not supported by the configured AI provider";
          return <li key={`${attachment.name}-${index}`}>{attachment.name || "Unnamed attachment"} — {reason}</li>;
        })}
      </ul>
    </Alert>
  );
}
