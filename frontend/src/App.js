import React, { useCallback, useEffect, useState } from "react";
import "./App.css";
import Header from "./components/Header";
import PdfUpload from "./components/PdfUpload";
import QuestionBox from "./components/QuestionBox";
import AnswerDisplay from "./components/AnswerDisplay";
import Sidebar from "./components/Sidebar";
import DocumentDetailsModal from "./components/DocumentDetailsModal";
import { askQuestion, deleteDocument, fetchDocuments } from "./api";

const QUESTIONS_BY_DOCUMENT_KEY = "oracleTrial.questionsByDocument";
const ALL_DOCUMENTS_KEY = "All Documents";
const MAX_QUESTIONS_PER_DOCUMENT = 8;
const BACKEND_POLL_INTERVAL_MS = 30000;

function loadQuestionsByDocument() {
  try {
    const raw = window.localStorage.getItem(QUESTIONS_BY_DOCUMENT_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function saveQuestionsByDocument(map) {
  try {
    window.localStorage.setItem(QUESTIONS_BY_DOCUMENT_KEY, JSON.stringify(map));
  } catch {
    // Non-fatal - history just won't persist across reloads in this case.
  }
}

export default function App() {
  const [documents, setDocuments] = useState([]);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(true);
  const [selectedDocumentName, setSelectedDocumentName] = useState(null);
  const [detailsDocument, setDetailsDocument] = useState(null);

  const [question, setQuestion] = useState("");
  const [lastQuestion, setLastQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [citations, setCitations] = useState([]);
  const [isAsking, setIsAsking] = useState(false);
  const [error, setError] = useState("");

  const [questionsByDocument, setQuestionsByDocument] = useState(loadQuestionsByDocument);

  const refreshDocuments = useCallback(async ({ silent } = {}) => {
    if (!silent) setIsLoadingDocuments(true);
    try {
      const docs = await fetchDocuments();
      setDocuments(docs);
    } catch (err) {
      if (!silent) console.error("Failed to load documents:", err.message);
    } finally {
      if (!silent) setIsLoadingDocuments(false);
    }
  }, []);

  // Load the document list once on startup, so the sidebar reflects
  // whatever is already indexed in Qdrant even after a page refresh. Then
  // poll quietly in the background so document counts stay current without
  // the user doing anything.
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
    const key = selectedDocumentName || ALL_DOCUMENTS_KEY;
    setQuestionsByDocument((prev) => {
      const existingForKey = prev[key] || [];
      const withoutDuplicate = existingForKey.filter((existing) => existing !== q);
      const updatedForKey = [q, ...withoutDuplicate].slice(0, MAX_QUESTIONS_PER_DOCUMENT);
      const updated = { ...prev, [key]: updatedForKey };
      saveQuestionsByDocument(updated);
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

  function handleSelectDocument(documentName) {
    setSelectedDocumentName((prev) => (prev === documentName ? null : documentName));
  }

  function handleAskAboutDocument(documentName) {
    setQuestion(`Regarding "${documentName}", `);
    setDetailsDocument(null);
    focusQuestionBox();
  }

  // Permanently deletes a document: calls the backend DELETE endpoint (which
  // removes every one of its vectors from Qdrant), then re-syncs the
  // document list from the backend and cleans up anything kept client-side
  // for it (its question history, and clearing it if it was selected/open
  // in the details modal). Errors are left to propagate so the Sidebar's
  // delete-confirmation dialog can show them.
  async function handleDeleteDocument(documentName) {
    await deleteDocument(documentName);
    await refreshDocuments();

    setQuestionsByDocument((prev) => {
      if (!(documentName in prev)) return prev;
      const updated = { ...prev };
      delete updated[documentName];
      saveQuestionsByDocument(updated);
      return updated;
    });

    setSelectedDocumentName((prev) => (prev === documentName ? null : prev));
    setDetailsDocument((prev) => (prev && prev.documentName === documentName ? null : prev));
  }

  return (
    <div className="app-shell">
      <Header documentCount={documents.length} />

      <div className="app-body">
        <Sidebar
          documents={documents}
          isLoading={isLoadingDocuments}
          selectedDocumentName={selectedDocumentName}
          onSelectDocument={handleSelectDocument}
          questionsByDocument={questionsByDocument}
          onSelectRecentQuestion={handleSelectRecentQuestion}
          onViewDetails={setDetailsDocument}
          onAskAboutDocument={handleAskAboutDocument}
          onDeleteDocument={handleDeleteDocument}
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

      {detailsDocument && (
        <DocumentDetailsModal
          document={detailsDocument}
          questionsAskedCount={(questionsByDocument[detailsDocument.documentName] || []).length}
          onClose={() => setDetailsDocument(null)}
          onAskAboutDocument={handleAskAboutDocument}
        />
      )}
    </div>
  );
}
