import React, { useMemo, useState } from "react";
import {
  Sparkles,
  FolderOpen,
  FileText,
  Loader2,
  Search,
  History,
  Info,
} from "lucide-react";
import AboutModal from "./AboutModal";

/**
 * Left-hand app sidebar: branding, at-a-glance stats, a search box over the
 * document list, the list of documents indexed in Qdrant (clickable to open
 * details), a short history of recently asked questions, and an About
 * link at the bottom. All document/stat data comes straight from the
 * `documents` prop (from GET /api/documents) - nothing here is fabricated.
 */
export default function Sidebar({
  documents,
  isLoading,
  onSelectDocument,
  recentQuestions,
  onSelectRecentQuestion,
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [isAboutOpen, setIsAboutOpen] = useState(false);

  const stats = useMemo(
    () => ({
      documents: documents.length,
      pages: documents.reduce((sum, d) => sum + (d.pageCount || 0), 0),
      chunks: documents.reduce((sum, d) => sum + (d.chunkCount || 0), 0),
    }),
    [documents]
  );

  const filteredDocuments = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return documents;
    return documents.filter((d) => d.documentName.toLowerCase().includes(term));
  }, [documents, searchTerm]);

  const remaining = Math.max(0, 5 - documents.length);

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
        {/* Knowledge base stats */}
        <div className="stat-tiles">
          <div className="stat-tile">
            <div className="stat-tile-value">{stats.documents}</div>
            <div className="stat-tile-label">Documents</div>
          </div>
          <div className="stat-tile">
            <div className="stat-tile-value">{stats.pages}</div>
            <div className="stat-tile-label">Pages</div>
          </div>
          <div className="stat-tile">
            <div className="stat-tile-value">{stats.chunks}</div>
            <div className="stat-tile-label">Chunks</div>
          </div>
        </div>

        {/* Knowledge base list */}
        <div className="sidebar-section">
          <div className="sidebar-heading">
            <FolderOpen size={15} strokeWidth={2} />
            <span>Knowledge base</span>
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
              <FileText size={26} strokeWidth={1.5} className="sidebar-empty-icon" />
              <p className="muted">No documents yet. Upload a PDF to get started.</p>
            </div>
          )}

          {documents.length > 0 && filteredDocuments.length === 0 && (
            <p className="muted sidebar-hint">No documents match "{searchTerm}".</p>
          )}

          {filteredDocuments.length > 0 && (
            <ul className="sidebar-list">
              {filteredDocuments.map((doc) => (
                <li key={doc.documentName}>
                  <button
                    type="button"
                    className="sidebar-doc-button"
                    onClick={() => onSelectDocument(doc)}
                  >
                    <FileText size={16} strokeWidth={2} className="sidebar-doc-icon" />
                    <div className="sidebar-doc-info">
                      <div className="sidebar-doc-name">{doc.documentName}</div>
                      <div className="sidebar-doc-meta">
                        <span className="badge badge-neutral">
                          {doc.pageCount} page{doc.pageCount === 1 ? "" : "s"}
                        </span>
                        <span className="badge badge-neutral">
                          {doc.chunkCount} chunk{doc.chunkCount === 1 ? "" : "s"}
                        </span>
                      </div>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}

          {documents.length > 0 && (
            <div className="sidebar-progress">
              <div className="sidebar-progress-track">
                <div
                  className="sidebar-progress-fill"
                  style={{ width: `${Math.min(100, (documents.length / 5) * 100)}%` }}
                />
              </div>
              <p className="muted sidebar-hint">
                {remaining > 0
                  ? `Add ${remaining} more document${remaining === 1 ? "" : "s"} to reach the recommended 5+.`
                  : "5+ documents indexed — nice knowledge base."}
              </p>
            </div>
          )}
        </div>

        {/* Recent questions */}
        <div className="sidebar-section">
          <div className="sidebar-heading">
            <History size={15} strokeWidth={2} />
            <span>Recent questions</span>
          </div>

          {recentQuestions.length === 0 ? (
            <p className="muted sidebar-hint">Questions you ask will show up here.</p>
          ) : (
            <ul className="recent-questions-list">
              {recentQuestions.map((q, index) => (
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
