import React from "react";
import { FileText, SearchX, MessageCircleQuestion, Loader2, Sparkles } from "lucide-react";

const NOT_FOUND_TEXT = "I couldn't find this information in the provided documents.";

/**
 * Shows the most recent answer along with its source citations (document
 * name + page number). Unlike before, this card is always visible - it now
 * has a distinct empty state (nothing asked yet), loading state (waiting on
 * the backend), not-found state, and the normal answer+citations state.
 */
export default function AnswerDisplay({ answer, citations, question, error, isAsking }) {
  const isNotFound = answer === NOT_FOUND_TEXT;
  const hasAskedBefore = Boolean(question);

  return (
    <div className="card answer-card">
      <div className="card-heading">
        <span className="card-step">3</span>
        <div>
          <h2>Answer</h2>
        </div>
      </div>

      {hasAskedBefore && (
        <div className="asked-question">
          <MessageCircleQuestion size={15} strokeWidth={2} />
          <span>{question}</span>
        </div>
      )}

      {isAsking ? (
        <div className="answer-loading">
          <Loader2 size={18} className="spin" />
          <span>The Oracle is reading your documents...</span>
        </div>
      ) : error ? (
        <p className="error">{error}</p>
      ) : !hasAskedBefore ? (
        <div className="answer-empty-state">
          <Sparkles size={22} strokeWidth={1.5} />
          <p className="muted">Ask a question above to see the Oracle's answer here.</p>
        </div>
      ) : (
        <>
          <div className={`answer-body ${isNotFound ? "answer-body-not-found" : ""}`}>
            {isNotFound && <SearchX size={18} strokeWidth={1.75} />}
            <p className="answer-text">{answer}</p>
          </div>

          {citations && citations.length > 0 && (
            <div className="citations">
              <h3>Sources</h3>
              <div className="citation-cards">
                {citations.map((citation, index) => (
                  <div className="citation-card" key={index}>
                    <span className="citation-card-icon">
                      <FileText size={14} strokeWidth={2} />
                    </span>
                    <span className="citation-card-name">{citation.documentName}</span>
                    <span className="citation-card-page">p.{citation.pageNumber}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
