import React, { useCallback, useRef, useState } from "react";
import { UploadCloud, CheckCircle2, AlertTriangle, XCircle, Loader2 } from "lucide-react";
import { uploadPdf } from "../api";

let nextId = 1;

/**
 * A drag-and-drop dropzone for uploading one or more PDFs. Supports 5+
 * files at once - just drop them all in together, or click to browse and
 * multi-select. After each successful upload, calls onUploaded() so the
 * parent can refresh the sidebar's document list from the backend.
 *
 * Each file shows a real upload progress bar (from the browser's actual
 * upload byte count via XMLHttpRequest, not a fake animation), then
 * switches to an indexing spinner while the backend chunks/embeds it, then
 * a final success / duplicate / error state.
 */
export default function PdfUpload({ onUploaded }) {
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const [uploads, setUploads] = useState([]);
  const fileInputRef = useRef(null);

  const handleFiles = useCallback(
    (fileList) => {
      const files = Array.from(fileList || []).filter((f) =>
        f.name.toLowerCase().endsWith(".pdf")
      );
      if (files.length === 0) return;

      const entries = files.map((file) => ({
        id: nextId++,
        name: file.name,
        status: "uploading",
        progress: 0,
        detail: "Uploading...",
      }));
      setUploads((prev) => [...entries, ...prev]);

      entries.forEach(async (entry, index) => {
        try {
          const result = await uploadPdf(files[index], (progress) => {
            setUploads((prev) =>
              prev.map((u) =>
                u.id === entry.id
                  ? {
                      ...u,
                      progress,
                      detail: progress < 100 ? "Uploading..." : "Indexing...",
                      status: progress < 100 ? "uploading" : "indexing",
                    }
                  : u
              )
            );
          });

          setUploads((prev) =>
            prev.map((u) =>
              u.id === entry.id
                ? {
                    ...u,
                    status: result.duplicate ? "duplicate" : "success",
                    progress: 100,
                    detail: result.duplicate
                      ? `Already indexed — ${result.chunkCount} chunk${result.chunkCount === 1 ? "" : "s"} on file`
                      : `${result.pageCount} page${result.pageCount === 1 ? "" : "s"}, ${result.chunkCount} chunk${result.chunkCount === 1 ? "" : "s"} indexed`,
                  }
                : u
            )
          );
          onUploaded();
        } catch (err) {
          setUploads((prev) =>
            prev.map((u) =>
              u.id === entry.id ? { ...u, status: "error", detail: err.message } : u
            )
          );
        }
      });
    },
    [onUploaded]
  );

  function handleDrop(event) {
    event.preventDefault();
    setIsDraggingOver(false);
    handleFiles(event.dataTransfer.files);
  }

  function handleBrowseChange(event) {
    handleFiles(event.target.files);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  return (
    <div className="card">
      <div className="card-heading">
        <span className="card-step">1</span>
        <div>
          <h2>Upload your PDFs</h2>
          <p className="muted">Add five or more documents for The Oracle to study.</p>
        </div>
      </div>

      <div
        className={`dropzone ${isDraggingOver ? "dropzone-active" : ""}`}
        onClick={() => fileInputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setIsDraggingOver(true);
        }}
        onDragLeave={() => setIsDraggingOver(false)}
        onDrop={handleDrop}
        role="button"
        tabIndex={0}
      >
        <UploadCloud size={28} strokeWidth={1.5} />
        <p className="dropzone-title">Drag &amp; drop PDFs here</p>
        <p className="muted dropzone-subtitle">or</p>
        <span className="btn-secondary dropzone-browse">Browse files</span>
        <p className="muted dropzone-subtitle">Supports multiple files · PDF only</p>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf"
          multiple
          onChange={handleBrowseChange}
          hidden
        />
      </div>

      {uploads.length > 0 && (
        <ul className="upload-list">
          {uploads.map((u) => (
            <li key={u.id} className="upload-item">
              <UploadStatusIcon status={u.status} />
              <div className="upload-item-text">
                <div className="upload-item-name">{u.name}</div>
                <div
                  className={`muted upload-item-detail ${u.status === "error" ? "error" : ""}`}
                >
                  {u.detail}
                </div>
                {(u.status === "uploading" || u.status === "indexing") && (
                  <div className="upload-progress-track">
                    <div
                      className="upload-progress-fill"
                      style={{ width: `${u.status === "indexing" ? 100 : u.progress}%` }}
                    />
                  </div>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function UploadStatusIcon({ status }) {
  switch (status) {
    case "uploading":
      return <Loader2 size={18} className="spin upload-icon-uploading" />;
    case "indexing":
      return <Loader2 size={18} className="spin upload-icon-uploading" />;
    case "success":
      return <CheckCircle2 size={18} className="upload-icon-success" />;
    case "duplicate":
      return <AlertTriangle size={18} className="upload-icon-duplicate" />;
    case "error":
      return <XCircle size={18} className="upload-icon-error" />;
    default:
      return null;
  }
}
