import React, { useCallback, useEffect, useState } from "react";
import "./App.css";
import Header from "./components/Header";
import PdfUpload from "./components/PdfUpload";
import QuestionBox from "./components/QuestionBox";
import AnswerDisplay from "./components/AnswerDisplay";
import Sidebar from "./components/Sidebar";
import DocumentDetailsModal from "./components/DocumentDetailsModal";
import { askQuestion, fetchDocuments } from "./api";

const RECENT_QUESTIONS_KEY = "oracleTrial.recentQuestions";
const MAX_RECENT_QUESTIONS = 8;
const BACKEND_POLL_INTERVAL_MS = 30000;

function loadRecentQuestions() {
  try {
    const raw = window.localStorage.getItem(RECENT_QUESTIONS_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveRecentQuestions(questions) {
  try {
    window.localStorage.setItem(RECENT_QUESTIONS_KEY, JSON.stringify(questions));
  } catch {
    // Non-fatal - history just won't persist across reloads in this case.
  }
}

export default function App() {
  const [documents, setDocuments] = useState([]);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(true);
  const [backendStatus, setBackendStatus] = useState("checking");
  const [selectedDocument, setSelectedDocument] = useState(null);

  const [question, setQuestion] = useState("");
  const [lastQuestion, setLastQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [citations, setCitations] = useState([]);
  const [isAsking, setIsAsking] = useState(false);
  const [error, setError] = useState("");

  const [recentQuestions, setRecentQuestions] = useState(loadRecentQuestions);

  const refreshDocuments = useCallback(async ({ silent } = {}) => {
    if (!silent) setIsLoadingDocuments(true);
    try {
      const docs = await fetchDocuments();
      setDocuments(docs);
      setBackendStatus("online");
    } catch (err) {
      setBackendStatus("offline");
      if (!silent) console.error("Failed to load documents:", err.message);
    } finally {
      if (!silent) setIsLoadingDocuments(false);
    }
  }, []);

  // Load the document list once on startup, so the sidebar reflects
  // whatever is already indexed in Qdrant even after a page refresh. Then
  // poll quietly in the background so the online/offline indicator and
  // document counts stay current without the user doing anything.
  useEffect(() => {
    refreshDocuments();
    const interval = setInterval(() => refreshDocuments({ silent: true }), BACKEND_POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [refreshDocuments]);

  function focusQuestionBox() {
    const container = document.getElementById("question-box");
    if (container) container.scrollIntoView({ behavior: "smooth", block: "center" });
    window.setTimeout(() => {
      document.getElementById("question-textarea")?.focus();
    }, 250);
  }

  function rememberQuestion(q) {
    setRecentQuestions((prev) => {
      const withoutDuplicate = prev.filter((existing) => existing !== q);
      const updated = [q, ...withoutDuplicate].slice(0, MAX_RECENT_QUESTIONS);
      saveRecentQuestions(updated);
      return updated;
    });
  }

  async function handleAsk(submittedQuestion) {
    setIsAsking(true);
    setError("");
    setLastQuestion(submittedQuestion);
    rememberQuestion(submittedQuestion);

    try {
      const result = await askQuestion(submittedQuestion);
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

  function handleSelectRecentQuestion(q) {
    setQuestion(q);
    focusQuestionBox();
  }

  function handleAskAboutDocument(documentName) {
    setQuestion(`Regarding "${documentName}", `);
    setSelectedDocument(null);
    focusQuestionBox();
  }

  return (
    <div className="app-shell">
      <Header documentCount={documents.length} backendStatus={backendStatus} />

      <div className="app-body">
        <Sidebar
          documents={documents}
          isLoading={isLoadingDocuments}
          onSelectDocument={setSelectedDocument}
          recentQuestions={recentQuestions}
          onSelectRecentQuestion={handleSelectRecentQuestion}
        />

        <main className="app-main">
          <div className="content-column">
            <PdfUpload onUploaded={refreshDocuments} />

            <QuestionBox
              value={question}
              onChange={setQuestion}
              onAsk={handleAsk}
              isAsking={isAsking}
              disabled={documents.length === 0}
            />

            <AnswerDisplay
              answer={answer}
              citations={citations}
              question={lastQuestion}
              error={error}
              isAsking={isAsking}
            />
          </div>
        </main>
      </div>

      {selectedDocument && (
        <DocumentDetailsModal
          document={selectedDocument}
          onClose={() => setSelectedDocument(null)}
          onAskAboutDocument={handleAskAboutDocument}
        />
      )}
    </div>
  );
}
