/*
 * The Oracle's Trial - UI logic
 *
 * Plain JavaScript, no framework: the page only needs to call two REST
 * endpoints (upload and ask) and show the results. Keeping it framework-free
 * matches the rest of the project's "no React, no separate frontend" rule.
 */

const uploadForm = document.getElementById("upload-form");
const uploadButton = document.getElementById("upload-button");
const uploadStatus = document.getElementById("upload-status");
const fileInput = document.getElementById("file-input");

const askForm = document.getElementById("ask-form");
const askButton = document.getElementById("ask-button");
const questionInput = document.getElementById("question-input");

const answerText = document.getElementById("answer-text");
const sourcesList = document.getElementById("sources-list");

/** Uploads the chosen PDF and reports how many chunks were stored. */
uploadForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const file = fileInput.files[0];
    if (!file) {
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setUploadStatus(`Uploading "${file.name}"...`, null);
    uploadButton.disabled = true;

    try {
        const response = await fetch("/api/documents/upload", {
            method: "POST",
            body: formData
        });
        const body = await response.json();

        if (!response.ok) {
            setUploadStatus(body.error || "Upload failed", "error");
            return;
        }

        setUploadStatus(
            `"${body.document}" processed successfully - ${body.chunks} chunks stored.`,
            "success"
        );
    } catch (err) {
        setUploadStatus("Could not reach the server. Is the app running?", "error");
    } finally {
        uploadButton.disabled = false;
    }
});

/** Asks the Oracle a question and renders the answer plus its sources. */
askForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const question = questionInput.value.trim();
    if (!question) {
        return;
    }

    askButton.disabled = true;
    answerText.textContent = "Thinking...";
    answerText.classList.add("placeholder");
    sourcesList.innerHTML = "";

    try {
        const response = await fetch("/api/ask", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ question })
        });
        const body = await response.json();

        if (!response.ok) {
            showAnswer(body.error || "Something went wrong", []);
            return;
        }

        showAnswer(body.answer, body.sources || []);
    } catch (err) {
        showAnswer("Could not reach the server. Is the app running?", []);
    } finally {
        askButton.disabled = false;
    }
});

function setUploadStatus(message, kind) {
    uploadStatus.textContent = message;
    uploadStatus.className = "status" + (kind ? ` ${kind}` : "");
}

function showAnswer(answer, sources) {
    answerText.textContent = answer;
    answerText.classList.remove("placeholder");

    sourcesList.innerHTML = "";
    if (sources.length === 0) {
        const li = document.createElement("li");
        li.textContent = "No sources for this answer.";
        sourcesList.appendChild(li);
        return;
    }

    sources.forEach((source) => {
        const li = document.createElement("li");
        const page = source.page !== null && source.page !== undefined ? source.page : "-";
        li.innerHTML = `<strong>${source.document}</strong> &middot; Page ${page} &middot; Chunk ${source.chunk}`;
        sourcesList.appendChild(li);
    });
}
