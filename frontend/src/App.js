import React, { useState } from "react";
import "./App.css";
import PdfUpload from "./components/PdfUpload";
import QuestionBox from "./components/QuestionBox";
import AnswerDisplay from "./components/AnswerDisplay";
import { askQuestion } from "./api";

export default function App() {
  const [documents, setDocuments] = useState([]);
  const [lastQuestion, setLastQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [citations, setCitations] = useState([]);
  const [isAsking, setIsAsking] = useState(false);
  const [error, setError] = useState("");

  async function handleAsk(question) {
    setIsAsking(true);
    setError("");
    setLastQuestion(question);

    try {
      const result = await askQuestion(question);
      setAnswer(result.answer);
      setCitations(result.citations || []);
    } catch (err) {
      setAnswer("");
      setCitations([]);
      setError(err.message);
    } finally {
      setIsAsking(false);
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>The Oracle's Trial</h1>
        <p className="muted">Upload PDFs, then ask questions and get cited answers.</p>
      </header>

      <main className="app-main">
        <PdfUpload documents={documents} setDocuments={setDocuments} />

        <QuestionBox onAsk={handleAsk} isAsking={isAsking} disabled={documents.length === 0} />

        <AnswerDisplay
          answer={answer}
          citations={citations}
          question={lastQuestion}
          error={error}
        />
      </main>
    </div>
  );
}
