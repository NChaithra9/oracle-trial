import React from "react";

/**
 * Shows the most recent answer along with its source citations
 * (document name + page number). Renders nothing until a question has
 * been asked at least once.
 */
export default function AnswerDisplay({ answer, citations, question, error }) {
  if (!answer && !error) return null;

  return (
    <div className="card">
      <h2>3. Answer</h2>

      {question && <p className="muted">Q: {question}</p>}

      {error ? (
        <p className="error">{error}</p>
      ) : (
        <>
          <p className="answer-text">{answer}</p>

          {citations && citations.length > 0 && (
            <div className="citations">
              <h3>Sources</h3>
              <ul>
                {citations.map((citation, index) => (
                  <li key={index}>
                    {citation.documentName} — page {citation.pageNumber}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </div>
  );
}
