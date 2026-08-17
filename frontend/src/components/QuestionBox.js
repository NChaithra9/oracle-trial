import React, { useState } from "react";

/**
 * A simple text input + button for asking a question.
 * The parent (App) owns the actual API call so it can also manage the
 * answer/citations state shown by AnswerDisplay.
 */
export default function QuestionBox({ onAsk, isAsking, disabled }) {
  const [question, setQuestion] = useState("");

  function handleSubmit(event) {
    event.preventDefault();
    if (!question.trim() || isAsking) return;
    onAsk(question.trim());
  }

  return (
    <div className="card">
      <h2>2. Ask The Oracle</h2>
      <p className="muted">
        {disabled
          ? "Upload at least one PDF first."
          : "Ask a question about the documents you uploaded."}
      </p>

      <form onSubmit={handleSubmit} className="question-form">
        <textarea
          rows={3}
          placeholder="e.g. What does the warranty section say about returns?"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          disabled={disabled || isAsking}
        />
        <button type="submit" disabled={disabled || isAsking || !question.trim()}>
          {isAsking ? "Consulting the Oracle..." : "Ask"}
        </button>
      </form>
    </div>
  );
}
