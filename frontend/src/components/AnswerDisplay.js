import React, { useState } from "react";
import {
  FileText,
  SearchX,
  MessageCircleQuestion,
  Loader2,
  Sparkles,
  ChevronDown,
  ChevronUp,
} from "lucide-react";

const NOT_FOUND_TEXT = "I couldn't find this information in the provided documents.";

/**
 * Shows the most recent answer along with its source citations (document
 * name + page number). Each citation card can be expanded to show the
 * actual retrieved Qdrant chunk text ("excerpt") that the answer was based
 * on - this comes straight from the backend's AnswerResponse.citations[].excerpt,
 * never generated on the frontend.
 */
export default function AnswerDisplay({ answer, citations, question, error, isAsking }) {
  const isNotFound = answer === NOT_FOUND_TEXT;
  const hasAskedBefore = Boolean(question);

  // Keyed by citation index within the current answer - reset naturally
  // whenever a new question is asked, since this component re-renders with
  // a fresh `citations` array.
  const [expandedIndex, setExpandedIndex] = useState(null);

  function toggleCitation(index) {
    setExpandedIndex((prev) => (prev === index ? null : index));
  }

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
                {citations.map((citation, index) => {
                  const isExpanded = expandedIndex === index;
                  return (
                    <div
                      className={`citation-card ${isExpanded ? "citation-card-expanded" : ""}`}
                      key={index}
                    >
                      <button
                        type="button"
                        className="citation-card-header"
                        onClick={() => toggleCitation(index)}
                        aria-expanded={isExpanded}
                      >
                        <span className="citation-card-icon">
                          <FileText size={14} strokeWidth={2} />
                        </span>
                        <span className="citation-card-name">{citation.documentName}</span>
                        <span className="citation-card-page">p.{citation.pageNumber}</span>
                        {isExpanded ? (
                          <ChevronUp size={14} strokeWidth={2} className="citation-card-chevron" />
                        ) : (
                          <ChevronDown size={14} strokeWidth={2} className="citation-card-chevron" />
                        )}
                      </button>

                      {isExpanded && (
                        <div className="citation-excerpt">
                          {citation.excerpt ? (
                            <p>&ldquo;{citation.excerpt}&rdquo;</p>
                          ) : (
                            <p className="muted">Source excerpt not available for this citation.</p>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
