import React, { useMemo, useState } from "react";
import {
  Sparkles,
  FolderOpen,
  FileText,
  Loader2,
  Search,
  Info,
  ChevronRight,
  ChevronDown,
} from "lucide-react";
import AboutModal from "./AboutModal";

/**
 * Left-hand app sidebar: branding, a search box over the document list, and
 * the list of documents indexed in Qdrant. Each document is collapsible -
 * clicking it selects/highlights it and expands a list of questions
 * previously asked while that document was selected (tracked client-side
 * in App.js and passed in here as `questionsByDocument`). All document
 * data still comes straight from the `documents` prop (GET /api/documents)
 * - nothing here is fabricated.
 */
export default function Sidebar({
  documents,
  isLoading,
  selectedDocumentName,
  onSelectDocument,
  questionsByDocument,
  onSelectRecentQuestion,
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [isAboutOpen, setIsAboutOpen] = useState(false);

  const filteredDocuments = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return documents;
    return documents.filter((d) => d.documentName.toLowerCase().includes(term));
  }, [documents, searchTerm]);

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-brand-mark">
          <Sparkles size={17} strokeWidth={2.25} />
        </span>
        <div>
          <div className="sidebar-brand-title">The Oracle's Trial</div>
          <div className="sidebar-brand-subtitle">AI document assistant</div>
        </div>
      </div>

      <div className="sidebar-scroll">
        <div className="sidebar-section">
          <div className="sidebar-heading">
            <FolderOpen size={15} strokeWidth={2} />
            <span>Documents</span>
          </div>

          {documents.length > 0 && (
            <div className="sidebar-search">
              <Search size={14} strokeWidth={2} className="sidebar-search-icon" />
              <input
                type="text"
                placeholder="Search documents..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          )}

          {isLoading && documents.length === 0 && (
            <div className="sidebar-empty">
              <Loader2 size={18} className="spin" />
              <p className="muted">Loading documents...</p>
            </div>
          )}

          {!isLoading && documents.length === 0 && (
            <div className="sidebar-empty">
              <FolderOpen size={26} strokeWidth={1.5} className="sidebar-empty-icon" />
              <p className="muted">No documents indexed yet.</p>
              <p className="muted">Upload a PDF to get started.</p>
            </div>
          )}

          {documents.length > 0 && filteredDocuments.length === 0 && (
            <p className="muted sidebar-hint">No documents match "{searchTerm}".</p>
          )}

          {filteredDocuments.length > 0 && (
            <ul className="sidebar-list">
              {filteredDocuments.map((doc) => {
                const isExpanded = doc.documentName === selectedDocumentName;
                const docQuestions = questionsByDocument[doc.documentName] || [];

                return (
                  <li
                    key={doc.documentName}
                    className={`sidebar-doc-item ${isExpanded ? "sidebar-doc-item-expanded" : ""}`}
                  >
                    <button
                      type="button"
                      className="sidebar-doc-button"
                      onClick={() => onSelectDocument(doc.documentName)}
                      aria-expanded={isExpanded}
                    >
                      <FileText size={16} strokeWidth={2} className="sidebar-doc-icon" />
                      <div className="sidebar-doc-info">
                        <div className="sidebar-doc-name">{doc.documentName}</div>
                        <div className="sidebar-doc-meta">
                          {doc.pageCount} page{doc.pageCount === 1 ? "" : "s"} ·{" "}
                          {doc.chunkCount} chunk{doc.chunkCount === 1 ? "" : "s"}
                        </div>
                      </div>
                      {isExpanded ? (
                        <ChevronDown size={16} strokeWidth={2} className="sidebar-doc-chevron" />
                      ) : (
                        <ChevronRight size={16} strokeWidth={2} className="sidebar-doc-chevron" />
                      )}
                    </button>

                    {isExpanded && (
                      <div className="sidebar-doc-questions">
                        {docQuestions.length === 0 ? (
                          <p className="muted sidebar-hint">No questions asked yet.</p>
                        ) : (
                          <ul className="recent-questions-list">
                            {docQuestions.map((q, index) => (
                              <li key={index}>
                                <button
                                  type="button"
                                  className="recent-question-item"
                                  onClick={() => onSelectRecentQuestion(q)}
                                  title={q}
                                >
                                  {q}
                                </button>
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>

      <div className="sidebar-footer">
        <button type="button" className="sidebar-footer-button" onClick={() => setIsAboutOpen(true)}>
          <Info size={15} strokeWidth={2} />
          About
        </button>
      </div>

      {isAboutOpen && <AboutModal onClose={() => setIsAboutOpen(false)} />}
    </aside>
  );
}
