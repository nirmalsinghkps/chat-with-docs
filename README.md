# Chat With Your Docs (Java)

A small full-stack RAG application built with Java 17, Spring Boot, and local Ollama models. Upload PDF/TXT documents, extract and chunk text, create embeddings, retrieve relevant chunks, and ask grounded questions through a browser UI.

## Stack

- Java 17
- Spring Boot 3.4
- Spring MVC / REST
- Thymeleaf-rendered HTML, CSS and browser JavaScript
- Apache PDFBox for PDF text extraction
- Ollama embeddings: `nomic-embed-text`
- Ollama chat: `qwen3:4b`
- Maven

## Prerequisites

- JDK 17 or newer
- Git
- Ollama installed and running locally
- The `nomic-embed-text` and `qwen3:4b` Ollama models

## Run locally (Windows PowerShell)

```powershell
# If Java and Maven are not already on PATH, configure the local installations
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Programs\Apache\Maven\bin;$env:Path"

# Download the models (only needed once)
ollama pull nomic-embed-text
ollama pull qwen3:4b

# Run from the project root
java -version
mvn -version
mvn spring-boot:run
```

Open http://localhost:8080.

Ollama must be running while using the app. For macOS/Linux, install Ollama and the models above, then run `mvn spring-boot:run`.

## GitHub setup

The source code is maintained in [nirmalsinghkps/chat-with-docs](https://github.com/nirmalsinghkps/chat-with-docs). To get a local copy, run:

```bash
git clone https://github.com/nirmalsinghkps/chat-with-docs.git
cd chat-with-docs
```

The application sends embeddings and chat prompts to the local Ollama service; it does not require an OpenAI API key.

## How it works

```text
Browser UI
   |
   v
Spring Boot REST controllers
   |
   +---- POST /api/documents
   |          |
   |          v
   |    PDFBox / TXT extraction
   |          |
   |          v
   |    900-character chunks, 150 overlap
   |          |
   |          v
   |    Ollama embedding API
   |          |
   |          v
   |    In-memory chunk + vector collection
   |
   +---- POST /api/chat
              |
              v
        Embed question
              |
              v
        Cosine similarity, top 4
              |
              v
        Context + grounded prompt
              |
              v
        Ollama chat API
              |
              v
        Answer + source filenames
```

## Key decisions

- **Local AI:** uses Spring `RestClient` to call Ollama, so document and chat requests run against models hosted locally.
- **Chunking:** 900-character chunks with 150-character overlap. This is a simple baseline; token-aware and document-structure-aware chunking would be better for production.
- **Embeddings:** `nomic-embed-text` through Ollama.
- **Vector storage:** in-memory Java list and cosine similarity. Easy to run locally, but not persistent or suitable for multiple application instances.
- **Retrieval:** top-4 chunks by cosine similarity.
- **LLM:** `qwen3:4b` through Ollama.
- **Context and guardrails:** only retrieved chunks are supplied as document context. The prompt tells the model to state when the answer is absent and treat document content as reference data, not instructions.
- **Sources:** returns the filenames of retrieved chunks. These are source-file references, not precise page-level citations.

## Engineering practices in this demo

- Separate controller, service and model packages
- Configuration outside source code
- API key supplied through an environment variable
- `.gitignore` excludes build output and local secrets
- Small methods with focused responsibilities
- Basic upload validation and error handling
- The UI activity feed and application logs report upload/chat progress and failures without logging document contents or questions.

## Limitations

- PDF and TXT only; scanned PDFs need OCR.
- Embeddings and chunks are held in memory and disappear on restart.
- No authentication, authorization or tenant isolation.
- No document-level page citations, reranker or hybrid search.
- No automated RAG evaluation, structured telemetry or CI pipeline yet.
- Uploading many chunks makes sequential embedding calls; production ingestion should batch and run asynchronously.
- Prompt instructions are not a complete defense against prompt injection.

## Production roadmap (AWS example)

1. Containerize the service and deploy on ECS Fargate or EKS.
2. Store source documents in S3.
3. Use SQS and background workers for ingestion.
4. Store vectors in pgvector or OpenSearch.
5. Add PostgreSQL metadata for users, documents, access policies and processing state.
6. Add SSO/OIDC, tenant isolation, encryption, malware scanning and retention controls.
7. Add OCR, token-aware chunking, hybrid retrieval and reranking.
8. Add tests and a golden evaluation dataset for retrieval and answer quality.
9. Add structured logs, traces, latency, token/cost metrics and dashboards.
10. Add CI/CD, container scanning, rate limiting and load testing.

## Before submitting

Run the app yourself, upload a few documents, ask questions with answers and questions not covered by the documents, and capture screenshots. Add your own account of what you tested, what failed, what you fixed, and why you made these trade-offs. Do not claim tests or production controls you have not implemented.
