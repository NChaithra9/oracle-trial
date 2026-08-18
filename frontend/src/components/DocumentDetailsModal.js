import React from "react";
import { FileText, CheckCircle2, MessageSquarePlus, MessageSquare } from "lucide-react";
import Modal from "./Modal";

/**
 * Shows metadata for one document from the sidebar's list - page/chunk
 * counts come from the same DocumentSummary the backend already returns via
 * GET /api/documents. "Questions asked" comes from the existing client-side
 * question history (questionsByDocument, kept in localStorage since the
 * backend doesn't track this) - no new backend call is made here.
 * "Indexed" is always true for anything in this list, since the backend
 * only lists documents it actually found in Qdrant.
 */
export default function DocumentDetailsModal({ document, questionsAskedCount, onClose, onAskAboutDocument }) {
  if (!document) return null;

  return (
    <Modal title="Document details" onClose={onClose}>
      <div className="doc-detail-header">
        <span className="doc-detail-icon">
          <FileText size={20} strokeWidth={2} />
        </span>
        <div className="doc-detail-name">{document.documentName}</div>
      </div>

      <div className="detail-rows">
        <div className="detail-row">
          <span className="muted">Status</span>
          <span className="status-badge-indexed">
            <CheckCircle2 size={13} strokeWidth={2.25} />
            Indexed
          </span>
        </div>
        <div className="detail-row">
          <span className="muted">Pages</span>
          <span>{document.pageCount}</span>
        </div>
        <div className="detail-row">
          <span className="muted">Chunks indexed</span>
          <span>{document.chunkCount}</span>
        </div>
        <div className="detail-row">
          <span className="muted">
            <MessageSquare size={13} strokeWidth={2.25} style={{ marginRight: 5, verticalAlign: -2 }} />
            Questions asked
          </span>
          <span>{questionsAskedCount || 0}</span>
        </div>
      </div>

      <button
        type="button"
        className="btn-primary modal-cta"
        onClick={() => onAskAboutDocument(document.documentName)}
      >
        <MessageSquarePlus size={15} strokeWidth={2.25} />
        Ask about this document
      </button>
      <p className="muted modal-note">
        Questions still search across all indexed documents - mentioning the
        file name can help focus the answer.
      </p>
    </Modal>
  );
}
