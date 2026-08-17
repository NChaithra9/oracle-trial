import React, { useRef, useState } from "react";
import { uploadPdf } from "../api";

/**
 * Lets the user pick one or more PDFs and uploads them one at a time to
 * the backend. Shows a running list of documents that have been
 * successfully processed (supports 5+ PDFs - there's no limit here).
 */
export default function PdfUpload({ documents, setDocuments }) {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState("");
  const fileInputRef = useRef(null);

  async function handleFilesSelected(event) {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;

    setError("");
    setIsUploading(true);

    for (const file of files) {
      try {
        const result = await uploadPdf(file);
        setDocuments((prev) => [
          ...prev,
          {
            name: result.documentName,
            pages: result.pageCount,
            chunks: result.chunkCount,
          },
        ]);
      } catch (err) {
        setError(`Failed to upload "${file.name}": ${err.message}`);
      }
    }

    setIsUploading(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  return (
    <div className="card">
      <h2>1. Upload your PDFs</h2>
      <p className="muted">Add five or more documents for The Oracle to study.</p>

      <input
        ref={fileInputRef}
        type="file"
        accept="application/pdf"
        multiple
        onChange={handleFilesSelected}
        disabled={isUploading}
      />

      {isUploading && <p className="muted">Uploading and indexing...</p>}
      {error && <p className="error">{error}</p>}

      {documents.length > 0 && (
        <ul className="doc-list">
          {documents.map((doc, index) => (
            <li key={`${doc.name}-${index}`}>
              <strong>{doc.name}</strong>
              <span className="muted">
                {" "}
                — {doc.pages} page{doc.pages === 1 ? "" : "s"}, {doc.chunks} chunk
                {doc.chunks === 1 ? "" : "s"} indexed
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
