# The Oracle's Trial

A beginner-friendly full-stack RAG (Retrieval-Augmented Generation) project.
Upload PDF documents, then ask questions and get answers backed by citations
(document name + page number).

## Tech stack

- **Backend:** Java 17 + Spring Boot 3
- **AI:** LangChain4j + OpenAI (embeddings + chat completions)
- **Vector database:** Qdrant, called directly over its REST API using
  Spring's `RestClient` (no Qdrant Java client library)
- **Frontend:** React
- **PDF text extraction:** Apache PDFBox
- **Config:** OpenAI API key is read from a `.env` file, never committed to Git

## How it works

1. **Upload:** You upload a PDF in the browser. The backend extracts text
   page by page (PDFBox), splits each page into overlapping chunks, and
   asks OpenAI to embed each chunk. Each chunk's vector is stored in Qdrant
   together with metadata: document name, page number, and the chunk text.
2. **Ask:** You type a question. The backend embeds the question with the
   same OpenAI embedding model, sends that vector to Qdrant's `/search`
   endpoint to find the most similar chunks, and builds a context out of
   them.
3. **Answer:** The context + question are sent to OpenAI's chat model,
   which is instructed to answer using only that context. The answer is
   returned to the browser along with the citations (document + page) of
   the chunks that were used. If nothing relevant was found, the app
   replies: *"I couldn't find this information in the provided documents."*

## Project layout

```
oracles-trial/
├── docker-compose.yml        # Runs Qdrant
├── .gitignore
├── backend/                  # Spring Boot API
│   ├── pom.xml
│   ├── .env                  # OPENAI_API_KEY goes here (not committed)
│   └── src/main/java/com/oracle/trial/
│       ├── OracleTrialApplication.java
│       ├── config/CorsConfig.java
│       ├── controller/       # REST endpoints
│       ├── service/          # PDF, chunking, embeddings, Qdrant, answers
│       └── model/            # Request/response records
└── frontend/                 # React app
    └── src/
        ├── App.js
        ├── api.js
        └── components/
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ and npm
- Docker (for Qdrant)
- An OpenAI API key

## Setup & run (3 steps)

### 1. Start Qdrant

```bash
docker-compose up -d
```

This starts Qdrant on `http://localhost:6333`. Verify it's running by
visiting `http://localhost:6333/dashboard` in a browser.

### 2. Start the backend

Add your key to `backend/.env`:

```
OPENAI_API_KEY=sk-your-real-key-here
```

Then run:

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. It automatically creates the
Qdrant collection ("documents") on first startup if it doesn't exist yet.

### 3. Start the frontend

```bash
cd frontend
npm install
npm start
```

The app opens at `http://localhost:3000`.

## Using the app

1. Upload 5 or more PDF files (one at a time or select several at once —
   there is no limit).
2. Type a question about the content of those documents.
3. Read the answer and its source citations (document name + page number).
4. If the documents don't contain the answer, you'll see:
   *"I couldn't find this information in the provided documents."*

## Notes

- `backend/.env` is listed in `.gitignore` — never commit your real API key.
  The committed `.env` only contains a placeholder.
- Chunk size, overlap, and how many chunks are retrieved per question are
  all configurable in `backend/src/main/resources/application.properties`.
- Qdrant data is persisted in a Docker volume (`qdrant_data`), so uploaded
  documents survive a restart of the containers.

## Troubleshooting

**"OPENAI_API_KEY is not set" / placeholder resolution error on startup**
The app looks for `.env` in a few likely locations (`backend/`, the project
root, and one level up) to cover both `mvn spring-boot:run` (working
directory = `backend/`) and running the main class from an IDE (working
directory is often the project root instead). If you still see this error:
- Confirm `backend/.env` exists and contains a real line like
  `OPENAI_API_KEY=sk-...` (not the placeholder value).
- Or simply export it in your shell before starting the app:
  `export OPENAI_API_KEY=sk-...` (macOS/Linux) or set it as an environment
  variable in your IDE's run configuration.

**Connection refused talking to Qdrant**
Make sure `docker-compose up -d` is running and `http://localhost:6333/dashboard`
loads in a browser before starting the backend.
