# Chat With Your Docs

A Java/Spring Boot retrieval-augmented generation (RAG) demo. Upload a PDF or TXT file, retrieve relevant passages, and ask questions grounded in the uploaded content. Ollama runs the embedding and chat models locally; no hosted AI API key is required.

## Features

- Upload text-based PDFs and TXT files (up to 15 MB).
- Extract text, split it into overlapping chunks, and create embeddings.
- Retrieve the four most similar chunks for each question using cosine similarity.
- Generate an answer from the retrieved context and show the source filenames.
- See upload and chat progress, successes, and errors in the UI activity feed.
- Clear the in-memory document index.

## Technology

- Java 17 and Spring Boot 3.4
- Spring MVC, REST, and Thymeleaf
- Apache PDFBox 3 for PDF text extraction
- Ollama: `nomic-embed-text` for embeddings and `qwen3:4b` for chat
- Maven

## Run locally

### Prerequisites

- JDK 17 or newer
- Maven 3.6 or newer
- [Ollama](https://ollama.com/) installed and running

Verify the tools are available in your terminal:

```powershell
java -version
mvn -version
ollama --version
```

Clone the repository and download the models (the model download is needed only once):

```powershell
git clone https://github.com/nirmalsinghkps/chat-with-docs.git
cd chat-with-docs
ollama pull nomic-embed-text
ollama pull qwen3:4b
```

If Ollama is not already running, start it in a separate terminal:

```powershell
ollama serve
```

Start the application from the project directory:

```powershell
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080), upload a PDF or TXT document, and ask a question. Keep both the application and Ollama running. The Ollama URL and model names can be changed in `src/main/resources/application.properties`.

The commands above also work in macOS/Linux terminals if Java, Maven, and Ollama are installed and on `PATH`.

## How it works

```text
Upload PDF/TXT
    -> Extract text (PDFBox for PDFs)
    -> Split into 900-character chunks with 150-character overlap
    -> Create embeddings through Ollama
    -> Keep chunks and vectors in memory

Ask a question
    -> Embed the question through Ollama
    -> Rank chunks by cosine similarity and select the top four
    -> Send the question and retrieved context to the Ollama chat model
    -> Return an answer and source filenames
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/documents` | Upload one PDF or TXT file as multipart form data using the `file` field. |
| `GET` | `/api/status` | Return the number of indexed chunks. |
| `POST` | `/api/chat` | Accept `{"question":"..."}` and return an answer with source filenames. |
| `DELETE` | `/api/documents` | Clear the in-memory index. |

## Design decisions and trade-offs

- **Local model serving:** Ollama avoids a hosted AI API key and keeps model requests on the local machine. The user must install and run Ollama and download the models.
- **Simple chunking:** fixed-size character chunks with overlap are easy to understand and implement. Token-aware, structure-aware chunking could preserve document meaning better.
- **In-memory vector search:** a Java list and cosine similarity keep the demo self-contained. Data is lost when the application restarts, and this approach is not suitable for large collections or multiple application instances.
- **Top-four retrieval:** a small fixed number of chunks keeps the prompt focused. Retrieval quality has not been evaluated against a test dataset.
- **Grounded responses:** the model prompt asks it to use retrieved context, say when the answer is not present, and treat document text as reference rather than instructions. Prompt wording alone cannot eliminate prompt-injection risk.
- **Source filenames:** answers include the filenames of retrieved chunks, but not page numbers or exact quoted passages.

## Validation and logging

Run the Maven verification command:

```powershell
mvn test
```

There are currently no automated test classes, so this command verifies compilation and the Maven build; it does not demonstrate behavioral test coverage. The application logs upload/chat milestones and surfaces errors in the UI, without logging document contents or question text.

## Development reflection

Use these notes to explain the project in your own words.

### Why I built it this way

I wanted to make a small app that shows how document chat works. I used Spring Boot for the app and Ollama to run the AI models on my computer. I split documents into pieces, find the four pieces most related to a question, and give them to the chat model.

I used simple search and memory storage to keep the code easy to understand. This also means uploaded documents are lost when the app stops. Ollama avoids a paid AI API, but users need to install Ollama and download the models.

### What works and what is missing

The code is split into controllers, services, and data classes. The app checks file types and shows errors in the page. It also shows progress when files are uploaded and questions are asked.

There are no automated tests yet. `mvn test` checks that the project builds, but it does not check whether answers are correct. The app also does not save documents after a restart, support scanned PDFs, or have user accounts.

### How I used AI tools

I used GitHub Copilot to help write and improve code. I reviewed its suggestions, checked that they fit the project, and ran the app and Maven build to find problems. I made the final choices about how the app should work.

Before your interview, add one real example of a suggestion you changed or did not use, and explain why. Use your own experience.

### What I would do next

I would add tests for document uploads, search, and errors. Then I would try the app with sample documents and questions to see how good the answers are. If I needed the app to support many users, I would add user accounts and save documents between restarts.

### Screenshots or video

Add screenshots or a short video showing an upload and a question. Do not show private documents or passwords.

## Limitations and next steps

- Scanned PDFs require OCR; only PDF and TXT uploads are supported.
- The index is in memory, shared by all users of the running instance, and cleared on restart.
- The demo has no authentication, authorization, tenant isolation, or persistent document storage; do not use it as a multi-user production service.
- It has no page-level citations, hybrid search, reranker, or RAG evaluation suite.
- Upload embedding requests are sequential; large documents can take time to process.

Possible next steps include adding automated retrieval/API tests, token-aware chunking, persistent vector storage, background ingestion, page-level citations, and authentication before any multi-user deployment.
