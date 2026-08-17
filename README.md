# The Oracle's Trial

A simple Java full-stack RAG (Retrieval-Augmented Generation) application. Upload internal company PDFs (HR policies, product manuals, onboarding guides), then ask questions in plain English and get answers backed by citations to the actual source pages.

Built as a learning project for understanding Spring Boot, LangChain4j, embeddings, vector databases, and RAG - so the code favors clarity over cleverness.

## Problem Statement

Companies build up large libraries of internal PDFs over time: HR policies, product manuals, onboarding guides, SOPs. Employees who need one specific fact - "how many sick days do I get?" - end up opening several documents and scanning through pages to find it. That's slow, and it doesn't scale as the document library grows.

## Solution

The Oracle's Trial lets an employee upload those PDFs once, then ask questions directly. Instead of a keyword search that returns whole documents, it finds the *specific paragraphs* that are relevant to the question, and asks an LLM to write a short answer using only those paragraphs - along with the document, page, and chunk the answer came from, so the employee can verify it.

## Features

- Upload a PDF and have it automatically extracted, cleaned, chunked, embedded, and stored.
- Ask a natural-language question and get back the most relevant chunks (`/api/search`).
- Ask a natural-language question and get back a generated answer with citations (`/api/ask`).
- Answers are grounded only in the uploaded documents - if the answer isn't in there, the app says so instead of guessing.
- A small built-in web UI (no separate frontend project, no React).

## Technology Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (Spring WebMVC, Spring Validation) |
| Build tool | Maven |
| PDF parsing | Apache PDFBox |
| Embeddings & LLM orchestration | LangChain4j |
| Vector database | Qdrant |
| UI | Thymeleaf + plain HTML/CSS/JS |
| Boilerplate reduction | Lombok |

## Architecture

The project follows one simple rule everywhere: **Controller calls Service, Service calls an external API or library.** There are no extra layers - no repositories, no factories, no managers - because none are needed at this scale.

```
controller/   HealthController, DocumentController, RagController, PageController
service/      DocumentService, EmbeddingService, RagService
config/       AiConfig, QdrantConfig
model/        DocumentChunk, SearchResult, AskRequest, AskResponse
dto/          UploadResponse
exception/    GlobalExceptionHandler
```

## Document Processing Flow

This is what happens when a PDF is uploaded:

```
PDF file
  -> DocumentController.uploadPdf()      (validates: present, non-empty, is a PDF)
  -> DocumentService.extractText()       (Apache PDFBox reads the raw text)
  -> DocumentService.cleanText()         (collapses messy whitespace/blank lines)
  -> DocumentService.chunkText()         (splits into ~600-word overlapping chunks)
  -> EmbeddingService.save()             (embeds each chunk, stores vector + metadata in Qdrant)
```

Each chunk is stored in Qdrant with its vector plus metadata: the source document name, the chunk number, and the page number (when known). That metadata is what makes citations possible later.

## RAG Flow

This is what happens when a question is asked:

```
Question
  -> RagController.ask()
  -> RagService.ask()
  -> EmbeddingService.embedText()        (turns the question into a vector)
  -> Qdrant similarity search            (cosine similarity, top 5 chunks)
  -> Build context from the top chunks
  -> ChatModel.chat(prompt)              (LLM answers using ONLY that context)
  -> Answer + sources returned to the caller
```

`/api/search` stops after the Qdrant similarity search step and returns the raw chunks. `/api/ask` continues on to the LLM and returns a generated answer plus the sources it was built from.

## What is RAG?

RAG stands for Retrieval-Augmented Generation. A plain LLM only knows what it was trained on, and it has no idea what's inside your company's PDFs. RAG fixes this without retraining anything: before asking the LLM a question, the app first *retrieves* the most relevant pieces of your own documents, and hands them to the LLM as context. The LLM then only has to summarize/answer from that context, instead of relying on (and possibly hallucinating from) its general training data.

## What are Embeddings?

An embedding is a list of numbers (a vector) that represents the *meaning* of a piece of text. Text with similar meaning ends up with similar numbers. This is what makes it possible to search by meaning instead of by exact keyword match - the question "how much time off do I get?" can match a chunk about "annual leave" even though they don't share any words.

## What is Qdrant?

Qdrant is a vector database: a database built specifically to store embeddings and quickly find the ones most similar to a given query vector. Instead of scanning every stored chunk with brute force, Qdrant can search millions of vectors efficiently. This project stores one point per document chunk in Qdrant, using cosine similarity to measure "closeness" between the question and each chunk.

## Why LangChain4j?

