import React from "react";
import { Database } from "lucide-react";

/**
 * Slim top bar: app name/subtitle on the left, live indexed document count
 * on the right (from the same `documents` list App.js already loads via
 * GET /api/documents).
 */
export default function Header({ documentCount }) {
  return (
    <header className="topbar">
      <div className="topbar-inner">
        <div className="topbar-titles">
          <span className="topbar-title">The Oracle's Trial</span>
          <span className="topbar-subtitle">RAG-powered document Q&amp;A</span>
        </div>

        <div className="topbar-right">
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
