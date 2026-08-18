import React from "react";
import Modal from "./Modal";

/**
 * Static "About" panel - purely informational, no API calls. Describes what
 * the app does and the stack it's built on.
 */
export default function AboutModal({ onClose }) {
  return (
    <Modal title="About The Oracle's Trial" onClose={onClose}>
      <p>
        The Oracle's Trial is a retrieval-augmented Q&amp;A app. Upload PDFs,
        ask a question, and get an answer generated only from the content of
        your documents - with citations back to the exact document and page
        it came from.
      </p>

      <div className="about-section">
        <h3>How answers are produced</h3>
        <p className="muted">
          Each question is embedded and matched against your documents in a
          vector database. Only the closest-matching passages are sent to
          the language model, which is instructed to answer strictly from
          that content - or say so plainly when nothing relevant is found.
        </p>
      </div>

      <div className="about-section">
        <h3>Built with</h3>
        <div className="about-stack">
          <span className="badge badge-neutral">Spring Boot</span>
          <span className="badge badge-neutral">LangChain4j</span>
          <span className="badge badge-neutral">OpenAI</span>
          <span className="badge badge-neutral">Qdrant</span>
          <span className="badge badge-neutral">React</span>
        </div>
      </div>
    </Modal>
  );
}