LangChain4j provides a single, consistent Java API over different embedding models, chat models, and vector stores. Instead of writing custom HTTP calls for OpenAI and a separate custom client for Qdrant, the app codes against LangChain4j's `EmbeddingModel`, `ChatModel`, and `EmbeddingStore` interfaces - which keeps `AiConfig` and `QdrantConfig` as the only two places that know which specific providers are being used.

## API Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Confirms the API is running |
| POST | `/api/documents/upload` | Uploads a PDF and stores its embeddings |
| POST | `/api/search` | Returns the raw chunks most relevant to a question |
| POST | `/api/ask` | Returns a generated answer with citations |

### GET /api/health

Returns a plain-text confirmation string, no request body.

### POST /api/documents/upload

`multipart/form-data` with a single field `file` (the PDF).

```json
{
  "document": "hr-policy.pdf",
  "chunks": 4,
  "message": "Document processed successfully"
}
```

### POST /api/search

```json
{ "question": "What is the annual leave policy?" }
```

```json
{
  "results": [
    {
      "text": "Employees receive 24 days of paid annual leave...",
      "document": "hr-policy.pdf",
      "chunk": 1,
      "page": 1,
      "score": 0.91
    }
  ]
}
```

### POST /api/ask

```json
{ "question": "How many annual leave days do employees receive?" }
```

```json
{
  "answer": "Employees receive 24 days of annual leave per calendar year.",
  "sources": [
    { "document": "hr-policy.pdf", "page": 1, "chunk": 1 }
  ]
}
```

## Postman Testing

A ready-made collection is included at `postman_collection.json` - import it into Postman and it has all four requests set up, using a `baseUrl` variable (defaults to `http://localhost:8080`).

To test manually instead:

1. `GET {{baseUrl}}/api/health` - expect a 200 with a plain-text message.
2. `POST {{baseUrl}}/api/documents/upload` - body type `form-data`, key `file` of type `File`, value one of the PDFs in `sample-documents/`.
3. `POST {{baseUrl}}/api/search` - raw JSON body `{"question": "..."}`.
4. `POST {{baseUrl}}/api/ask` - raw JSON body `{"question": "..."}`.

## Environment Variables

Copy `.env.example` to `.env` and fill in real values (never commit `.env` - it's already git-ignored).

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `OPENAI_API_KEY` | Yes | - | API key for the OpenAI-compatible embedding/chat provider |
| `OPENAI_EMBEDDING_MODEL` | No | `text-embedding-3-small` | Embedding model name |
| `OPENAI_CHAT_MODEL` | No | `gpt-4o-mini` | Chat model name used to generate answers |
| `QDRANT_URL` | No | `http://localhost:6334` | Qdrant gRPC endpoint (note: port 6334, not the 6333 REST port) |
| `QDRANT_API_KEY` | No | *(empty)* | Required only for Qdrant Cloud |
| `QDRANT_COLLECTION` | No | `oracle_trial_documents` | Name of the Qdrant collection to store chunks in |

The app creates the Qdrant collection automatically on startup if it doesn't already exist, using cosine similarity and a vector size that matches the embedding model - no manual setup step required.

## How to Run

1. Start a Qdrant instance. The quickest way locally is Docker:
   ```
   docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
   ```
2. Copy `.env.example` to `.env` and set your real `OPENAI_API_KEY`.
3. Export the variables from `.env` into your shell (or configure them in your IDE's run configuration), then run:
   ```
   ./mvnw spring-boot:run
   ```
4. Open `http://localhost:8080` in a browser for the UI, or use Postman against the endpoints above.

## Sample Questions

Using the three sample PDFs in `sample-documents/`:

- "What is the annual leave policy?" (hr-policy.pdf)
- "How many sick days do employees get?" (hr-policy.pdf)
- "How many days a week can employees work remotely?" (hr-policy.pdf)
- "What is the warranty period for the product?" (product-manual.pdf)
- "What are the customer support hours?" (product-manual.pdf)
- "When do new employees receive their laptop?" (onboarding-guide.pdf)
- "How long is onboarding training?" (onboarding-guide.pdf)

## Source Citations

Every answer from `/api/ask` includes a `sources` array built directly from the chunks Qdrant actually returned for that question - the document name, page number, and chunk number are read straight off the stored metadata. Nothing is invented: if a citation is missing or wrong, it means the retrieval step didn't have the right chunk, not that the LLM fabricated a source.

## Future Improvements

- Track exact page numbers during PDF extraction (currently chunking doesn't split per page, so `page` can be `null`).
- Let a user delete or re-upload a document without leaving orphaned chunks in Qdrant.
- Show which document is currently uploaded/indexed in the UI, instead of just the last upload's status.
- Support more embedding/chat providers beyond OpenAI-compatible APIs.
- Add pagination or filtering to `/api/search` for larger document libraries.
