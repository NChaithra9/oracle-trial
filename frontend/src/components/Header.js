import React from "react";
import { Database } from "lucide-react";

const STATUS_LABEL = {
  checking: "Checking...",
  online: "Online",
  offline: "Backend unreachable",
};

/**
 * Slim top bar: app name/subtitle on the left, live backend status +
 * indexed document count on the right. The status is a real signal, not
 * decoration - it reflects whether the last call to GET /api/documents
 * (polled periodically by App.js) actually succeeded.
 */
export default function Header({ documentCount, backendStatus }) {
  return (
    <header className="topbar">
      <div className="topbar-inner">
        <div className="topbar-titles">
          <span className="topbar-title">The Oracle's Trial</span>
          <span className="topbar-subtitle">RAG-powered document Q&amp;A</span>
        </div>

        <div className="topbar-right">
          <div className="status-indicator" title={STATUS_LABEL[backendStatus]}>
            <span className={`status-dot status-dot-${backendStatus}`} />
            <span className="status-text">{STATUS_LABEL[backendStatus]}</span>
          </div>

          <div className="topbar-status">
            <Database size={15} strokeWidth={2} />
            <span>
              {documentCount} document{documentCount === 1 ? "" : "s"} indexed
            </span>
          </div>
        </div>
      </div>
    </header>
  );
}
