import React from "react";
import { Send, Loader2 } from "lucide-react";

const EXAMPLE_QUESTIONS = [
  "What is this document about?",
  "Summarize the key points.",
  "What are the main policies mentioned?",
];

/**
 * A text input + send button for asking a question, plus a few clickable
 * example prompts to help first-time users get started quickly.
 *
 * The question text is a controlled prop (value/onChange) owned by App, so
 * clicking a recent question in the sidebar or "Ask about this document"
 * in the details modal can populate it from outside this component. The
 * parent still owns the actual /api/questions/ask call.
 */
export default function QuestionBox({ value, onChange, onAsk, isAsking, disabled }) {
  function handleSubmit(event) {
    event.preventDefault();
    if (!value.trim() || isAsking) return;
    onAsk(value.trim());
  }

  return (
    <div className="card" id="question-box">
      <div className="card-heading">
        <span className="card-step">2</span>
        <div>
          <h2>Ask The Oracle</h2>
          <p className="muted">
            {disabled
              ? "Upload at least one PDF first."
              : "Ask a question about the documents you uploaded."}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="question-form">
        <textarea
          id="question-textarea"
          rows={3}
          placeholder="e.g. What does the warranty section say about returns?"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled || isAsking}
        />

        <div className="question-form-footer">
          <div className="example-chips">
            {!disabled &&
              EXAMPLE_QUESTIONS.map((example) => (
                <button
                  type="button"
                  key={example}
                  className="chip"
                  onClick={() => onChange(example)}
                  disabled={isAsking}
                >
                  {example}
                </button>
              ))}
          </div>

          <button
            type="submit"
            className="btn-primary"
            disabled={disabled || isAsking || !value.trim()}
          >
            {isAsking ? (
              <>
                <Loader2 size={14} className="spin" />
                Consulting...
              </>
            ) : (
              <>
                Ask
                <Send size={15} strokeWidth={2.25} />
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
